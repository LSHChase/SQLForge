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
    private final String sqlColumnName;
    private final Integer sqlOrdinalInReport;
    private final Instant createdAt;
    private Instant updatedAt;
    private String sqlText;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private String accessServiceStatus;
    private String accessConnectionStatus;
    private String planAnalysisStatus;
    private String combinedAnalysisStatus;
    private String planAnalysisJson;
    private String failureReason;
    private String historyId;
    private Boolean historyPersisted;
    private String historyPersistenceStatus;
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
                           String sqlColumnName,
                           Integer sqlOrdinalInReport,
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
        this.sqlColumnName = sqlColumnName;
        this.sqlOrdinalInReport = sqlOrdinalInReport;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = Status.PENDING;
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
                                         String sqlColumnName,
                                         Integer sqlOrdinalInReport,
                                         Instant createdAt) {
        return new ReportBatchItem(
            itemId,
            batchId,
            sequenceNumber,
            reportCode,
            reportName,
            datasourceCode,
            stage,
            priority,
            sourceFileLine,
            sqlColumnName,
            sqlOrdinalInReport,
            createdAt
        );
    }

    public static ReportBatchItem restore(String itemId,
                                          String batchId,
                                          int sequenceNumber,
                                          String reportCode,
                                          String reportName,
                                          String datasourceCode,
                                          String stage,
                                          String priority,
                                          String sourceFileLine,
                                          String sqlColumnName,
                                          Integer sqlOrdinalInReport,
                                          String sqlText,
                                          String parseTaskId,
                                          String structureSyntaxStatus,
                                          String accessServiceStatus,
                                          String accessConnectionStatus,
                                          String planAnalysisStatus,
                                          String combinedAnalysisStatus,
                                          String planAnalysisJson,
                                          String failureReason,
                                          String historyId,
                                          Boolean historyPersisted,
                                          String historyPersistenceStatus,
                                          Status status,
                                          List<String> issueScenes,
                                          List<String> logicalObjectKeys,
                                          Instant createdAt,
                                          Instant updatedAt) {
        ReportBatchItem item = new ReportBatchItem(
            itemId,
            batchId,
            sequenceNumber,
            reportCode,
            reportName,
            datasourceCode,
            stage,
            priority,
            sourceFileLine,
            sqlColumnName,
            sqlOrdinalInReport,
            createdAt
        );
        item.sqlText = sqlText;
        item.parseTaskId = parseTaskId;
        item.structureSyntaxStatus = structureSyntaxStatus;
        item.accessServiceStatus = accessServiceStatus;
        item.accessConnectionStatus = accessConnectionStatus;
        item.planAnalysisStatus = planAnalysisStatus;
        item.combinedAnalysisStatus = combinedAnalysisStatus;
        item.planAnalysisJson = planAnalysisJson;
        item.failureReason = failureReason;
        item.historyId = historyId;
        item.historyPersisted = historyPersisted;
        item.historyPersistenceStatus = historyPersistenceStatus;
        item.status = status;
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

    public void recordHistory(String historyId,
                              Boolean historyPersisted,
                              String historyPersistenceStatus,
                              Instant occurredAt) {
        this.historyId = historyId;
        this.historyPersisted = historyPersisted;
        this.historyPersistenceStatus = historyPersistenceStatus;
        this.updatedAt = occurredAt;
    }

    public void recordPlanAnalysis(String planAnalysisStatus,
                                   String combinedAnalysisStatus,
                                   String planAnalysisJson,
                                   Instant occurredAt) {
        this.planAnalysisStatus = planAnalysisStatus;
        this.combinedAnalysisStatus = combinedAnalysisStatus;
        this.planAnalysisJson = planAnalysisJson;
        this.updatedAt = occurredAt;
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
    public String getSqlColumnName() { return sqlColumnName; }
    public Integer getSqlOrdinalInReport() { return sqlOrdinalInReport; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getSqlText() { return sqlText; }
    public String getParseTaskId() { return parseTaskId; }
    public String getStructureSyntaxStatus() { return structureSyntaxStatus; }
    public String getAccessServiceStatus() { return accessServiceStatus; }
    public String getAccessConnectionStatus() { return accessConnectionStatus; }
    public String getPlanAnalysisStatus() { return planAnalysisStatus; }
    public String getCombinedAnalysisStatus() { return combinedAnalysisStatus; }
    public String getPlanAnalysisJson() { return planAnalysisJson; }
    public String getFailureReason() { return failureReason; }
    public String getHistoryId() { return historyId; }
    public Boolean getHistoryPersisted() { return historyPersisted; }
    public String getHistoryPersistenceStatus() { return historyPersistenceStatus; }
    public Status getStatus() { return status; }
    public List<String> getIssueScenes() { return Collections.unmodifiableList(issueScenes); }
    public List<String> getLogicalObjectKeys() { return Collections.unmodifiableList(logicalObjectKeys); }

    public enum Status {
        PENDING,
        RESOLVED,
        PARTIAL_RESOLVED,
        FAILED
    }
}
