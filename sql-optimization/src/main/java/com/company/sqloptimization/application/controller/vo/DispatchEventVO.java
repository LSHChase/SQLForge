package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class DispatchEventVO {

    private String dispatchEventId;
    private String tenantId;
    private String recommendationId;
    private String dispatchType;
    private String dispatchPayloadJson;
    private String targetEngine;
    private String targetDatasource;
    private String reportCode;
    private String logicalObjectKey;
    private String status;
    private String pulledBy;
    private Instant pulledAt;
    private String ackedBy;
    private Instant ackedAt;
    private String failedBy;
    private Instant failedAt;
    private String resultMessage;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<DispatchEventStatusHistoryVO> statusHistory;

    public String getDispatchEventId() { return dispatchEventId; }
    public void setDispatchEventId(String dispatchEventId) { this.dispatchEventId = dispatchEventId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getDispatchType() { return dispatchType; }
    public void setDispatchType(String dispatchType) { this.dispatchType = dispatchType; }
    public String getDispatchPayloadJson() { return dispatchPayloadJson; }
    public void setDispatchPayloadJson(String dispatchPayloadJson) { this.dispatchPayloadJson = dispatchPayloadJson; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public void setTargetDatasource(String targetDatasource) { this.targetDatasource = targetDatasource; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPulledBy() { return pulledBy; }
    public void setPulledBy(String pulledBy) { this.pulledBy = pulledBy; }
    public Instant getPulledAt() { return pulledAt; }
    public void setPulledAt(Instant pulledAt) { this.pulledAt = pulledAt; }
    public String getAckedBy() { return ackedBy; }
    public void setAckedBy(String ackedBy) { this.ackedBy = ackedBy; }
    public Instant getAckedAt() { return ackedAt; }
    public void setAckedAt(Instant ackedAt) { this.ackedAt = ackedAt; }
    public String getFailedBy() { return failedBy; }
    public void setFailedBy(String failedBy) { this.failedBy = failedBy; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<DispatchEventStatusHistoryVO> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<DispatchEventStatusHistoryVO> statusHistory) { this.statusHistory = statusHistory; }
}
