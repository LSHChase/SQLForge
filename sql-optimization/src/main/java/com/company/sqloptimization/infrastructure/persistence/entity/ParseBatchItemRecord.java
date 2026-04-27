package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class ParseBatchItemRecord {

    private String itemId;
    private String batchId;
    private Integer sequenceNumber;
    private String reportCode;
    private String reportName;
    private String datasourceCode;
    private String stage;
    private String bizDate;
    private String priority;
    private String owner;
    private String tags;
    private String sqlText;
    private String sqlTemplateText;
    private String bindParametersJson;
    private String bindingMode;
    private String status;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private String accessServiceStatus;
    private String accessConnectionStatus;
    private String failureReason;
    private String issueScenesJson;
    private String logicalObjectKeysJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
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
    public String getBizDate() { return bizDate; }
    public void setBizDate(String bizDate) { this.bizDate = bizDate; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlTemplateText() { return sqlTemplateText; }
    public void setSqlTemplateText(String sqlTemplateText) { this.sqlTemplateText = sqlTemplateText; }
    public String getBindParametersJson() { return bindParametersJson; }
    public void setBindParametersJson(String bindParametersJson) { this.bindParametersJson = bindParametersJson; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
    public String getIssueScenesJson() { return issueScenesJson; }
    public void setIssueScenesJson(String issueScenesJson) { this.issueScenesJson = issueScenesJson; }
    public String getLogicalObjectKeysJson() { return logicalObjectKeysJson; }
    public void setLogicalObjectKeysJson(String logicalObjectKeysJson) { this.logicalObjectKeysJson = logicalObjectKeysJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
