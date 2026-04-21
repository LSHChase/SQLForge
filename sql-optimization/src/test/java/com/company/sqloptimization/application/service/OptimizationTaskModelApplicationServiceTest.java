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
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import java.time.Instant;
import java.util.Arrays;
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
        assertEquals("ASYNC_TASK_API_SKELETON", submitResponse.getImplementationStage());

        assertEquals(OptimizationTaskType.REWRITE, statusResponse.getTaskType());
        assertEquals(OptimizationTaskPriority.NORMAL, statusResponse.getPriority());
        assertEquals(2, statusResponse.getStatusHistory().size());
        assertNull(statusResponse.getError());
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
