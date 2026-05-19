package com.company.sqloptimization.domain.rewrite;

import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewritePublishStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
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
    private final RewriteReviewStatus reviewStatus;
    private final String reviewNote;
    private final String reviewedBy;
    private final Instant reviewedAt;
    private final RewritePublishStatus publishStatus;
    private final String runtimeBindingId;
    private final Instant runtimeBindingAt;
    private final String runtimeBindingBy;
    private final String runtimeBindingScope;
    private final String publishedSqlFingerprint;
    private final String runtimeRuleVersion;
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
        this.reviewStatus = builder.reviewStatus == null ? RewriteReviewStatus.PENDING_REVIEW : builder.reviewStatus;
        this.reviewNote = builder.reviewNote;
        this.reviewedBy = builder.reviewedBy;
        this.reviewedAt = builder.reviewedAt;
        this.publishStatus = builder.publishStatus == null ? RewritePublishStatus.UNPUBLISHED : builder.publishStatus;
        this.runtimeBindingId = builder.runtimeBindingId;
        this.runtimeBindingAt = builder.runtimeBindingAt;
        this.runtimeBindingBy = builder.runtimeBindingBy;
        this.runtimeBindingScope = builder.runtimeBindingScope;
        this.publishedSqlFingerprint = builder.publishedSqlFingerprint;
        this.runtimeRuleVersion = builder.runtimeRuleVersion;
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

    public boolean canTransitionReviewTo(RewriteReviewStatus nextReviewStatus) {
        if (nextReviewStatus == null) {
            return false;
        }
        if (reviewStatus == RewriteReviewStatus.PENDING_REVIEW) {
            return nextReviewStatus == RewriteReviewStatus.APPROVED
                || nextReviewStatus == RewriteReviewStatus.REJECTED
                || nextReviewStatus == RewriteReviewStatus.CHANGES_REQUESTED;
        }
        if (reviewStatus == RewriteReviewStatus.REJECTED
            || reviewStatus == RewriteReviewStatus.CHANGES_REQUESTED) {
            return nextReviewStatus == RewriteReviewStatus.PENDING_REVIEW;
        }
        return false;
    }

    public void requirePublishableRuntimeState() {
        if (reviewStatus != RewriteReviewStatus.APPROVED) {
            throw new IllegalStateException("只有 APPROVED 状态的改写记录才能发布");
        }
        if (publishStatus == RewritePublishStatus.UNPUBLISHED
            || publishStatus == RewritePublishStatus.PUBLISH_FAILED
            || publishStatus == RewritePublishStatus.PAUSED) {
            return;
        }
        throw new IllegalStateException("只有 UNPUBLISHED、PUBLISH_FAILED 或 PAUSED 状态的改写记录才能发布");
    }

    public void requirePauseableRuntimeState() {
        if (publishStatus == RewritePublishStatus.PUBLISHED) {
            return;
        }
        throw new IllegalStateException("只有 PUBLISHED 状态的改写记录才能执行 pause");
    }

    public void requireUnpublishableRuntimeState() {
        if (publishStatus == RewritePublishStatus.PUBLISHED
            || publishStatus == RewritePublishStatus.PAUSED
            || publishStatus == RewritePublishStatus.UNPUBLISH_FAILED) {
            return;
        }
        throw new IllegalStateException("只有 PUBLISHED、PAUSED 或 UNPUBLISH_FAILED 状态的改写记录才能下线");
    }

    public SqlRewriteRecord withReview(RewriteReviewStatus nextReviewStatus,
                                       String nextReviewNote,
                                       String nextReviewedBy,
                                       Instant nextReviewedAt,
                                       Map<String, Object> nextTraceRefs) {
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
            .autoApplyAllowed(nextReviewStatus == RewriteReviewStatus.APPROVED)
            .manualReviewRequired(manualReviewRequired)
            .reviewStatus(nextReviewStatus)
            .reviewNote(nextReviewNote)
            .reviewedBy(nextReviewedBy)
            .reviewedAt(nextReviewedAt)
            .publishStatus(publishStatus)
            .runtimeBindingId(runtimeBindingId)
            .runtimeBindingAt(runtimeBindingAt)
            .runtimeBindingBy(runtimeBindingBy)
            .runtimeBindingScope(runtimeBindingScope)
            .publishedSqlFingerprint(publishedSqlFingerprint)
            .runtimeRuleVersion(runtimeRuleVersion)
            .validationPolicyId(validationPolicyId)
            .lastValidationRunId(lastValidationRunId)
            .lastComparedAt(lastComparedAt)
            .alertStatus(alertStatus)
            .originalSqlText(originalSqlText)
            .recommendedSqlText(recommendedSqlText)
            .executedSqlText(executedSqlText)
            .createdBy(createdBy)
            .createdAt(createdAt)
            .updatedAt(nextReviewedAt)
            .ruleChain(ruleChain)
            .diffSummary(diffSummary)
            .risk(risk)
            .traceRefs(nextTraceRefs)
            .build();
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
            .reviewStatus(reviewStatus)
            .reviewNote(reviewNote)
            .reviewedBy(reviewedBy)
            .reviewedAt(reviewedAt)
            .publishStatus(publishStatus)
            .runtimeBindingId(runtimeBindingId)
            .runtimeBindingAt(runtimeBindingAt)
            .runtimeBindingBy(runtimeBindingBy)
            .runtimeBindingScope(runtimeBindingScope)
            .publishedSqlFingerprint(publishedSqlFingerprint)
            .runtimeRuleVersion(runtimeRuleVersion)
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
            .reviewStatus(reviewStatus)
            .reviewNote(reviewNote)
            .reviewedBy(reviewedBy)
            .reviewedAt(reviewedAt)
            .publishStatus(publishStatus)
            .runtimeBindingId(runtimeBindingId)
            .runtimeBindingAt(runtimeBindingAt)
            .runtimeBindingBy(runtimeBindingBy)
            .runtimeBindingScope(runtimeBindingScope)
            .publishedSqlFingerprint(publishedSqlFingerprint)
            .runtimeRuleVersion(runtimeRuleVersion)
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

    public SqlRewriteRecord withPublishedRuntimeBinding(String nextRuntimeBindingId,
                                                        String nextRuntimeRuleVersion,
                                                        String nextPublishedSqlFingerprint,
                                                        String nextRuntimeBindingScope,
                                                        String operator,
                                                        Instant updatedAt,
                                                        Map<String, Object> nextTraceRefs) {
        return copyBuilder(updatedAt, nextTraceRefs)
            .publishStatus(RewritePublishStatus.PUBLISHED)
            .runtimeBindingId(nextRuntimeBindingId)
            .runtimeBindingAt(updatedAt)
            .runtimeBindingBy(operator)
            .runtimeBindingScope(nextRuntimeBindingScope)
            .publishedSqlFingerprint(nextPublishedSqlFingerprint)
            .runtimeRuleVersion(nextRuntimeRuleVersion)
            .build();
    }

    public SqlRewriteRecord withPublishFailed(String operator,
                                              Instant updatedAt,
                                              Map<String, Object> nextTraceRefs) {
        return copyBuilder(updatedAt, nextTraceRefs)
            .publishStatus(RewritePublishStatus.PUBLISH_FAILED)
            .runtimeBindingAt(updatedAt)
            .runtimeBindingBy(operator)
            .build();
    }

    public SqlRewriteRecord withPausedRuntimeBinding(String operator,
                                                    Instant updatedAt,
                                                    Map<String, Object> nextTraceRefs) {
        return copyBuilder(updatedAt, nextTraceRefs)
            .publishStatus(RewritePublishStatus.PAUSED)
            .runtimeBindingAt(updatedAt)
            .runtimeBindingBy(operator)
            .build();
    }

    public SqlRewriteRecord withUnpublishedRuntimeBinding(String operator,
                                                         Instant updatedAt,
                                                         Map<String, Object> nextTraceRefs) {
        return copyBuilder(updatedAt, nextTraceRefs)
            .publishStatus(RewritePublishStatus.UNPUBLISHED)
            .runtimeBindingAt(updatedAt)
            .runtimeBindingBy(operator)
            .build();
    }

    public SqlRewriteRecord withUnpublishFailed(String operator,
                                                Instant updatedAt,
                                                Map<String, Object> nextTraceRefs) {
        return copyBuilder(updatedAt, nextTraceRefs)
            .publishStatus(RewritePublishStatus.UNPUBLISH_FAILED)
            .runtimeBindingAt(updatedAt)
            .runtimeBindingBy(operator)
            .build();
    }

    private Builder copyBuilder(Instant nextUpdatedAt, Map<String, Object> nextTraceRefs) {
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
            .reviewStatus(reviewStatus)
            .reviewNote(reviewNote)
            .reviewedBy(reviewedBy)
            .reviewedAt(reviewedAt)
            .publishStatus(publishStatus)
            .runtimeBindingId(runtimeBindingId)
            .runtimeBindingAt(runtimeBindingAt)
            .runtimeBindingBy(runtimeBindingBy)
            .runtimeBindingScope(runtimeBindingScope)
            .publishedSqlFingerprint(publishedSqlFingerprint)
            .runtimeRuleVersion(runtimeRuleVersion)
            .validationPolicyId(validationPolicyId)
            .lastValidationRunId(lastValidationRunId)
            .lastComparedAt(lastComparedAt)
            .alertStatus(alertStatus)
            .originalSqlText(originalSqlText)
            .recommendedSqlText(recommendedSqlText)
            .executedSqlText(executedSqlText)
            .createdBy(createdBy)
            .createdAt(createdAt)
            .updatedAt(nextUpdatedAt)
            .ruleChain(ruleChain)
            .diffSummary(diffSummary)
            .risk(risk)
            .traceRefs(nextTraceRefs);
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
            throw new IllegalArgumentException("sourceType 为必填项");
        }
        if (sourceKind == null) {
            throw new IllegalArgumentException("sourceKind 为必填项");
        }
        requireText(sourceId, "sourceId");
        if (evidenceLevel == null) {
            throw new IllegalArgumentException("evidenceLevel 为必填项");
        }
        requireText(originalSqlText, "originalSqlText");
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
    public RewriteReviewStatus getReviewStatus() { return reviewStatus; }
    public String getReviewNote() { return reviewNote; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public RewritePublishStatus getPublishStatus() { return publishStatus; }
    public String getRuntimeBindingId() { return runtimeBindingId; }
    public Instant getRuntimeBindingAt() { return runtimeBindingAt; }
    public String getRuntimeBindingBy() { return runtimeBindingBy; }
    public String getRuntimeBindingScope() { return runtimeBindingScope; }
    public String getPublishedSqlFingerprint() { return publishedSqlFingerprint; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
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
        private RewriteReviewStatus reviewStatus;
        private String reviewNote;
        private String reviewedBy;
        private Instant reviewedAt;
        private RewritePublishStatus publishStatus;
        private String runtimeBindingId;
        private Instant runtimeBindingAt;
        private String runtimeBindingBy;
        private String runtimeBindingScope;
        private String publishedSqlFingerprint;
        private String runtimeRuleVersion;
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
        public Builder reviewStatus(RewriteReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; return this; }
        public Builder reviewNote(String reviewNote) { this.reviewNote = reviewNote; return this; }
        public Builder reviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; return this; }
        public Builder reviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public Builder publishStatus(RewritePublishStatus publishStatus) { this.publishStatus = publishStatus; return this; }
        public Builder runtimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; return this; }
        public Builder runtimeBindingAt(Instant runtimeBindingAt) { this.runtimeBindingAt = runtimeBindingAt; return this; }
        public Builder runtimeBindingBy(String runtimeBindingBy) { this.runtimeBindingBy = runtimeBindingBy; return this; }
        public Builder runtimeBindingScope(String runtimeBindingScope) { this.runtimeBindingScope = runtimeBindingScope; return this; }
        public Builder publishedSqlFingerprint(String publishedSqlFingerprint) { this.publishedSqlFingerprint = publishedSqlFingerprint; return this; }
        public Builder runtimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; return this; }
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
