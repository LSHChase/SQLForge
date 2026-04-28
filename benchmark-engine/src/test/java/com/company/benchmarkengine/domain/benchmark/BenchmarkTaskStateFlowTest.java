package com.company.benchmarkengine.domain.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BenchmarkTaskStateFlowTest {

    @Test
    void shouldAdvanceComparisonTaskThroughExpectedPhaseFlow() {
        BenchmarkTask task = BenchmarkTask.submit(
            "benchmark-task-101",
            new BenchmarkTaskSubmission(
                "tenant-a",
                BenchmarkTaskType.COMPARISON,
                "SELECT * FROM orders",
                "fp-benchmark-1",
                BenchmarkTaskPriority.HIGH,
                Arrays.asList(DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE),
                Integer.valueOf(16),
                Integer.valueOf(300),
                Integer.valueOf(30),
                "TEN_GB",
                "comparison-dual-engine",
                BenchmarkTemplateType.CROSS_ENGINE_COMPARISON,
                "v2026.04",
                "set-route-comparison",
                BenchmarkTestSetSource.RECOMMENDATION_GENERATION,
                Arrays.asList(
                    new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.SCENARIO, "COMPARISON"),
                    new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.DOMAIN, "ROUTE_GOVERNANCE")
                ),
                Arrays.asList(
                    new BenchmarkSourceReference(BenchmarkSourceReferenceType.RECOMMENDATION, "rec-001"),
                    new BenchmarkSourceReference(BenchmarkSourceReferenceType.SQL_FINGERPRINT, "fp-benchmark-1")
                ),
                Boolean.TRUE,
                ShadowEnvironmentMode.REQUIRED,
                DesensitizationRequirement.REQUIRED,
                Arrays.asList(
                    new BenchmarkThreshold(
                        BenchmarkThresholdMetric.QPS,
                        BenchmarkThresholdOperator.GREATER_THAN_OR_EQUAL,
                        new BigDecimal("120"),
                        BenchmarkThresholdSeverity.CRITICAL,
                        "Throughput baseline"
                    )
                )
            ),
            Instant.parse("2026-04-20T00:00:00Z")
        );

        task.markRunning(Instant.parse("2026-04-20T00:00:05Z"));
        task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 25, "BASELINE_PREPARED");
        task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 40, "SHADOW_ENVIRONMENT_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "WARMUP_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 90, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-benchmark-task-101", Instant.parse("2026-04-20T00:00:20Z"));

        assertEquals(BenchmarkTaskStatus.SUCCEEDED, task.getStatus());
        assertEquals(BenchmarkTaskPhase.FINISHED, task.getCurrentPhase());
        assertEquals(Integer.valueOf(100), task.getProgressPercent());
        assertEquals("report-benchmark-task-101", task.getReportId());
        assertEquals(8, task.getStatusHistory().size());
    }

    @Test
    void shouldRejectRegressionGuardSkippingThresholdEvaluation() {
        BenchmarkTask task = BenchmarkTask.submit(
            "benchmark-task-102",
            new BenchmarkTaskSubmission(
                "tenant-a",
                BenchmarkTaskType.REGRESSION_GUARD,
                null,
                "fp-benchmark-2",
                BenchmarkTaskPriority.NORMAL,
                Arrays.asList(DataSourceTypeEnum.HETU),
                Integer.valueOf(8),
                Integer.valueOf(180),
                Integer.valueOf(15),
                "ONE_GB",
                "regression-guard",
                BenchmarkTemplateType.REGRESSION_GUARD,
                "v2026.04",
                "set-regression-gate",
                BenchmarkTestSetSource.PARSE_RESULT_GENERATION,
                Arrays.asList(new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.SCENARIO, "REGRESSION_GUARD")),
                Arrays.asList(new BenchmarkSourceReference(BenchmarkSourceReferenceType.QUERY_HISTORY, "history-001")),
                Boolean.TRUE,
                ShadowEnvironmentMode.REQUIRED,
                DesensitizationRequirement.REQUIRED,
                null
            ),
            Instant.parse("2026-04-20T00:10:00Z")
        );

        task.markRunning(Instant.parse("2026-04-20T00:10:05Z"));
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 50, "SHADOW_VALIDATED");
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> task.advancePhase(BenchmarkTaskPhase.REPORTING, 85, "SKIP_THRESHOLD_EVALUATION")
        );

        assertTrue(
            ex.getMessage().startsWith(String.valueOf(ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_STATE_TRANSITION_INVALID))
        );
    }

    @Test
    void shouldAllowQueuedTaskCancellation() {
        BenchmarkTask task = BenchmarkTask.submit(
            "benchmark-task-103",
            new BenchmarkTaskSubmission(
                "tenant-a",
                BenchmarkTaskType.BASELINE,
                "SELECT * FROM orders",
                "fp-benchmark-3",
                BenchmarkTaskPriority.LOW,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            Instant.parse("2026-04-20T00:20:00Z")
        );

        task.cancel("USER_CANCELLED", Instant.parse("2026-04-20T00:20:01Z"));

        assertEquals(BenchmarkTaskStatus.CANCELLED, task.getStatus());
        assertEquals(BenchmarkTaskPhase.FINISHED, task.getCurrentPhase());
        assertEquals(2, task.getStatusHistory().size());
    }
}
