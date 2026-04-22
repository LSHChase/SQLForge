package com.company.benchmarkengine.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class BenchmarkTaskRecord {

    private String taskId;
    private String tenantId;
    private String taskType;
    private String sqlText;
    private String sqlFingerprint;
    private String priority;
    private String targetEnginesJson;
    private Integer concurrency;
    private Integer durationSeconds;
    private Integer rampUpSeconds;
    private String datasetSizeLabel;
    private Boolean readonlyRequired;
    private String shadowEnvironmentMode;
    private String desensitizationRequirement;
    private String thresholdsJson;
    private String status;
    private String currentPhase;
    private Integer progressPercent;
    private String reportId;
    private Integer errorCode;
    private String errorMessage;
    private String errorSuggestedAction;
    private Boolean errorRetryable;
    private String statusHistoryJson;
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTargetEnginesJson() {
        return targetEnginesJson;
    }

    public void setTargetEnginesJson(String targetEnginesJson) {
        this.targetEnginesJson = targetEnginesJson;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getRampUpSeconds() {
        return rampUpSeconds;
    }

    public void setRampUpSeconds(Integer rampUpSeconds) {
        this.rampUpSeconds = rampUpSeconds;
    }

    public String getDatasetSizeLabel() {
        return datasetSizeLabel;
    }

    public void setDatasetSizeLabel(String datasetSizeLabel) {
        this.datasetSizeLabel = datasetSizeLabel;
    }

    public Boolean getReadonlyRequired() {
        return readonlyRequired;
    }

    public void setReadonlyRequired(Boolean readonlyRequired) {
        this.readonlyRequired = readonlyRequired;
    }

    public String getShadowEnvironmentMode() {
        return shadowEnvironmentMode;
    }

    public void setShadowEnvironmentMode(String shadowEnvironmentMode) {
        this.shadowEnvironmentMode = shadowEnvironmentMode;
    }

    public String getDesensitizationRequirement() {
        return desensitizationRequirement;
    }

    public void setDesensitizationRequirement(String desensitizationRequirement) {
        this.desensitizationRequirement = desensitizationRequirement;
    }

    public String getThresholdsJson() {
        return thresholdsJson;
    }

    public void setThresholdsJson(String thresholdsJson) {
        this.thresholdsJson = thresholdsJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorSuggestedAction() {
        return errorSuggestedAction;
    }

    public void setErrorSuggestedAction(String errorSuggestedAction) {
        this.errorSuggestedAction = errorSuggestedAction;
    }

    public Boolean getErrorRetryable() {
        return errorRetryable;
    }

    public void setErrorRetryable(Boolean errorRetryable) {
        this.errorRetryable = errorRetryable;
    }

    public String getStatusHistoryJson() {
        return statusHistoryJson;
    }

    public void setStatusHistoryJson(String statusHistoryJson) {
        this.statusHistoryJson = statusHistoryJson;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
