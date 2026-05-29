package com.company.queryexecution.domain.rewrite;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.time.Instant;
import java.util.List;

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
    private final String originalSqlText;
    private final String recommendedSqlText;
    private final String rewriteMatchMode;
    private final String rewriteProgramJson;
    private final String templateFamilyFingerprint;
    private final List<LogicalObjectSurface> runtimeMatchObjectRefs;
    private final List<String> runtimeMatchObjectNames;
    private final List<LogicalObjectSurface> analysisPhysicalObjectRefs;
    private final String metadataSnapshotVersion;
    private final String viewDefinitionHash;
    private final String metadataDegradationReason;
    private final String datasourceCode;
    private final RuntimeRewriteBindingStatus status;
    private final long ruleVersion;
    private final String runtimeRuleVersion;
    private final String activatedBy;
    private final Instant activatedAt;
    private final String pausedBy;
    private final Instant pausedAt;
    private final String pauseReason;
    private final Instant createdAt;
    private final Instant updatedAt;

    private RuntimeRewriteBinding(Builder builder) {
        this.runtimeBindingId = RuntimeRewriteBindingValues.requireText(builder.runtimeBindingId, "runtimeBindingId");
        this.tenantId = RuntimeRewriteBindingValues.requireText(builder.tenantId, "tenantId");
        this.rewriteRecordId = RuntimeRewriteBindingValues.requireText(builder.rewriteRecordId, "rewriteRecordId");
        this.recommendationId = RuntimeRewriteBindingValues.trimToNull(builder.recommendationId);
        this.sourceType = RuntimeRewriteBindingValues.requireText(builder.sourceType, "sourceType");
        this.sourceKind = RuntimeRewriteBindingValues.requireText(builder.sourceKind, "sourceKind");
        this.sourceId = RuntimeRewriteBindingValues.requireText(builder.sourceId, "sourceId");
        this.sqlFingerprint = RuntimeRewriteBindingValues.requireText(builder.sqlFingerprint, "sqlFingerprint");
        this.originalSqlDigest = RuntimeRewriteBindingValues.requireText(builder.originalSqlDigest, "originalSqlDigest");
        this.originalSqlText = RuntimeRewriteBindingValues.trimToNull(builder.originalSqlText);
        this.recommendedSqlText = RuntimeRewriteBindingValues.requireText(builder.recommendedSqlText, "recommendedSqlText");
        this.rewriteMatchMode = RuntimeRewriteBindingValues.trimToNull(builder.rewriteMatchMode) == null
            ? "EXACT_FINGERPRINT"
            : RuntimeRewriteBindingValues.trimToNull(builder.rewriteMatchMode);
        this.rewriteProgramJson = RuntimeRewriteBindingValues.trimToNull(builder.rewriteProgramJson);
        this.templateFamilyFingerprint = RuntimeRewriteBindingValues.trimToNull(builder.templateFamilyFingerprint);
        this.runtimeMatchObjectRefs = RuntimeRewriteBindingValues.immutableSurfaceList(builder.runtimeMatchObjectRefs);
        this.runtimeMatchObjectNames = RuntimeRewriteBindingValues.immutableStringList(builder.runtimeMatchObjectNames);
        this.analysisPhysicalObjectRefs = RuntimeRewriteBindingValues.immutableSurfaceList(builder.analysisPhysicalObjectRefs);
        this.metadataSnapshotVersion = RuntimeRewriteBindingValues.trimToNull(builder.metadataSnapshotVersion);
        this.viewDefinitionHash = RuntimeRewriteBindingValues.trimToNull(builder.viewDefinitionHash);
        this.metadataDegradationReason = RuntimeRewriteBindingValues.trimToNull(builder.metadataDegradationReason);
        this.datasourceCode = RuntimeRewriteBindingValues.requireText(builder.datasourceCode, "datasourceCode");
        this.status = builder.status == null ? RuntimeRewriteBindingStatus.ACTIVE : builder.status;
        this.ruleVersion = builder.ruleVersion <= 0 ? 1L : builder.ruleVersion;
        this.runtimeRuleVersion = RuntimeRewriteBindingValues.requireText(builder.runtimeRuleVersion, "runtimeRuleVersion");
        this.activatedBy = RuntimeRewriteBindingValues.requireText(builder.activatedBy, "activatedBy");
        this.activatedAt = builder.activatedAt == null ? Instant.now() : builder.activatedAt;
        this.pausedBy = RuntimeRewriteBindingValues.trimToNull(builder.pausedBy);
        this.pausedAt = builder.pausedAt;
        this.pauseReason = RuntimeRewriteBindingValues.trimToNull(builder.pauseReason);
        this.createdAt = builder.createdAt == null ? this.activatedAt : builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? this.createdAt : builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public RuntimeRewriteBinding pause(String actorId, String reason, Instant now) {
        if (status == RuntimeRewriteBindingStatus.PAUSED) {
            return this;
        }
        Instant changedAt = now == null ? Instant.now() : now;
        return RuntimeRewriteBindingCopyFactory.copy(this)
            .status(RuntimeRewriteBindingStatus.PAUSED)
            .pausedBy(RuntimeRewriteBindingValues.requireText(actorId, "actorId"))
            .pausedAt(changedAt)
            .pauseReason(RuntimeRewriteBindingValues.trimToNull(reason))
            .updatedAt(changedAt)
            .build();
    }

    public boolean isActive() {
        return status == RuntimeRewriteBindingStatus.ACTIVE;
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
    public String getOriginalSqlText() { return originalSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public String getRewriteMatchMode() { return rewriteMatchMode; }
    public String getRewriteProgramJson() { return rewriteProgramJson; }
    public String getTemplateFamilyFingerprint() { return templateFamilyFingerprint; }
    public List<LogicalObjectSurface> getRuntimeMatchObjectRefs() { return runtimeMatchObjectRefs; }
    public List<String> getRuntimeMatchObjectNames() { return runtimeMatchObjectNames; }
    public List<LogicalObjectSurface> getAnalysisPhysicalObjectRefs() { return analysisPhysicalObjectRefs; }
    public String getMetadataSnapshotVersion() { return metadataSnapshotVersion; }
    public String getViewDefinitionHash() { return viewDefinitionHash; }
    public String getMetadataDegradationReason() { return metadataDegradationReason; }
    public String getDatasourceCode() { return datasourceCode; }
    public RuntimeRewriteBindingStatus getStatus() { return status; }
    public long getRuleVersion() { return ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public String getActivatedBy() { return activatedBy; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getPausedBy() { return pausedBy; }
    public Instant getPausedAt() { return pausedAt; }
    public String getPauseReason() { return pauseReason; }
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
        private String originalSqlText;
        private String recommendedSqlText;
        private String rewriteMatchMode;
        private String rewriteProgramJson;
        private String templateFamilyFingerprint;
        private List<LogicalObjectSurface> runtimeMatchObjectRefs;
        private List<String> runtimeMatchObjectNames;
        private List<LogicalObjectSurface> analysisPhysicalObjectRefs;
        private String metadataSnapshotVersion;
        private String viewDefinitionHash;
        private String metadataDegradationReason;
        private String datasourceCode;
        private RuntimeRewriteBindingStatus status;
        private long ruleVersion;
        private String runtimeRuleVersion;
        private String activatedBy;
        private Instant activatedAt;
        private String pausedBy;
        private Instant pausedAt;
        private String pauseReason;
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
        public Builder originalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; return this; }
        public Builder recommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; return this; }
        public Builder rewriteMatchMode(String rewriteMatchMode) { this.rewriteMatchMode = rewriteMatchMode; return this; }
        public Builder rewriteProgramJson(String rewriteProgramJson) { this.rewriteProgramJson = rewriteProgramJson; return this; }
        public Builder templateFamilyFingerprint(String templateFamilyFingerprint) { this.templateFamilyFingerprint = templateFamilyFingerprint; return this; }
        public Builder runtimeMatchObjectRefs(List<LogicalObjectSurface> runtimeMatchObjectRefs) { this.runtimeMatchObjectRefs = runtimeMatchObjectRefs; return this; }
        public Builder runtimeMatchObjectNames(List<String> runtimeMatchObjectNames) { this.runtimeMatchObjectNames = runtimeMatchObjectNames; return this; }
        public Builder analysisPhysicalObjectRefs(List<LogicalObjectSurface> analysisPhysicalObjectRefs) { this.analysisPhysicalObjectRefs = analysisPhysicalObjectRefs; return this; }
        public Builder metadataSnapshotVersion(String metadataSnapshotVersion) { this.metadataSnapshotVersion = metadataSnapshotVersion; return this; }
        public Builder viewDefinitionHash(String viewDefinitionHash) { this.viewDefinitionHash = viewDefinitionHash; return this; }
        public Builder metadataDegradationReason(String metadataDegradationReason) { this.metadataDegradationReason = metadataDegradationReason; return this; }
        public Builder datasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; return this; }
        public Builder status(RuntimeRewriteBindingStatus status) { this.status = status; return this; }
        public Builder ruleVersion(long ruleVersion) { this.ruleVersion = ruleVersion; return this; }
        public Builder runtimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; return this; }
        public Builder activatedBy(String activatedBy) { this.activatedBy = activatedBy; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder pausedBy(String pausedBy) { this.pausedBy = pausedBy; return this; }
        public Builder pausedAt(Instant pausedAt) { this.pausedAt = pausedAt; return this; }
        public Builder pauseReason(String pauseReason) { this.pauseReason = pauseReason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public RuntimeRewriteBinding build() { return new RuntimeRewriteBinding(this); }
    }
}
