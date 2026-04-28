package com.company.governance.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class AlertEventRecord {

    private String alertId;
    private String tenantId;
    private String alertType;
    private String alertLevel;
    private String alertStatus;
    private String notifyStatus;
    private String policyId;
    private String dedupeKey;
    private String sourceService;
    private String summary;
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
    private String evidenceJson;
    private String notifyMessage;
    private LocalDateTime notifiedAt;
    private String ackedBy;
    private LocalDateTime ackedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }
    public String getNotifyStatus() { return notifyStatus; }
    public void setNotifyStatus(String notifyStatus) { this.notifyStatus = notifyStatus; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
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
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getNotifyMessage() { return notifyMessage; }
    public void setNotifyMessage(String notifyMessage) { this.notifyMessage = notifyMessage; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public String getAckedBy() { return ackedBy; }
    public void setAckedBy(String ackedBy) { this.ackedBy = ackedBy; }
    public LocalDateTime getAckedAt() { return ackedAt; }
    public void setAckedAt(LocalDateTime ackedAt) { this.ackedAt = ackedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
