package com.company.benchmarkengine.infrastructure.sqloptimization;

public class SqlOptimizationStructureParseIssue {

    private String issueDomain;
    private String issueScene;
    private String severity;
    private Boolean important;
    private Boolean urgent;
    private Integer priorityScore;
    private String priorityLevel;

    public String getIssueDomain() {
        return issueDomain;
    }

    public void setIssueDomain(String issueDomain) {
        this.issueDomain = issueDomain;
    }

    public String getIssueScene() {
        return issueScene;
    }

    public void setIssueScene(String issueScene) {
        this.issueScene = issueScene;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getImportant() {
        return important;
    }

    public void setImportant(Boolean important) {
        this.important = important;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }
}
