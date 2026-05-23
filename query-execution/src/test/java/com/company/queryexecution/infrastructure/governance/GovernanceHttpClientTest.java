package com.company.queryexecution.infrastructure.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.queryexecution.config.QueryExecutionGovernanceProperties;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GovernanceHttpClientTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
        RequestMetadataContext.clear();
    }

    @Test
    void shouldWriteAuditWithRealRequestMetadata() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        RequestMetadataContext.set("10.0.0.7", "SQLForge-Query-Test-UA", "api");
        server.expect(requestTo("http://governance.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"accessChannel\":\"API\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sourceIp\":\"10.0.0.7\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"userAgent\":\"SQLForge-Query-Test-UA\"")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.writeAudit(new QueryExecutionAuditRecord(
            "QUERY_EXECUTE_SYNC",
            "QUERY",
            "query-001",
            "SUCCESS",
            11L,
            "{\"tenantId\":\"tenant-a\"}",
            "{\"resultStatus\":\"SUCCESS\"}"
        ));

        server.verify();
    }

    @Test
    void shouldWriteQueryExecutionHistoryToGovernanceRoute() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        GovernanceQueryExecutionHistoryWriteRequest request = new GovernanceQueryExecutionHistoryWriteRequest();
        request.setTenantId("tenant-a");
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-001");
        request.setDatasourceType("HETU");
        request.setHistoryType("QUERY_EXECUTION");
        request.setResultStatus("SUCCESS");
        request.setTargetEngine("HETU");
        request.setReturnedRowCount(Long.valueOf(1L));
        request.setCacheHit(Boolean.FALSE);
        server.expect(requestTo("http://governance.test/api/governance/internal/query-execution-history/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"historyType\":\"QUERY_EXECUTION\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resultStatus\":\"SUCCESS\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"targetEngine\":\"HETU\"")))
            .andRespond(withSuccess(
                "{\"resultId\":\"result-qe-001\",\"historyId\":\"history-qe-001\"}",
                MediaType.APPLICATION_JSON
            ));

        GovernanceQueryExecutionHistoryWriteResponse response = client.writeQueryExecutionHistory(request);

        assertEquals("history-qe-001", response.getHistoryId());
        server.verify();
    }

    @Test
    void shouldResolveJdbcDatasourceViaGovernanceRoute() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/datasources/jdbc/resolve"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"datasourceCode\":\"hetu_main\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"engineType\":\"HETU\"")))
            .andRespond(withSuccess(
                "{\"resolved\":true,\"tenantId\":\"tenant-a\",\"datasourceCode\":\"hetu_main\","
                    + "\"engineType\":\"HETU\",\"jdbcUrl\":\"jdbc:hetu://coordinator:8080/hive/default\","
                    + "\"username\":\"hetu_user\",\"password\":\"secret\"}",
                MediaType.APPLICATION_JSON
            ));
        GovernanceJdbcDatasourceResolveRequest request = new GovernanceJdbcDatasourceResolveRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setEngineType("HETU");

        GovernanceJdbcDatasourceResolveResponse response = client.resolveJdbcDatasource(request);

        assertEquals(true, response.isResolved());
        assertEquals("jdbc:hetu://coordinator:8080/hive/default", response.getJdbcUrl());
        assertEquals("hetu_user", response.getUsername());
        server.verify();
    }

    @Test
    void shouldAllowAuthorizationWhenGovernanceApproves() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/datasource-access/check"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"serviceCode\":\"QUERY_EXECUTION\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceType\":\"QUERY_EXECUTION_QUERY\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceId\":\"query-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"operationCode\":\"QUERY_EXECUTE_SYNC\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"datasourceId\":\"query-hetu\"")))
            .andRespond(withSuccess("{\"allowed\":true,\"reason\":\"ok\"}", MediaType.APPLICATION_JSON));

        client.assertDatasourceAccess(
            "tenant-a",
            DataSourceTypeEnum.HETU,
            "QUERY_EXECUTION_QUERY",
            "query-001",
            "QUERY_EXECUTE_SYNC"
        );

        server.verify();
    }

    @Test
    void shouldRejectAuthorizationWhenGovernanceDenies() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/datasource-access/check"))
            .andRespond(withSuccess(
                "{\"allowed\":false,\"reason\":\"datasource denied\",\"errorCode\":403,"
                    + "\"contractStage\":\"LONG_TERM_BASELINE\",\"implementationStage\":\"DATASOURCE_ACCESS_SCOPE_BASELINE\"}",
                MediaType.APPLICATION_JSON
            ));

        AccessDeniedException ex = assertThrows(
            AccessDeniedException.class,
            () -> client.assertDatasourceAccess(
                "tenant-a",
                null,
                "QUERY_EXECUTION_QUERY",
                "query-001",
                "QUERY_EXECUTE_SYNC"
            )
        );

        assertEquals("datasource denied", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldFailFastWhenDatasourceMappingIsMissing() {
        QueryExecutionGovernanceProperties properties = baseProperties();
        properties.getDatasourceIdMap().clear();
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertDatasourceAccess(
                "tenant-a",
                DataSourceTypeEnum.HIVE,
                "QUERY_EXECUTION_QUERY",
                "query-001",
                "QUERY_EXECUTE_SYNC"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("缺少数据源映射：HIVE", ex.getMessage());
    }

    @Test
    void shouldWrapRestClientFailureAsBizException() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/datasource-access/check"))
            .andRespond(withServerError());

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertDatasourceAccess(
                "tenant-a",
                DataSourceTypeEnum.HETU,
                "QUERY_EXECUTION_QUERY",
                "query-001",
                "QUERY_EXECUTE_SYNC"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, ex.getCode());
        assertEquals("治理能力路由不可用", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldRequireConfiguredBaseUrl() {
        QueryExecutionGovernanceProperties properties = baseProperties();
        properties.setBaseUrl(" ");
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertDatasourceAccess(
                "tenant-a",
                DataSourceTypeEnum.HETU,
                "QUERY_EXECUTION_QUERY",
                "query-001",
                "QUERY_EXECUTE_SYNC"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("query-execution governance baseUrl 未配置", ex.getMessage());
    }

    @Test
    void shouldExposeGovernanceContractBeans() {
        GovernanceDatasourceAccessCheckRequest authorizationRequest = new GovernanceDatasourceAccessCheckRequest();
        authorizationRequest.setServiceCode("QUERY_EXECUTION");
        authorizationRequest.setTenantId("tenant-a");
        authorizationRequest.setResourceType("QUERY_EXECUTION_QUERY");
        authorizationRequest.setResourceId("query-001");
        authorizationRequest.setOperationCode("QUERY_EXECUTE_SYNC");
        authorizationRequest.setDatasourceId("query-hetu");
        assertEquals("QUERY_EXECUTION", authorizationRequest.getServiceCode());
        assertEquals("tenant-a", authorizationRequest.getTenantId());
        assertEquals("QUERY_EXECUTION_QUERY", authorizationRequest.getResourceType());
        assertEquals("query-001", authorizationRequest.getResourceId());
        assertEquals("QUERY_EXECUTE_SYNC", authorizationRequest.getOperationCode());
        assertEquals("query-hetu", authorizationRequest.getDatasourceId());

        GovernanceDatasourceAccessCheckResponse authorizationResponse = new GovernanceDatasourceAccessCheckResponse();
        authorizationResponse.setTenantId("tenant-a");
        authorizationResponse.setResourceType("QUERY_EXECUTION_QUERY");
        authorizationResponse.setResourceId("query-001");
        authorizationResponse.setOperationCode("QUERY_EXECUTE_SYNC");
        authorizationResponse.setDatasourceId("query-hetu");
        authorizationResponse.setAllowed(false);
        authorizationResponse.setReason("denied");
        authorizationResponse.setErrorCode(Integer.valueOf(403));
        authorizationResponse.setContractStage("LONG_TERM_BASELINE");
        authorizationResponse.setImplementationStage("DATASOURCE_ACCESS_SCOPE_BASELINE");
        assertEquals("tenant-a", authorizationResponse.getTenantId());
        assertEquals("QUERY_EXECUTION_QUERY", authorizationResponse.getResourceType());
        assertEquals("query-001", authorizationResponse.getResourceId());
        assertEquals("QUERY_EXECUTE_SYNC", authorizationResponse.getOperationCode());
        assertEquals("query-hetu", authorizationResponse.getDatasourceId());
        assertEquals(false, authorizationResponse.isAllowed());
        assertEquals("denied", authorizationResponse.getReason());
        assertEquals(Integer.valueOf(403), authorizationResponse.getErrorCode());
        assertEquals("LONG_TERM_BASELINE", authorizationResponse.getContractStage());
        assertEquals("DATASOURCE_ACCESS_SCOPE_BASELINE", authorizationResponse.getImplementationStage());

        GovernanceAuditWriteRequest auditWriteRequest = new GovernanceAuditWriteRequest();
        auditWriteRequest.setServiceCode("QUERY_EXECUTION");
        auditWriteRequest.setOperationCode("QUERY_EXECUTE_SYNC");
        auditWriteRequest.setResourceType("QUERY");
        auditWriteRequest.setResourceId("query-001");
        auditWriteRequest.setResultStatus("SUCCESS");
        auditWriteRequest.setElapsedMs(Long.valueOf(11));
        auditWriteRequest.setSourceIp("10.0.0.7");
        auditWriteRequest.setUserAgent("SQLForge-Query-Test-UA");
        auditWriteRequest.setRequestParams("{\"tenantId\":\"tenant-a\"}");
        auditWriteRequest.setResponseSummary("{\"resultStatus\":\"SUCCESS\"}");
        assertEquals("QUERY_EXECUTION", auditWriteRequest.getServiceCode());
        assertEquals("QUERY_EXECUTE_SYNC", auditWriteRequest.getOperationCode());
        assertEquals("QUERY", auditWriteRequest.getResourceType());
        assertEquals("query-001", auditWriteRequest.getResourceId());
        assertEquals("SUCCESS", auditWriteRequest.getResultStatus());
        assertEquals(Long.valueOf(11), auditWriteRequest.getElapsedMs());
        assertEquals("10.0.0.7", auditWriteRequest.getSourceIp());
        assertEquals("SQLForge-Query-Test-UA", auditWriteRequest.getUserAgent());
        assertEquals("{\"tenantId\":\"tenant-a\"}", auditWriteRequest.getRequestParams());
        assertEquals("{\"resultStatus\":\"SUCCESS\"}", auditWriteRequest.getResponseSummary());
    }

    private GovernanceHttpClient createClient() {
        return new GovernanceHttpClient(new RestTemplateBuilder(), baseProperties());
    }

    private QueryExecutionGovernanceProperties baseProperties() {
        QueryExecutionGovernanceProperties properties = new QueryExecutionGovernanceProperties();
        properties.setBaseUrl("http://governance.test/api/governance/internal");
        return properties;
    }

    private void setProtectedRequestContext() {
        RequestContext.set(
            "tenant-a",
            "user-001",
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
    }
}
