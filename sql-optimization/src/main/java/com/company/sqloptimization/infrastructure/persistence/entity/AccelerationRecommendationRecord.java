package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class AccelerationRecommendationRecord {

    private String recommendationId;
    private String tenantId;
    private String recommendationType;
    private String sourceSqlId;
    private String sqlFingerprint;
    private String sourceSqlText;
    private String recommendedSqlText;
    private String targetEngine;
    private String targetDatasource;
    private String reportCode;
    private String logicalObjectKey;
    private String summary;
    private String reason;
    private String expectedGain;
    private String benefitLevel;
    private String riskLevel;
    private String riskSummary;
    private Boolean requiresDispatch;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public String getSourceSqlId() { return sourceSqlId; }
    public void setSourceSqlId(String sourceSqlId) { this.sourceSqlId = sourceSqlId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getSourceSqlText() { return sourceSqlText; }
    public void setSourceSqlText(String sourceSqlText) { this.sourceSqlText = sourceSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public void setTargetDatasource(String targetDatasource) { this.targetDatasource = targetDatasource; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getExpectedGain() { return expectedGain; }
    public void setExpectedGain(String expectedGain) { this.expectedGain = expectedGain; }
    public String getBenefitLevel() { return benefitLevel; }
    public void setBenefitLevel(String benefitLevel) { this.benefitLevel = benefitLevel; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public Boolean getRequiresDispatch() { return requiresDispatch; }
    public void setRequiresDispatch(Boolean requiresDispatch) { this.requiresDispatch = requiresDispatch; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
