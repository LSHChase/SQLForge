package com.company.sqloptimization.domain.recommendation;

import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccelerationRecommendation {

    public static final String DEFAULT_SCHEMA_VERSION = "SQL_RECOMMENDATION_RULE_MODEL_V1";

    private final String recommendationId;
    private final String tenantId;
    private final RecommendationType recommendationType;
    private final String sourceSqlId;
    private final String historyId;
    private final String parseTaskId;
    private final String batchId;
    private final String routeDecisionId;
    private final String alertId;
    private final String sqlFingerprint;
    private final String sourceSqlText;
    private final String recommendedSqlText;
    private final String targetEngine;
    private final String targetDatasource;
    private final String reportCode;
    private final String logicalObjectKey;
    private final String summary;
    private final String reason;
    private final String expectedGain;
    private final BenefitLevel benefitLevel;
    private final RiskLevel riskLevel;
    private final String riskSummary;
    private final boolean requiresDispatch;
    private final RecommendationStatus status;
    private final GovernanceSourceType sourceType;
    private final GovernanceSourceKind sourceKind;
    private final String sourceId;
    private final EvidenceLevel evidenceLevel;
    private final String schemaVersion;
    private final List<Map<String, Object>> ruleChain;
    private final List<Map<String, Object>> unappliedRules;
    private final List<Map<String, Object>> preconditions;
    private final List<Map<String, Object>> semanticRisks;
    private final Map<String, Object> expectedBenefit;
    private final Map<String, Object> estimatedCost;
    private final Integer confidence;
    private final String validationMethod;
    private final RewriteValidationStatus validationStatus;
    private final boolean autoApplyAllowed;
    private final boolean manualReviewRequired;
    private final List<Map<String, Object>> sourceProblems;
    private final List<Map<String, Object>> issueRuleLinks;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    private AccelerationRecommendation(Builder builder) {
        this.recommendationId = builder.recommendationId;
        this.tenantId = builder.tenantId;
        this.recommendationType = builder.recommendationType;
        this.sourceSqlId = builder.sourceSqlId;
        this.historyId = builder.historyId;
        this.parseTaskId = builder.parseTaskId;
        this.batchId = builder.batchId;
        this.routeDecisionId = builder.routeDecisionId;
        this.alertId = builder.alertId;
        this.sqlFingerprint = builder.sqlFingerprint;
        this.sourceSqlText = builder.sourceSqlText;
        this.recommendedSqlText = builder.recommendedSqlText;
        this.targetEngine = builder.targetEngine;
        this.targetDatasource = builder.targetDatasource;
        this.reportCode = builder.reportCode;
        this.logicalObjectKey = builder.logicalObjectKey;
        this.summary = builder.summary;
        this.reason = builder.reason;
        this.expectedGain = builder.expectedGain;
        this.benefitLevel = builder.benefitLevel == null ? BenefitLevel.UNKNOWN : builder.benefitLevel;
        this.riskLevel = builder.riskLevel == null ? RiskLevel.UNKNOWN : builder.riskLevel;
        this.riskSummary = builder.riskSummary;
        this.requiresDispatch = builder.requiresDispatch;
        this.status = builder.status == null ? RecommendationStatus.RECOMMENDED : builder.status;
        this.sourceType = builder.sourceType;
        this.sourceKind = builder.sourceKind;
        this.sourceId = builder.sourceId;
        this.evidenceLevel = builder.evidenceLevel;
        this.schemaVersion = builder.schemaVersion == null ? DEFAULT_SCHEMA_VERSION : builder.schemaVersion;
        this.ruleChain = immutableListCopy(builder.ruleChain);
        this.unappliedRules = immutableListCopy(builder.unappliedRules);
        this.preconditions = immutableListCopy(builder.preconditions);
        this.semanticRisks = immutableListCopy(builder.semanticRisks);
        this.expectedBenefit = immutableMapCopy(builder.expectedBenefit);
        this.estimatedCost = immutableMapCopy(builder.estimatedCost);
        this.confidence = builder.confidence;
        this.validationMethod = builder.validationMethod;
        this.validationStatus = builder.validationStatus == null
            ? RewriteValidationStatus.NOT_VALIDATED
            : builder.validationStatus;
        this.autoApplyAllowed = Boolean.TRUE.equals(builder.autoApplyAllowed);
        this.manualReviewRequired = builder.manualReviewRequired == null
            ? true
            : Boolean.TRUE.equals(builder.manualReviewRequired);
        this.sourceProblems = immutableListCopy(builder.sourceProblems);
        this.issueRuleLinks = immutableListCopy(builder.issueRuleLinks);
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        requireText(recommendationId, "recommendationId");
        requireText(tenantId, "tenantId");
        if (recommendationType == null) {
            throw new IllegalArgumentException("recommendationType 为必填项");
        }
        requireText(recommendedSqlText, "recommendedSqlText");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt 为必填项");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    private Map<String, Object> immutableMapCopy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
    }

    private List<Map<String, Object>> immutableListCopy(List<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(value.size());
        for (Map<String, Object> item : value) {
            result.add(immutableMapCopy(item));
        }
        return Collections.unmodifiableList(result);
    }

    public String getRecommendationId() { return recommendationId; }
    public String getTenantId() { return tenantId; }
    public RecommendationType getRecommendationType() { return recommendationType; }
    public String getSourceSqlId() { return sourceSqlId; }
    public String getHistoryId() { return historyId; }
    public String getParseTaskId() { return parseTaskId; }
    public String getBatchId() { return batchId; }
    public String getRouteDecisionId() { return routeDecisionId; }
    public String getAlertId() { return alertId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public String getSourceSqlText() { return sourceSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public String getTargetEngine() { return targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public String getReportCode() { return reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public String getSummary() { return summary; }
    public String getReason() { return reason; }
    public String getExpectedGain() { return expectedGain; }
    public BenefitLevel getBenefitLevel() { return benefitLevel; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getRiskSummary() { return riskSummary; }
    public boolean isRequiresDispatch() { return requiresDispatch; }
    public RecommendationStatus getStatus() { return status; }
    public GovernanceSourceType getSourceType() { return sourceType; }
    public GovernanceSourceKind getSourceKind() { return sourceKind; }
    public String getSourceId() { return sourceId; }
    public EvidenceLevel getEvidenceLevel() { return evidenceLevel; }
    public String getSchemaVersion() { return schemaVersion; }
    public List<Map<String, Object>> getRuleChain() { return ruleChain; }
    public List<Map<String, Object>> getUnappliedRules() { return unappliedRules; }
    public List<Map<String, Object>> getPreconditions() { return preconditions; }
    public List<Map<String, Object>> getSemanticRisks() { return semanticRisks; }
    public Map<String, Object> getExpectedBenefit() { return expectedBenefit; }
    public Map<String, Object> getEstimatedCost() { return estimatedCost; }
    public Integer getConfidence() { return confidence; }
    public String getValidationMethod() { return validationMethod; }
    public RewriteValidationStatus getValidationStatus() { return validationStatus; }
    public boolean isAutoApplyAllowed() { return autoApplyAllowed; }
    public boolean isManualReviewRequired() { return manualReviewRequired; }
    public List<Map<String, Object>> getSourceProblems() { return sourceProblems; }
    public List<Map<String, Object>> getIssueRuleLinks() { return issueRuleLinks; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum RecommendationType {
        REWRITE,
        ACCELERATION,
        CREATE_TABLE,
        PREWARM,
        MAINTENANCE
    }

    public enum BenefitLevel {
        UNKNOWN,
        LOW,
        MEDIUM,
        HIGH
    }

    public enum RiskLevel {
        UNKNOWN,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum RecommendationStatus {
        RECOMMENDED,
        REVIEWING,
        DISPATCH_READY,
        CANCELLED
    }

    public static final class Builder {
        private String recommendationId;
        private String tenantId;
        private RecommendationType recommendationType;
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
        private BenefitLevel benefitLevel;
        private RiskLevel riskLevel;
        private String riskSummary;
        private boolean requiresDispatch;
        private RecommendationStatus status;
        private GovernanceSourceType sourceType;
        private GovernanceSourceKind sourceKind;
        private String sourceId;
        private EvidenceLevel evidenceLevel;
        private String schemaVersion;
        private List<Map<String, Object>> ruleChain;
        private List<Map<String, Object>> unappliedRules;
        private List<Map<String, Object>> preconditions;
        private List<Map<String, Object>> semanticRisks;
        private Map<String, Object> expectedBenefit;
        private Map<String, Object> estimatedCost;
        private Integer confidence;
        private String validationMethod;
        private RewriteValidationStatus validationStatus;
        private Boolean autoApplyAllowed;
        private Boolean manualReviewRequired;
        private List<Map<String, Object>> sourceProblems;
        private List<Map<String, Object>> issueRuleLinks;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder recommendationId(String recommendationId) { this.recommendationId = recommendationId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder recommendationType(RecommendationType recommendationType) { this.recommendationType = recommendationType; return this; }
        public Builder sourceSqlId(String sourceSqlId) { this.sourceSqlId = sourceSqlId; return this; }
        public Builder historyId(String historyId) { this.historyId = historyId; return this; }
        public Builder parseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; return this; }
        public Builder batchId(String batchId) { this.batchId = batchId; return this; }
        public Builder routeDecisionId(String routeDecisionId) { this.routeDecisionId = routeDecisionId; return this; }
        public Builder alertId(String alertId) { this.alertId = alertId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder sourceSqlText(String sourceSqlText) { this.sourceSqlText = sourceSqlText; return this; }
        public Builder recommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; return this; }
        public Builder targetEngine(String targetEngine) { this.targetEngine = targetEngine; return this; }
        public Builder targetDatasource(String targetDatasource) { this.targetDatasource = targetDatasource; return this; }
        public Builder reportCode(String reportCode) { this.reportCode = reportCode; return this; }
        public Builder logicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder expectedGain(String expectedGain) { this.expectedGain = expectedGain; return this; }
        public Builder benefitLevel(BenefitLevel benefitLevel) { this.benefitLevel = benefitLevel; return this; }
        public Builder riskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; return this; }
        public Builder riskSummary(String riskSummary) { this.riskSummary = riskSummary; return this; }
        public Builder requiresDispatch(boolean requiresDispatch) { this.requiresDispatch = requiresDispatch; return this; }
        public Builder status(RecommendationStatus status) { this.status = status; return this; }
        public Builder sourceType(GovernanceSourceType sourceType) { this.sourceType = sourceType; return this; }
        public Builder sourceKind(GovernanceSourceKind sourceKind) { this.sourceKind = sourceKind; return this; }
        public Builder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public Builder evidenceLevel(EvidenceLevel evidenceLevel) { this.evidenceLevel = evidenceLevel; return this; }
        public Builder schemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder ruleChain(List<Map<String, Object>> ruleChain) { this.ruleChain = ruleChain; return this; }
        public Builder unappliedRules(List<Map<String, Object>> unappliedRules) { this.unappliedRules = unappliedRules; return this; }
        public Builder preconditions(List<Map<String, Object>> preconditions) { this.preconditions = preconditions; return this; }
        public Builder semanticRisks(List<Map<String, Object>> semanticRisks) { this.semanticRisks = semanticRisks; return this; }
        public Builder expectedBenefit(Map<String, Object> expectedBenefit) { this.expectedBenefit = expectedBenefit; return this; }
        public Builder estimatedCost(Map<String, Object> estimatedCost) { this.estimatedCost = estimatedCost; return this; }
        public Builder confidence(Integer confidence) { this.confidence = confidence; return this; }
        public Builder validationMethod(String validationMethod) { this.validationMethod = validationMethod; return this; }
        public Builder validationStatus(RewriteValidationStatus validationStatus) { this.validationStatus = validationStatus; return this; }
        public Builder autoApplyAllowed(Boolean autoApplyAllowed) { this.autoApplyAllowed = autoApplyAllowed; return this; }
        public Builder manualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; return this; }
        public Builder sourceProblems(List<Map<String, Object>> sourceProblems) { this.sourceProblems = sourceProblems; return this; }
        public Builder issueRuleLinks(List<Map<String, Object>> issueRuleLinks) { this.issueRuleLinks = issueRuleLinks; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public AccelerationRecommendation build() {
            return new AccelerationRecommendation(this);
        }
    }
}
