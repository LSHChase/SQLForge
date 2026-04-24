package com.company.benchmarkengine.infrastructure.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.benchmarkengine.config.BenchmarkEngineGovernanceProperties;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;
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
        RequestMetadataContext.set("10.0.0.9", "SQLForge-Benchmark-Test-UA");
        server.expect(requestTo("http://governance.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sourceIp\":\"10.0.0.9\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"userAgent\":\"SQLForge-Benchmark-Test-UA\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"configSnapshotId\":\"cfg-benchmark-report-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resultId\":\"result-benchmark-report-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"historyId\":\"history-benchmark-report-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"exportId\":\"export-benchmark-report-001-pdf-export\"")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.writeAudit(new BenchmarkAuditRecord(
            "BENCHMARK_TASK_SUBMIT",
            "BENCHMARK_TASK",
            "task-001",
            "QUEUED",
            16L,
            "benchmark-report-task-001",
            "cfg-benchmark-report-001",
            "result-benchmark-report-001",
            "history-benchmark-report-001",
            "export-benchmark-report-001-pdf-export",
            "{\"tenantId\":\"tenant-a\"}",
            "{\"resultStatus\":\"QUEUED\"}"
        ));

        server.verify();
    }

    @Test
    void shouldResolveTenantArtifactPolicyWithSyntheticProtectedHeadersWhenRequestContextIsMissing() {
        GovernanceHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://governance.test/api/governance/internal/tenant-artifact-policy/resolve"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"tenantId\":\"tenant-a\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"policyScope\":\"BENCHMARK_ARTIFACT\"")))
            .andRespond(withSuccess(
                "{\"tenantId\":\"tenant-a\",\"retentionDays\":180,"
                    + "\"retentionPolicySource\":\"GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS\","
                    + "\"retentionPolicyStatus\":\"TENANT_RETENTION_ACTIVE\"}",
                MediaType.APPLICATION_JSON
            ));

        GovernanceTenantArtifactPolicyResponse response =
            client.resolveTenantArtifactPolicy("tenant-a", "BENCHMARK_ARTIFACT");

        assertEquals("tenant-a", response.getTenantId());
        assertEquals(Integer.valueOf(180), response.getRetentionDays());
        assertEquals("GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS", response.getRetentionPolicySource());
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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"serviceCode\":\"BENCHMARK_ENGINE\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceType\":\"BENCHMARK_ENGINE_TASK\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resourceId\":\"task-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"operationCode\":\"BENCHMARK_TASK_SUBMIT\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"datasourceId\":\"benchmark-hetu\"")))
            .andRespond(withSuccess("{\"allowed\":true,\"reason\":\"ok\"}", MediaType.APPLICATION_JSON));

        client.assertAuthorization(
            "tenant-a",
            DataSourceTypeEnum.HETU,
            "BENCHMARK_ENGINE_TASK",
            "task-001",
            "BENCHMARK_TASK_SUBMIT"
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
                DataSourceTypeEnum.HETU,
                "BENCHMARK_ENGINE_TASK",
                "task-001",
                "BENCHMARK_TASK_SUBMIT"
            )
        );

        assertEquals("datasource denied", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldFailFastWhenDatasourceMappingIsMissing() {
        BenchmarkEngineGovernanceProperties properties = baseProperties();
        properties.getDatasourceIdMap().clear();
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertAuthorization(
                "tenant-a",
                DataSourceTypeEnum.HIVE,
                "BENCHMARK_ENGINE_TASK",
                "task-001",
                "BENCHMARK_TASK_SUBMIT"
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
                DataSourceTypeEnum.HETU,
                "BENCHMARK_ENGINE_TASK",
                "task-001",
                "BENCHMARK_TASK_SUBMIT"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, ex.getCode());
        assertEquals("Governance capability route is unavailable", ex.getMessage());
        server.verify();
    }

    @Test
    void shouldRequireConfiguredBaseUrl() {
        BenchmarkEngineGovernanceProperties properties = baseProperties();
        properties.setBaseUrl(" ");
        GovernanceHttpClient client = new GovernanceHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(
            BizException.class,
            () -> client.assertAuthorization(
                "tenant-a",
                DataSourceTypeEnum.HETU,
                "BENCHMARK_ENGINE_TASK",
                "task-001",
                "BENCHMARK_TASK_SUBMIT"
            )
        );

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("benchmark-engine governance baseUrl is not configured", ex.getMessage());
    }

    @Test
    void shouldExposeGovernanceContractBeans() {
        GovernanceAuthorizationDecisionRequest authorizationRequest = new GovernanceAuthorizationDecisionRequest();
        authorizationRequest.setServiceCode("BENCHMARK_ENGINE");
        authorizationRequest.setTenantId("tenant-a");
        authorizationRequest.setResourceType("BENCHMARK_ENGINE_TASK");
        authorizationRequest.setResourceId("task-001");
        authorizationRequest.setOperationCode("BENCHMARK_TASK_SUBMIT");
        authorizationRequest.setDatasourceId("benchmark-hetu");
        assertEquals("BENCHMARK_ENGINE", authorizationRequest.getServiceCode());
        assertEquals("tenant-a", authorizationRequest.getTenantId());
        assertEquals("BENCHMARK_ENGINE_TASK", authorizationRequest.getResourceType());
        assertEquals("task-001", authorizationRequest.getResourceId());
        assertEquals("BENCHMARK_TASK_SUBMIT", authorizationRequest.getOperationCode());
        assertEquals("benchmark-hetu", authorizationRequest.getDatasourceId());

        GovernanceAuthorizationDecisionResponse authorizationResponse = new GovernanceAuthorizationDecisionResponse();
        authorizationResponse.setTenantId("tenant-a");
        authorizationResponse.setResourceType("BENCHMARK_ENGINE_TASK");
        authorizationResponse.setResourceId("task-001");
        authorizationResponse.setOperationCode("BENCHMARK_TASK_SUBMIT");
        authorizationResponse.setDatasourceId("benchmark-hetu");
        authorizationResponse.setAllowed(false);
        authorizationResponse.setReason("denied");
        authorizationResponse.setErrorCode(Integer.valueOf(403));
        authorizationResponse.setContractStage("LONG_TERM_BASELINE");
        authorizationResponse.setImplementationStage("AUTHORIZATION_MATRIX_BASELINE");
        assertEquals("tenant-a", authorizationResponse.getTenantId());
        assertEquals("BENCHMARK_ENGINE_TASK", authorizationResponse.getResourceType());
        assertEquals("task-001", authorizationResponse.getResourceId());
        assertEquals("BENCHMARK_TASK_SUBMIT", authorizationResponse.getOperationCode());
        assertEquals("benchmark-hetu", authorizationResponse.getDatasourceId());
        assertEquals(false, authorizationResponse.isAllowed());
        assertEquals("denied", authorizationResponse.getReason());
        assertEquals(Integer.valueOf(403), authorizationResponse.getErrorCode());
        assertEquals("LONG_TERM_BASELINE", authorizationResponse.getContractStage());
        assertEquals("AUTHORIZATION_MATRIX_BASELINE", authorizationResponse.getImplementationStage());

        GovernanceAuditWriteRequest auditWriteRequest = new GovernanceAuditWriteRequest();
        auditWriteRequest.setServiceCode("BENCHMARK_ENGINE");
        auditWriteRequest.setOperationCode("BENCHMARK_TASK_SUBMIT");
        auditWriteRequest.setResourceType("BENCHMARK_TASK");
        auditWriteRequest.setResourceId("task-001");
        auditWriteRequest.setResultStatus("QUEUED");
        auditWriteRequest.setElapsedMs(Long.valueOf(16));
        auditWriteRequest.setSourceIp("10.0.0.9");
        auditWriteRequest.setUserAgent("SQLForge-Benchmark-Test-UA");
        auditWriteRequest.setRequestParams("{\"tenantId\":\"tenant-a\"}");
        auditWriteRequest.setResponseSummary("{\"resultStatus\":\"QUEUED\"}");
        assertEquals("BENCHMARK_ENGINE", auditWriteRequest.getServiceCode());
        assertEquals("BENCHMARK_TASK_SUBMIT", auditWriteRequest.getOperationCode());
        assertEquals("BENCHMARK_TASK", auditWriteRequest.getResourceType());
        assertEquals("task-001", auditWriteRequest.getResourceId());
        assertEquals("QUEUED", auditWriteRequest.getResultStatus());
        assertEquals(Long.valueOf(16), auditWriteRequest.getElapsedMs());
        assertEquals("10.0.0.9", auditWriteRequest.getSourceIp());
        assertEquals("SQLForge-Benchmark-Test-UA", auditWriteRequest.getUserAgent());
        assertEquals("{\"tenantId\":\"tenant-a\"}", auditWriteRequest.getRequestParams());
        assertEquals("{\"resultStatus\":\"QUEUED\"}", auditWriteRequest.getResponseSummary());
    }

    private GovernanceHttpClient createClient() {
        return new GovernanceHttpClient(new RestTemplateBuilder(), baseProperties());
    }

    private BenchmarkEngineGovernanceProperties baseProperties() {
        BenchmarkEngineGovernanceProperties properties = new BenchmarkEngineGovernanceProperties();
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
