package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class DispatchEventRecord {

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
    private LocalDateTime pulledAt;
    private String ackedBy;
    private LocalDateTime ackedAt;
    private String failedBy;
    private LocalDateTime failedAt;
    private String resultMessage;
    private String statusHistoryJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
    public LocalDateTime getPulledAt() { return pulledAt; }
    public void setPulledAt(LocalDateTime pulledAt) { this.pulledAt = pulledAt; }
    public String getAckedBy() { return ackedBy; }
    public void setAckedBy(String ackedBy) { this.ackedBy = ackedBy; }
    public LocalDateTime getAckedAt() { return ackedAt; }
    public void setAckedAt(LocalDateTime ackedAt) { this.ackedAt = ackedAt; }
    public String getFailedBy() { return failedBy; }
    public void setFailedBy(String failedBy) { this.failedBy = failedBy; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getStatusHistoryJson() { return statusHistoryJson; }
    public void setStatusHistoryJson(String statusHistoryJson) { this.statusHistoryJson = statusHistoryJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
