package com.company.governance.application.controller.vo;

import java.util.List;
import java.util.Map;

public class GovernanceAlertDetailVO extends GovernanceAlertSummaryVO {

    private String tenantId;
    private String historyId;
    private String parseTaskId;
    private String batchId;
    private String routeDecisionId;
    private String recommendationId;
    private String dispatchEventId;
    private String reportCode;
    private String logicalObjectKey;
    private String datasourceId;
    private String sqlFingerprint;
    private Map<String, Object> evidence;
    private List<GovernanceAlertNotificationLogVO> notificationLogs;

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
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getDispatchEventId() { return dispatchEventId; }
    public void setDispatchEventId(String dispatchEventId) { this.dispatchEventId = dispatchEventId; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
    public String getDatasourceId() { return datasourceId; }
    public void setDatasourceId(String datasourceId) { this.datasourceId = datasourceId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public Map<String, Object> getEvidence() { return evidence; }
    public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
    public List<GovernanceAlertNotificationLogVO> getNotificationLogs() { return notificationLogs; }
    public void setNotificationLogs(List<GovernanceAlertNotificationLogVO> notificationLogs) { this.notificationLogs = notificationLogs; }
}
