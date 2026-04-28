package com.company.sqlforge.common.governance;

public class GovernanceBenchmarkRegressionAlertRequest {

    private String tenantId;
    private String reportId;
    private String taskId;
    private String historyId;
    private String sqlFingerprint;
    private String verdict;
    private Integer thresholdHitCount;
    private Integer failedThresholdCount;
    private Integer warningThresholdCount;
    private String summary;
    private String reportQueryPath;
    private String rawDataDownloadPath;
    private String thresholdAssessmentsJson;
    private String executionSummaryJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Integer getThresholdHitCount() {
        return thresholdHitCount;
    }

    public void setThresholdHitCount(Integer thresholdHitCount) {
        this.thresholdHitCount = thresholdHitCount;
    }

    public Integer getFailedThresholdCount() {
        return failedThresholdCount;
    }

    public void setFailedThresholdCount(Integer failedThresholdCount) {
        this.failedThresholdCount = failedThresholdCount;
    }

    public Integer getWarningThresholdCount() {
        return warningThresholdCount;
    }

    public void setWarningThresholdCount(Integer warningThresholdCount) {
        this.warningThresholdCount = warningThresholdCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getReportQueryPath() {
        return reportQueryPath;
    }

    public void setReportQueryPath(String reportQueryPath) {
        this.reportQueryPath = reportQueryPath;
    }

    public String getRawDataDownloadPath() {
        return rawDataDownloadPath;
    }

    public void setRawDataDownloadPath(String rawDataDownloadPath) {
        this.rawDataDownloadPath = rawDataDownloadPath;
    }

    public String getThresholdAssessmentsJson() {
        return thresholdAssessmentsJson;
    }

    public void setThresholdAssessmentsJson(String thresholdAssessmentsJson) {
        this.thresholdAssessmentsJson = thresholdAssessmentsJson;
    }

    public String getExecutionSummaryJson() {
        return executionSummaryJson;
    }

    public void setExecutionSummaryJson(String executionSummaryJson) {
        this.executionSummaryJson = executionSummaryJson;
    }
}
