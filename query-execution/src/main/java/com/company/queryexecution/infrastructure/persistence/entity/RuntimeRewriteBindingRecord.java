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
    private String recommendedSqlText;
    private String datasourceCode;
    private String status;
    private Long ruleVersion;
    private String runtimeRuleVersion;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private String pausedBy;
    private LocalDateTime pausedAt;
    private String pauseReason;
    private String unpublishedBy;
    private LocalDateTime unpublishedAt;
    private String unpublishReason;
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
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Long ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public void setRuntimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getPausedBy() { return pausedBy; }
    public void setPausedBy(String pausedBy) { this.pausedBy = pausedBy; }
    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }
    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }
    public String getUnpublishedBy() { return unpublishedBy; }
    public void setUnpublishedBy(String unpublishedBy) { this.unpublishedBy = unpublishedBy; }
    public LocalDateTime getUnpublishedAt() { return unpublishedAt; }
    public void setUnpublishedAt(LocalDateTime unpublishedAt) { this.unpublishedAt = unpublishedAt; }
    public String getUnpublishReason() { return unpublishReason; }
    public void setUnpublishReason(String unpublishReason) { this.unpublishReason = unpublishReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
