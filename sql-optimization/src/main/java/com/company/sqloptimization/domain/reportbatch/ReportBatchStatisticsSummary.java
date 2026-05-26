package com.company.sqloptimization.domain.reportbatch;

import java.time.Instant;

public class ReportBatchStatisticsSummary {

    private final String batchId;
    private final String tenantId;
    private final int totalSqlCount;
    private final int resolvedSqlCount;
    private final int partialResolvedSqlCount;
    private final int failedSqlCount;
    private final int issueSqlCount;
    private final int totalIssueCount;
    private final int issueSceneCount;
    private final int importantSqlCount;
    private final int urgentSqlCount;
    private final int mergeCandidateReportCount;
    private final int planAnalysisSuccessCount;
    private final int planAnalysisPartialCount;
    private final int planAnalysisFailedCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ReportBatchStatisticsSummary(String batchId,
                                        String tenantId,
                                        int totalSqlCount,
                                        int resolvedSqlCount,
                                        int partialResolvedSqlCount,
                                        int failedSqlCount,
                                        int issueSqlCount,
                                        int totalIssueCount,
                                        int issueSceneCount,
                                        int importantSqlCount,
                                        int urgentSqlCount,
                                        int mergeCandidateReportCount,
                                        int planAnalysisSuccessCount,
                                        int planAnalysisPartialCount,
                                        int planAnalysisFailedCount,
                                        Instant createdAt,
                                        Instant updatedAt) {
        this.batchId = batchId;
        this.tenantId = tenantId;
        this.totalSqlCount = totalSqlCount;
        this.resolvedSqlCount = resolvedSqlCount;
        this.partialResolvedSqlCount = partialResolvedSqlCount;
        this.failedSqlCount = failedSqlCount;
        this.issueSqlCount = issueSqlCount;
        this.totalIssueCount = totalIssueCount;
        this.issueSceneCount = issueSceneCount;
        this.importantSqlCount = importantSqlCount;
        this.urgentSqlCount = urgentSqlCount;
        this.mergeCandidateReportCount = mergeCandidateReportCount;
        this.planAnalysisSuccessCount = planAnalysisSuccessCount;
        this.planAnalysisPartialCount = planAnalysisPartialCount;
        this.planAnalysisFailedCount = planAnalysisFailedCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getBatchId() { return batchId; }
    public String getTenantId() { return tenantId; }
    public int getTotalSqlCount() { return totalSqlCount; }
    public int getResolvedSqlCount() { return resolvedSqlCount; }
    public int getPartialResolvedSqlCount() { return partialResolvedSqlCount; }
    public int getFailedSqlCount() { return failedSqlCount; }
    public int getNonResolvedSqlCount() { return partialResolvedSqlCount + failedSqlCount; }
    public int getIssueSqlCount() { return issueSqlCount; }
    public int getTotalIssueCount() { return totalIssueCount; }
    public int getIssueSceneCount() { return issueSceneCount; }
    public int getImportantSqlCount() { return importantSqlCount; }
    public int getUrgentSqlCount() { return urgentSqlCount; }
    public int getMergeCandidateReportCount() { return mergeCandidateReportCount; }
    public int getPlanAnalysisSuccessCount() { return planAnalysisSuccessCount; }
    public int getPlanAnalysisPartialCount() { return planAnalysisPartialCount; }
    public int getPlanAnalysisFailedCount() { return planAnalysisFailedCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
