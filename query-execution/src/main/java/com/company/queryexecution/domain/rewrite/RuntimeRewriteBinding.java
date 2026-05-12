package com.company.queryexecution.domain.rewrite;

import java.time.Instant;

public class RuntimeRewriteBinding {

    private final String runtimeBindingId;
    private final String tenantId;
    private final String rewriteRecordId;
    private final String recommendationId;
    private final String sourceType;
    private final String sourceKind;
    private final String sourceId;
    private final String sqlFingerprint;
    private final String originalSqlDigest;
    private final String recommendedSqlText;
    private final String datasourceCode;
    private final RuntimeRewriteBindingStatus status;
    private final long ruleVersion;
    private final String runtimeRuleVersion;
    private final String publishedBy;
    private final Instant publishedAt;
    private final String pausedBy;
    private final Instant pausedAt;
    private final String pauseReason;
    private final String unpublishedBy;
    private final Instant unpublishedAt;
    private final String unpublishReason;
    private final Instant createdAt;
    private final Instant updatedAt;

    private RuntimeRewriteBinding(Builder builder) {
        this.runtimeBindingId = requireText(builder.runtimeBindingId, "runtimeBindingId");
        this.tenantId = requireText(builder.tenantId, "tenantId");
        this.rewriteRecordId = requireText(builder.rewriteRecordId, "rewriteRecordId");
        this.recommendationId = trimToNull(builder.recommendationId);
        this.sourceType = requireText(builder.sourceType, "sourceType");
        this.sourceKind = requireText(builder.sourceKind, "sourceKind");
        this.sourceId = requireText(builder.sourceId, "sourceId");
        this.sqlFingerprint = requireText(builder.sqlFingerprint, "sqlFingerprint");
        this.originalSqlDigest = requireText(builder.originalSqlDigest, "originalSqlDigest");
        this.recommendedSqlText = requireText(builder.recommendedSqlText, "recommendedSqlText");
        this.datasourceCode = requireText(builder.datasourceCode, "datasourceCode");
        this.status = builder.status == null ? RuntimeRewriteBindingStatus.ACTIVE : builder.status;
        this.ruleVersion = builder.ruleVersion <= 0 ? 1L : builder.ruleVersion;
        this.runtimeRuleVersion = requireText(builder.runtimeRuleVersion, "runtimeRuleVersion");
        this.publishedBy = requireText(builder.publishedBy, "publishedBy");
        this.publishedAt = builder.publishedAt == null ? Instant.now() : builder.publishedAt;
        this.pausedBy = trimToNull(builder.pausedBy);
        this.pausedAt = builder.pausedAt;
        this.pauseReason = trimToNull(builder.pauseReason);
        this.unpublishedBy = trimToNull(builder.unpublishedBy);
        this.unpublishedAt = builder.unpublishedAt;
        this.unpublishReason = trimToNull(builder.unpublishReason);
        this.createdAt = builder.createdAt == null ? this.publishedAt : builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? this.createdAt : builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public RuntimeRewriteBinding pause(String operatorId, String reason, Instant now) {
        if (status == RuntimeRewriteBindingStatus.PAUSED || status == RuntimeRewriteBindingStatus.UNPUBLISHED) {
            return this;
        }
        Instant changedAt = now == null ? Instant.now() : now;
        return copyBuilder()
            .status(RuntimeRewriteBindingStatus.PAUSED)
            .pausedBy(requireText(operatorId, "operatorId"))
            .pausedAt(changedAt)
            .pauseReason(trimToNull(reason))
            .updatedAt(changedAt)
            .build();
    }

    public RuntimeRewriteBinding unpublish(String operatorId, String reason, Instant now) {
        if (status == RuntimeRewriteBindingStatus.UNPUBLISHED) {
            return this;
        }
        Instant changedAt = now == null ? Instant.now() : now;
        return copyBuilder()
            .status(RuntimeRewriteBindingStatus.UNPUBLISHED)
            .unpublishedBy(requireText(operatorId, "operatorId"))
            .unpublishedAt(changedAt)
            .unpublishReason(trimToNull(reason))
            .updatedAt(changedAt)
            .build();
    }

    public boolean isActive() {
        return status == RuntimeRewriteBindingStatus.ACTIVE;
    }

    private Builder copyBuilder() {
        return RuntimeRewriteBinding.builder()
            .runtimeBindingId(runtimeBindingId)
            .tenantId(tenantId)
            .rewriteRecordId(rewriteRecordId)
            .recommendationId(recommendationId)
            .sourceType(sourceType)
            .sourceKind(sourceKind)
            .sourceId(sourceId)
            .sqlFingerprint(sqlFingerprint)
            .originalSqlDigest(originalSqlDigest)
            .recommendedSqlText(recommendedSqlText)
            .datasourceCode(datasourceCode)
            .status(status)
            .ruleVersion(ruleVersion)
            .runtimeRuleVersion(runtimeRuleVersion)
            .publishedBy(publishedBy)
            .publishedAt(publishedAt)
            .pausedBy(pausedBy)
            .pausedAt(pausedAt)
            .pauseReason(pauseReason)
            .unpublishedBy(unpublishedBy)
            .unpublishedAt(unpublishedAt)
            .unpublishReason(unpublishReason)
            .createdAt(createdAt)
            .updatedAt(updatedAt);
    }

    private static String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    public String getRuntimeBindingId() { return runtimeBindingId; }
    public String getTenantId() { return tenantId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public String getRecommendationId() { return recommendationId; }
    public String getSourceType() { return sourceType; }
    public String getSourceKind() { return sourceKind; }
    public String getSourceId() { return sourceId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public String getOriginalSqlDigest() { return originalSqlDigest; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public String getDatasourceCode() { return datasourceCode; }
    public RuntimeRewriteBindingStatus getStatus() { return status; }
    public long getRuleVersion() { return ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public String getPublishedBy() { return publishedBy; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getPausedBy() { return pausedBy; }
    public Instant getPausedAt() { return pausedAt; }
    public String getPauseReason() { return pauseReason; }
    public String getUnpublishedBy() { return unpublishedBy; }
    public Instant getUnpublishedAt() { return unpublishedAt; }
    public String getUnpublishReason() { return unpublishReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static class Builder {
        private String runtimeBindingId;
        private String tenantId;
        private String rewriteRecordId;
        private String recommendationId;
        private String sourceType;
        private String sourceKind;
        private String sourceId;
        private String sqlFingerprint;
        private String originalSqlDigest;
        private String recommendedSqlText;
        private String datasourceCode;
        private RuntimeRewriteBindingStatus status;
        private long ruleVersion;
        private String runtimeRuleVersion;
        private String publishedBy;
        private Instant publishedAt;
        private String pausedBy;
        private Instant pausedAt;
        private String pauseReason;
        private String unpublishedBy;
        private Instant unpublishedAt;
        private String unpublishReason;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder runtimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder rewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; return this; }
        public Builder recommendationId(String recommendationId) { this.recommendationId = recommendationId; return this; }
        public Builder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public Builder sourceKind(String sourceKind) { this.sourceKind = sourceKind; return this; }
        public Builder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder originalSqlDigest(String originalSqlDigest) { this.originalSqlDigest = originalSqlDigest; return this; }
        public Builder recommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; return this; }
        public Builder datasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; return this; }
        public Builder status(RuntimeRewriteBindingStatus status) { this.status = status; return this; }
        public Builder ruleVersion(long ruleVersion) { this.ruleVersion = ruleVersion; return this; }
        public Builder runtimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; return this; }
        public Builder publishedBy(String publishedBy) { this.publishedBy = publishedBy; return this; }
        public Builder publishedAt(Instant publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder pausedBy(String pausedBy) { this.pausedBy = pausedBy; return this; }
        public Builder pausedAt(Instant pausedAt) { this.pausedAt = pausedAt; return this; }
        public Builder pauseReason(String pauseReason) { this.pauseReason = pauseReason; return this; }
        public Builder unpublishedBy(String unpublishedBy) { this.unpublishedBy = unpublishedBy; return this; }
        public Builder unpublishedAt(Instant unpublishedAt) { this.unpublishedAt = unpublishedAt; return this; }
        public Builder unpublishReason(String unpublishReason) { this.unpublishReason = unpublishReason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public RuntimeRewriteBinding build() { return new RuntimeRewriteBinding(this); }
    }
}
