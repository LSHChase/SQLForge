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
    private HetuRouteCalibrationService hetuRouteCalibrationService;

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
            "Mode is ready for calibrated routing.",
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
    void shouldApplyAccelerationPlanThroughInternalEndpoint() throws Exception {
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId("tenant-a");
        response.setPlanId("plan-001");
        response.setSqlFingerprint("fp-001");
        response.setTargetEngine("HETU");
        response.setActive(true);
        response.setStatus("APPLIED");
        response.setRuntimeSummary("Approved acceleration plan is now active for runtime preference gating.");
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
