package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class StructureParseResponseVO {

    private String parseTaskId;
    private String parseType = "STRUCTURE";
    private String syntaxStatus;
    private String complexityLevel;
    private String sqlType;
    private StructureParseQueryDateSummaryVO queryDateSummary;
    private List<StructureParseLogicalObjectHitVO> logicalObjectHits;
    private List<String> riskTags;
    private List<String> rewriteCandidates;
    private List<StructureParseIssueVO> issues;
    private Integer priorityScore;
    private String priorityLevel;
    private Boolean important;
    private Boolean urgent;

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

    public StructureParseQueryDateSummaryVO getQueryDateSummary() {
        return queryDateSummary;
    }

    public void setQueryDateSummary(StructureParseQueryDateSummaryVO queryDateSummary) {
        this.queryDateSummary = queryDateSummary;
    }

    public List<StructureParseLogicalObjectHitVO> getLogicalObjectHits() {
        return logicalObjectHits;
    }

    public void setLogicalObjectHits(List<StructureParseLogicalObjectHitVO> logicalObjectHits) {
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
}
