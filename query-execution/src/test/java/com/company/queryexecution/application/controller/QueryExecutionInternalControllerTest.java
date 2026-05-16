package com.company.queryexecution.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.queryexecution.application.interceptor.AuthInterceptor;
import com.company.queryexecution.application.service.HetuRouteCalibrationService;
import com.company.queryexecution.application.service.QueryExecutionAccelerationRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionBenchmarkWorkloadService;
import com.company.queryexecution.application.service.QueryExecutionCacheGovernanceRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionResultDigestService;
import com.company.queryexecution.application.service.QueryExecutionRuntimeRewriteBindingService;
import com.company.queryexecution.config.AuthProperties;
import com.company.queryexecution.config.WebMvcConfig;
import com.company.queryexecution.domain.query.HetuClusterEvidenceSnapshot;
import com.company.queryexecution.domain.query.HetuRouteCalibrationModeSnapshot;
import com.company.queryexecution.domain.query.HetuRouteCalibrationSnapshot;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadEngineSnapshot;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QueryExecutionInternalController.class)
@Import({
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    AuthInterceptor.class,
    QueryExecutionInternalControllerTest.TestConfig.class
})
class QueryExecutionInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService;

    @MockBean
    private QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService;

    @MockBean
    private QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService;

    @MockBean
    private HetuRouteCalibrationService hetuRouteCalibrationService;

    @MockBean
    private QueryExecutionResultDigestService queryExecutionResultDigestService;

    @MockBean
    private QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService;

    @Test
    void shouldReturnBenchmarkWorkloadSnapshot() throws Exception {
        QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot = new QueryExecutionBenchmarkWorkloadEngineSnapshot();
        snapshot.setTargetEngine(com.company.sqlforge.common.constants.DataSourceTypeEnum.HETU);
        snapshot.setWorkloadSource("QUERY_EXECUTION_SYNC");
        snapshot.setExecutionMode("CLIENT");
        snapshot.setElapsedMs(Long.valueOf(42));
        QueryExecutionBenchmarkWorkloadResponse response = new QueryExecutionBenchmarkWorkloadResponse();
        response.setTenantId("tenant-a");
        response.setBenchmarkTaskId("benchmark-task-001");
        response.setSqlFingerprint("fp-001");
        response.setWorkloadDigest("digest-001");
        response.setWorkloadSource("QUERY_EXECUTION_SYNC");
        response.setBackfillApplied(false);
        response.setEngineSnapshots(Collections.singletonList(snapshot));
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE");
        when(queryExecutionBenchmarkWorkloadService.capture(any())).thenReturn(response);

        mockMvc.perform(post("/api/query-execution/internal/benchmark/workload/capture")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-001")
                .header("X-Trace-Id", "trace-001")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"benchmarkTaskId\":\"benchmark-task-001\",\"sqlText\":\"SELECT 1\"}"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.workloadSource").value("QUERY_EXECUTION_SYNC"))
            .andExpect(jsonPath("$.engineSnapshots[0].targetEngine").value("HETU"))
            .andExpect(jsonPath("$.implementationStage").value("BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE"));

        verify(queryExecutionBenchmarkWorkloadService).capture(any());
    }

    @Test
    void shouldReturnHetuRouteCalibrationSnapshot() throws Exception {
        HetuClusterEvidenceSnapshot clusterEvidence = new HetuClusterEvidenceSnapshot(
            "REPO_CLOSED_CONFIGURATION",
            "repo-default",
            "UNSPECIFIED",
            "",
            "docs/deployments/hetu-test-environment-deployment-runbook.md",
            "HARN-016/INBOX-002",
            "REPO_CLOSED_DEFAULT",
            "PENDING_ENV_WINDOW",
            ""
        );
        HetuRouteCalibrationModeSnapshot clientMode = new HetuRouteCalibrationModeSnapshot(
            QueryExecutionAccessMode.CLIENT,
            1,
            true,
            true,
            true,
            true,
            true,
            true,
            "READY",
            "模式已准备好用于校准路由。",
            Collections.<String, Object>singletonMap("clientEnabled", Boolean.TRUE)
        );
        when(hetuRouteCalibrationService.currentSnapshot()).thenReturn(
            new HetuRouteCalibrationSnapshot(
                true,
                "REPO_CLOSED_BASELINE",
                Arrays.asList(QueryExecutionAccessMode.JDBC, QueryExecutionAccessMode.CLIENT),
                Arrays.asList(QueryExecutionAccessMode.CLIENT, QueryExecutionAccessMode.JDBC),
                true,
                "REPO_CLOSED_CONFIGURATION",
                "PENDING_ENV_WINDOW",
                "REPO_CLOSED_DEFAULT",
                "summary",
                clusterEvidence,
                Collections.singletonList(clientMode)
            )
        );

        mockMvc.perform(get("/api/query-execution/internal/hetu/route-calibration")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-003")
                .header("X-Trace-Id", "trace-003")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.routeProfile").value("REPO_CLOSED_BASELINE"))
            .andExpect(jsonPath("$.effectiveRouteOrder[0]").value("CLIENT"))
            .andExpect(jsonPath("$.clusterEvidence.evidenceSource").value("REPO_CLOSED_CONFIGURATION"))
            .andExpect(jsonPath("$.modeCalibrations[0].mode").value("CLIENT"))
            .andExpect(jsonPath("$.implementationStage").value("HETU_ROUTE_CALIBRATION_BASELINE"));

        verify(hetuRouteCalibrationService).currentSnapshot();
    }

    @Test
    void shouldExecuteReadonlyResultDigestThroughInternalEndpoint() throws Exception {
        QueryExecutionResultDigestResponse response = new QueryExecutionResultDigestResponse();
        response.setTenantId("tenant-a");
        response.setValidationRunId("validation-001");
        response.setRewriteRecordId("rewrite-001");
        response.setSqlFingerprint("fp-001");
        response.setStatus("SUCCESS");
        response.setTargetEngine("HETU");
        response.setResultDigest(Collections.<String, Object>singletonMap("checksumDigest", "checksum-001"));
        response.setLimitedSample(Collections.<java.util.Map<String, Object>>emptyList());
        response.setExecutionEvidence(Collections.<String, Object>singletonMap("readonlyDigestOnly", Boolean.TRUE));
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("READONLY_RESULT_DIGEST_BASELINE");
        when(queryExecutionResultDigestService.executeDigest(any())).thenReturn(response);

        mockMvc.perform(post("/api/query-execution/internal/result-digests/execute")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-005")
                .header("X-Trace-Id", "trace-005")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"validationRunId\":\"validation-001\","
                    + "\"rewriteRecordId\":\"rewrite-001\",\"sqlText\":\"SELECT 1\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.resultDigest.checksumDigest").value("checksum-001"))
            .andExpect(jsonPath("$.executionEvidence.readonlyDigestOnly").value(true))
            .andExpect(jsonPath("$.implementationStage").value("READONLY_RESULT_DIGEST_BASELINE"));

        verify(queryExecutionResultDigestService).executeDigest(any());
    }

    @Test
    void shouldPublishRuntimeRewriteBindingThroughInternalEndpoint() throws Exception {
        RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
        response.setTenantId("tenant-a");
        response.setRuntimeBindingId("rwb-001");
        response.setRewriteRecordId("rewrite-001");
        response.setSqlFingerprint("fp-001");
        response.setDatasourceCode("hetu_main");
        response.setStatus("ACTIVE");
        response.setActive(true);
        response.setRuleVersion(Long.valueOf(1));
        response.setRuntimeRuleVersion("runtime-rewrite-v1");
        response.setRuntimeSummary("运行时改写绑定已生效，可用于生产自动改写查找。");
        response.setRuntimeDetailsJson("{\"bindingState\":\"ACTIVE\"}");
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("RUNTIME_REWRITE_BINDING_DB_BASELINE");
        when(queryExecutionRuntimeRewriteBindingService.publish(any())).thenReturn(response);

        mockMvc.perform(post("/api/query-execution/internal/rewrite-bindings/publish")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-006")
                .header("X-Trace-Id", "trace-006")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"rewriteRecordId\":\"rewrite-001\","
                    + "\"sourceType\":\"QUERY\",\"sourceKind\":\"QUERY_HISTORY\",\"sourceId\":\"history-001\","
                    + "\"sqlFingerprint\":\"fp-001\",\"originalSqlDigest\":\"digest-001\","
                    + "\"recommendedSqlText\":\"SELECT id FROM orders\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runtimeBindingId").value("rwb-001"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.runtimeRuleVersion").value("runtime-rewrite-v1"))
            .andExpect(jsonPath("$.implementationStage").value("RUNTIME_REWRITE_BINDING_DB_BASELINE"));

        verify(queryExecutionRuntimeRewriteBindingService).publish(any());
    }

    @Test
    void shouldApplyAccelerationPlanThroughInternalEndpoint() throws Exception {
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId("tenant-a");
        response.setPlanId("plan-001");
        response.setSqlFingerprint("fp-001");
        response.setTargetEngine("HETU");
        response.setActive(true);
        response.setStatus("APPLIED");
        response.setRuntimeSummary("已批准加速方案已在运行时偏好门控中生效。");
        response.setRuntimeDetailsJson("{\"bindingState\":\"ACTIVE\"}");
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("APPROVED_ACCELERATION_RUNTIME_BASELINE");
        when(queryExecutionAccelerationRuntimeService.apply(any())).thenReturn(response);

        mockMvc.perform(post("/api/query-execution/internal/acceleration-plans/apply")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-002")
                .header("X-Trace-Id", "trace-002")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"planId\":\"plan-001\",\"sqlFingerprint\":\"fp-001\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.implementationStage").value("APPROVED_ACCELERATION_RUNTIME_BASELINE"));

        verify(queryExecutionAccelerationRuntimeService).apply(any());
    }

    @Test
    void shouldApplyCachePolicyThroughInternalEndpoint() throws Exception {
        QueryExecutionCachePolicyResponse response = new QueryExecutionCachePolicyResponse();
        response.setTenantId("tenant-a");
        response.setPolicyId("cache-policy-001");
        response.setSqlFingerprint("fp-001");
        response.setTargetEngine("HETU");
        response.setSchemaVersion("schema-v1");
        response.setActive(true);
        response.setStatus("APPLIED");
        response.setPolicySummary("受治理缓存策略已生效，可用于结果缓存资格判断与版本校验。");
        response.setRuntimeDetailsJson("{\"bindingState\":\"ACTIVE\"}");
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("CACHE_GOVERNANCE_RUNTIME_BASELINE");
        when(queryExecutionCacheGovernanceRuntimeService.apply(any())).thenReturn(response);

        mockMvc.perform(post("/api/query-execution/internal/cache-policies/apply")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-004")
                .header("X-Trace-Id", "trace-004")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"policyId\":\"cache-policy-001\",\"sqlFingerprint\":\"fp-001\",\"datasourceType\":\"HETU\",\"schemaVersion\":\"schema-v1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.schemaVersion").value("schema-v1"))
            .andExpect(jsonPath("$.implementationStage").value("CACHE_GOVERNANCE_RUNTIME_BASELINE"));

        verify(queryExecutionCacheGovernanceRuntimeService).apply(any());
    }

    @Test
    void shouldVerifyAndInvalidateCachePolicyThroughInternalEndpoints() throws Exception {
        QueryExecutionCachePolicyResponse verifyResponse = new QueryExecutionCachePolicyResponse();
        verifyResponse.setTenantId("tenant-a");
        verifyResponse.setPolicyId("cache-policy-001");
        verifyResponse.setSqlFingerprint("fp-001");
        verifyResponse.setTargetEngine("HETU");
        verifyResponse.setSchemaVersion("schema-v1");
        verifyResponse.setActive(true);
        verifyResponse.setStatus("VERIFIED");
        verifyResponse.setPolicySummary("Governed cache policy remains active with version-aware runtime checks.");
        verifyResponse.setRuntimeDetailsJson("{\"bindingState\":\"ACTIVE\"}");
        verifyResponse.setContractStage("LONG_TERM_BASELINE");
        verifyResponse.setImplementationStage("CACHE_GOVERNANCE_RUNTIME_BASELINE");
        when(queryExecutionCacheGovernanceRuntimeService.verify(any())).thenReturn(verifyResponse);

        QueryExecutionCachePolicyResponse invalidateResponse = new QueryExecutionCachePolicyResponse();
        invalidateResponse.setTenantId("tenant-a");
        invalidateResponse.setPolicyId("cache-policy-001");
        invalidateResponse.setSqlFingerprint("fp-001");
        invalidateResponse.setTargetEngine("HETU");
        invalidateResponse.setSchemaVersion("schema-v1");
        invalidateResponse.setActive(true);
        invalidateResponse.setStatus("INVALIDATED");
        invalidateResponse.setPolicySummary("受治理缓存条目已失效，将在下一次符合条件的执行中回填。");
        invalidateResponse.setRuntimeDetailsJson("{\"invalidatedEntryCount\":1}");
        invalidateResponse.setContractStage("LONG_TERM_BASELINE");
        invalidateResponse.setImplementationStage("CACHE_GOVERNANCE_RUNTIME_BASELINE");
        when(queryExecutionCacheGovernanceRuntimeService.invalidate(any())).thenReturn(invalidateResponse);

        mockMvc.perform(post("/api/query-execution/internal/cache-policies/verify")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-005")
                .header("X-Trace-Id", "trace-005")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"policyId\":\"cache-policy-001\",\"sqlFingerprint\":\"fp-001\",\"datasourceType\":\"HETU\",\"schemaVersion\":\"schema-v1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"))
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/query-execution/internal/cache-policies/invalidate")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "service-user")
                .header("X-Role-Codes", "SERVICE")
                .header("X-Request-Id", "request-006")
                .header("X-Trace-Id", "trace-006")
                .header("X-Auth-Source", "header")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"policyId\":\"cache-policy-001\",\"sqlFingerprint\":\"fp-001\",\"datasourceType\":\"HETU\",\"schemaVersion\":\"schema-v1\",\"invalidateReason\":\"schema refresh\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INVALIDATED"))
            .andExpect(jsonPath("$.active").value(true));

        verify(queryExecutionCacheGovernanceRuntimeService).verify(any());
        verify(queryExecutionCacheGovernanceRuntimeService).invalidate(any());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AuthProperties authProperties() {
            AuthProperties authProperties = new AuthProperties();
            authProperties.setEnabled(false);
            authProperties.getTrustedAuthSources().add("header");
            return authProperties;
        }
    }
}
