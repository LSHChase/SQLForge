package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class RewriteTrialItemRecord {

    private String trialItemId;
    private String runId;
    private String batchItemId;
    private String parseTaskId;
    private String parseHistoryId;
    private String historyId;
    private String sqlFingerprint;
    private String datasourceCode;
    private String sourceSqlText;
    private String sourceProblemsJson;
    private String taskId;
    private String recommendationId;
    private String rewriteRecordId;
    private String trialStatus;
    private String failureReason;
    private String candidateSql;
    private String validationStatus;
    private String issueRuleLinksJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getTrialItemId() { return trialItemId; }
    public void setTrialItemId(String trialItemId) { this.trialItemId = trialItemId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getBatchItemId() { return batchItemId; }
    public void setBatchItemId(String batchItemId) { this.batchItemId = batchItemId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getSourceSqlText() { return sourceSqlText; }
    public void setSourceSqlText(String sourceSqlText) { this.sourceSqlText = sourceSqlText; }
    public String getSourceProblemsJson() { return sourceProblemsJson; }
    public void setSourceProblemsJson(String sourceProblemsJson) { this.sourceProblemsJson = sourceProblemsJson; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getTrialStatus() { return trialStatus; }
    public void setTrialStatus(String trialStatus) { this.trialStatus = trialStatus; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCandidateSql() { return candidateSql; }
    public void setCandidateSql(String candidateSql) { this.candidateSql = candidateSql; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getIssueRuleLinksJson() { return issueRuleLinksJson; }
    public void setIssueRuleLinksJson(String issueRuleLinksJson) { this.issueRuleLinksJson = issueRuleLinksJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
