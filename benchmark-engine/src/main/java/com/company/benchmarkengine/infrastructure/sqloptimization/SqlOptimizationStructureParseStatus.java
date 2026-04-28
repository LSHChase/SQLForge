package com.company.benchmarkengine.infrastructure.sqloptimization;

import java.util.List;

public class SqlOptimizationStructureParseStatus {

    private String syntaxStatus;
    private String sqlType;
    private Integer priorityScore;
    private String priorityLevel;
    private Boolean important;
    private Boolean urgent;
    private List<SqlOptimizationStructureParseIssue> issues;

    public String getSyntaxStatus() {
        return syntaxStatus;
    }

    public void setSyntaxStatus(String syntaxStatus) {
        this.syntaxStatus = syntaxStatus;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
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

    public List<SqlOptimizationStructureParseIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<SqlOptimizationStructureParseIssue> issues) {
        this.issues = issues;
    }
}
