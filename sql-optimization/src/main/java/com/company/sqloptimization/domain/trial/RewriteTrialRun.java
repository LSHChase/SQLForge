package com.company.sqloptimization.domain.trial;

import java.time.Instant;

public class RewriteTrialRun {

    private final String runId;
    private final String tenantId;
    private final String sourceKind;
    private final String sourceId;
    private final String batchId;
    private RewriteTrialStatus status;
    private int totalCount;
    private int acceptedCount;
    private int skippedCount;
    private final String createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private RewriteTrialRun(Builder builder) {
        this.runId = builder.runId;
        this.tenantId = builder.tenantId;
        this.sourceKind = builder.sourceKind;
        this.sourceId = builder.sourceId;
        this.batchId = builder.batchId;
        this.status = builder.status == null ? RewriteTrialStatus.NOT_REQUESTED : builder.status;
        this.totalCount = builder.totalCount;
        this.acceptedCount = builder.acceptedCount;
        this.skippedCount = builder.skippedCount;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        requireText(runId, "runId");
        requireText(tenantId, "tenantId");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt 为必填项");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    public void refreshSummary(int totalCount,
                               int acceptedCount,
                               int skippedCount,
                               RewriteTrialStatus status,
                               Instant updatedAt) {
        this.totalCount = totalCount;
        this.acceptedCount = acceptedCount;
        this.skippedCount = skippedCount;
        this.status = status == null ? this.status : status;
        this.updatedAt = updatedAt;
    }

    public String getRunId() { return runId; }
    public String getTenantId() { return tenantId; }
    public String getSourceKind() { return sourceKind; }
    public String getSourceId() { return sourceId; }
    public String getBatchId() { return batchId; }
    public RewriteTrialStatus getStatus() { return status; }
    public int getTotalCount() { return totalCount; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getSkippedCount() { return skippedCount; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static final class Builder {
        private String runId;
        private String tenantId;
        private String sourceKind;
        private String sourceId;
        private String batchId;
        private RewriteTrialStatus status;
        private int totalCount;
        private int acceptedCount;
        private int skippedCount;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder runId(String runId) { this.runId = runId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder sourceKind(String sourceKind) { this.sourceKind = sourceKind; return this; }
        public Builder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public Builder batchId(String batchId) { this.batchId = batchId; return this; }
        public Builder status(RewriteTrialStatus status) { this.status = status; return this; }
        public Builder totalCount(int totalCount) { this.totalCount = totalCount; return this; }
        public Builder acceptedCount(int acceptedCount) { this.acceptedCount = acceptedCount; return this; }
        public Builder skippedCount(int skippedCount) { this.skippedCount = skippedCount; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public RewriteTrialRun build() {
            return new RewriteTrialRun(this);
        }
    }
}
