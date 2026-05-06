package com.company.sqloptimization.application.controller.vo;

public class ReportBatchImportanceStatisticVO {

    private String importanceBucket;
    private Integer sqlCount;
    private Integer issueCount;
    private Integer reportCount;

    public String getImportanceBucket() { return importanceBucket; }
    public void setImportanceBucket(String importanceBucket) { this.importanceBucket = importanceBucket; }
    public Integer getSqlCount() { return sqlCount; }
    public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
    public Integer getIssueCount() { return issueCount; }
    public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
}
