package com.company.sqloptimization.application.controller.vo;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.util.List;

public class StructureParseResponseVO {

    private String parseTaskId;
    private String parseType = "STRUCTURE";
    private String syntaxStatus;
    private String complexityLevel;
    private String sqlType;
    private String sqlFingerprint;
    private StructureParseIntentProfileVO intentProfile;
    private StructureParseFeatureSummaryVO featureSummary;
    private StructureParseResourceEstimateVO estimatedResourceCost;
    private List<StructureParseRiskVO> riskChecklist;
    private StructureParseQueryDateSummaryVO queryDateSummary;
    private List<LogicalObjectSurface> logicalObjectHits;
    private List<String> riskTags;
    private List<String> rewriteCandidates;
    private List<StructureParseIssueVO> issues;
    private Integer priorityScore;
    private String priorityLevel;
    private Boolean important;
    private Boolean urgent;
    private String failureReason;
    private Integer failureLine;
    private Integer failureColumn;
    private Integer failureOffset;
    private String failureToken;
    private String failureSnippet;
    private String historyId;
    private Boolean historyPersisted;
    private String historyPersistenceStatus;

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getParseType() {
        return parseType;
    }

    public void setParseType(String parseType) {
        this.parseType = parseType;
    }

    public String getSyntaxStatus() {
        return syntaxStatus;
    }

    public void setSyntaxStatus(String syntaxStatus) {
        this.syntaxStatus = syntaxStatus;
    }

    public String getComplexityLevel() {
        return complexityLevel;
    }

    public void setComplexityLevel(String complexityLevel) {
        this.complexityLevel = complexityLevel;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public StructureParseIntentProfileVO getIntentProfile() {
        return intentProfile;
    }

    public void setIntentProfile(StructureParseIntentProfileVO intentProfile) {
        this.intentProfile = intentProfile;
    }

    public StructureParseFeatureSummaryVO getFeatureSummary() {
        return featureSummary;
    }

    public void setFeatureSummary(StructureParseFeatureSummaryVO featureSummary) {
        this.featureSummary = featureSummary;
    }

    public StructureParseResourceEstimateVO getEstimatedResourceCost() {
        return estimatedResourceCost;
    }

    public void setEstimatedResourceCost(StructureParseResourceEstimateVO estimatedResourceCost) {
        this.estimatedResourceCost = estimatedResourceCost;
    }

    public List<StructureParseRiskVO> getRiskChecklist() {
        return riskChecklist;
    }

    public void setRiskChecklist(List<StructureParseRiskVO> riskChecklist) {
        this.riskChecklist = riskChecklist;
    }

    public StructureParseQueryDateSummaryVO getQueryDateSummary() {
        return queryDateSummary;
    }

    public void setQueryDateSummary(StructureParseQueryDateSummaryVO queryDateSummary) {
        this.queryDateSummary = queryDateSummary;
    }

    public List<LogicalObjectSurface> getLogicalObjectHits() {
        return logicalObjectHits;
    }

    public void setLogicalObjectHits(List<LogicalObjectSurface> logicalObjectHits) {
        this.logicalObjectHits = logicalObjectHits;
    }

    public List<String> getRiskTags() {
        return riskTags;
    }

    public void setRiskTags(List<String> riskTags) {
        this.riskTags = riskTags;
    }

    public List<String> getRewriteCandidates() {
        return rewriteCandidates;
    }

    public void setRewriteCandidates(List<String> rewriteCandidates) {
        this.rewriteCandidates = rewriteCandidates;
    }

    public List<StructureParseIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<StructureParseIssueVO> issues) {
        this.issues = issues;
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public Boolean getHistoryPersisted() {
        return historyPersisted;
    }

    public void setHistoryPersisted(Boolean historyPersisted) {
        this.historyPersisted = historyPersisted;
    }

    public String getHistoryPersistenceStatus() {
        return historyPersistenceStatus;
    }

    public void setHistoryPersistenceStatus(String historyPersistenceStatus) {
        this.historyPersistenceStatus = historyPersistenceStatus;
    }
}
