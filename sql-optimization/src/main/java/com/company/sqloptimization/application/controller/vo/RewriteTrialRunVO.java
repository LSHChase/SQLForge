package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class RewriteTrialRunVO {

    private String runId;
    private String tenantId;
    private String sourceKind;
    private String sourceId;
    private String batchId;
    private String trialStatus;
    private Integer totalCount;
    private Integer acceptedCount;
    private Integer skippedCount;
    private Integer queuedCount;
    private Integer runningCount;
    private Integer candidateGeneratedCount;
    private Integer noSafeRewriteCount;
    private Integer failedCount;
    private Integer recommendedCount;
    private Integer recordCreatedCount;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RewriteTrialItemVO> items;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getTrialStatus() { return trialStatus; }
    public void setTrialStatus(String trialStatus) { this.trialStatus = trialStatus; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(Integer acceptedCount) { this.acceptedCount = acceptedCount; }
    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }
    public Integer getQueuedCount() { return queuedCount; }
    public void setQueuedCount(Integer queuedCount) { this.queuedCount = queuedCount; }
    public Integer getRunningCount() { return runningCount; }
    public void setRunningCount(Integer runningCount) { this.runningCount = runningCount; }
    public Integer getCandidateGeneratedCount() { return candidateGeneratedCount; }
    public void setCandidateGeneratedCount(Integer candidateGeneratedCount) { this.candidateGeneratedCount = candidateGeneratedCount; }
    public Integer getNoSafeRewriteCount() { return noSafeRewriteCount; }
    public void setNoSafeRewriteCount(Integer noSafeRewriteCount) { this.noSafeRewriteCount = noSafeRewriteCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public Integer getRecommendedCount() { return recommendedCount; }
    public void setRecommendedCount(Integer recommendedCount) { this.recommendedCount = recommendedCount; }
    public Integer getRecordCreatedCount() { return recordCreatedCount; }
    public void setRecordCreatedCount(Integer recordCreatedCount) { this.recordCreatedCount = recordCreatedCount; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<RewriteTrialItemVO> getItems() { return items; }
    public void setItems(List<RewriteTrialItemVO> items) { this.items = items; }
}
