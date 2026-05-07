package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class ParseIssueSceneStatisticVO {

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
    private List<String> sampleReportCodes;
    private List<String> sampleLogicalObjectKeys;

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
    public List<String> getSampleReportCodes() { return sampleReportCodes; }
    public void setSampleReportCodes(List<String> sampleReportCodes) { this.sampleReportCodes = sampleReportCodes; }
    public List<String> getSampleLogicalObjectKeys() { return sampleLogicalObjectKeys; }
    public void setSampleLogicalObjectKeys(List<String> sampleLogicalObjectKeys) {
        this.sampleLogicalObjectKeys = sampleLogicalObjectKeys;
    }
}
