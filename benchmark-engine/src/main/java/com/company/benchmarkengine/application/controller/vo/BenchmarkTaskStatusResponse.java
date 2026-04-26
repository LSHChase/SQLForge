package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.Instant;
import java.util.List;

public class BenchmarkTaskStatusResponse {

    private final String taskId;
    private final BenchmarkTaskType taskType;
    private final BenchmarkTaskStatus status;
    private final BenchmarkTaskPhase currentPhase;
    private final BenchmarkTaskPriority priority;
    private final Integer progressPercent;
    private final List<DataSourceTypeEnum> targetEngines;
    private final Boolean readonlyRequired;
    private final ShadowEnvironmentMode shadowEnvironmentMode;
    private final DesensitizationRequirement desensitizationRequirement;
    private final Integer thresholdCount;
    private final String reportId;
    private final BenchmarkTaskErrorVO error;
    private final Instant submittedAt;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String queueMode;
    private final String queueEvidence;
    private final String contractStage;
    private final String implementationStage;

    public BenchmarkTaskStatusResponse(String taskId,
                                       BenchmarkTaskType taskType,
                                       BenchmarkTaskStatus status,
                                       BenchmarkTaskPhase currentPhase,
                                       BenchmarkTaskPriority priority,
                                       Integer progressPercent,
                                       List<DataSourceTypeEnum> targetEngines,
                                       Boolean readonlyRequired,
                                       ShadowEnvironmentMode shadowEnvironmentMode,
                                       DesensitizationRequirement desensitizationRequirement,
                                       Integer thresholdCount,
                                       String reportId,
                                       BenchmarkTaskErrorVO error,
                                       Instant submittedAt,
                                       Instant startedAt,
                                       Instant finishedAt,
                                       String queueMode,
                                       String queueEvidence,
                                       String contractStage,
                                       String implementationStage) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.status = status;
        this.currentPhase = currentPhase;
        this.priority = priority;
        this.progressPercent = progressPercent;
        this.targetEngines = targetEngines;
        this.readonlyRequired = readonlyRequired;
        this.shadowEnvironmentMode = shadowEnvironmentMode;
        this.desensitizationRequirement = desensitizationRequirement;
        this.thresholdCount = thresholdCount;
        this.reportId = reportId;
        this.error = error;
        this.submittedAt = submittedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.queueMode = queueMode;
        this.queueEvidence = queueEvidence;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getTaskId() {
        return taskId;
    }

    public BenchmarkTaskType getTaskType() {
        return taskType;
    }

    public BenchmarkTaskStatus getStatus() {
        return status;
    }

    public BenchmarkTaskPhase getCurrentPhase() {
        return currentPhase;
    }

    public BenchmarkTaskPriority getPriority() {
        return priority;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public List<DataSourceTypeEnum> getTargetEngines() {
        return targetEngines;
    }

    public Boolean getReadonlyRequired() {
        return readonlyRequired;
    }

    public ShadowEnvironmentMode getShadowEnvironmentMode() {
        return shadowEnvironmentMode;
    }

    public DesensitizationRequirement getDesensitizationRequirement() {
        return desensitizationRequirement;
    }

    public Integer getThresholdCount() {
        return thresholdCount;
    }

    public String getReportId() {
        return reportId;
    }

    public BenchmarkTaskErrorVO getError() {
        return error;
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

    public String getQueueMode() {
        return queueMode;
    }

    public String getQueueEvidence() {
        return queueEvidence;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
