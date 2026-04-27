package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ParseSqlIssueStatisticVO {

    private String itemId;
    private String batchId;
    private String parseTaskId;
    private String reportCode;
    private String datasourceCode;
    private String stage;
    private String sqlDigest;
    private Integer issueCount;
    private String highestPriorityLevel;
    private Integer highestPriorityScore;
    private Boolean important;
    private Boolean urgent;
    private List<String> issueScenes;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
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
}
