package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ReportBatchIssueSceneDetailVO {

    private String issueScene;
    private String issueDomain;
    private String severity;
    private String priorityLevel;
    private Integer priorityScore;
    private Integer affectedSqlCount;
    private Integer affectedIssueCount;
    private Integer reportCount;
    private Integer logicalObjectCount;
    private String reportCodeFilter;
    private String logicalObjectKeyFilter;
    private Integer reportDetailPageNumber;
    private Integer reportDetailPageSize;
    private Integer reportDetailPageCount;
    private Integer reportDetailTotalCount;
    private Integer logicalObjectDetailPageNumber;
    private Integer logicalObjectDetailPageSize;
    private Integer logicalObjectDetailPageCount;
    private Integer logicalObjectDetailTotalCount;
    private Integer sqlStatisticPageNumber;
    private Integer sqlStatisticPageSize;
    private Integer sqlStatisticPageCount;
    private Integer sqlStatisticTotalCount;
    private List<ReportBatchIssueSceneReportDetailVO> reportDetails;
    private List<ReportBatchIssueSceneLogicalObjectDetailVO> logicalObjectDetails;
    private List<ReportBatchSqlStatisticVO> sqlStatistics;

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
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Integer getLogicalObjectCount() { return logicalObjectCount; }
    public void setLogicalObjectCount(Integer logicalObjectCount) { this.logicalObjectCount = logicalObjectCount; }
    public String getReportCodeFilter() { return reportCodeFilter; }
    public void setReportCodeFilter(String reportCodeFilter) { this.reportCodeFilter = reportCodeFilter; }
    public String getLogicalObjectKeyFilter() { return logicalObjectKeyFilter; }
    public void setLogicalObjectKeyFilter(String logicalObjectKeyFilter) { this.logicalObjectKeyFilter = logicalObjectKeyFilter; }
    public Integer getReportDetailPageNumber() { return reportDetailPageNumber; }
    public void setReportDetailPageNumber(Integer reportDetailPageNumber) { this.reportDetailPageNumber = reportDetailPageNumber; }
    public Integer getReportDetailPageSize() { return reportDetailPageSize; }
    public void setReportDetailPageSize(Integer reportDetailPageSize) { this.reportDetailPageSize = reportDetailPageSize; }
    public Integer getReportDetailPageCount() { return reportDetailPageCount; }
    public void setReportDetailPageCount(Integer reportDetailPageCount) { this.reportDetailPageCount = reportDetailPageCount; }
    public Integer getReportDetailTotalCount() { return reportDetailTotalCount; }
    public void setReportDetailTotalCount(Integer reportDetailTotalCount) { this.reportDetailTotalCount = reportDetailTotalCount; }
    public Integer getLogicalObjectDetailPageNumber() { return logicalObjectDetailPageNumber; }
    public void setLogicalObjectDetailPageNumber(Integer logicalObjectDetailPageNumber) {
        this.logicalObjectDetailPageNumber = logicalObjectDetailPageNumber;
    }
    public Integer getLogicalObjectDetailPageSize() { return logicalObjectDetailPageSize; }
    public void setLogicalObjectDetailPageSize(Integer logicalObjectDetailPageSize) {
        this.logicalObjectDetailPageSize = logicalObjectDetailPageSize;
    }
    public Integer getLogicalObjectDetailPageCount() { return logicalObjectDetailPageCount; }
    public void setLogicalObjectDetailPageCount(Integer logicalObjectDetailPageCount) {
        this.logicalObjectDetailPageCount = logicalObjectDetailPageCount;
    }
    public Integer getLogicalObjectDetailTotalCount() { return logicalObjectDetailTotalCount; }
    public void setLogicalObjectDetailTotalCount(Integer logicalObjectDetailTotalCount) {
        this.logicalObjectDetailTotalCount = logicalObjectDetailTotalCount;
    }
    public Integer getSqlStatisticPageNumber() { return sqlStatisticPageNumber; }
    public void setSqlStatisticPageNumber(Integer sqlStatisticPageNumber) { this.sqlStatisticPageNumber = sqlStatisticPageNumber; }
    public Integer getSqlStatisticPageSize() { return sqlStatisticPageSize; }
    public void setSqlStatisticPageSize(Integer sqlStatisticPageSize) { this.sqlStatisticPageSize = sqlStatisticPageSize; }
    public Integer getSqlStatisticPageCount() { return sqlStatisticPageCount; }
    public void setSqlStatisticPageCount(Integer sqlStatisticPageCount) { this.sqlStatisticPageCount = sqlStatisticPageCount; }
    public Integer getSqlStatisticTotalCount() { return sqlStatisticTotalCount; }
    public void setSqlStatisticTotalCount(Integer sqlStatisticTotalCount) { this.sqlStatisticTotalCount = sqlStatisticTotalCount; }
    public List<ReportBatchIssueSceneReportDetailVO> getReportDetails() { return reportDetails; }
    public void setReportDetails(List<ReportBatchIssueSceneReportDetailVO> reportDetails) { this.reportDetails = reportDetails; }
    public List<ReportBatchIssueSceneLogicalObjectDetailVO> getLogicalObjectDetails() { return logicalObjectDetails; }
    public void setLogicalObjectDetails(List<ReportBatchIssueSceneLogicalObjectDetailVO> logicalObjectDetails) {
        this.logicalObjectDetails = logicalObjectDetails;
    }
    public List<ReportBatchSqlStatisticVO> getSqlStatistics() { return sqlStatistics; }
    public void setSqlStatistics(List<ReportBatchSqlStatisticVO> sqlStatistics) { this.sqlStatistics = sqlStatistics; }
}
