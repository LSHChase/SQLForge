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
    private String sqlText;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private String accessServiceStatus;
    private String accessConnectionStatus;
    private String failureReason;
    private String status;
    private List<String> issueScenes;
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getIssueScenes() { return issueScenes; }
    public void setIssueScenes(List<String> issueScenes) { this.issueScenes = issueScenes; }
    public List<String> getLogicalObjectKeys() { return logicalObjectKeys; }
    public void setLogicalObjectKeys(List<String> logicalObjectKeys) { this.logicalObjectKeys = logicalObjectKeys; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
