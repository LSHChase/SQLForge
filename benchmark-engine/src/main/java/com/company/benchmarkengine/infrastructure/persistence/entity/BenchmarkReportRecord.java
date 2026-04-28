package com.company.benchmarkengine.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class BenchmarkReportRecord {

    private String reportId;
    private String taskId;
    private String tenantId;
    private String taskType;
    private String sqlFingerprint;
    private LocalDateTime generatedAt;
    private String verdict;
    private String engineProfilesJson;
    private String thresholdAssessmentsJson;
    private String recommendationsJson;
    private String executionSummaryJson;
    private String regressionSummaryJson;
    private String alertLinkagesJson;
    private String exportArtifactsJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

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

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getEngineProfilesJson() {
        return engineProfilesJson;
    }

    public void setEngineProfilesJson(String engineProfilesJson) {
        this.engineProfilesJson = engineProfilesJson;
    }

    public String getThresholdAssessmentsJson() {
        return thresholdAssessmentsJson;
    }

    public void setThresholdAssessmentsJson(String thresholdAssessmentsJson) {
        this.thresholdAssessmentsJson = thresholdAssessmentsJson;
    }

    public String getRecommendationsJson() {
        return recommendationsJson;
    }

    public void setRecommendationsJson(String recommendationsJson) {
        this.recommendationsJson = recommendationsJson;
    }

    public String getExecutionSummaryJson() {
        return executionSummaryJson;
    }

    public void setExecutionSummaryJson(String executionSummaryJson) {
        this.executionSummaryJson = executionSummaryJson;
    }

    public String getRegressionSummaryJson() {
        return regressionSummaryJson;
    }

    public void setRegressionSummaryJson(String regressionSummaryJson) {
        this.regressionSummaryJson = regressionSummaryJson;
    }

    public String getAlertLinkagesJson() {
        return alertLinkagesJson;
    }

    public void setAlertLinkagesJson(String alertLinkagesJson) {
        this.alertLinkagesJson = alertLinkagesJson;
    }

    public String getExportArtifactsJson() {
        return exportArtifactsJson;
    }

    public void setExportArtifactsJson(String exportArtifactsJson) {
        this.exportArtifactsJson = exportArtifactsJson;
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
