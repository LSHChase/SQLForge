package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ReportBatchLogicalObjectStatisticVO {

    private String objectKey;
    private Integer sqlCount;
    private Integer issueCount;
    private Integer reportCount;
    private List<String> reportCodes;

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public Integer getSqlCount() { return sqlCount; }
    public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
    public Integer getIssueCount() { return issueCount; }
    public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public List<String> getReportCodes() { return reportCodes; }
    public void setReportCodes(List<String> reportCodes) { this.reportCodes = reportCodes; }
}
