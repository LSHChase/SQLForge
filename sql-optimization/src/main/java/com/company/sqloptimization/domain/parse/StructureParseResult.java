package com.company.sqloptimization.domain.parse;

import java.util.List;

public class StructureParseResult {

    private String parseTaskId;
    private StructureParseSyntaxStatus syntaxStatus;
    private StructureParseComplexityLevel complexityLevel;
    private String sqlType;
    private StructureParseQueryDateSummary queryDateSummary;
    private List<StructureParseLogicalObjectHit> logicalObjectHits;
    private List<String> riskTags;
    private List<String> rewriteCandidates;
    private List<StructureParseIssue> issues;
    private Integer priorityScore;
    private StructureParsePriorityLevel priorityLevel;
    private Boolean important;
    private Boolean urgent;

    public void applyAssessment(StructureParsePriorityAssessment assessment) {
        if (assessment == null) {
            return;
        }
        this.priorityScore = Integer.valueOf(assessment.getPriorityScore());
        this.priorityLevel = assessment.getPriorityLevel();
        this.important = Boolean.valueOf(assessment.isImportant());
        this.urgent = Boolean.valueOf(assessment.isUrgent());
    }

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public StructureParseSyntaxStatus getSyntaxStatus() {
        return syntaxStatus;
    }

    public void setSyntaxStatus(StructureParseSyntaxStatus syntaxStatus) {
        this.syntaxStatus = syntaxStatus;
    }

    public StructureParseComplexityLevel getComplexityLevel() {
        return complexityLevel;
    }

    public void setComplexityLevel(StructureParseComplexityLevel complexityLevel) {
        this.complexityLevel = complexityLevel;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public StructureParseQueryDateSummary getQueryDateSummary() {
        return queryDateSummary;
    }

    public void setQueryDateSummary(StructureParseQueryDateSummary queryDateSummary) {
        this.queryDateSummary = queryDateSummary;
    }

    public List<StructureParseLogicalObjectHit> getLogicalObjectHits() {
        return logicalObjectHits;
    }

    public void setLogicalObjectHits(List<StructureParseLogicalObjectHit> logicalObjectHits) {
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

    public List<StructureParseIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<StructureParseIssue> issues) {
        this.issues = issues;
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
