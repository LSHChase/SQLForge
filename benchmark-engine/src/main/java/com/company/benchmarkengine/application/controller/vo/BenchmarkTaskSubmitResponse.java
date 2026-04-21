package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;

public class BenchmarkTaskSubmitResponse {

    private final String taskId;
    private final BenchmarkTaskStatus status;
    private final BenchmarkTaskPhase currentPhase;
    private final String contractStage;
    private final String implementationStage;

    public BenchmarkTaskSubmitResponse(String taskId,
                                       BenchmarkTaskStatus status,
                                       BenchmarkTaskPhase currentPhase,
                                       String contractStage,
                                       String implementationStage) {
        this.taskId = taskId;
        this.status = status;
        this.currentPhase = currentPhase;
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

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
