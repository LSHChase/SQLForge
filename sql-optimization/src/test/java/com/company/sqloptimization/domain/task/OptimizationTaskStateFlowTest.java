package com.company.sqloptimization.domain.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OptimizationTaskStateFlowTest {

    @Test
    void shouldAdvanceRewriteTaskThroughExpectedPhaseFlow() {
        OptimizationTask task = OptimizationTask.submit(
            "task-101",
            new OptimizationTaskSubmission(
                "tenant-a",
                OptimizationTaskType.REWRITE,
                "SELECT * FROM orders",
                "fp-1",
                DataSourceTypeEnum.HETU,
                OptimizationTaskPriority.HIGH,
                OptimizationParseDepth.DEEP,
                null,
                null
            ),
            Instant.parse("2026-04-20T00:00:00Z")
        );

        task.markRunning(Instant.parse("2026-04-20T00:00:05Z"));
        task.advancePhase(OptimizationTaskPhase.SQL_REWRITING, 45, "REWRITE_RULES_APPLIED");
        task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "RESULT_MERGED");
        task.markSucceeded(
            new OptimizationTaskSuggestion(
                "rewrite suggestion ready",
                "validate rewritten sql",
                Integer.valueOf(80),
                Collections.<OptimizationTaskArtifact>emptyList(),
                Collections.<OptimizationTaskBenefit>emptyList(),
                Collections.<OptimizationTaskCost>emptyList(),
                Collections.<OptimizationTaskRisk>emptyList()
            ),
            Instant.parse("2026-04-20T00:00:15Z")
        );

        assertEquals(OptimizationTaskStatus.SUCCEEDED, task.getStatus());
        assertEquals(OptimizationTaskPhase.FINISHED, task.getCurrentPhase());
        assertEquals(Integer.valueOf(100), task.getProgressPercent());
        assertEquals(5, task.getStatusHistory().size());
    }

    @Test
    void shouldRejectUnsupportedAccelerationShortcutPhaseTransition() {
        OptimizationTask task = OptimizationTask.submit(
            "task-102",
            new OptimizationTaskSubmission(
                "tenant-a",
                OptimizationTaskType.ACCELERATION_SUGGESTION,
                "SELECT * FROM orders",
                "fp-2",
                DataSourceTypeEnum.HETU,
                OptimizationTaskPriority.NORMAL,
                OptimizationParseDepth.DEEP,
                null,
                Arrays.asList(AccelerationSuggestionType.PARTITION)
            ),
            Instant.parse("2026-04-20T00:10:00Z")
        );

        task.markRunning(Instant.parse("2026-04-20T00:10:05Z"));
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 70, "SKIP_COSTING")
        );

        assertEquals(
            true,
            ex.getMessage().startsWith(String.valueOf(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID))
        );
    }

    @Test
    void shouldAllowQueuedTaskCancellation() {
        OptimizationTask task = OptimizationTask.submit(
            "task-103",
            new OptimizationTaskSubmission(
                "tenant-a",
                OptimizationTaskType.PARSE,
                "SELECT * FROM orders",
                "fp-3",
                DataSourceTypeEnum.HETU,
                OptimizationTaskPriority.LOW,
                OptimizationParseDepth.LIGHT,
                null,
                null
            ),
            Instant.parse("2026-04-20T00:20:00Z")
        );

        task.cancel("USER_CANCELLED", Instant.parse("2026-04-20T00:20:01Z"));

        assertEquals(OptimizationTaskStatus.CANCELLED, task.getStatus());
        assertEquals(OptimizationTaskPhase.FINISHED, task.getCurrentPhase());
        assertEquals(2, task.getStatusHistory().size());
    }
}
