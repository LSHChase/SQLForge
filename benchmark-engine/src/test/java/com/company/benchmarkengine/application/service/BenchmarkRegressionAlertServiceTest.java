package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class BenchmarkRegressionAlertServiceTest {

    @Test
    void shouldAttachGovernanceLinkageForRegressionGuardFailure() {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = modelService.createQueuedTask(
            regressionRequest(),
            "benchmark-task-regression-alert-001",
            Instant.parse("2026-04-27T18:00:00Z")
        );
        task.markRunning(Instant.parse("2026-04-27T18:00:01Z"));
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "RUNNING");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "REPORTING");
        task.markSucceeded("report-benchmark-task-regression-alert-001", Instant.parse("2026-04-27T18:00:10Z"));

        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkIsolatedExecutionResult executionResult =
            new BenchmarkIsolatedExecutionService(properties, modelService).execute(task, Instant.parse("2026-04-27T18:00:11Z"));
        BenchmarkReport report = modelService.buildExecutedReport(task, executionResult, Instant.parse("2026-04-27T18:00:11Z"));
        BenchmarkReportResponse response = modelService.buildReportResponse(report);

        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        GovernanceBenchmarkRegressionAlertLinkage linkage = new GovernanceBenchmarkRegressionAlertLinkage();
        linkage.setAlertId("alert-001");
        linkage.setAlertType("BENCHMARK_REGRESSION_FAILED");
        linkage.setAlertLevel("HIGH");
        linkage.setAlertStatus("OPEN");
        linkage.setNotifyStatus("SIMULATED_NOTIFIED");
        linkage.setSummary("Regression guard hit 1 threshold(s): failed=1, warning=0.");
        linkage.setDetailPath("");
        linkage.setLinkageMode("EMITTED");
        GovernanceBenchmarkRegressionAlertResponse alertResponse = new GovernanceBenchmarkRegressionAlertResponse();
        alertResponse.setAlertTriggered(Boolean.TRUE);
        alertResponse.setAlertLinkages(Collections.singletonList(linkage));
        when(governanceCapabilityClient.emitBenchmarkRegressionAlert(any())).thenReturn(alertResponse);

        BenchmarkReport enriched = new BenchmarkRegressionAlertService(governanceCapabilityClient)
            .attachAlertLinkage(task, report, response, "history-benchmark-report-001");

        assertEquals(1, enriched.getAlertLinkages().size());
        assertEquals("alert-001", enriched.getAlertLinkages().get(0).getAlertId());
        verify(governanceCapabilityClient).emitBenchmarkRegressionAlert(any());
    }

    @Test
    void shouldSkipAlertEmissionWhenRegressionSummaryDoesNotRequireAlert() {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = regressionRequest();
        request.getTaskContext().setThresholds(Arrays.asList(
            threshold(
                BenchmarkThresholdMetric.QPS,
                BenchmarkThresholdOperator.GREATER_THAN_OR_EQUAL,
                "100",
                BenchmarkThresholdSeverity.WARNING,
                "lenient throughput gate"
            )
        ));
        BenchmarkTask task = modelService.createQueuedTask(
            request,
            "benchmark-task-regression-alert-002",
            Instant.parse("2026-04-27T18:10:00Z")
        );
        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkIsolatedExecutionResult executionResult =
            new BenchmarkIsolatedExecutionService(properties, modelService).execute(task, Instant.parse("2026-04-27T18:10:01Z"));
        BenchmarkReport report = modelService.buildExecutedReport(task, executionResult, Instant.parse("2026-04-27T18:10:01Z"));

        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        BenchmarkReport enriched = new BenchmarkRegressionAlertService(governanceCapabilityClient)
            .attachAlertLinkage(task, report, modelService.buildReportResponse(report), "history-benchmark-report-002");

        assertTrue(enriched.getAlertLinkages().isEmpty());
    }

    private BenchmarkTaskSubmitRequest regressionRequest() {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.REGRESSION_GUARD);
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-regression-001");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        request.getTaskContext().setTargetEngines(Collections.singletonList(DataSourceTypeEnum.HETU));
        request.getTaskContext().setThresholds(Arrays.asList(
            threshold(
                BenchmarkThresholdMetric.P99_LATENCY_MS,
                BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                "70",
                BenchmarkThresholdSeverity.CRITICAL,
                "P99 regression gate"
            )
        ));
        return request;
    }

    private BenchmarkThresholdDTO threshold(BenchmarkThresholdMetric metric,
                                            BenchmarkThresholdOperator operator,
                                            String targetValue,
                                            BenchmarkThresholdSeverity severity,
                                            String description) {
        BenchmarkThresholdDTO threshold = new BenchmarkThresholdDTO();
        threshold.setMetric(metric);
        threshold.setOperator(operator);
        threshold.setTargetValue(new BigDecimal(targetValue));
        threshold.setSeverity(severity);
        threshold.setDescription(description);
        return threshold;
    }
}
