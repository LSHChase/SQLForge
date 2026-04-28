package com.company.governance.application.controller.vo;

import java.time.LocalDateTime;

public class GovernanceAlertSummaryVO {

    private String alertId;
    private String alertType;
    private String alertLevel;
    private String alertStatus;
    private String notifyStatus;
    private String policyId;
    private String dedupeKey;
    private String sourceService;
    private String summary;
    private String notifyMessage;
    private LocalDateTime notifiedAt;
    private String ackedBy;
    private LocalDateTime ackedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
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
    public String getNotifyMessage() { return notifyMessage; }
    public void setNotifyMessage(String notifyMessage) { this.notifyMessage = notifyMessage; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public String getAckedBy() { return ackedBy; }
    public void setAckedBy(String ackedBy) { this.ackedBy = ackedBy; }
    public LocalDateTime getAckedAt() { return ackedAt; }
    public void setAckedAt(LocalDateTime ackedAt) { this.ackedAt = ackedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
