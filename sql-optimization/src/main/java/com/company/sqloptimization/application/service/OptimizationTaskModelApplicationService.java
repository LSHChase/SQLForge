package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskErrorVO;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskStatusTransition;
import com.company.sqloptimization.domain.task.OptimizationTaskSubmission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds canonical domain and contract objects for async optimization tasks.
 */
@Service
public class OptimizationTaskModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "TASK_MODEL_BASELINE";
    private static final String STATUS_QUERY_PATH_TEMPLATE = "/api/sql-optimization/tasks/%s";

    public OptimizationTask createQueuedTask(OptimizationTaskSubmitRequest request, String taskId, Instant submittedAt) {
        OptimizationTaskContextDTO taskContext = request.getTaskContext() == null
            ? new OptimizationTaskContextDTO()
            : request.getTaskContext();
        OptimizationTaskSubmission submission = new OptimizationTaskSubmission(
            request.getTenantId(),
            request.getTaskType(),
            request.getSqlText(),
            request.getSqlFingerprint(),
            request.getDatasourceType(),
            taskContext.getPriority(),
            taskContext.getParseDepth(),
            taskContext.getCallbackUrl(),
            taskContext.getRequestedSuggestionTypes()
        );
        return OptimizationTask.submit(taskId, submission, submittedAt);
    }

    public OptimizationTaskSubmitResponse buildSubmitResponse(OptimizationTask task, Instant estimatedReadyAt) {
        return new OptimizationTaskSubmitResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getCurrentPhase(),
            estimatedReadyAt,
            buildStatusQueryPath(task.getTaskId()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    public OptimizationTaskStatusResponse buildStatusResponse(OptimizationTask task) {
        return new OptimizationTaskStatusResponse(
            task.getTaskId(),
            task.getTaskType(),
            task.getStatus(),
            task.getCurrentPhase(),
            task.getPriority(),
            task.getProgressPercent(),
            task.getRequestedSuggestionTypes(),
            task.getSummary(),
            toErrorVO(task.getError()),
            task.getSubmittedAt(),
            task.getStartedAt(),
            task.getFinishedAt(),
            toHistoryVO(task.getStatusHistory()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    private OptimizationTaskErrorVO toErrorVO(OptimizationTaskError error) {
        if (error == null) {
            return null;
        }
        return new OptimizationTaskErrorVO(
            error.getCode(),
            error.getMessage(),
            error.getSuggestedAction(),
            error.isRetryable()
        );
    }

    private List<OptimizationTaskStatusHistoryVO> toHistoryVO(List<OptimizationTaskStatusTransition> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationTaskStatusHistoryVO> items = new ArrayList<OptimizationTaskStatusHistoryVO>(history.size());
        for (OptimizationTaskStatusTransition item : history) {
            items.add(
                new OptimizationTaskStatusHistoryVO(
                    item.getPreviousStatus(),
                    item.getCurrentStatus(),
                    item.getPreviousPhase(),
                    item.getCurrentPhase(),
                    item.getOccurredAt(),
                    item.getNote()
                )
            );
        }
        return Collections.unmodifiableList(items);
    }

    private String buildStatusQueryPath(String taskId) {
        return String.format(STATUS_QUERY_PATH_TEMPLATE, taskId);
    }
}
