package com.company.sqloptimization.domain.reportbatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportBatchItem {

    private final String itemId;
    private final String batchId;
    private final int sequenceNumber;
    private final String reportCode;
    private final String reportName;
    private final String datasourceCode;
    private final String stage;
    private final String priority;
    private final String sourceFileLine;
    private final Instant createdAt;
    private Instant updatedAt;
    private String sqlText;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private String accessServiceStatus;
    private String accessConnectionStatus;
    private String failureReason;
    private Status status;
    private final List<String> issueScenes;
    private final List<String> logicalObjectKeys;

    public ReportBatchItem(String itemId,
                           String batchId,
                           int sequenceNumber,
                           String reportCode,
                           String reportName,
                           String datasourceCode,
                           String stage,
                           String priority,
                           String sourceFileLine,
                           Instant createdAt) {
        this.itemId = itemId;
        this.batchId = batchId;
        this.sequenceNumber = sequenceNumber;
        this.reportCode = reportCode;
        this.reportName = reportName;
        this.datasourceCode = datasourceCode;
        this.stage = stage;
        this.priority = priority;
        this.sourceFileLine = sourceFileLine;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.issueScenes = new ArrayList<String>();
        this.logicalObjectKeys = new ArrayList<String>();
    }

    public static ReportBatchItem create(String itemId,
                                         String batchId,
                                         int sequenceNumber,
                                         String reportCode,
                                         String reportName,
                                         String datasourceCode,
                                         String stage,
                                         String priority,
                                         String sourceFileLine,
                                         Instant createdAt) {
        return new ReportBatchItem(itemId, batchId, sequenceNumber, reportCode, reportName, datasourceCode, stage, priority, sourceFileLine, createdAt);
    }

    public void complete(String sqlText,
                         String parseTaskId,
                         String structureSyntaxStatus,
                         String accessServiceStatus,
                         String accessConnectionStatus,
                         Status status,
                         String failureReason,
                         List<String> issueScenes,
                         List<String> logicalObjectKeys,
                         Instant occurredAt) {
        this.sqlText = sqlText;
        this.parseTaskId = parseTaskId;
        this.structureSyntaxStatus = structureSyntaxStatus;
        this.accessServiceStatus = accessServiceStatus;
        this.accessConnectionStatus = accessConnectionStatus;
        this.status = status;
        this.failureReason = failureReason;
        this.updatedAt = occurredAt;
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
    public String getPriority() { return priority; }
    public String getSourceFileLine() { return sourceFileLine; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getSqlText() { return sqlText; }
    public String getParseTaskId() { return parseTaskId; }
    public String getStructureSyntaxStatus() { return structureSyntaxStatus; }
    public String getAccessServiceStatus() { return accessServiceStatus; }
    public String getAccessConnectionStatus() { return accessConnectionStatus; }
    public String getFailureReason() { return failureReason; }
    public Status getStatus() { return status; }
    public List<String> getIssueScenes() { return Collections.unmodifiableList(issueScenes); }
    public List<String> getLogicalObjectKeys() { return Collections.unmodifiableList(logicalObjectKeys); }

    public enum Status {
        RESOLVED,
        PARTIAL_RESOLVED,
        FAILED
    }
}
