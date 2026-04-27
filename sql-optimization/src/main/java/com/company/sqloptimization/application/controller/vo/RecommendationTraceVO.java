package com.company.sqloptimization.application.controller.vo;

import java.util.List;
import java.util.Map;

public class RecommendationTraceVO {

    private String recommendationId;
    private String tenantId;
    private String historyId;
    private String parseTaskId;
    private String batchId;
    private String routeDecisionId;
    private String alertId;
    private String sqlFingerprint;
    private String reportCode;
    private String logicalObjectKey;
    private AccelerationRecommendationVO recommendation;
    private List<DispatchEventVO> dispatchEvents;
    private Map<String, Object> traceRefs;

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getRouteDecisionId() { return routeDecisionId; }
    public void setRouteDecisionId(String routeDecisionId) { this.routeDecisionId = routeDecisionId; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
    public AccelerationRecommendationVO getRecommendation() { return recommendation; }
    public void setRecommendation(AccelerationRecommendationVO recommendation) { this.recommendation = recommendation; }
    public List<DispatchEventVO> getDispatchEvents() { return dispatchEvents; }
    public void setDispatchEvents(List<DispatchEventVO> dispatchEvents) { this.dispatchEvents = dispatchEvents; }
    public Map<String, Object> getTraceRefs() { return traceRefs; }
    public void setTraceRefs(Map<String, Object> traceRefs) { this.traceRefs = traceRefs; }
}
