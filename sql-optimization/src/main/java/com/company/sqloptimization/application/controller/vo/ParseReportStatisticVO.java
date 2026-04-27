package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ParseReportStatisticVO {

    private String reportCode;
    private Integer sqlCount;
    private Integer issueSqlCount;
    private Integer issueCount;
    private Double issueSqlRatio;
    private String highestPriorityLevel;
    private Integer highestPriorityScore;
    private Boolean important;
    private Boolean urgent;
    private List<String> issueScenes;

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
    public List<String> getIssueScenes() { return issueScenes; }
    public void setIssueScenes(List<String> issueScenes) { this.issueScenes = issueScenes; }
}
