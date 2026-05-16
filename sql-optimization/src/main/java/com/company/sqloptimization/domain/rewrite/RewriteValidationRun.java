package com.company.sqloptimization.domain.rewrite;

import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RewriteValidationRun {

    private final String validationRunId;
    private final String tenantId;
    private final String rewriteRecordId;
    private final String recommendationId;
    private final String historyId;
    private final String sqlFingerprint;
    private final ValidationRunStatus status;
    private final ComparisonStatus comparisonStatus;
    private final DifferenceType differenceType;
    private final boolean autoApplyPaused;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Map<String, Object> comparisonPolicy;
    private final Map<String, Object> originalResultDigest;
    private final Map<String, Object> recommendedResultDigest;
    private final Map<String, Object> differenceSample;
    private final Map<String, Object> executionEvidence;

    private RewriteValidationRun(Builder builder) {
        this.validationRunId = builder.validationRunId;
        this.tenantId = builder.tenantId;
        this.rewriteRecordId = builder.rewriteRecordId;
        this.recommendationId = builder.recommendationId;
        this.historyId = builder.historyId;
        this.sqlFingerprint = builder.sqlFingerprint;
        this.status = builder.status == null ? ValidationRunStatus.PENDING : builder.status;
        this.comparisonStatus = builder.comparisonStatus == null ? ComparisonStatus.NOT_COMPARED : builder.comparisonStatus;
        this.differenceType = builder.differenceType == null ? DifferenceType.NONE : builder.differenceType;
        this.autoApplyPaused = builder.autoApplyPaused;
        this.startedAt = builder.startedAt;
        this.finishedAt = builder.finishedAt;
        this.comparisonPolicy = immutableCopy(builder.comparisonPolicy);
        this.originalResultDigest = immutableCopy(builder.originalResultDigest);
        this.recommendedResultDigest = immutableCopy(builder.recommendedResultDigest);
        this.differenceSample = immutableCopy(builder.differenceSample);
        this.executionEvidence = immutableCopy(builder.executionEvidence);
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        requireText(validationRunId, "validationRunId");
        requireText(tenantId, "tenantId");
        requireText(rewriteRecordId, "rewriteRecordId");
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt 为必填项");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    private Map<String, Object> immutableCopy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
    }

    public String getValidationRunId() { return validationRunId; }
    public String getTenantId() { return tenantId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public String getRecommendationId() { return recommendationId; }
    public String getHistoryId() { return historyId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public ValidationRunStatus getStatus() { return status; }
    public ComparisonStatus getComparisonStatus() { return comparisonStatus; }
    public DifferenceType getDifferenceType() { return differenceType; }
    public boolean isAutoApplyPaused() { return autoApplyPaused; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Map<String, Object> getComparisonPolicy() { return comparisonPolicy; }
    public Map<String, Object> getOriginalResultDigest() { return originalResultDigest; }
    public Map<String, Object> getRecommendedResultDigest() { return recommendedResultDigest; }
    public Map<String, Object> getDifferenceSample() { return differenceSample; }
    public Map<String, Object> getExecutionEvidence() { return executionEvidence; }

    public static final class Builder {
        private String validationRunId;
        private String tenantId;
        private String rewriteRecordId;
        private String recommendationId;
        private String historyId;
        private String sqlFingerprint;
        private ValidationRunStatus status;
        private ComparisonStatus comparisonStatus;
        private DifferenceType differenceType;
        private boolean autoApplyPaused;
        private Instant startedAt;
        private Instant finishedAt;
        private Map<String, Object> comparisonPolicy;
        private Map<String, Object> originalResultDigest;
        private Map<String, Object> recommendedResultDigest;
        private Map<String, Object> differenceSample;
        private Map<String, Object> executionEvidence;

        private Builder() {
        }

        public Builder validationRunId(String validationRunId) { this.validationRunId = validationRunId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder rewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; return this; }
        public Builder recommendationId(String recommendationId) { this.recommendationId = recommendationId; return this; }
        public Builder historyId(String historyId) { this.historyId = historyId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder status(ValidationRunStatus status) { this.status = status; return this; }
        public Builder comparisonStatus(ComparisonStatus comparisonStatus) { this.comparisonStatus = comparisonStatus; return this; }
        public Builder differenceType(DifferenceType differenceType) { this.differenceType = differenceType; return this; }
        public Builder autoApplyPaused(boolean autoApplyPaused) { this.autoApplyPaused = autoApplyPaused; return this; }
        public Builder startedAt(Instant startedAt) { this.startedAt = startedAt; return this; }
        public Builder finishedAt(Instant finishedAt) { this.finishedAt = finishedAt; return this; }
        public Builder comparisonPolicy(Map<String, Object> comparisonPolicy) { this.comparisonPolicy = comparisonPolicy; return this; }
        public Builder originalResultDigest(Map<String, Object> originalResultDigest) { this.originalResultDigest = originalResultDigest; return this; }
        public Builder recommendedResultDigest(Map<String, Object> recommendedResultDigest) { this.recommendedResultDigest = recommendedResultDigest; return this; }
        public Builder differenceSample(Map<String, Object> differenceSample) { this.differenceSample = differenceSample; return this; }
        public Builder executionEvidence(Map<String, Object> executionEvidence) { this.executionEvidence = executionEvidence; return this; }

        public RewriteValidationRun build() {
            return new RewriteValidationRun(this);
        }
    }
}
