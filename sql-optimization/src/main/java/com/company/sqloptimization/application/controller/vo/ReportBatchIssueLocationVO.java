package com.company.sqloptimization.application.controller.vo;

public class ReportBatchIssueLocationVO {

    private String issueScene;
    private String issueDomain;
    private String severity;
    private String priorityLevel;
    private Integer priorityScore;
    private Boolean important;
    private Boolean urgent;
    private String locationSnippet;
    private String locationSource;
    private Integer failureLine;
    private Integer failureColumn;
    private Integer failureOffset;
    private String failureToken;

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
    public Boolean getImportant() { return important; }
    public void setImportant(Boolean important) { this.important = important; }
    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }
    public String getLocationSnippet() { return locationSnippet; }
    public void setLocationSnippet(String locationSnippet) { this.locationSnippet = locationSnippet; }
    public String getLocationSource() { return locationSource; }
    public void setLocationSource(String locationSource) { this.locationSource = locationSource; }
    public Integer getFailureLine() { return failureLine; }
    public void setFailureLine(Integer failureLine) { this.failureLine = failureLine; }
    public Integer getFailureColumn() { return failureColumn; }
    public void setFailureColumn(Integer failureColumn) { this.failureColumn = failureColumn; }
    public Integer getFailureOffset() { return failureOffset; }
    public void setFailureOffset(Integer failureOffset) { this.failureOffset = failureOffset; }
    public String getFailureToken() { return failureToken; }
    public void setFailureToken(String failureToken) { this.failureToken = failureToken; }
}
