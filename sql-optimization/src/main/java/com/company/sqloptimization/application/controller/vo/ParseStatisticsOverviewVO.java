package com.company.sqloptimization.application.controller.vo;

import java.util.Map;

public class ParseStatisticsOverviewVO {

    private Integer totalSqlCount;
    private Integer issueSqlCount;
    private Integer totalIssueCount;
    private Integer issueSceneCount;
    private Integer importantSqlCount;
    private Integer urgentSqlCount;
    private Map<String, Integer> priorityDistribution;

    public Integer getTotalSqlCount() {
        return totalSqlCount;
    }

    public void setTotalSqlCount(Integer totalSqlCount) {
        this.totalSqlCount = totalSqlCount;
    }

    public Integer getIssueSqlCount() {
        return issueSqlCount;
    }

    public void setIssueSqlCount(Integer issueSqlCount) {
        this.issueSqlCount = issueSqlCount;
    }

    public Integer getTotalIssueCount() {
        return totalIssueCount;
    }

    public void setTotalIssueCount(Integer totalIssueCount) {
        this.totalIssueCount = totalIssueCount;
    }

    public Integer getIssueSceneCount() {
        return issueSceneCount;
    }

    public void setIssueSceneCount(Integer issueSceneCount) {
        this.issueSceneCount = issueSceneCount;
    }

    public Integer getImportantSqlCount() {
        return importantSqlCount;
    }

    public void setImportantSqlCount(Integer importantSqlCount) {
        this.importantSqlCount = importantSqlCount;
    }

    public Integer getUrgentSqlCount() {
        return urgentSqlCount;
    }

    public void setUrgentSqlCount(Integer urgentSqlCount) {
        this.urgentSqlCount = urgentSqlCount;
    }

    public Map<String, Integer> getPriorityDistribution() {
        return priorityDistribution;
    }

    public void setPriorityDistribution(Map<String, Integer> priorityDistribution) {
        this.priorityDistribution = priorityDistribution;
    }
}
