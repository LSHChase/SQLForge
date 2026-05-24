package com.company.queryexecution.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class RuntimeRewriteBindingRecord {

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
    private String runtimeMatchObjectRefsJson;
    private String runtimeMatchObjectNamesJson;
    private String analysisPhysicalObjectRefsJson;
    private String metadataSnapshotVersion;
    private String viewDefinitionHash;
    private String metadataDegradationReason;
    private String datasourceCode;
    private String status;
    private Long ruleVersion;
    private String runtimeRuleVersion;
    private String activatedBy;
    private LocalDateTime activatedAt;
    private String pausedBy;
    private LocalDateTime pausedAt;
    private String pauseReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getRuntimeBindingId() { return runtimeBindingId; }
    public void setRuntimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getOriginalSqlDigest() { return originalSqlDigest; }
    public void setOriginalSqlDigest(String originalSqlDigest) { this.originalSqlDigest = originalSqlDigest; }
    public String getOriginalSqlText() { return originalSqlText; }
    public void setOriginalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getRewriteMatchMode() { return rewriteMatchMode; }
    public void setRewriteMatchMode(String rewriteMatchMode) { this.rewriteMatchMode = rewriteMatchMode; }
    public String getRewriteProgramJson() { return rewriteProgramJson; }
    public void setRewriteProgramJson(String rewriteProgramJson) { this.rewriteProgramJson = rewriteProgramJson; }
    public String getTemplateFamilyFingerprint() { return templateFamilyFingerprint; }
    public void setTemplateFamilyFingerprint(String templateFamilyFingerprint) { this.templateFamilyFingerprint = templateFamilyFingerprint; }
    public String getRuntimeMatchObjectRefsJson() { return runtimeMatchObjectRefsJson; }
    public void setRuntimeMatchObjectRefsJson(String runtimeMatchObjectRefsJson) { this.runtimeMatchObjectRefsJson = runtimeMatchObjectRefsJson; }
    public String getRuntimeMatchObjectNamesJson() { return runtimeMatchObjectNamesJson; }
    public void setRuntimeMatchObjectNamesJson(String runtimeMatchObjectNamesJson) { this.runtimeMatchObjectNamesJson = runtimeMatchObjectNamesJson; }
    public String getAnalysisPhysicalObjectRefsJson() { return analysisPhysicalObjectRefsJson; }
    public void setAnalysisPhysicalObjectRefsJson(String analysisPhysicalObjectRefsJson) { this.analysisPhysicalObjectRefsJson = analysisPhysicalObjectRefsJson; }
    public String getMetadataSnapshotVersion() { return metadataSnapshotVersion; }
    public void setMetadataSnapshotVersion(String metadataSnapshotVersion) { this.metadataSnapshotVersion = metadataSnapshotVersion; }
    public String getViewDefinitionHash() { return viewDefinitionHash; }
    public void setViewDefinitionHash(String viewDefinitionHash) { this.viewDefinitionHash = viewDefinitionHash; }
    public String getMetadataDegradationReason() { return metadataDegradationReason; }
    public void setMetadataDegradationReason(String metadataDegradationReason) { this.metadataDegradationReason = metadataDegradationReason; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Long ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public void setRuntimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; }
    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
    public String getPausedBy() { return pausedBy; }
    public void setPausedBy(String pausedBy) { this.pausedBy = pausedBy; }
    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }
    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
