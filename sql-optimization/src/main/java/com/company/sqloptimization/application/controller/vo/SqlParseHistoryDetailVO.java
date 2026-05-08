package com.company.sqloptimization.application.controller.vo;

import java.util.List;
import java.util.Map;

public class SqlParseHistoryDetailVO extends SqlParseHistorySummaryVO {

    private String sqlText;
    private String sqlTemplateText;
    private Map<String, Object> sqlState;
    private Object structureParseSummary;
    private Object accessParseSummary;
    private Object executionSummary;
    private Object queryContext;
    private Object commentContext;
    private Object bindingSummary;
    private Object logicalObjectHits;
    private Object issueScenes;
    private Object logicalObjectKeys;
    private List<Map<String, Object>> recommendationRefs;
    private List<Map<String, Object>> benchmarkRefs;
    private List<Map<String, Object>> alertRefs;
    private List<Map<String, Object>> auditRefs;

    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlTemplateText() { return sqlTemplateText; }
    public void setSqlTemplateText(String sqlTemplateText) { this.sqlTemplateText = sqlTemplateText; }
    public Map<String, Object> getSqlState() { return sqlState; }
    public void setSqlState(Map<String, Object> sqlState) { this.sqlState = sqlState; }
    public Object getStructureParseSummary() { return structureParseSummary; }
    public void setStructureParseSummary(Object structureParseSummary) { this.structureParseSummary = structureParseSummary; }
    public Object getAccessParseSummary() { return accessParseSummary; }
    public void setAccessParseSummary(Object accessParseSummary) { this.accessParseSummary = accessParseSummary; }
    public Object getExecutionSummary() { return executionSummary; }
    public void setExecutionSummary(Object executionSummary) { this.executionSummary = executionSummary; }
    public Object getQueryContext() { return queryContext; }
    public void setQueryContext(Object queryContext) { this.queryContext = queryContext; }
    public Object getCommentContext() { return commentContext; }
    public void setCommentContext(Object commentContext) { this.commentContext = commentContext; }
    public Object getBindingSummary() { return bindingSummary; }
    public void setBindingSummary(Object bindingSummary) { this.bindingSummary = bindingSummary; }
    public Object getLogicalObjectHits() { return logicalObjectHits; }
    public void setLogicalObjectHits(Object logicalObjectHits) { this.logicalObjectHits = logicalObjectHits; }
    public Object getIssueScenes() { return issueScenes; }
    public void setIssueScenes(Object issueScenes) { this.issueScenes = issueScenes; }
    public Object getLogicalObjectKeys() { return logicalObjectKeys; }
    public void setLogicalObjectKeys(Object logicalObjectKeys) { this.logicalObjectKeys = logicalObjectKeys; }
    public List<Map<String, Object>> getRecommendationRefs() { return recommendationRefs; }
    public void setRecommendationRefs(List<Map<String, Object>> recommendationRefs) { this.recommendationRefs = recommendationRefs; }
    public List<Map<String, Object>> getBenchmarkRefs() { return benchmarkRefs; }
    public void setBenchmarkRefs(List<Map<String, Object>> benchmarkRefs) { this.benchmarkRefs = benchmarkRefs; }
    public List<Map<String, Object>> getAlertRefs() { return alertRefs; }
    public void setAlertRefs(List<Map<String, Object>> alertRefs) { this.alertRefs = alertRefs; }
    public List<Map<String, Object>> getAuditRefs() { return auditRefs; }
    public void setAuditRefs(List<Map<String, Object>> auditRefs) { this.auditRefs = auditRefs; }
}
