package com.company.sqloptimization.domain.parse;

public class StructureParseIssue {

    private String issueCode;
    private StructureParseIssueDomain issueDomain;
    private String issueScene;
    private StructureParseIssueSeverity severity;
    private String summary;
    private String detail;
    private String suggestedAction;
    private Boolean important;
    private Boolean urgent;
    private Integer affectedSqlCount;
    private Integer affectedReportCount;
    private Integer priorityScore;
    private StructureParsePriorityLevel priorityLevel;
    private Integer failureLine;
    private Integer failureColumn;
    private Integer failureOffset;
    private String failureToken;
    private String failureSnippet;

    public String getIssueCode() {
        return issueCode;
    }

    public void setIssueCode(String issueCode) {
        this.issueCode = issueCode;
    }

    public StructureParseIssueDomain getIssueDomain() {
        return issueDomain;
    }

    public void setIssueDomain(StructureParseIssueDomain issueDomain) {
        this.issueDomain = issueDomain;
    }

    public String getIssueScene() {
        return issueScene;
    }

    public void setIssueScene(String issueScene) {
        this.issueScene = issueScene;
    }

    public StructureParseIssueSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(StructureParseIssueSeverity severity) {
        this.severity = severity;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
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

    public Integer getAffectedSqlCount() {
        return affectedSqlCount;
    }

    public void setAffectedSqlCount(Integer affectedSqlCount) {
        this.affectedSqlCount = affectedSqlCount;
    }

    public Integer getAffectedReportCount() {
        return affectedReportCount;
    }

    public void setAffectedReportCount(Integer affectedReportCount) {
        this.affectedReportCount = affectedReportCount;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public StructureParsePriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(StructureParsePriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Integer getFailureLine() {
        return failureLine;
    }

    public void setFailureLine(Integer failureLine) {
        this.failureLine = failureLine;
    }

    public Integer getFailureColumn() {
        return failureColumn;
    }

    public void setFailureColumn(Integer failureColumn) {
        this.failureColumn = failureColumn;
    }

    public Integer getFailureOffset() {
        return failureOffset;
    }

    public void setFailureOffset(Integer failureOffset) {
        this.failureOffset = failureOffset;
    }

    public String getFailureToken() {
        return failureToken;
    }

    public void setFailureToken(String failureToken) {
        this.failureToken = failureToken;
    }

    public String getFailureSnippet() {
        return failureSnippet;
    }

    public void setFailureSnippet(String failureSnippet) {
        this.failureSnippet = failureSnippet;
    }
}
