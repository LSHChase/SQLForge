package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import java.time.Instant;

public class BenchmarkTaskSubmitResponse {

    private final String taskId;
    private final BenchmarkTaskStatus status;
    private final BenchmarkTaskPhase currentPhase;
    private final Instant estimatedReadyAt;
    private final String statusQueryPath;
    private final String contractStage;
    private final String implementationStage;

    public BenchmarkTaskSubmitResponse(String taskId,
                                       BenchmarkTaskStatus status,
                                       BenchmarkTaskPhase currentPhase,
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

    public BenchmarkTaskStatus getStatus() {
        return status;
    }

    public BenchmarkTaskPhase getCurrentPhase() {
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
