package com.company.sqloptimization.application.controller.vo;

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
}
