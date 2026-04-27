package com.company.sqloptimization.domain.batch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParseBatchItem {

    private final String itemId;
    private final String batchId;
    private final int sequenceNumber;
    private final String reportCode;
    private final String reportName;
    private final String datasourceCode;
    private final String stage;
    private final String bizDate;
    private final String priority;
    private final String owner;
    private final String tags;
    private final String sqlText;
    private final String sqlTemplateText;
    private final String bindParametersJson;
    private final String bindingMode;
    private final Instant createdAt;
    private Instant updatedAt;
    private ParseBatchItemStatus status;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private String accessServiceStatus;
    private String accessConnectionStatus;
    private String failureReason;
    private final List<String> issueScenes;
    private final List<String> logicalObjectKeys;

    public ParseBatchItem(String itemId,
                          String batchId,
                          int sequenceNumber,
                          String reportCode,
                          String reportName,
                          String datasourceCode,
                          String stage,
                          String bizDate,
                          String priority,
                          String owner,
                          String tags,
                          String sqlText,
                          String sqlTemplateText,
                          String bindParametersJson,
                          String bindingMode,
                          Instant createdAt) {
        this.itemId = itemId;
        this.batchId = batchId;
        this.sequenceNumber = sequenceNumber;
        this.reportCode = reportCode;
        this.reportName = reportName;
        this.datasourceCode = datasourceCode;
        this.stage = stage;
        this.bizDate = bizDate;
        this.priority = priority;
        this.owner = owner;
        this.tags = tags;
        this.sqlText = sqlText;
        this.sqlTemplateText = sqlTemplateText;
        this.bindParametersJson = bindParametersJson;
        this.bindingMode = bindingMode;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.issueScenes = new ArrayList<String>();
        this.logicalObjectKeys = new ArrayList<String>();
    }

    public static ParseBatchItem create(String itemId,
                                        String batchId,
                                        int sequenceNumber,
                                        String reportCode,
                                        String reportName,
                                        String datasourceCode,
                                        String stage,
                                        String bizDate,
                                        String priority,
                                        String owner,
                                        String tags,
                                        String sqlText,
                                        String sqlTemplateText,
                                        String bindParametersJson,
                                        String bindingMode,
                                        Instant createdAt) {
        return new ParseBatchItem(
            itemId,
            batchId,
            sequenceNumber,
            reportCode,
            reportName,
            datasourceCode,
            stage,
            bizDate,
            priority,
            owner,
            tags,
            sqlText,
            sqlTemplateText,
            bindParametersJson,
            bindingMode,
            createdAt
        );
    }

    public static ParseBatchItem restore(String itemId,
                                         String batchId,
                                         int sequenceNumber,
                                         String reportCode,
                                         String reportName,
                                         String datasourceCode,
                                         String stage,
                                         String bizDate,
                                         String priority,
                                         String owner,
                                         String tags,
                                         String sqlText,
                                         String sqlTemplateText,
                                         String bindParametersJson,
                                         String bindingMode,
                                         ParseBatchItemStatus status,
                                         String parseTaskId,
                                         String structureSyntaxStatus,
                                         String accessServiceStatus,
                                         String accessConnectionStatus,
                                         String failureReason,
                                         List<String> issueScenes,
                                         List<String> logicalObjectKeys,
                                         Instant createdAt,
                                         Instant updatedAt) {
        ParseBatchItem item = new ParseBatchItem(
            itemId,
            batchId,
            sequenceNumber,
            reportCode,
            reportName,
            datasourceCode,
            stage,
            bizDate,
            priority,
            owner,
            tags,
            sqlText,
            sqlTemplateText,
            bindParametersJson,
            bindingMode,
            createdAt
        );
        item.status = status;
        item.parseTaskId = parseTaskId;
        item.structureSyntaxStatus = structureSyntaxStatus;
        item.accessServiceStatus = accessServiceStatus;
        item.accessConnectionStatus = accessConnectionStatus;
        item.failureReason = failureReason;
        item.updatedAt = updatedAt;
        item.issueScenes.clear();
        if (issueScenes != null) {
            item.issueScenes.addAll(issueScenes);
        }
        item.logicalObjectKeys.clear();
        if (logicalObjectKeys != null) {
            item.logicalObjectKeys.addAll(logicalObjectKeys);
        }
        return item;
    }

    public void complete(String parseTaskId,
                         String structureSyntaxStatus,
                         String accessServiceStatus,
                         String accessConnectionStatus,
                         ParseBatchItemStatus status,
                         String failureReason,
                         List<String> issueScenes,
                         List<String> logicalObjectKeys,
                         Instant completedAt) {
        this.parseTaskId = parseTaskId;
        this.structureSyntaxStatus = structureSyntaxStatus;
        this.accessServiceStatus = accessServiceStatus;
        this.accessConnectionStatus = accessConnectionStatus;
        this.status = status;
        this.failureReason = failureReason;
        this.updatedAt = completedAt;
        this.issueScenes.clear();
        if (issueScenes != null) {
            this.issueScenes.addAll(issueScenes);
        }
        this.logicalObjectKeys.clear();
        if (logicalObjectKeys != null) {
            this.logicalObjectKeys.addAll(logicalObjectKeys);
        }
    }

    public String getItemId() { return itemId; }
    public String getBatchId() { return batchId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getReportCode() { return reportCode; }
    public String getReportName() { return reportName; }
    public String getDatasourceCode() { return datasourceCode; }
    public String getStage() { return stage; }
    public String getBizDate() { return bizDate; }
    public String getPriority() { return priority; }
    public String getOwner() { return owner; }
    public String getTags() { return tags; }
    public String getSqlText() { return sqlText; }
    public String getSqlTemplateText() { return sqlTemplateText; }
    public String getBindParametersJson() { return bindParametersJson; }
    public String getBindingMode() { return bindingMode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public ParseBatchItemStatus getStatus() { return status; }
    public String getParseTaskId() { return parseTaskId; }
    public String getStructureSyntaxStatus() { return structureSyntaxStatus; }
    public String getAccessServiceStatus() { return accessServiceStatus; }
    public String getAccessConnectionStatus() { return accessConnectionStatus; }
    public String getFailureReason() { return failureReason; }
    public List<String> getIssueScenes() { return Collections.unmodifiableList(issueScenes); }
    public List<String> getLogicalObjectKeys() { return Collections.unmodifiableList(logicalObjectKeys); }
}
