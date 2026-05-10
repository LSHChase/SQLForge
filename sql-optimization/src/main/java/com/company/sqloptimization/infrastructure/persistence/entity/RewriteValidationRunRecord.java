package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class RewriteValidationRunRecord {

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
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String comparisonPolicyJson;
    private String originalResultDigestJson;
    private String recommendedResultDigestJson;
    private String differenceSampleJson;
    private String executionEvidenceJson;

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
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getComparisonPolicyJson() { return comparisonPolicyJson; }
    public void setComparisonPolicyJson(String comparisonPolicyJson) { this.comparisonPolicyJson = comparisonPolicyJson; }
    public String getOriginalResultDigestJson() { return originalResultDigestJson; }
    public void setOriginalResultDigestJson(String originalResultDigestJson) { this.originalResultDigestJson = originalResultDigestJson; }
    public String getRecommendedResultDigestJson() { return recommendedResultDigestJson; }
    public void setRecommendedResultDigestJson(String recommendedResultDigestJson) { this.recommendedResultDigestJson = recommendedResultDigestJson; }
    public String getDifferenceSampleJson() { return differenceSampleJson; }
    public void setDifferenceSampleJson(String differenceSampleJson) { this.differenceSampleJson = differenceSampleJson; }
    public String getExecutionEvidenceJson() { return executionEvidenceJson; }
    public void setExecutionEvidenceJson(String executionEvidenceJson) { this.executionEvidenceJson = executionEvidenceJson; }
}
