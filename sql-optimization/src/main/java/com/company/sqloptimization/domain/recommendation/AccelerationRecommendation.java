package com.company.sqloptimization.domain.recommendation;

import java.time.Instant;

public class AccelerationRecommendation {

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
            throw new IllegalArgumentException("recommendationType is required");
        }
        requireText(recommendedSqlText, "recommendedSqlText");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
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
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public AccelerationRecommendation build() {
            return new AccelerationRecommendation(this);
        }
    }
}
