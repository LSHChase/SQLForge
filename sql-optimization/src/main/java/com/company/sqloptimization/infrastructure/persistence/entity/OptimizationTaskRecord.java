package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class OptimizationTaskRecord {

    private String taskId;
    private String tenantId;
    private String taskType;
    private String sqlText;
    private String sqlFingerprint;
    private String datasourceType;
    private String priority;
    private String parseDepth;
    private String callbackUrl;
    private String requestedSuggestionTypesJson;
    private String taskContextJson;
    private String status;
    private String currentPhase;
    private Integer progressPercent;
    private String summary;
    private String suggestionPayloadJson;
    private Integer errorCode;
    private String errorMessage;
    private String errorSuggestedAction;
    private Boolean errorRetryable;
    private String failedPhase;
    private String errorRisksJson;
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

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getParseDepth() {
        return parseDepth;
    }

    public void setParseDepth(String parseDepth) {
        this.parseDepth = parseDepth;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getRequestedSuggestionTypesJson() {
        return requestedSuggestionTypesJson;
    }

    public void setRequestedSuggestionTypesJson(String requestedSuggestionTypesJson) {
        this.requestedSuggestionTypesJson = requestedSuggestionTypesJson;
    }

    public String getTaskContextJson() {
        return taskContextJson;
    }

    public void setTaskContextJson(String taskContextJson) {
        this.taskContextJson = taskContextJson;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSuggestionPayloadJson() {
        return suggestionPayloadJson;
    }

    public void setSuggestionPayloadJson(String suggestionPayloadJson) {
        this.suggestionPayloadJson = suggestionPayloadJson;
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

    public String getFailedPhase() {
        return failedPhase;
    }

    public void setFailedPhase(String failedPhase) {
        this.failedPhase = failedPhase;
    }

    public String getErrorRisksJson() {
        return errorRisksJson;
    }

    public void setErrorRisksJson(String errorRisksJson) {
        this.errorRisksJson = errorRisksJson;
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
