package com.company.sqloptimization.application.controller.vo;

import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import java.time.Instant;
import java.util.List;

public class OptimizationTaskStatusResponse {

    private final String taskId;
    private final OptimizationTaskType taskType;
    private final OptimizationTaskStatus status;
    private final OptimizationTaskPhase currentPhase;
    private final OptimizationTaskPriority priority;
    private final Integer progressPercent;
    private final List<AccelerationSuggestionType> requestedSuggestionTypes;
    private final OptimizationSuggestionVO suggestion;
    private final OptimizationFailureVO failure;
    private final Instant submittedAt;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final List<OptimizationTaskStatusHistoryVO> statusHistory;
    private final String contractStage;
    private final String implementationStage;

    public OptimizationTaskStatusResponse(String taskId,
                                          OptimizationTaskType taskType,
                                          OptimizationTaskStatus status,
                                          OptimizationTaskPhase currentPhase,
                                          OptimizationTaskPriority priority,
                                          Integer progressPercent,
                                          List<AccelerationSuggestionType> requestedSuggestionTypes,
                                          OptimizationSuggestionVO suggestion,
                                          OptimizationFailureVO failure,
                                          Instant submittedAt,
                                          Instant startedAt,
                                          Instant finishedAt,
                                          List<OptimizationTaskStatusHistoryVO> statusHistory,
                                          String contractStage,
                                          String implementationStage) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.status = status;
        this.currentPhase = currentPhase;
        this.priority = priority;
        this.progressPercent = progressPercent;
        this.requestedSuggestionTypes = requestedSuggestionTypes;
        this.suggestion = suggestion;
        this.failure = failure;
        this.submittedAt = submittedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.statusHistory = statusHistory;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getTaskId() {
        return taskId;
    }

    public OptimizationTaskType getTaskType() {
        return taskType;
    }

    public OptimizationTaskStatus getStatus() {
        return status;
    }

    public OptimizationTaskPhase getCurrentPhase() {
        return currentPhase;
    }

    public OptimizationTaskPriority getPriority() {
        return priority;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public List<AccelerationSuggestionType> getRequestedSuggestionTypes() {
        return requestedSuggestionTypes;
    }

    public OptimizationSuggestionVO getSuggestion() {
        return suggestion;
    }

    public OptimizationFailureVO getFailure() {
        return failure;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public List<OptimizationTaskStatusHistoryVO> getStatusHistory() {
        return statusHistory;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
