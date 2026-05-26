package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public final class ReportBatchStatisticsRecords {

    private ReportBatchStatisticsRecords() {
    }

    public static class SummaryRecord {
        private String batchId;
        private String tenantId;
        private Integer totalSqlCount;
        private Integer resolvedSqlCount;
        private Integer partialResolvedSqlCount;
        private Integer failedSqlCount;
        private Integer issueSqlCount;
        private Integer totalIssueCount;
        private Integer issueSceneCount;
        private Integer importantSqlCount;
        private Integer urgentSqlCount;
        private Integer mergeCandidateReportCount;
        private Integer planAnalysisSuccessCount;
        private Integer planAnalysisPartialCount;
        private Integer planAnalysisFailedCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public Integer getTotalSqlCount() { return totalSqlCount; }
        public void setTotalSqlCount(Integer totalSqlCount) { this.totalSqlCount = totalSqlCount; }
        public Integer getResolvedSqlCount() { return resolvedSqlCount; }
        public void setResolvedSqlCount(Integer resolvedSqlCount) { this.resolvedSqlCount = resolvedSqlCount; }
        public Integer getPartialResolvedSqlCount() { return partialResolvedSqlCount; }
        public void setPartialResolvedSqlCount(Integer partialResolvedSqlCount) { this.partialResolvedSqlCount = partialResolvedSqlCount; }
        public Integer getFailedSqlCount() { return failedSqlCount; }
        public void setFailedSqlCount(Integer failedSqlCount) { this.failedSqlCount = failedSqlCount; }
        public Integer getIssueSqlCount() { return issueSqlCount; }
        public void setIssueSqlCount(Integer issueSqlCount) { this.issueSqlCount = issueSqlCount; }
        public Integer getTotalIssueCount() { return totalIssueCount; }
        public void setTotalIssueCount(Integer totalIssueCount) { this.totalIssueCount = totalIssueCount; }
        public Integer getIssueSceneCount() { return issueSceneCount; }
        public void setIssueSceneCount(Integer issueSceneCount) { this.issueSceneCount = issueSceneCount; }
        public Integer getImportantSqlCount() { return importantSqlCount; }
        public void setImportantSqlCount(Integer importantSqlCount) { this.importantSqlCount = importantSqlCount; }
        public Integer getUrgentSqlCount() { return urgentSqlCount; }
        public void setUrgentSqlCount(Integer urgentSqlCount) { this.urgentSqlCount = urgentSqlCount; }
        public Integer getMergeCandidateReportCount() { return mergeCandidateReportCount; }
        public void setMergeCandidateReportCount(Integer mergeCandidateReportCount) { this.mergeCandidateReportCount = mergeCandidateReportCount; }
        public Integer getPlanAnalysisSuccessCount() { return planAnalysisSuccessCount; }
        public void setPlanAnalysisSuccessCount(Integer planAnalysisSuccessCount) { this.planAnalysisSuccessCount = planAnalysisSuccessCount; }
        public Integer getPlanAnalysisPartialCount() { return planAnalysisPartialCount; }
        public void setPlanAnalysisPartialCount(Integer planAnalysisPartialCount) { this.planAnalysisPartialCount = planAnalysisPartialCount; }
        public Integer getPlanAnalysisFailedCount() { return planAnalysisFailedCount; }
        public void setPlanAnalysisFailedCount(Integer planAnalysisFailedCount) { this.planAnalysisFailedCount = planAnalysisFailedCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class IssueSceneRecord {
        private String batchId;
        private String tenantId;
        private String issueScene;
        private String issueDomain;
        private String severity;
        private String priorityLevel;
        private Integer priorityScore;
        private Integer affectedSqlCount;
        private Integer affectedIssueCount;
        private Double sqlRatio;
        private Boolean important;
        private Boolean urgent;
        private Integer reportCount;
        private Integer logicalObjectCount;
        private String sampleReportCodesJson;
        private String sampleLogicalObjectKeysJson;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getIssueScene() { return issueScene; }
        public void setIssueScene(String issueScene) { this.issueScene = issueScene; }
        public String getIssueDomain() { return issueDomain; }
        public void setIssueDomain(String issueDomain) { this.issueDomain = issueDomain; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
        public Integer getPriorityScore() { return priorityScore; }
        public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }
        public Integer getAffectedSqlCount() { return affectedSqlCount; }
        public void setAffectedSqlCount(Integer affectedSqlCount) { this.affectedSqlCount = affectedSqlCount; }
        public Integer getAffectedIssueCount() { return affectedIssueCount; }
        public void setAffectedIssueCount(Integer affectedIssueCount) { this.affectedIssueCount = affectedIssueCount; }
        public Double getSqlRatio() { return sqlRatio; }
        public void setSqlRatio(Double sqlRatio) { this.sqlRatio = sqlRatio; }
        public Boolean getImportant() { return important; }
        public void setImportant(Boolean important) { this.important = important; }
        public Boolean getUrgent() { return urgent; }
        public void setUrgent(Boolean urgent) { this.urgent = urgent; }
        public Integer getReportCount() { return reportCount; }
        public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
        public Integer getLogicalObjectCount() { return logicalObjectCount; }
        public void setLogicalObjectCount(Integer logicalObjectCount) { this.logicalObjectCount = logicalObjectCount; }
        public String getSampleReportCodesJson() { return sampleReportCodesJson; }
        public void setSampleReportCodesJson(String sampleReportCodesJson) { this.sampleReportCodesJson = sampleReportCodesJson; }
        public String getSampleLogicalObjectKeysJson() { return sampleLogicalObjectKeysJson; }
        public void setSampleLogicalObjectKeysJson(String sampleLogicalObjectKeysJson) { this.sampleLogicalObjectKeysJson = sampleLogicalObjectKeysJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class SeverityRecord {
        private String batchId;
        private String tenantId;
        private String severity;
        private Integer issueCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Integer getIssueCount() { return issueCount; }
        public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ReportRecord {
        private String batchId;
        private String tenantId;
        private String reportCode;
        private Integer sqlCount;
        private Integer issueSqlCount;
        private Integer issueCount;
        private Double issueSqlRatio;
        private String highestPriorityLevel;
        private Integer highestPriorityScore;
        private Boolean important;
        private Boolean urgent;
        private String issueScenesJson;
        private Boolean mergeCandidate;
        private Integer mergeCandidateSqlCount;
        private String mergeCandidateReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getReportCode() { return reportCode; }
        public void setReportCode(String reportCode) { this.reportCode = reportCode; }
        public Integer getSqlCount() { return sqlCount; }
        public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
        public Integer getIssueSqlCount() { return issueSqlCount; }
        public void setIssueSqlCount(Integer issueSqlCount) { this.issueSqlCount = issueSqlCount; }
        public Integer getIssueCount() { return issueCount; }
        public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
        public Double getIssueSqlRatio() { return issueSqlRatio; }
        public void setIssueSqlRatio(Double issueSqlRatio) { this.issueSqlRatio = issueSqlRatio; }
        public String getHighestPriorityLevel() { return highestPriorityLevel; }
        public void setHighestPriorityLevel(String highestPriorityLevel) { this.highestPriorityLevel = highestPriorityLevel; }
        public Integer getHighestPriorityScore() { return highestPriorityScore; }
        public void setHighestPriorityScore(Integer highestPriorityScore) { this.highestPriorityScore = highestPriorityScore; }
        public Boolean getImportant() { return important; }
        public void setImportant(Boolean important) { this.important = important; }
        public Boolean getUrgent() { return urgent; }
        public void setUrgent(Boolean urgent) { this.urgent = urgent; }
        public String getIssueScenesJson() { return issueScenesJson; }
        public void setIssueScenesJson(String issueScenesJson) { this.issueScenesJson = issueScenesJson; }
        public Boolean getMergeCandidate() { return mergeCandidate; }
        public void setMergeCandidate(Boolean mergeCandidate) { this.mergeCandidate = mergeCandidate; }
        public Integer getMergeCandidateSqlCount() { return mergeCandidateSqlCount; }
        public void setMergeCandidateSqlCount(Integer mergeCandidateSqlCount) { this.mergeCandidateSqlCount = mergeCandidateSqlCount; }
        public String getMergeCandidateReason() { return mergeCandidateReason; }
        public void setMergeCandidateReason(String mergeCandidateReason) { this.mergeCandidateReason = mergeCandidateReason; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class SqlRecord {
        private String batchId;
        private String tenantId;
        private String itemId;
        private String parseTaskId;
        private String reportCode;
        private String reportName;
        private String datasourceCode;
        private String stage;
        private String sqlColumnName;
        private Integer sqlOrdinalInReport;
        private String status;
        private String sqlDigest;
        private Integer issueCount;
        private String highestPriorityLevel;
        private Integer highestPriorityScore;
        private Boolean important;
        private Boolean urgent;
        private String issueLocationsJson;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getParseTaskId() { return parseTaskId; }
        public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
        public String getReportCode() { return reportCode; }
        public void setReportCode(String reportCode) { this.reportCode = reportCode; }
        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }
        public String getDatasourceCode() { return datasourceCode; }
        public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getSqlColumnName() { return sqlColumnName; }
        public void setSqlColumnName(String sqlColumnName) { this.sqlColumnName = sqlColumnName; }
        public Integer getSqlOrdinalInReport() { return sqlOrdinalInReport; }
        public void setSqlOrdinalInReport(Integer sqlOrdinalInReport) { this.sqlOrdinalInReport = sqlOrdinalInReport; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSqlDigest() { return sqlDigest; }
        public void setSqlDigest(String sqlDigest) { this.sqlDigest = sqlDigest; }
        public Integer getIssueCount() { return issueCount; }
        public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
        public String getHighestPriorityLevel() { return highestPriorityLevel; }
        public void setHighestPriorityLevel(String highestPriorityLevel) { this.highestPriorityLevel = highestPriorityLevel; }
        public Integer getHighestPriorityScore() { return highestPriorityScore; }
        public void setHighestPriorityScore(Integer highestPriorityScore) { this.highestPriorityScore = highestPriorityScore; }
        public Boolean getImportant() { return important; }
        public void setImportant(Boolean important) { this.important = important; }
        public Boolean getUrgent() { return urgent; }
        public void setUrgent(Boolean urgent) { this.urgent = urgent; }
        public String getIssueLocationsJson() { return issueLocationsJson; }
        public void setIssueLocationsJson(String issueLocationsJson) { this.issueLocationsJson = issueLocationsJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class SqlIssueSceneRecord {
        private String batchId;
        private String tenantId;
        private String itemId;
        private String issueScene;
        private LocalDateTime createdAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getIssueScene() { return issueScene; }
        public void setIssueScene(String issueScene) { this.issueScene = issueScene; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class SqlLogicalObjectRecord {
        private String batchId;
        private String tenantId;
        private String itemId;
        private String logicalObjectKey;
        private LocalDateTime createdAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getLogicalObjectKey() { return logicalObjectKey; }
        public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class PriorityRecord {
        private String batchId;
        private String tenantId;
        private String priorityLevel;
        private String urgencyBucket;
        private Integer sqlCount;
        private Integer issueCount;
        private Integer reportCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
        public String getUrgencyBucket() { return urgencyBucket; }
        public void setUrgencyBucket(String urgencyBucket) { this.urgencyBucket = urgencyBucket; }
        public Integer getSqlCount() { return sqlCount; }
        public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
        public Integer getIssueCount() { return issueCount; }
        public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
        public Integer getReportCount() { return reportCount; }
        public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ImportanceRecord {
        private String batchId;
        private String tenantId;
        private String importanceBucket;
        private Integer sqlCount;
        private Integer issueCount;
        private Integer reportCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getImportanceBucket() { return importanceBucket; }
        public void setImportanceBucket(String importanceBucket) { this.importanceBucket = importanceBucket; }
        public Integer getSqlCount() { return sqlCount; }
        public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
        public Integer getIssueCount() { return issueCount; }
        public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
        public Integer getReportCount() { return reportCount; }
        public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class LogicalObjectRecord {
        private String batchId;
        private String tenantId;
        private String logicalObjectKey;
        private Integer sqlCount;
        private Integer issueCount;
        private Integer reportCount;
        private String reportCodesJson;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getLogicalObjectKey() { return logicalObjectKey; }
        public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
        public Integer getSqlCount() { return sqlCount; }
        public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
        public Integer getIssueCount() { return issueCount; }
        public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
        public Integer getReportCount() { return reportCount; }
        public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
        public String getReportCodesJson() { return reportCodesJson; }
        public void setReportCodesJson(String reportCodesJson) { this.reportCodesJson = reportCodesJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
