package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ReportBatchSqlStatisticVO {

    private String itemId;
    private String batchId;
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
    private List<String> issueScenes;
    private List<ReportBatchIssueLocationVO> issueLocations;
    private List<String> logicalObjectKeys;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
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
    public List<String> getIssueScenes() { return issueScenes; }
    public void setIssueScenes(List<String> issueScenes) { this.issueScenes = issueScenes; }
    public List<ReportBatchIssueLocationVO> getIssueLocations() { return issueLocations; }
    public void setIssueLocations(List<ReportBatchIssueLocationVO> issueLocations) {
        this.issueLocations = issueLocations;
    }
    public List<String> getLogicalObjectKeys() { return logicalObjectKeys; }
    public void setLogicalObjectKeys(List<String> logicalObjectKeys) { this.logicalObjectKeys = logicalObjectKeys; }
}
