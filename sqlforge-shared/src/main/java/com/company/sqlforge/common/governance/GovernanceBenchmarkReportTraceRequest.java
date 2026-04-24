package com.company.sqlforge.common.governance;

import java.util.List;

public class GovernanceBenchmarkReportTraceRequest {

    private String reportId;
    private String taskId;
    private String taskType;
    private String sqlFingerprint;
    private String sqlText;
    private String resultStatus;
    private String readonlyRequired;
    private String shadowEnvironmentMode;
    private String desensitizationRequirement;
    private String generatedAt;
    private String startedAt;
    private String finishedAt;
    private String reportQueryPath;
    private String rawDataDownloadPath;
    private String workloadDigest;
    private String workloadSource;
    private Boolean backfillApplied;
    private String workloadEvidenceJson;
    private String executionSummaryJson;
    private List<String> targetEngines;
    private List<GovernanceBenchmarkArtifactTraceRequest> artifacts;

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

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getReadonlyRequired() {
        return readonlyRequired;
    }

    public void setReadonlyRequired(String readonlyRequired) {
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

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
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

    public String getWorkloadDigest() {
        return workloadDigest;
    }

    public void setWorkloadDigest(String workloadDigest) {
        this.workloadDigest = workloadDigest;
    }

    public String getWorkloadSource() {
        return workloadSource;
    }

    public void setWorkloadSource(String workloadSource) {
        this.workloadSource = workloadSource;
    }

    public Boolean getBackfillApplied() {
        return backfillApplied;
    }

    public void setBackfillApplied(Boolean backfillApplied) {
        this.backfillApplied = backfillApplied;
    }

    public String getWorkloadEvidenceJson() {
        return workloadEvidenceJson;
    }

    public void setWorkloadEvidenceJson(String workloadEvidenceJson) {
        this.workloadEvidenceJson = workloadEvidenceJson;
    }

    public String getExecutionSummaryJson() {
        return executionSummaryJson;
    }

    public void setExecutionSummaryJson(String executionSummaryJson) {
        this.executionSummaryJson = executionSummaryJson;
    }

    public List<String> getTargetEngines() {
        return targetEngines;
    }

    public void setTargetEngines(List<String> targetEngines) {
        this.targetEngines = targetEngines;
    }

    public List<GovernanceBenchmarkArtifactTraceRequest> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<GovernanceBenchmarkArtifactTraceRequest> artifacts) {
        this.artifacts = artifacts;
    }
}
