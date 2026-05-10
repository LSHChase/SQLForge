package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.Instant;
import java.util.Map;

public class RewriteValidationRunCreateRequest {

    private String tenantId;
    private String recommendationId;
    private String historyId;
    private String sqlFingerprint;
    private DataSourceTypeEnum datasourceType;
    private String triggerReason;
    private ValidationRunStatus status;
    private ComparisonStatus comparisonStatus;
    private DifferenceType differenceType;
    private Boolean autoApplyPaused;
    private Instant startedAt;
    private Instant finishedAt;
    private Map<String, Object> comparisonPolicy;
    private Map<String, Object> originalResultDigest;
    private Map<String, Object> recommendedResultDigest;
    private Map<String, Object> differenceSample;
    private Map<String, Object> executionEvidence;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public DataSourceTypeEnum getDatasourceType() { return datasourceType; }
    public void setDatasourceType(DataSourceTypeEnum datasourceType) { this.datasourceType = datasourceType; }
    public String getTriggerReason() { return triggerReason; }
    public void setTriggerReason(String triggerReason) { this.triggerReason = triggerReason; }
    public ValidationRunStatus getStatus() { return status; }
    public void setStatus(ValidationRunStatus status) { this.status = status; }
    public ComparisonStatus getComparisonStatus() { return comparisonStatus; }
    public void setComparisonStatus(ComparisonStatus comparisonStatus) { this.comparisonStatus = comparisonStatus; }
    public DifferenceType getDifferenceType() { return differenceType; }
    public void setDifferenceType(DifferenceType differenceType) { this.differenceType = differenceType; }
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
}
