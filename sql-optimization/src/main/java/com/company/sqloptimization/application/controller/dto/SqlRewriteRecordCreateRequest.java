package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SqlRewriteRecordCreateRequest {

    private String tenantId;
    private String recommendationId;
    private String optimizationTaskId;

    @NotNull(message = "sourceType is required")
    private GovernanceSourceType sourceType;

    @NotNull(message = "sourceKind is required")
    private GovernanceSourceKind sourceKind;

    @NotBlank(message = "sourceId is required")
    private String sourceId;

    @NotNull(message = "evidenceLevel is required")
    private EvidenceLevel evidenceLevel;

    private String historyId;
    private String parseHistoryId;
    private String sqlFingerprint;
    private String datasourceCode;
    private RewriteRecordStatus status;
    private RewriteValidationStatus validationStatus;
    private Boolean autoApplyAllowed;
    private Boolean manualReviewRequired;
    private String validationPolicyId;
    private RewriteAlertStatus alertStatus;

    @NotBlank(message = "originalSqlText is required")
    private String originalSqlText;

    @NotBlank(message = "recommendedSqlText is required")
    private String recommendedSqlText;

    private String executedSqlText;
    private List<Map<String, Object>> ruleChain;
    private Map<String, Object> diffSummary;
    private Map<String, Object> risk;
    private Map<String, Object> traceRefs;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getOptimizationTaskId() { return optimizationTaskId; }
    public void setOptimizationTaskId(String optimizationTaskId) { this.optimizationTaskId = optimizationTaskId; }
    public GovernanceSourceType getSourceType() { return sourceType; }
    public void setSourceType(GovernanceSourceType sourceType) { this.sourceType = sourceType; }
    public GovernanceSourceKind getSourceKind() { return sourceKind; }
    public void setSourceKind(GovernanceSourceKind sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public EvidenceLevel getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(EvidenceLevel evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public RewriteRecordStatus getStatus() { return status; }
    public void setStatus(RewriteRecordStatus status) { this.status = status; }
    public RewriteValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(RewriteValidationStatus validationStatus) { this.validationStatus = validationStatus; }
    public Boolean getAutoApplyAllowed() { return autoApplyAllowed; }
    public void setAutoApplyAllowed(Boolean autoApplyAllowed) { this.autoApplyAllowed = autoApplyAllowed; }
    public Boolean getManualReviewRequired() { return manualReviewRequired; }
    public void setManualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; }
    public String getValidationPolicyId() { return validationPolicyId; }
    public void setValidationPolicyId(String validationPolicyId) { this.validationPolicyId = validationPolicyId; }
    public RewriteAlertStatus getAlertStatus() { return alertStatus; }
    public void setAlertStatus(RewriteAlertStatus alertStatus) { this.alertStatus = alertStatus; }
    public String getOriginalSqlText() { return originalSqlText; }
    public void setOriginalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getExecutedSqlText() { return executedSqlText; }
    public void setExecutedSqlText(String executedSqlText) { this.executedSqlText = executedSqlText; }
    public List<Map<String, Object>> getRuleChain() { return ruleChain; }
    public void setRuleChain(List<Map<String, Object>> ruleChain) { this.ruleChain = ruleChain; }
    public Map<String, Object> getDiffSummary() { return diffSummary; }
    public void setDiffSummary(Map<String, Object> diffSummary) { this.diffSummary = diffSummary; }
    public Map<String, Object> getRisk() { return risk; }
    public void setRisk(Map<String, Object> risk) { this.risk = risk; }
    public Map<String, Object> getTraceRefs() { return traceRefs; }
    public void setTraceRefs(Map<String, Object> traceRefs) { this.traceRefs = traceRefs; }
}
