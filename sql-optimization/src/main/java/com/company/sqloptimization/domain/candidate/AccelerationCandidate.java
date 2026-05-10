package com.company.sqloptimization.domain.candidate;

import com.company.sqloptimization.domain.governance.CandidateStatus;
import com.company.sqloptimization.domain.governance.CandidateType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AccelerationCandidate {

    private final String candidateId;
    private final String tenantId;
    private final GovernanceSourceType sourceType;
    private final GovernanceSourceKind sourceKind;
    private final String sourceId;
    private final String historyId;
    private final String parseHistoryId;
    private final String parseTaskId;
    private final String batchId;
    private final String batchItemId;
    private final String benchmarkTaskId;
    private final String optimizationTaskId;
    private final String sqlFingerprint;
    private final String datasourceCode;
    private final String stage;
    private final String reportCode;
    private final CandidateType candidateType;
    private final CandidateStatus status;
    private final BigDecimal confidence;
    private final Integer priority;
    private final EvidenceLevel evidenceLevel;
    private final String schemaVersion;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Map<String, Object> sourceEvidence;
    private final Map<String, Object> issueEvidence;
    private final Map<String, Object> runtimeEvidence;
    private final Map<String, Object> benefitEstimate;
    private final Map<String, Object> costEstimate;
    private final Map<String, Object> risk;

    private AccelerationCandidate(Builder builder) {
        this.candidateId = builder.candidateId;
        this.tenantId = builder.tenantId;
        this.sourceType = builder.sourceType;
        this.sourceKind = builder.sourceKind;
        this.sourceId = builder.sourceId;
        this.historyId = builder.historyId;
        this.parseHistoryId = builder.parseHistoryId;
        this.parseTaskId = builder.parseTaskId;
        this.batchId = builder.batchId;
        this.batchItemId = builder.batchItemId;
        this.benchmarkTaskId = builder.benchmarkTaskId;
        this.optimizationTaskId = builder.optimizationTaskId;
        this.sqlFingerprint = builder.sqlFingerprint;
        this.datasourceCode = builder.datasourceCode;
        this.stage = builder.stage;
        this.reportCode = builder.reportCode;
        this.candidateType = builder.candidateType == null ? CandidateType.ACCELERATION_AND_REWRITE : builder.candidateType;
        this.status = builder.status == null ? CandidateStatus.DRAFT : builder.status;
        this.confidence = builder.confidence;
        this.priority = builder.priority;
        this.evidenceLevel = builder.evidenceLevel;
        this.schemaVersion = builder.schemaVersion;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        this.sourceEvidence = immutableCopy(builder.sourceEvidence);
        this.issueEvidence = immutableCopy(builder.issueEvidence);
        this.runtimeEvidence = immutableCopy(builder.runtimeEvidence);
        this.benefitEstimate = immutableCopy(builder.benefitEstimate);
        this.costEstimate = immutableCopy(builder.costEstimate);
        this.risk = immutableCopy(builder.risk);
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        requireText(candidateId, "candidateId");
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
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private Map<String, Object> immutableCopy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
    }

    public String getCandidateId() { return candidateId; }
    public String getTenantId() { return tenantId; }
    public GovernanceSourceType getSourceType() { return sourceType; }
    public GovernanceSourceKind getSourceKind() { return sourceKind; }
    public String getSourceId() { return sourceId; }
    public String getHistoryId() { return historyId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public String getParseTaskId() { return parseTaskId; }
    public String getBatchId() { return batchId; }
    public String getBatchItemId() { return batchItemId; }
    public String getBenchmarkTaskId() { return benchmarkTaskId; }
    public String getOptimizationTaskId() { return optimizationTaskId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public String getStage() { return stage; }
    public String getReportCode() { return reportCode; }
    public CandidateType getCandidateType() { return candidateType; }
    public CandidateStatus getStatus() { return status; }
    public BigDecimal getConfidence() { return confidence; }
    public Integer getPriority() { return priority; }
    public EvidenceLevel getEvidenceLevel() { return evidenceLevel; }
    public String getSchemaVersion() { return schemaVersion; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Map<String, Object> getSourceEvidence() { return sourceEvidence; }
    public Map<String, Object> getIssueEvidence() { return issueEvidence; }
    public Map<String, Object> getRuntimeEvidence() { return runtimeEvidence; }
    public Map<String, Object> getBenefitEstimate() { return benefitEstimate; }
    public Map<String, Object> getCostEstimate() { return costEstimate; }
    public Map<String, Object> getRisk() { return risk; }

    public static final class Builder {
        private String candidateId;
        private String tenantId;
        private GovernanceSourceType sourceType;
        private GovernanceSourceKind sourceKind;
        private String sourceId;
        private String historyId;
        private String parseHistoryId;
        private String parseTaskId;
        private String batchId;
        private String batchItemId;
        private String benchmarkTaskId;
        private String optimizationTaskId;
        private String sqlFingerprint;
        private String datasourceCode;
        private String stage;
        private String reportCode;
        private CandidateType candidateType;
        private CandidateStatus status;
        private BigDecimal confidence;
        private Integer priority;
        private EvidenceLevel evidenceLevel;
        private String schemaVersion;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;
        private Map<String, Object> sourceEvidence;
        private Map<String, Object> issueEvidence;
        private Map<String, Object> runtimeEvidence;
        private Map<String, Object> benefitEstimate;
        private Map<String, Object> costEstimate;
        private Map<String, Object> risk;

        private Builder() {
        }

        public Builder candidateId(String candidateId) { this.candidateId = candidateId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder sourceType(GovernanceSourceType sourceType) { this.sourceType = sourceType; return this; }
        public Builder sourceKind(GovernanceSourceKind sourceKind) { this.sourceKind = sourceKind; return this; }
        public Builder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public Builder historyId(String historyId) { this.historyId = historyId; return this; }
        public Builder parseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; return this; }
        public Builder parseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; return this; }
        public Builder batchId(String batchId) { this.batchId = batchId; return this; }
        public Builder batchItemId(String batchItemId) { this.batchItemId = batchItemId; return this; }
        public Builder benchmarkTaskId(String benchmarkTaskId) { this.benchmarkTaskId = benchmarkTaskId; return this; }
        public Builder optimizationTaskId(String optimizationTaskId) { this.optimizationTaskId = optimizationTaskId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder datasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; return this; }
        public Builder stage(String stage) { this.stage = stage; return this; }
        public Builder reportCode(String reportCode) { this.reportCode = reportCode; return this; }
        public Builder candidateType(CandidateType candidateType) { this.candidateType = candidateType; return this; }
        public Builder status(CandidateStatus status) { this.status = status; return this; }
        public Builder confidence(BigDecimal confidence) { this.confidence = confidence; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder evidenceLevel(EvidenceLevel evidenceLevel) { this.evidenceLevel = evidenceLevel; return this; }
        public Builder schemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder sourceEvidence(Map<String, Object> sourceEvidence) { this.sourceEvidence = sourceEvidence; return this; }
        public Builder issueEvidence(Map<String, Object> issueEvidence) { this.issueEvidence = issueEvidence; return this; }
        public Builder runtimeEvidence(Map<String, Object> runtimeEvidence) { this.runtimeEvidence = runtimeEvidence; return this; }
        public Builder benefitEstimate(Map<String, Object> benefitEstimate) { this.benefitEstimate = benefitEstimate; return this; }
        public Builder costEstimate(Map<String, Object> costEstimate) { this.costEstimate = costEstimate; return this; }
        public Builder risk(Map<String, Object> risk) { this.risk = risk; return this; }

        public AccelerationCandidate build() {
            return new AccelerationCandidate(this);
        }
    }
}
