package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ReportBatchIssueSceneReportDetailVO {

    private String reportCode;
    private String reportName;
    private Integer sqlCount;
    private Integer issueCount;
    private Integer logicalObjectCount;
    private List<String> logicalObjectKeys;

    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public Integer getSqlCount() { return sqlCount; }
    public void setSqlCount(Integer sqlCount) { this.sqlCount = sqlCount; }
    public Integer getIssueCount() { return issueCount; }
    public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }
    public Integer getLogicalObjectCount() { return logicalObjectCount; }
    public void setLogicalObjectCount(Integer logicalObjectCount) { this.logicalObjectCount = logicalObjectCount; }
    public List<String> getLogicalObjectKeys() { return logicalObjectKeys; }
    public void setLogicalObjectKeys(List<String> logicalObjectKeys) { this.logicalObjectKeys = logicalObjectKeys; }
}
