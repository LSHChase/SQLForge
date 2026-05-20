package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SqlRewriteRecordVO {

    private String rewriteRecordId;
    private String tenantId;
    private String recommendationId;
    private String optimizationTaskId;
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String evidenceLevel;
    private String historyId;
    private String parseHistoryId;
    private String sqlFingerprint;
    private String datasourceCode;
    private String status;
    private String validationStatus;
    private Boolean autoApplyAllowed;
    private Boolean manualReviewRequired;
    private String reviewStatus;
    private String reviewNote;
    private String reviewedBy;
    private Instant reviewedAt;
    private String activationStatus;
    private String runtimeBindingId;
    private Instant runtimeBindingAt;
    private String runtimeBindingBy;
    private String runtimeBindingScope;
    private String activatedSqlFingerprint;
    private String runtimeRuleVersion;
    private String validationPolicyId;
    private String lastValidationRunId;
    private Instant lastComparedAt;
    private String alertStatus;
    private String originalSqlText;
    private String recommendedSqlText;
    private String executedSqlText;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Map<String, Object>> ruleChain;
    private List<Map<String, Object>> sourceProblems;
    private List<Map<String, Object>> issueRuleLinks;
    private Map<String, Object> diffSummary;
    private Map<String, Object> risk;
    private Map<String, Object> traceRefs;
    private String contractStage;
    private String implementationStage;

    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getOptimizationTaskId() { return optimizationTaskId; }
    public void setOptimizationTaskId(String optimizationTaskId) { this.optimizationTaskId = optimizationTaskId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public Boolean getAutoApplyAllowed() { return autoApplyAllowed; }
    public void setAutoApplyAllowed(Boolean autoApplyAllowed) { this.autoApplyAllowed = autoApplyAllowed; }
    public Boolean getManualReviewRequired() { return manualReviewRequired; }
    public void setManualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getActivationStatus() { return activationStatus; }
    public void setActivationStatus(String activationStatus) { this.activationStatus = activationStatus; }
    public String getRuntimeBindingId() { return runtimeBindingId; }
    public void setRuntimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; }
    public Instant getRuntimeBindingAt() { return runtimeBindingAt; }
    public void setRuntimeBindingAt(Instant runtimeBindingAt) { this.runtimeBindingAt = runtimeBindingAt; }
    public String getRuntimeBindingBy() { return runtimeBindingBy; }
    public void setRuntimeBindingBy(String runtimeBindingBy) { this.runtimeBindingBy = runtimeBindingBy; }
    public String getRuntimeBindingScope() { return runtimeBindingScope; }
    public void setRuntimeBindingScope(String runtimeBindingScope) { this.runtimeBindingScope = runtimeBindingScope; }
    public String getActivatedSqlFingerprint() { return activatedSqlFingerprint; }
    public void setActivatedSqlFingerprint(String activatedSqlFingerprint) { this.activatedSqlFingerprint = activatedSqlFingerprint; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public void setRuntimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; }
    public String getValidationPolicyId() { return validationPolicyId; }
    public void setValidationPolicyId(String validationPolicyId) { this.validationPolicyId = validationPolicyId; }
    public String getLastValidationRunId() { return lastValidationRunId; }
    public void setLastValidationRunId(String lastValidationRunId) { this.lastValidationRunId = lastValidationRunId; }
    public Instant getLastComparedAt() { return lastComparedAt; }
    public void setLastComparedAt(Instant lastComparedAt) { this.lastComparedAt = lastComparedAt; }
    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }
    public String getOriginalSqlText() { return originalSqlText; }
    public void setOriginalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getExecutedSqlText() { return executedSqlText; }
    public void setExecutedSqlText(String executedSqlText) { this.executedSqlText = executedSqlText; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<Map<String, Object>> getRuleChain() { return ruleChain; }
    public void setRuleChain(List<Map<String, Object>> ruleChain) { this.ruleChain = ruleChain; }
    public List<Map<String, Object>> getSourceProblems() { return sourceProblems; }
    public void setSourceProblems(List<Map<String, Object>> sourceProblems) { this.sourceProblems = sourceProblems; }
    public List<Map<String, Object>> getIssueRuleLinks() { return issueRuleLinks; }
    public void setIssueRuleLinks(List<Map<String, Object>> issueRuleLinks) { this.issueRuleLinks = issueRuleLinks; }
    public Map<String, Object> getDiffSummary() { return diffSummary; }
    public void setDiffSummary(Map<String, Object> diffSummary) { this.diffSummary = diffSummary; }
    public Map<String, Object> getRisk() { return risk; }
    public void setRisk(Map<String, Object> risk) { this.risk = risk; }
    public Map<String, Object> getTraceRefs() { return traceRefs; }
    public void setTraceRefs(Map<String, Object> traceRefs) { this.traceRefs = traceRefs; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
