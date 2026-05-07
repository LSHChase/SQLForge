package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class ReportBatchItemVO {

    private String itemId;
    private Integer sequenceNumber;
    private String reportCode;
    private String reportName;
    private String datasourceCode;
    private String stage;
    private String priority;
    private String sourceFileLine;
    private String sqlColumnName;
    private Integer sqlOrdinalInReport;
    private String sqlText;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private String accessServiceStatus;
    private String accessConnectionStatus;
    private String failureReason;
    private String historyId;
    private Boolean historyPersisted;
    private String historyPersistenceStatus;
    private Integer failureLine;
    private Integer failureColumn;
    private Integer failureOffset;
    private String failureToken;
    private String failureSnippet;
    private String diagnosticSummary;
    private String status;
    private List<String> issueScenes;
    private List<ReportBatchIssueLocationVO> issueLocations;
    private List<String> logicalObjectKeys;
    private Instant createdAt;
    private Instant updatedAt;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSourceFileLine() { return sourceFileLine; }
    public void setSourceFileLine(String sourceFileLine) { this.sourceFileLine = sourceFileLine; }
    public String getSqlColumnName() { return sqlColumnName; }
    public void setSqlColumnName(String sqlColumnName) { this.sqlColumnName = sqlColumnName; }
    public Integer getSqlOrdinalInReport() { return sqlOrdinalInReport; }
    public void setSqlOrdinalInReport(Integer sqlOrdinalInReport) { this.sqlOrdinalInReport = sqlOrdinalInReport; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getStructureSyntaxStatus() { return structureSyntaxStatus; }
    public void setStructureSyntaxStatus(String structureSyntaxStatus) { this.structureSyntaxStatus = structureSyntaxStatus; }
    public String getAccessServiceStatus() { return accessServiceStatus; }
    public void setAccessServiceStatus(String accessServiceStatus) { this.accessServiceStatus = accessServiceStatus; }
    public String getAccessConnectionStatus() { return accessConnectionStatus; }
    public void setAccessConnectionStatus(String accessConnectionStatus) { this.accessConnectionStatus = accessConnectionStatus; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public Boolean getHistoryPersisted() { return historyPersisted; }
    public void setHistoryPersisted(Boolean historyPersisted) { this.historyPersisted = historyPersisted; }
    public String getHistoryPersistenceStatus() { return historyPersistenceStatus; }
    public void setHistoryPersistenceStatus(String historyPersistenceStatus) {
        this.historyPersistenceStatus = historyPersistenceStatus;
    }
    public Integer getFailureLine() { return failureLine; }
    public void setFailureLine(Integer failureLine) { this.failureLine = failureLine; }
    public Integer getFailureColumn() { return failureColumn; }
    public void setFailureColumn(Integer failureColumn) { this.failureColumn = failureColumn; }
    public Integer getFailureOffset() { return failureOffset; }
    public void setFailureOffset(Integer failureOffset) { this.failureOffset = failureOffset; }
    public String getFailureToken() { return failureToken; }
    public void setFailureToken(String failureToken) { this.failureToken = failureToken; }
    public String getFailureSnippet() { return failureSnippet; }
    public void setFailureSnippet(String failureSnippet) { this.failureSnippet = failureSnippet; }
    public String getDiagnosticSummary() { return diagnosticSummary; }
    public void setDiagnosticSummary(String diagnosticSummary) { this.diagnosticSummary = diagnosticSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getIssueScenes() { return issueScenes; }
    public void setIssueScenes(List<String> issueScenes) { this.issueScenes = issueScenes; }
    public List<ReportBatchIssueLocationVO> getIssueLocations() { return issueLocations; }
    public void setIssueLocations(List<ReportBatchIssueLocationVO> issueLocations) {
        this.issueLocations = issueLocations;
    }
    public List<String> getLogicalObjectKeys() { return logicalObjectKeys; }
    public void setLogicalObjectKeys(List<String> logicalObjectKeys) { this.logicalObjectKeys = logicalObjectKeys; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
