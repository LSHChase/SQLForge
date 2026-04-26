package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OptimizationTaskModelApplicationServiceTest {

    @Test
    void shouldCreateQueuedAccelerationTaskAndNormalizeSuggestionTypes() {
        OptimizationTaskModelApplicationService service = new OptimizationTaskModelApplicationService();
        OptimizationTaskSubmitRequest request = baseRequest(OptimizationTaskType.ACCELERATION_SUGGESTION);
        request.getTaskContext().setRequestedSuggestionTypes(
            Arrays.asList(AccelerationSuggestionType.ALL, AccelerationSuggestionType.PARTITION)
        );

        OptimizationTask task = service.createQueuedTask(
            request,
            "task-001",
            Instant.parse("2026-04-20T00:00:00Z")
        );

        assertEquals("task-001", task.getTaskId());
        assertEquals(OptimizationTaskStatus.QUEUED, task.getStatus());
        assertEquals(OptimizationTaskPhase.SUBMITTED, task.getCurrentPhase());
        assertEquals(1, task.getRequestedSuggestionTypes().size());
        assertEquals(AccelerationSuggestionType.ALL, task.getRequestedSuggestionTypes().get(0));
        assertEquals(1, task.getStatusHistory().size());
        assertEquals("TASK_SUBMITTED", task.getStatusHistory().get(0).getNote());
    }

    @Test
    void shouldBuildSubmitAndStatusResponseFromTaskEntity() {
        OptimizationTaskModelApplicationService service = new OptimizationTaskModelApplicationService();
        OptimizationTask task = service.createQueuedTask(
            baseRequest(OptimizationTaskType.REWRITE),
            "task-002",
            Instant.parse("2026-04-20T00:05:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:05:05Z"));

        OptimizationTaskSubmitResponse submitResponse = service.buildSubmitResponse(
            task,
            Instant.parse("2026-04-20T00:06:00Z")
        );
        OptimizationTaskStatusResponse statusResponse = service.buildStatusResponse(task);

        assertEquals("task-002", submitResponse.getTaskId());
        assertEquals(OptimizationTaskStatus.RUNNING, submitResponse.getStatus());
        assertEquals(OptimizationTaskPhase.DEEP_PARSING, submitResponse.getCurrentPhase());
        assertEquals("/api/sql-optimization/tasks/task-002", submitResponse.getStatusQueryPath());
        assertEquals("LONG_TERM_BASELINE", submitResponse.getContractStage());
        assertEquals("REAL_PARSE_REWRITE_ACCELERATION_BASELINE", submitResponse.getImplementationStage());

        assertEquals(OptimizationTaskType.REWRITE, statusResponse.getTaskType());
        assertEquals(OptimizationTaskPriority.NORMAL, statusResponse.getPriority());
        assertEquals(2, statusResponse.getStatusHistory().size());
        assertNull(statusResponse.getFailure());
        assertNull(statusResponse.getSuggestion());
        assertNotNull(statusResponse.getStartedAt());
    }

    @Test
    void shouldDefaultNonAccelerationTasksToEmptySuggestionTypes() {
        OptimizationTaskModelApplicationService service = new OptimizationTaskModelApplicationService();
        OptimizationTaskSubmitRequest request = baseRequest(OptimizationTaskType.PARSE);
        request.getTaskContext().setRequestedSuggestionTypes(Arrays.asList(AccelerationSuggestionType.PARTITION));

        OptimizationTask task = service.createQueuedTask(
            request,
            "task-003",
            Instant.parse("2026-04-20T00:10:00Z")
        );

        assertTrue(task.getRequestedSuggestionTypes().isEmpty());
    }

    @Test
    void shouldExposeStructuredParseSuggestionForSucceededTask() {
        OptimizationTaskModelApplicationService service = new OptimizationTaskModelApplicationService();
        OptimizationTask task = service.createQueuedTask(
            baseRequest(OptimizationTaskType.PARSE),
            "task-004",
            Instant.parse("2026-04-20T00:12:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:12:01Z"));
        task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "PLACEHOLDER_PARSE_SUMMARY_READY");
        task.markSucceeded(
            new OptimizationTaskSuggestion(
                "parsed real sql",
                "use ast profile",
                Integer.valueOf(88),
                Collections.singletonList(
                    new OptimizationTaskArtifact("AST_PROFILE", "astProfile", "{\"tables\":[\"orders\"]}")
                ),
                Collections.singletonList(
                    new OptimizationTaskBenefit("REWRITE_READINESS", Integer.valueOf(60), "rewrite is ready")
                ),
                Collections.singletonList(
                    new OptimizationTaskCost("PARSER_OVERHEAD", "LOW", "offline only")
                ),
                Collections.singletonList(
                    new OptimizationTaskRisk("LOW", "SELECT_STAR", "wide projection", "project columns explicitly")
                )
            ),
            Instant.parse("2026-04-20T00:12:05Z")
        );

        OptimizationTaskStatusResponse response = service.buildStatusResponse(task);

        assertNotNull(response.getSuggestion());
        assertEquals("use ast profile", response.getSuggestion().getPrimaryRecommendation());
        assertEquals("AST_PROFILE", response.getSuggestion().getArtifacts().get(0).getCategory());
        assertEquals("REWRITE_READINESS", response.getSuggestion().getBenefits().get(0).getCategory());
        assertEquals("PARSER_OVERHEAD", response.getSuggestion().getCosts().get(0).getCategory());
        assertEquals("SELECT_STAR", response.getSuggestion().getRisks().get(0).getCategory());
    }

    @Test
    void shouldExposeStructuredFailureForFailedTask() {
        OptimizationTaskModelApplicationService service = new OptimizationTaskModelApplicationService();
        OptimizationTask task = service.createQueuedTask(
            baseRequest(OptimizationTaskType.ACCELERATION_SUGGESTION),
            "task-005",
            Instant.parse("2026-04-20T00:15:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:15:01Z"));
        task.markFailed(
            new com.company.sqloptimization.domain.task.OptimizationTaskError(
                13000,
                "SQL optimization worker failed before producing a suggestion payload",
                "retry later",
                true,
                OptimizationTaskPhase.ACCELERATION_PLANNING,
                Collections.singletonList(
                    new OptimizationTaskRisk("MEDIUM", "PIPELINE_READINESS", "task carrier unhealthy", "retry later")
                )
            ),
            Instant.parse("2026-04-20T00:15:05Z")
        );

        OptimizationTaskStatusResponse response = service.buildStatusResponse(task);

        assertNotNull(response.getFailure());
        assertEquals(Integer.valueOf(13000), Integer.valueOf(response.getFailure().getCode()));
        assertEquals("ACCELERATION_PLANNING", response.getFailure().getFailedPhase());
        assertEquals("PIPELINE_READINESS", response.getFailure().getRisks().get(0).getCategory());
        assertNull(response.getSuggestion());
    }

    private OptimizationTaskSubmitRequest baseRequest(OptimizationTaskType taskType) {
        OptimizationTaskSubmitRequest request = new OptimizationTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(taskType);
        request.setSqlText("SELECT * FROM orders");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setTaskContext(new OptimizationTaskContextDTO());
        return request;
    }
}
