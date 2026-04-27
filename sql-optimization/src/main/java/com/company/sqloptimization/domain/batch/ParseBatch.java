package com.company.sqloptimization.domain.batch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParseBatch {

    private final String batchId;
    private final String tenantId;
    private final String batchName;
    private final ParseBatchImportMode importMode;
    private final ParseBatchSourceType sourceType;
    private final ParseBatchFileType fileType;
    private final String templateVersion;
    private final String datasourceCode;
    private final boolean structureParseOnly;
    private final List<ParseBatchStatusTransition> statusHistory;
    private ParseBatchStatus status;
    private int totalRecords;
    private int successRecords;
    private int partialSuccessRecords;
    private int failedRecords;
    private Double structureParseSuccessRate;
    private Double accessParseSuccessRate;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public ParseBatch(String batchId,
                      String tenantId,
                      String batchName,
                      ParseBatchImportMode importMode,
                      ParseBatchSourceType sourceType,
                      ParseBatchFileType fileType,
                      String templateVersion,
                      String datasourceCode,
                      boolean structureParseOnly,
                      String createdBy,
                      Instant createdAt) {
        this.batchId = batchId;
        this.tenantId = tenantId;
        this.batchName = batchName;
        this.importMode = importMode;
        this.sourceType = sourceType;
        this.fileType = fileType;
        this.templateVersion = templateVersion;
        this.datasourceCode = datasourceCode;
        this.structureParseOnly = structureParseOnly;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.statusHistory = new ArrayList<ParseBatchStatusTransition>();
    }

    public static ParseBatch initialize(String batchId,
                                        String tenantId,
                                        String batchName,
                                        ParseBatchImportMode importMode,
                                        ParseBatchSourceType sourceType,
                                        ParseBatchFileType fileType,
                                        String templateVersion,
                                        String datasourceCode,
                                        boolean structureParseOnly,
                                        String createdBy,
                                        Instant createdAt) {
        ParseBatch batch = new ParseBatch(
            batchId,
            tenantId,
            batchName,
            importMode,
            sourceType,
            fileType,
            templateVersion,
            datasourceCode,
            structureParseOnly,
            createdBy,
            createdAt
        );
        batch.transitionTo(ParseBatchStatus.UPLOADED, createdAt, "BATCH_CREATED");
        batch.transitionTo(ParseBatchStatus.VALIDATING, createdAt, "CONTRACT_VALIDATION_STARTED");
        batch.transitionTo(ParseBatchStatus.READY, createdAt, "CONTRACT_VALIDATION_PASSED");
        batch.structureParseSuccessRate = Double.valueOf(0.0D);
        batch.accessParseSuccessRate = Double.valueOf(0.0D);
        return batch;
    }

    public static ParseBatch restore(String batchId,
                                     String tenantId,
                                     String batchName,
                                     ParseBatchImportMode importMode,
                                     ParseBatchSourceType sourceType,
                                     ParseBatchFileType fileType,
                                     String templateVersion,
                                     String datasourceCode,
                                     boolean structureParseOnly,
                                     ParseBatchStatus status,
                                     int totalRecords,
                                     int successRecords,
                                     int partialSuccessRecords,
                                     int failedRecords,
                                     Double structureParseSuccessRate,
                                     Double accessParseSuccessRate,
                                     String createdBy,
                                     Instant createdAt,
                                     Instant updatedAt,
                                     List<ParseBatchStatusTransition> statusHistory) {
        ParseBatch batch = new ParseBatch(
            batchId,
            tenantId,
            batchName,
            importMode,
            sourceType,
            fileType,
            templateVersion,
            datasourceCode,
            structureParseOnly,
            createdBy,
            createdAt
        );
        batch.status = status;
        batch.totalRecords = totalRecords;
        batch.successRecords = successRecords;
        batch.partialSuccessRecords = partialSuccessRecords;
        batch.failedRecords = failedRecords;
        batch.structureParseSuccessRate = structureParseSuccessRate;
        batch.accessParseSuccessRate = accessParseSuccessRate;
        batch.updatedAt = updatedAt;
        batch.statusHistory.clear();
        if (statusHistory != null) {
            batch.statusHistory.addAll(statusHistory);
        }
        return batch;
    }

    public void transitionTo(ParseBatchStatus nextStatus, Instant occurredAt, String note) {
        ParseBatchStatus previous = this.status;
        this.status = nextStatus;
        this.updatedAt = occurredAt;
        this.statusHistory.add(new ParseBatchStatusTransition(previous, nextStatus, occurredAt, note));
    }

    public void applyRunSummary(int totalRecords,
                                int successRecords,
                                int partialSuccessRecords,
                                int failedRecords,
                                Double structureParseSuccessRate,
                                Double accessParseSuccessRate,
                                ParseBatchStatus terminalStatus,
                                Instant occurredAt,
                                String note) {
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.partialSuccessRecords = partialSuccessRecords;
        this.failedRecords = failedRecords;
        this.structureParseSuccessRate = structureParseSuccessRate;
        this.accessParseSuccessRate = accessParseSuccessRate;
        transitionTo(terminalStatus, occurredAt, note);
    }

    public String getBatchId() { return batchId; }
    public String getTenantId() { return tenantId; }
    public String getBatchName() { return batchName; }
    public ParseBatchImportMode getImportMode() { return importMode; }
    public ParseBatchSourceType getSourceType() { return sourceType; }
    public ParseBatchFileType getFileType() { return fileType; }
    public String getTemplateVersion() { return templateVersion; }
    public String getDatasourceCode() { return datasourceCode; }
    public boolean isStructureParseOnly() { return structureParseOnly; }
    public ParseBatchStatus getStatus() { return status; }
    public int getTotalRecords() { return totalRecords; }
    public int getSuccessRecords() { return successRecords; }
    public int getPartialSuccessRecords() { return partialSuccessRecords; }
    public int getFailedRecords() { return failedRecords; }
    public Double getStructureParseSuccessRate() { return structureParseSuccessRate; }
    public Double getAccessParseSuccessRate() { return accessParseSuccessRate; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ParseBatchStatusTransition> getStatusHistory() { return Collections.unmodifiableList(statusHistory); }
}
