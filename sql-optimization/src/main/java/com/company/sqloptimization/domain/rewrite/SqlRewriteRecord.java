package com.company.sqloptimization.domain.rewrite;

import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SqlRewriteRecord {

    private final String rewriteRecordId;
    private final String tenantId;
    private final String recommendationId;
    private final String optimizationTaskId;
    private final GovernanceSourceType sourceType;
    private final GovernanceSourceKind sourceKind;
    private final String sourceId;
    private final EvidenceLevel evidenceLevel;
    private final String historyId;
    private final String parseHistoryId;
    private final String sqlFingerprint;
    private final String datasourceCode;
    private final RewriteRecordStatus status;
    private final RewriteValidationStatus validationStatus;
    private final boolean autoApplyAllowed;
    private final boolean manualReviewRequired;
    private final String validationPolicyId;
    private final String lastValidationRunId;
    private final Instant lastComparedAt;
    private final RewriteAlertStatus alertStatus;
    private final String originalSqlText;
    private final String recommendedSqlText;
    private final String executedSqlText;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<Map<String, Object>> ruleChain;
    private final Map<String, Object> diffSummary;
    private final Map<String, Object> risk;
    private final Map<String, Object> traceRefs;

    private SqlRewriteRecord(Builder builder) {
        this.rewriteRecordId = builder.rewriteRecordId;
        this.tenantId = builder.tenantId;
        this.recommendationId = builder.recommendationId;
        this.optimizationTaskId = builder.optimizationTaskId;
        this.sourceType = builder.sourceType;
        this.sourceKind = builder.sourceKind;
        this.sourceId = builder.sourceId;
        this.evidenceLevel = builder.evidenceLevel;
        this.historyId = builder.historyId;
        this.parseHistoryId = builder.parseHistoryId;
        this.sqlFingerprint = builder.sqlFingerprint;
        this.datasourceCode = builder.datasourceCode;
        this.status = builder.status == null ? RewriteRecordStatus.DRAFT : builder.status;
        this.validationStatus = builder.validationStatus == null
            ? RewriteValidationStatus.NOT_VALIDATED
            : builder.validationStatus;
        this.autoApplyAllowed = builder.autoApplyAllowed;
        this.manualReviewRequired = builder.manualReviewRequired;
        this.validationPolicyId = builder.validationPolicyId;
        this.lastValidationRunId = builder.lastValidationRunId;
        this.lastComparedAt = builder.lastComparedAt;
        this.alertStatus = builder.alertStatus == null ? RewriteAlertStatus.NONE : builder.alertStatus;
        this.originalSqlText = builder.originalSqlText;
        this.recommendedSqlText = builder.recommendedSqlText;
        this.executedSqlText = builder.executedSqlText;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        this.ruleChain = immutableListCopy(builder.ruleChain);
        this.diffSummary = immutableMapCopy(builder.diffSummary);
        this.risk = immutableMapCopy(builder.risk);
        this.traceRefs = immutableMapCopy(builder.traceRefs);
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public SqlRewriteRecord withValidationSummary(RewriteValidationRun run, Instant updatedAt) {
        RewriteValidationStatus nextValidationStatus = validationStatusFrom(run);
        return SqlRewriteRecord.builder()
            .rewriteRecordId(rewriteRecordId)
            .tenantId(tenantId)
            .recommendationId(recommendationId)
            .optimizationTaskId(optimizationTaskId)
            .sourceType(sourceType)
            .sourceKind(sourceKind)
            .sourceId(sourceId)
            .evidenceLevel(evidenceLevel)
            .historyId(historyId)
            .parseHistoryId(parseHistoryId)
            .sqlFingerprint(sqlFingerprint)
            .datasourceCode(datasourceCode)
            .status(Boolean.TRUE.equals(run.isAutoApplyPaused()) ? RewriteRecordStatus.PAUSED : status)
            .validationStatus(nextValidationStatus)
            .autoApplyAllowed(autoApplyAllowed && !Boolean.TRUE.equals(run.isAutoApplyPaused()))
            .manualReviewRequired(manualReviewRequired)
            .validationPolicyId(validationPolicyId)
            .lastValidationRunId(run.getValidationRunId())
            .lastComparedAt(run.getFinishedAt() == null ? run.getStartedAt() : run.getFinishedAt())
            .alertStatus(run.isAutoApplyPaused() ? RewriteAlertStatus.OPEN : alertStatus)
            .originalSqlText(originalSqlText)
            .recommendedSqlText(recommendedSqlText)
            .executedSqlText(executedSqlText)
            .createdBy(createdBy)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .ruleChain(ruleChain)
            .diffSummary(diffSummary)
            .risk(risk)
            .traceRefs(traceRefs)
            .build();
    }

    public SqlRewriteRecord withTraceRefs(Map<String, Object> nextTraceRefs, Instant updatedAt) {
        return SqlRewriteRecord.builder()
            .rewriteRecordId(rewriteRecordId)
            .tenantId(tenantId)
            .recommendationId(recommendationId)
            .optimizationTaskId(optimizationTaskId)
            .sourceType(sourceType)
            .sourceKind(sourceKind)
            .sourceId(sourceId)
            .evidenceLevel(evidenceLevel)
            .historyId(historyId)
            .parseHistoryId(parseHistoryId)
            .sqlFingerprint(sqlFingerprint)
            .datasourceCode(datasourceCode)
            .status(status)
            .validationStatus(validationStatus)
            .autoApplyAllowed(autoApplyAllowed)
            .manualReviewRequired(manualReviewRequired)
            .validationPolicyId(validationPolicyId)
            .lastValidationRunId(lastValidationRunId)
            .lastComparedAt(lastComparedAt)
            .alertStatus(alertStatus)
            .originalSqlText(originalSqlText)
            .recommendedSqlText(recommendedSqlText)
            .executedSqlText(executedSqlText)
            .createdBy(createdBy)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .ruleChain(ruleChain)
            .diffSummary(diffSummary)
            .risk(risk)
            .traceRefs(nextTraceRefs)
            .build();
    }

    private RewriteValidationStatus validationStatusFrom(RewriteValidationRun run) {
        if (run.getComparisonStatus() == null) {
            return RewriteValidationStatus.VALIDATING;
        }
        switch (run.getComparisonStatus()) {
            case EQUIVALENT:
                return RewriteValidationStatus.EQUIVALENT;
            case DIVERGED:
                return RewriteValidationStatus.DIVERGED;
            case FAILED:
                return RewriteValidationStatus.FAILED;
            case EXPIRED:
                return RewriteValidationStatus.EXPIRED;
            case NOT_COMPARED:
            default:
                return RewriteValidationStatus.VALIDATING;
        }
    }

    private void validate() {
        requireText(rewriteRecordId, "rewriteRecordId");
        requireText(tenantId, "tenantId");
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (sourceKind == null) {
            throw new IllegalArgumentException("sourceKind is required");
        }
        requireText(sourceId, "sourceId");
        if (evidenceLevel == null) {
            throw new IllegalArgumentException("evidenceLevel is required");
        }
        requireText(originalSqlText, "originalSqlText");
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

    public String getRewriteRecordId() { return rewriteRecordId; }
    public String getTenantId() { return tenantId; }
    public String getRecommendationId() { return recommendationId; }
    public String getOptimizationTaskId() { return optimizationTaskId; }
    public GovernanceSourceType getSourceType() { return sourceType; }
    public GovernanceSourceKind getSourceKind() { return sourceKind; }
    public String getSourceId() { return sourceId; }
    public EvidenceLevel getEvidenceLevel() { return evidenceLevel; }
    public String getHistoryId() { return historyId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public RewriteRecordStatus getStatus() { return status; }
    public RewriteValidationStatus getValidationStatus() { return validationStatus; }
    public boolean isAutoApplyAllowed() { return autoApplyAllowed; }
    public boolean isManualReviewRequired() { return manualReviewRequired; }
    public String getValidationPolicyId() { return validationPolicyId; }
    public String getLastValidationRunId() { return lastValidationRunId; }
    public Instant getLastComparedAt() { return lastComparedAt; }
    public RewriteAlertStatus getAlertStatus() { return alertStatus; }
    public String getOriginalSqlText() { return originalSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public String getExecutedSqlText() { return executedSqlText; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Map<String, Object>> getRuleChain() { return ruleChain; }
    public Map<String, Object> getDiffSummary() { return diffSummary; }
    public Map<String, Object> getRisk() { return risk; }
    public Map<String, Object> getTraceRefs() { return traceRefs; }

    public static final class Builder {
        private String rewriteRecordId;
        private String tenantId;
        private String recommendationId;
        private String optimizationTaskId;
        private GovernanceSourceType sourceType;
        private GovernanceSourceKind sourceKind;
        private String sourceId;
        private EvidenceLevel evidenceLevel;
        private String historyId;
        private String parseHistoryId;
        private String sqlFingerprint;
        private String datasourceCode;
        private RewriteRecordStatus status;
        private RewriteValidationStatus validationStatus;
        private boolean autoApplyAllowed;
        private boolean manualReviewRequired;
        private String validationPolicyId;
        private String lastValidationRunId;
        private Instant lastComparedAt;
        private RewriteAlertStatus alertStatus;
        private String originalSqlText;
        private String recommendedSqlText;
        private String executedSqlText;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;
        private List<Map<String, Object>> ruleChain;
        private Map<String, Object> diffSummary;
        private Map<String, Object> risk;
        private Map<String, Object> traceRefs;

        private Builder() {
        }

        public Builder rewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder recommendationId(String recommendationId) { this.recommendationId = recommendationId; return this; }
        public Builder optimizationTaskId(String optimizationTaskId) { this.optimizationTaskId = optimizationTaskId; return this; }
        public Builder sourceType(GovernanceSourceType sourceType) { this.sourceType = sourceType; return this; }
        public Builder sourceKind(GovernanceSourceKind sourceKind) { this.sourceKind = sourceKind; return this; }
        public Builder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public Builder evidenceLevel(EvidenceLevel evidenceLevel) { this.evidenceLevel = evidenceLevel; return this; }
        public Builder historyId(String historyId) { this.historyId = historyId; return this; }
        public Builder parseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder datasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; return this; }
        public Builder status(RewriteRecordStatus status) { this.status = status; return this; }
        public Builder validationStatus(RewriteValidationStatus validationStatus) { this.validationStatus = validationStatus; return this; }
        public Builder autoApplyAllowed(boolean autoApplyAllowed) { this.autoApplyAllowed = autoApplyAllowed; return this; }
        public Builder manualReviewRequired(boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; return this; }
        public Builder validationPolicyId(String validationPolicyId) { this.validationPolicyId = validationPolicyId; return this; }
        public Builder lastValidationRunId(String lastValidationRunId) { this.lastValidationRunId = lastValidationRunId; return this; }
        public Builder lastComparedAt(Instant lastComparedAt) { this.lastComparedAt = lastComparedAt; return this; }
        public Builder alertStatus(RewriteAlertStatus alertStatus) { this.alertStatus = alertStatus; return this; }
        public Builder originalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; return this; }
        public Builder recommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; return this; }
        public Builder executedSqlText(String executedSqlText) { this.executedSqlText = executedSqlText; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder ruleChain(List<Map<String, Object>> ruleChain) { this.ruleChain = ruleChain; return this; }
        public Builder diffSummary(Map<String, Object> diffSummary) { this.diffSummary = diffSummary; return this; }
        public Builder risk(Map<String, Object> risk) { this.risk = risk; return this; }
        public Builder traceRefs(Map<String, Object> traceRefs) { this.traceRefs = traceRefs; return this; }

        public SqlRewriteRecord build() {
            return new SqlRewriteRecord(this);
        }
    }
}
