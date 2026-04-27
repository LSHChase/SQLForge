package com.company.sqloptimization.domain.reportbatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportBatch {

    private final String batchId;
    private final String tenantId;
    private final String batchName;
    private final String fileType;
    private final String reportCodeField;
    private final String datasourceCode;
    private final String stage;
    private final String priority;
    private final String sourceType;
    private final List<ReportBatchStatusTransition> statusHistory;
    private ParseStatus status;
    private int totalReports;
    private int resolvedReports;
    private int failedReports;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public ReportBatch(String batchId,
                       String tenantId,
                       String batchName,
                       String fileType,
                       String reportCodeField,
                       String datasourceCode,
                       String stage,
                       String priority,
                       String sourceType,
                       String createdBy,
                       Instant createdAt) {
        this.batchId = batchId;
        this.tenantId = tenantId;
        this.batchName = batchName;
        this.fileType = fileType;
        this.reportCodeField = reportCodeField;
        this.datasourceCode = datasourceCode;
        this.stage = stage;
        this.priority = priority;
        this.sourceType = sourceType;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.statusHistory = new ArrayList<ReportBatchStatusTransition>();
    }

    public static ReportBatch initialize(String batchId,
                                         String tenantId,
                                         String batchName,
                                         String fileType,
                                         String reportCodeField,
                                         String datasourceCode,
                                         String stage,
                                         String priority,
                                         String sourceType,
                                         String createdBy,
                                         Instant createdAt) {
        ReportBatch batch = new ReportBatch(
            batchId,
            tenantId,
            batchName,
            fileType,
            reportCodeField,
            datasourceCode,
            stage,
            priority,
            sourceType,
            createdBy,
            createdAt
        );
        batch.transitionTo(ParseStatus.UPLOADED, createdAt, "REPORT_BATCH_CREATED");
        batch.transitionTo(ParseStatus.VALIDATING, createdAt, "REPORT_BATCH_VALIDATION_STARTED");
        batch.transitionTo(ParseStatus.READY, createdAt, "REPORT_BATCH_VALIDATION_PASSED");
        return batch;
    }

    public static ReportBatch restore(String batchId,
                                      String tenantId,
                                      String batchName,
                                      String fileType,
                                      String reportCodeField,
                                      String datasourceCode,
                                      String stage,
                                      String priority,
                                      String sourceType,
                                      ParseStatus status,
                                      int totalReports,
                                      int resolvedReports,
                                      int failedReports,
                                      String createdBy,
                                      Instant createdAt,
                                      Instant updatedAt,
                                      List<ReportBatchStatusTransition> statusHistory) {
        ReportBatch batch = new ReportBatch(
            batchId,
            tenantId,
            batchName,
            fileType,
            reportCodeField,
            datasourceCode,
            stage,
            priority,
            sourceType,
            createdBy,
            createdAt
        );
        batch.status = status;
        batch.totalReports = totalReports;
        batch.resolvedReports = resolvedReports;
        batch.failedReports = failedReports;
        batch.updatedAt = updatedAt;
        batch.statusHistory.clear();
        if (statusHistory != null) {
            batch.statusHistory.addAll(statusHistory);
        }
        return batch;
    }

    public void transitionTo(ParseStatus nextStatus, Instant occurredAt, String note) {
        ParseStatus previous = this.status;
        this.status = nextStatus;
        this.updatedAt = occurredAt;
        this.statusHistory.add(new ReportBatchStatusTransition(previous, nextStatus, occurredAt, note));
    }

    public void applySummary(int totalReports,
                             int resolvedReports,
                             int failedReports,
                             ParseStatus terminalStatus,
                             Instant occurredAt,
                             String note) {
        this.totalReports = totalReports;
        this.resolvedReports = resolvedReports;
        this.failedReports = failedReports;
        transitionTo(terminalStatus, occurredAt, note);
    }

    public void recordImportedItems(int totalReports, Instant occurredAt) {
        this.totalReports = totalReports;
        this.resolvedReports = 0;
        this.failedReports = 0;
        this.updatedAt = occurredAt;
    }

    public String getBatchId() { return batchId; }
    public String getTenantId() { return tenantId; }
    public String getBatchName() { return batchName; }
    public String getFileType() { return fileType; }
    public String getReportCodeField() { return reportCodeField; }
    public String getDatasourceCode() { return datasourceCode; }
    public String getStage() { return stage; }
    public String getPriority() { return priority; }
    public String getSourceType() { return sourceType; }
    public ParseStatus getStatus() { return status; }
    public int getTotalReports() { return totalReports; }
    public int getResolvedReports() { return resolvedReports; }
    public int getFailedReports() { return failedReports; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ReportBatchStatusTransition> getStatusHistory() { return Collections.unmodifiableList(statusHistory); }

    public enum ParseStatus {
        UPLOADED,
        VALIDATING,
        READY,
        RESOLVING_SQLS,
        COMPLETED,
        PARTIAL_COMPLETED,
        FAILED
    }
}
