package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BenchmarkTaskModelApplicationServiceTest {

    @Test
    void shouldCreateQueuedTaskWithIsolationDefaults() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setTaskContext(new BenchmarkTaskContextDTO());

        BenchmarkTask task = service.createQueuedTask(
            request,
            "benchmark-task-001",
            Instant.parse("2026-04-20T00:00:00Z")
        );

        assertEquals(BenchmarkTaskStatus.QUEUED, task.getStatus());
        assertEquals(ShadowEnvironmentMode.REQUIRED, task.getShadowEnvironmentMode());
        assertEquals(DesensitizationRequirement.REQUIRED, task.getDesensitizationRequirement());
        assertTrue(task.getReadonlyRequired().booleanValue());
        assertEquals(1, task.getTargetEngines().size());
        assertEquals(DataSourceTypeEnum.HETU, task.getTargetEngines().get(0));
    }

    @Test
    void shouldBuildSubmitAndStatusResponseFromRunningTask() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = service.createQueuedTask(
            baseRequest(BenchmarkTaskType.COMPARISON),
            "benchmark-task-002",
            Instant.parse("2026-04-20T00:05:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:05:05Z"));

        BenchmarkTaskSubmitResponse submitResponse = service.buildSubmitResponse(
            task,
            Instant.parse("2026-04-20T00:05:50Z")
        );
        BenchmarkTaskStatusResponse statusResponse = service.buildStatusResponse(task);

        assertEquals("benchmark-task-002", submitResponse.getTaskId());
        assertEquals(BenchmarkTaskStatus.RUNNING, submitResponse.getStatus());
        assertEquals(BenchmarkTaskPhase.BASELINE_PREPARING, submitResponse.getCurrentPhase());
        assertEquals("/api/benchmark-engine/tasks/benchmark-task-002", submitResponse.getStatusQueryPath());
        assertEquals("LONG_TERM_BASELINE", submitResponse.getContractStage());
        assertEquals("ASYNC_TASK_API_SKELETON", submitResponse.getImplementationStage());

        assertEquals(BenchmarkTaskType.COMPARISON, statusResponse.getTaskType());
        assertEquals(BenchmarkTaskPriority.NORMAL, statusResponse.getPriority());
        assertEquals(Integer.valueOf(2), Integer.valueOf(statusResponse.getTargetEngines().size()));
        assertEquals(Integer.valueOf(3), statusResponse.getThresholdCount());
        assertNull(statusResponse.getError());
        assertNotNull(statusResponse.getStartedAt());
    }

    @Test
    void shouldBuildComparisonReportWithWarningThreshold() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = service.createQueuedTask(
            baseRequest(BenchmarkTaskType.COMPARISON),
            "benchmark-task-003",
            Instant.parse("2026-04-20T00:10:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:10:01Z"));
        task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 30, "PREPARED");
        task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 45, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "WARMUP_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-benchmark-task-003", Instant.parse("2026-04-20T00:10:10Z"));

        BenchmarkReport report = service.buildPlaceholderReport(task, Instant.parse("2026-04-20T00:10:11Z"));
        BenchmarkReportResponse response = service.buildReportResponse(report);

        assertEquals(BenchmarkThresholdVerdict.WARNING, response.getVerdict());
        assertEquals(2, response.getEngineResults().size());
        assertEquals(3, response.getThresholdAssessments().size());
        assertEquals(BenchmarkThresholdVerdict.WARNING, response.getThresholdAssessments().get(2).getVerdict());
        assertEquals("ENGINE_SELECTION", response.getRecommendations().get(0).getCategory());
        assertEquals("MODEL_BASELINE", response.getImplementationStage());
    }

    @Test
    void shouldBuildRegressionGuardReportWithFailVerdict() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = baseRequest(BenchmarkTaskType.REGRESSION_GUARD);
        request.getTaskContext().setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU));
        request.getTaskContext().setThresholds(
            Arrays.asList(
                threshold(
                    BenchmarkThresholdMetric.P99_LATENCY_MS,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "70",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "P99 regression gate"
                )
            )
        );
        BenchmarkTask task = service.createQueuedTask(
            request,
            "benchmark-task-004",
            Instant.parse("2026-04-20T00:15:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:15:01Z"));
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-benchmark-task-004", Instant.parse("2026-04-20T00:15:10Z"));

        BenchmarkReportResponse response = service.buildReportResponse(
            service.buildPlaceholderReport(task, Instant.parse("2026-04-20T00:15:11Z"))
        );

        assertEquals(BenchmarkThresholdVerdict.FAIL, response.getVerdict());
        assertEquals(BenchmarkThresholdVerdict.FAIL, response.getThresholdAssessments().get(0).getVerdict());
        assertEquals("REGRESSION_GATE", response.getRecommendations().get(0).getCategory());
    }

    @Test
    void shouldExposeStructuredErrorForFailedTask() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = service.createQueuedTask(
            baseRequest(BenchmarkTaskType.BASELINE),
            "benchmark-task-005",
            Instant.parse("2026-04-20T00:20:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:20:01Z"));
        task.markFailed(
            new BenchmarkTaskError(
                14000,
                "压测引擎任务与报告模型已固化，但提交流程、执行链路和报告查询接口仍待接入",
                "retry after task submit flow is available",
                true
            ),
            Instant.parse("2026-04-20T00:20:05Z")
        );

        BenchmarkTaskStatusResponse response = service.buildStatusResponse(task);

        assertNotNull(response.getError());
        assertEquals(Integer.valueOf(14000), Integer.valueOf(response.getError().getCode()));
        assertEquals(BenchmarkTaskStatus.FAILED, response.getStatus());
        assertEquals(BenchmarkTaskPhase.FINISHED, response.getCurrentPhase());
    }

    private BenchmarkTaskSubmitRequest baseRequest(BenchmarkTaskType taskType) {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(taskType);
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-benchmark-base");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        request.getTaskContext().setPriority(BenchmarkTaskPriority.NORMAL);
        request.getTaskContext().setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE));
        request.getTaskContext().setConcurrency(Integer.valueOf(16));
        request.getTaskContext().setDurationSeconds(Integer.valueOf(300));
        request.getTaskContext().setRampUpSeconds(Integer.valueOf(30));
        request.getTaskContext().setDatasetSizeLabel("TEN_GB");
        request.getTaskContext().setReadonlyRequired(Boolean.TRUE);
        request.getTaskContext().setShadowEnvironmentMode(ShadowEnvironmentMode.REQUIRED);
        request.getTaskContext().setDesensitizationRequirement(DesensitizationRequirement.REQUIRED);
        request.getTaskContext().setThresholds(
            Arrays.asList(
                threshold(
                    BenchmarkThresholdMetric.QPS,
                    BenchmarkThresholdOperator.GREATER_THAN_OR_EQUAL,
                    "120",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "Throughput baseline"
                ),
                threshold(
                    BenchmarkThresholdMetric.P99_LATENCY_MS,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "90",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "Tail latency"
                ),
                threshold(
                    BenchmarkThresholdMetric.CPU_USAGE_PERCENT,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "50",
                    BenchmarkThresholdSeverity.WARNING,
                    "CPU pressure"
                )
            )
        );
        return request;
    }

    private BenchmarkThresholdDTO threshold(BenchmarkThresholdMetric metric,
                                            BenchmarkThresholdOperator operator,
                                            String targetValue,
                                            BenchmarkThresholdSeverity severity,
                                            String description) {
        BenchmarkThresholdDTO item = new BenchmarkThresholdDTO();
        item.setMetric(metric);
        item.setOperator(operator);
        item.setTargetValue(new BigDecimal(targetValue));
        item.setSeverity(severity);
        item.setDescription(description);
        return item;
    }
}
