package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class AccelerationRecommendationRecord {

    private String recommendationId;
    private String tenantId;
    private String recommendationType;
    private String sourceSqlId;
    private String historyId;
    private String parseTaskId;
    private String batchId;
    private String routeDecisionId;
    private String alertId;
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
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String evidenceLevel;
    private String schemaVersion;
    private String ruleChainJson;
    private String unappliedRulesJson;
    private String preconditionsJson;
    private String semanticRisksJson;
    private String expectedBenefitJson;
    private String estimatedCostJson;
    private Integer confidence;
    private String validationMethod;
    private String validationStatus;
    private Boolean autoApplyAllowed;
    private Boolean manualReviewRequired;
    private String sourceProblemsJson;
    private String issueRuleLinksJson;
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
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getRouteDecisionId() { return routeDecisionId; }
    public void setRouteDecisionId(String routeDecisionId) { this.routeDecisionId = routeDecisionId; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
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
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getRuleChainJson() { return ruleChainJson; }
    public void setRuleChainJson(String ruleChainJson) { this.ruleChainJson = ruleChainJson; }
    public String getUnappliedRulesJson() { return unappliedRulesJson; }
    public void setUnappliedRulesJson(String unappliedRulesJson) { this.unappliedRulesJson = unappliedRulesJson; }
    public String getPreconditionsJson() { return preconditionsJson; }
    public void setPreconditionsJson(String preconditionsJson) { this.preconditionsJson = preconditionsJson; }
    public String getSemanticRisksJson() { return semanticRisksJson; }
    public void setSemanticRisksJson(String semanticRisksJson) { this.semanticRisksJson = semanticRisksJson; }
    public String getExpectedBenefitJson() { return expectedBenefitJson; }
    public void setExpectedBenefitJson(String expectedBenefitJson) { this.expectedBenefitJson = expectedBenefitJson; }
    public String getEstimatedCostJson() { return estimatedCostJson; }
    public void setEstimatedCostJson(String estimatedCostJson) { this.estimatedCostJson = estimatedCostJson; }
    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }
    public String getValidationMethod() { return validationMethod; }
    public void setValidationMethod(String validationMethod) { this.validationMethod = validationMethod; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public Boolean getAutoApplyAllowed() { return autoApplyAllowed; }
    public void setAutoApplyAllowed(Boolean autoApplyAllowed) { this.autoApplyAllowed = autoApplyAllowed; }
    public Boolean getManualReviewRequired() { return manualReviewRequired; }
    public void setManualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; }
    public String getSourceProblemsJson() { return sourceProblemsJson; }
    public void setSourceProblemsJson(String sourceProblemsJson) { this.sourceProblemsJson = sourceProblemsJson; }
    public String getIssueRuleLinksJson() { return issueRuleLinksJson; }
    public void setIssueRuleLinksJson(String issueRuleLinksJson) { this.issueRuleLinksJson = issueRuleLinksJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
