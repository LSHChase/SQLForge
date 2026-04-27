package com.company.sqloptimization.application.controller.vo;

public class ParseBatchReportStatisticVO {

    private String reportCode;
    private Integer sqlCount;
    private Integer issueCount;
    private Double ratio;

    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public Integer getSqlCount() { return sqlCount; }
    public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
    public Integer getIssueCount() { return issueCount; }
    public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
    public Double getRatio() { return ratio; }
    public void setRatio(Double ratio) { this.ratio = ratio; }
}
