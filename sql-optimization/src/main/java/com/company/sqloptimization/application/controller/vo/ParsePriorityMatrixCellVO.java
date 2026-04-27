package com.company.sqloptimization.application.controller.vo;

public class ParsePriorityMatrixCellVO {

    private String priorityLevel;
    private String urgencyBucket;
    private Integer sqlCount;
    private Integer issueCount;
    private Integer reportCount;

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
}
