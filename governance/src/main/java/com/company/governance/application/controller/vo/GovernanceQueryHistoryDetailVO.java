package com.company.governance.application.controller.vo;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class GovernanceQueryHistoryDetailVO {

    private String historyId;
    private String resultId;
    private String traceId;
    private String historyType;
    private String reportCode;
    private String datasourceCode;
    private String datasourceType;
    private String stageCode;
    private LocalDate bizDate;
    private String sqlText;
    private String sqlTemplateText;
    private String boundSqlText;
    private Map<String, Object> sqlState;
    private Map<String, Object> commentContext;
    private Map<String, Object> queryDateSummary;
    private List<LogicalObjectSurface> logicalObjectHits;
    private Map<String, Object> executionSummary;
    private Map<String, Object> structureParseSummary;
    private Map<String, Object> accessParseSummary;
    private Map<String, Object> routeDecision;
    private Map<String, Object> cacheSummary;
    private Map<String, Object> bindingSummary;
    private Map<String, Object> queryContext;
    private List<Map<String, Object>> recommendationRefs;
    private List<Map<String, Object>> benchmarkRefs;
    private List<Map<String, Object>> auditRefs;
    private List<Map<String, Object>> alertRefs;
    private LocalDateTime submittedAt;
    private String submittedBy;
    private GovernanceTraceDetailVO traceDetail;

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getHistoryType() {
        return historyType;
    }

    public void setHistoryType(String historyType) {
        this.historyType = historyType;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public LocalDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(LocalDate bizDate) {
        this.bizDate = bizDate;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getSqlTemplateText() {
        return sqlTemplateText;
    }

    public void setSqlTemplateText(String sqlTemplateText) {
        this.sqlTemplateText = sqlTemplateText;
    }

    public String getBoundSqlText() {
        return boundSqlText;
    }

    public void setBoundSqlText(String boundSqlText) {
        this.boundSqlText = boundSqlText;
    }

    public Map<String, Object> getSqlState() {
        return sqlState;
    }

    public void setSqlState(Map<String, Object> sqlState) {
        this.sqlState = sqlState;
    }

    public Map<String, Object> getCommentContext() {
        return commentContext;
    }

    public void setCommentContext(Map<String, Object> commentContext) {
        this.commentContext = commentContext;
    }

    public Map<String, Object> getQueryDateSummary() {
        return queryDateSummary;
    }

    public void setQueryDateSummary(Map<String, Object> queryDateSummary) {
        this.queryDateSummary = queryDateSummary;
    }

    public List<LogicalObjectSurface> getLogicalObjectHits() {
        return logicalObjectHits;
    }

    public void setLogicalObjectHits(List<LogicalObjectSurface> logicalObjectHits) {
        this.logicalObjectHits = logicalObjectHits;
    }

    public Map<String, Object> getExecutionSummary() {
        return executionSummary;
    }

    public void setExecutionSummary(Map<String, Object> executionSummary) {
        this.executionSummary = executionSummary;
    }

    public Map<String, Object> getStructureParseSummary() {
        return structureParseSummary;
    }

    public void setStructureParseSummary(Map<String, Object> structureParseSummary) {
        this.structureParseSummary = structureParseSummary;
    }

    public Map<String, Object> getAccessParseSummary() {
        return accessParseSummary;
    }

    public void setAccessParseSummary(Map<String, Object> accessParseSummary) {
        this.accessParseSummary = accessParseSummary;
    }

    public Map<String, Object> getRouteDecision() {
        return routeDecision;
    }

    public void setRouteDecision(Map<String, Object> routeDecision) {
        this.routeDecision = routeDecision;
    }

    public Map<String, Object> getCacheSummary() {
        return cacheSummary;
    }

    public void setCacheSummary(Map<String, Object> cacheSummary) {
        this.cacheSummary = cacheSummary;
    }

    public Map<String, Object> getBindingSummary() {
        return bindingSummary;
    }

    public void setBindingSummary(Map<String, Object> bindingSummary) {
        this.bindingSummary = bindingSummary;
    }

    public Map<String, Object> getQueryContext() {
        return queryContext;
    }

    public void setQueryContext(Map<String, Object> queryContext) {
        this.queryContext = queryContext;
    }

    public List<Map<String, Object>> getRecommendationRefs() {
        return recommendationRefs;
    }

    public void setRecommendationRefs(List<Map<String, Object>> recommendationRefs) {
        this.recommendationRefs = recommendationRefs;
    }

    public List<Map<String, Object>> getBenchmarkRefs() {
        return benchmarkRefs;
    }

    public void setBenchmarkRefs(List<Map<String, Object>> benchmarkRefs) {
        this.benchmarkRefs = benchmarkRefs;
    }

    public List<Map<String, Object>> getAuditRefs() {
        return auditRefs;
    }

    public void setAuditRefs(List<Map<String, Object>> auditRefs) {
        this.auditRefs = auditRefs;
    }

    public List<Map<String, Object>> getAlertRefs() {
        return alertRefs;
    }

    public void setAlertRefs(List<Map<String, Object>> alertRefs) {
        this.alertRefs = alertRefs;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public GovernanceTraceDetailVO getTraceDetail() {
        return traceDetail;
    }

    public void setTraceDetail(GovernanceTraceDetailVO traceDetail) {
        this.traceDetail = traceDetail;
    }
}
