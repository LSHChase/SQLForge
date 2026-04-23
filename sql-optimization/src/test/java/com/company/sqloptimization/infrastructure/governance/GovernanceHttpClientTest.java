package com.company.sqloptimization.infrastructure.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqloptimization.config.OptimizationGovernanceProperties;
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
        RequestMetadataContext.set("10.0.0.8", "SQLForge-Test-UA");
        server.expect(requestTo("http://governance.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sourceIp\":\"10.0.0.8\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"userAgent\":\"SQLForge-Test-UA\"")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.writeAudit(new OptimizationAuditRecord(
            "OPTIMIZATION_TASK_SUBMIT",
            "OPTIMIZATION_TASK",
            "task-001",
            "QUEUED",
            12L,
            "{\"tenantId\":\"tenant-a\"}",
            "{\"resultStatus\":\"QUEUED\"}"
        ));

        server.verify();
    }

    @Test
    void shouldAllowAuthorizationWhenGovernanceApproves() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/authorization/decide"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"serviceCode\":\"SQL_OPTIMIZATION\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceType\":\"SQL_OPTIMIZATION_TASK\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceId\":\"task-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"operationCode\":\"OPTIMIZATION_TASK_SUBMIT\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"datasourceId\":\"optimization-hive\"")))
            .andRespond(withSuccess("{\"allowed\":true,\"reason\":\"ok\"}", MediaType.APPLICATION_JSON));

        client.assertAuthorization(
            "tenant-a",
            DataSourceTypeEnum.HIVE,
            "SQL_OPTIMIZATION_TASK",
            "task-001",
            "OPTIMIZATION_TASK_SUBMIT"
        );

        server.verify();
    }

    @Test
    void shouldRejectAuthorizationWhenGovernanceDenies() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/authorization/decide"))
            .andRespond(withSuccess(
                "{\"allowed\":false,\"reason\":\"datasource denied\",\"errorCode\":403,"
                    + "\"contractStage\":\"LONG_TERM_BASELINE\",\"implementationStage\":\"AUTHORIZATION_MATRIX_BASELINE\"}",
                MediaType.APPLICATION_JSON
            ));

        AccessDeniedException ex = assertThrows(
            AccessDeniedException.class,
            () -> client.assertAuthorization(
                "tenant-a",
                DataSourceTypeEnum.HIVE,
                "SQL_OPTIMIZATION_TASK",
                "task-001",
                "OPTIMIZATION_TASK_SUBMIT"
            )
        );

        assertEquals("datasource denied", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldFailFastWhenDatasourceMappingIsMissing() {
        OptimizationGovernanceProperties properties = baseProperties();
        properties.getDatasourceIdMap().clear();
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertAuthorization(
                "tenant-a",
                DataSourceTypeEnum.HIVE,
                "SQL_OPTIMIZATION_TASK",
                "task-001",
                "OPTIMIZATION_TASK_SUBMIT"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("Datasource mapping is missing for HIVE", ex.getMessage());
    }

    @Test
    void shouldWrapRestClientFailureAsBizException() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/authorization/decide"))
            .andRespond(withServerError());

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertAuthorization(
                "tenant-a",
                DataSourceTypeEnum.HIVE,
                "SQL_OPTIMIZATION_TASK",
                "task-001",
                "OPTIMIZATION_TASK_SUBMIT"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, ex.getCode());
        assertEquals("Governance capability route is unavailable", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldRequireConfiguredBaseUrl() {
        OptimizationGovernanceProperties properties = baseProperties();
        properties.setBaseUrl(" ");
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertAuthorization(
                "tenant-a",
                DataSourceTypeEnum.HIVE,
                "SQL_OPTIMIZATION_TASK",
                "task-001",
                "OPTIMIZATION_TASK_SUBMIT"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("sql-optimization governance baseUrl is not configured", ex.getMessage());
    }

    @Test
    void shouldExposeGovernanceContractBeans() {
        GovernanceAuthorizationDecisionRequest authorizationRequest = new GovernanceAuthorizationDecisionRequest();
        authorizationRequest.setServiceCode("SQL_OPTIMIZATION");
        authorizationRequest.setTenantId("tenant-a");
        authorizationRequest.setResourceType("SQL_OPTIMIZATION_TASK");
        authorizationRequest.setResourceId("task-001");
        authorizationRequest.setOperationCode("OPTIMIZATION_TASK_SUBMIT");
        authorizationRequest.setDatasourceId("optimization-hive");
        assertEquals("SQL_OPTIMIZATION", authorizationRequest.getServiceCode());
        assertEquals("tenant-a", authorizationRequest.getTenantId());
        assertEquals("SQL_OPTIMIZATION_TASK", authorizationRequest.getResourceType());
        assertEquals("task-001", authorizationRequest.getResourceId());
        assertEquals("OPTIMIZATION_TASK_SUBMIT", authorizationRequest.getOperationCode());
        assertEquals("optimization-hive", authorizationRequest.getDatasourceId());

        GovernanceAuthorizationDecisionResponse authorizationResponse = new GovernanceAuthorizationDecisionResponse();
        authorizationResponse.setTenantId("tenant-a");
        authorizationResponse.setResourceType("SQL_OPTIMIZATION_TASK");
        authorizationResponse.setResourceId("task-001");
        authorizationResponse.setOperationCode("OPTIMIZATION_TASK_SUBMIT");
        authorizationResponse.setDatasourceId("optimization-hive");
        authorizationResponse.setAllowed(false);
        authorizationResponse.setReason("denied");
        authorizationResponse.setErrorCode(Integer.valueOf(403));
        authorizationResponse.setContractStage("LONG_TERM_BASELINE");
        authorizationResponse.setImplementationStage("AUTHORIZATION_MATRIX_BASELINE");
        assertEquals("tenant-a", authorizationResponse.getTenantId());
        assertEquals("SQL_OPTIMIZATION_TASK", authorizationResponse.getResourceType());
        assertEquals("task-001", authorizationResponse.getResourceId());
        assertEquals("OPTIMIZATION_TASK_SUBMIT", authorizationResponse.getOperationCode());
        assertEquals("optimization-hive", authorizationResponse.getDatasourceId());
        assertEquals(false, authorizationResponse.isAllowed());
        assertEquals("denied", authorizationResponse.getReason());
        assertEquals(Integer.valueOf(403), authorizationResponse.getErrorCode());
        assertEquals("LONG_TERM_BASELINE", authorizationResponse.getContractStage());
        assertEquals("AUTHORIZATION_MATRIX_BASELINE", authorizationResponse.getImplementationStage());

        GovernanceAuditWriteRequest auditWriteRequest = new GovernanceAuditWriteRequest();
        auditWriteRequest.setServiceCode("SQL_OPTIMIZATION");
        auditWriteRequest.setOperationCode("OPTIMIZATION_TASK_SUBMIT");
        auditWriteRequest.setResourceType("OPTIMIZATION_TASK");
        auditWriteRequest.setResourceId("task-001");
        auditWriteRequest.setResultStatus("QUEUED");
        auditWriteRequest.setElapsedMs(Long.valueOf(12));
        auditWriteRequest.setSourceIp("10.0.0.8");
        auditWriteRequest.setUserAgent("SQLForge-Test-UA");
        auditWriteRequest.setRequestParams("{\"tenantId\":\"tenant-a\"}");
        auditWriteRequest.setResponseSummary("{\"resultStatus\":\"QUEUED\"}");
        assertEquals("SQL_OPTIMIZATION", auditWriteRequest.getServiceCode());
        assertEquals("OPTIMIZATION_TASK_SUBMIT", auditWriteRequest.getOperationCode());
        assertEquals("OPTIMIZATION_TASK", auditWriteRequest.getResourceType());
        assertEquals("task-001", auditWriteRequest.getResourceId());
        assertEquals("QUEUED", auditWriteRequest.getResultStatus());
        assertEquals(Long.valueOf(12), auditWriteRequest.getElapsedMs());
        assertEquals("10.0.0.8", auditWriteRequest.getSourceIp());
        assertEquals("SQLForge-Test-UA", auditWriteRequest.getUserAgent());
        assertEquals("{\"tenantId\":\"tenant-a\"}", auditWriteRequest.getRequestParams());
        assertEquals("{\"resultStatus\":\"QUEUED\"}", auditWriteRequest.getResponseSummary());
    }

    private GovernanceHttpClient createClient() {
        return new GovernanceHttpClient(new RestTemplateBuilder(), baseProperties());
    }

    private OptimizationGovernanceProperties baseProperties() {
        OptimizationGovernanceProperties properties = new OptimizationGovernanceProperties();
        properties.setBaseUrl("http://governance.test/api/governance/internal");
        return properties;
    }

    private void setProtectedRequestContext() {
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
    }
}
