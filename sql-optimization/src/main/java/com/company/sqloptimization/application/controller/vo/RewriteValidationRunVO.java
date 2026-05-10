package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.Map;

public class RewriteValidationRunVO {

    private String validationRunId;
    private String tenantId;
    private String rewriteRecordId;
    private String recommendationId;
    private String historyId;
    private String sqlFingerprint;
    private String status;
    private String comparisonStatus;
    private String differenceType;
    private Boolean autoApplyPaused;
    private Instant startedAt;
    private Instant finishedAt;
    private Map<String, Object> comparisonPolicy;
    private Map<String, Object> originalResultDigest;
    private Map<String, Object> recommendedResultDigest;
    private Map<String, Object> differenceSample;
    private Map<String, Object> executionEvidence;
    private String contractStage;
    private String implementationStage;

    public String getValidationRunId() { return validationRunId; }
    public void setValidationRunId(String validationRunId) { this.validationRunId = validationRunId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComparisonStatus() { return comparisonStatus; }
    public void setComparisonStatus(String comparisonStatus) { this.comparisonStatus = comparisonStatus; }
    public String getDifferenceType() { return differenceType; }
    public void setDifferenceType(String differenceType) { this.differenceType = differenceType; }
    public Boolean getAutoApplyPaused() { return autoApplyPaused; }
    public void setAutoApplyPaused(Boolean autoApplyPaused) { this.autoApplyPaused = autoApplyPaused; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Map<String, Object> getComparisonPolicy() { return comparisonPolicy; }
    public void setComparisonPolicy(Map<String, Object> comparisonPolicy) { this.comparisonPolicy = comparisonPolicy; }
    public Map<String, Object> getOriginalResultDigest() { return originalResultDigest; }
    public void setOriginalResultDigest(Map<String, Object> originalResultDigest) { this.originalResultDigest = originalResultDigest; }
    public Map<String, Object> getRecommendedResultDigest() { return recommendedResultDigest; }
    public void setRecommendedResultDigest(Map<String, Object> recommendedResultDigest) { this.recommendedResultDigest = recommendedResultDigest; }
    public Map<String, Object> getDifferenceSample() { return differenceSample; }
    public void setDifferenceSample(Map<String, Object> differenceSample) { this.differenceSample = differenceSample; }
    public Map<String, Object> getExecutionEvidence() { return executionEvidence; }
    public void setExecutionEvidence(Map<String, Object> executionEvidence) { this.executionEvidence = executionEvidence; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
