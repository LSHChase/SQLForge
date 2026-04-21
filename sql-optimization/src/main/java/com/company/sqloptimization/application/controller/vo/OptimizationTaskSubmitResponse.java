package com.company.sqloptimization.application.controller.vo;

import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import java.time.Instant;

public class OptimizationTaskSubmitResponse {

    private final String taskId;
    private final OptimizationTaskStatus status;
    private final OptimizationTaskPhase currentPhase;
    private final Instant estimatedReadyAt;
    private final String statusQueryPath;
    private final String contractStage;
    private final String implementationStage;

    public OptimizationTaskSubmitResponse(String taskId,
                                          OptimizationTaskStatus status,
                                          OptimizationTaskPhase currentPhase,
                                          Instant estimatedReadyAt,
                                          String statusQueryPath,
                                          String contractStage,
                                          String implementationStage) {
        this.taskId = taskId;
        this.status = status;
        this.currentPhase = currentPhase;
        this.estimatedReadyAt = estimatedReadyAt;
        this.statusQueryPath = statusQueryPath;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getTaskId() {
        return taskId;
    }

    public OptimizationTaskStatus getStatus() {
        return status;
    }

    public OptimizationTaskPhase getCurrentPhase() {
        return currentPhase;
    }

    public Instant getEstimatedReadyAt() {
        return estimatedReadyAt;
    }

    public String getStatusQueryPath() {
        return statusQueryPath;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
