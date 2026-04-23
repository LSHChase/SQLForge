package com.company.sqloptimization.infrastructure.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
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
        OptimizationGovernanceProperties properties = new OptimizationGovernanceProperties();
        properties.setBaseUrl("http://governance.test/api/governance/internal");
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
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
    void shouldAllowTenantScopeWhenGovernanceApproves() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/tenant-scope/check"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"tenantId\":\"tenant-a\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"targetTenantId\":\"tenant-a\"")))
            .andRespond(withSuccess("{\"allowed\":true,\"reason\":\"ok\"}", MediaType.APPLICATION_JSON));

        client.assertTenantScope("tenant-a");

        server.verify();
    }

    @Test
    void shouldRejectDatasourceAccessWhenGovernanceDenies() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setProtectedRequestContext();
        server.expect(requestTo("http://governance.test/api/governance/internal/datasource-access/check"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"datasourceId\":\"optimization-hive\"")))
            .andRespond(withSuccess(
                "{\"allowed\":false,\"reason\":\"datasource denied\",\"errorCode\":403,"
                    + "\"contractStage\":\"LONG_TERM_BASELINE\",\"implementationStage\":\"REAL\"}",
                MediaType.APPLICATION_JSON
            ));

        AccessDeniedException ex = assertThrows(
            AccessDeniedException.class,
            () -> client.assertDatasourceAccess("tenant-a", DataSourceTypeEnum.HIVE)
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
            () -> client.assertDatasourceAccess("tenant-a", DataSourceTypeEnum.HIVE)
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
        server.expect(requestTo("http://governance.test/api/governance/internal/tenant-scope/check"))
            .andRespond(withServerError());

        BizException ex = assertThrows(BizException.class, () -> client.assertTenantScope("tenant-a"));

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, ex.getCode());
        assertEquals("Governance capability route is unavailable", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldRequireConfiguredBaseUrl() {
        OptimizationGovernanceProperties properties = baseProperties();
        properties.setBaseUrl(" ");
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(BizException.class, () -> client.assertTenantScope("tenant-a"));

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("sql-optimization governance baseUrl is not configured", ex.getMessage());
    }

    @Test
    void shouldExposeGovernanceContractBeans() {
        GovernanceHttpClient.TenantScopeCheckRequest tenantRequest = new GovernanceHttpClient.TenantScopeCheckRequest();
        tenantRequest.setTenantId("tenant-a");
        tenantRequest.setTargetTenantId("tenant-b");
        assertEquals("tenant-a", tenantRequest.getTenantId());
        assertEquals("tenant-b", tenantRequest.getTargetTenantId());

        GovernanceHttpClient.TenantScopeCheckResponse tenantResponse = new GovernanceHttpClient.TenantScopeCheckResponse();
        tenantResponse.setTenantId("tenant-a");
        tenantResponse.setTargetTenantId("tenant-b");
        tenantResponse.setAllowed(true);
        tenantResponse.setReason("ok");
        assertEquals("tenant-a", tenantResponse.getTenantId());
        assertEquals("tenant-b", tenantResponse.getTargetTenantId());
        assertEquals(true, tenantResponse.isAllowed());
        assertEquals("ok", tenantResponse.getReason());

        GovernanceHttpClient.DatasourceAccessCheckRequest datasourceRequest =
            new GovernanceHttpClient.DatasourceAccessCheckRequest();
        datasourceRequest.setTenantId("tenant-a");
        datasourceRequest.setDatasourceId("optimization-hive");
        assertEquals("tenant-a", datasourceRequest.getTenantId());
        assertEquals("optimization-hive", datasourceRequest.getDatasourceId());

        GovernanceHttpClient.DatasourceAccessCheckResponse datasourceResponse =
            new GovernanceHttpClient.DatasourceAccessCheckResponse();
        datasourceResponse.setTenantId("tenant-a");
        datasourceResponse.setDatasourceId("optimization-hive");
        datasourceResponse.setAllowed(false);
        datasourceResponse.setReason("denied");
        datasourceResponse.setErrorCode(Integer.valueOf(403));
        datasourceResponse.setContractStage("LONG_TERM_BASELINE");
        datasourceResponse.setImplementationStage("REAL");
        assertEquals("tenant-a", datasourceResponse.getTenantId());
        assertEquals("optimization-hive", datasourceResponse.getDatasourceId());
        assertEquals(false, datasourceResponse.isAllowed());
        assertEquals("denied", datasourceResponse.getReason());
        assertEquals(Integer.valueOf(403), datasourceResponse.getErrorCode());
        assertEquals("LONG_TERM_BASELINE", datasourceResponse.getContractStage());
        assertEquals("REAL", datasourceResponse.getImplementationStage());

        GovernanceHttpClient.AuditWriteRequest auditWriteRequest = new GovernanceHttpClient.AuditWriteRequest();
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
