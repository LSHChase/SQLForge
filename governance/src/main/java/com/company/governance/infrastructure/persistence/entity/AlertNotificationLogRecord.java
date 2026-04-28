package com.company.governance.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class AlertNotificationLogRecord {

    private String notificationLogId;
    private String tenantId;
    private String alertId;
    private String sourceAlertId;
    private String dedupeKey;
    private String notifyChannel;
    private String deliveryStatus;
    private String templateCode;
    private String messageSubject;
    private String messageBody;
    private String deliverySummary;
    private String payloadJson;
    private String createdBy;
    private LocalDateTime createdAt;

    public String getNotificationLogId() { return notificationLogId; }
    public void setNotificationLogId(String notificationLogId) { this.notificationLogId = notificationLogId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getSourceAlertId() { return sourceAlertId; }
    public void setSourceAlertId(String sourceAlertId) { this.sourceAlertId = sourceAlertId; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public String getNotifyChannel() { return notifyChannel; }
    public void setNotifyChannel(String notifyChannel) { this.notifyChannel = notifyChannel; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getMessageSubject() { return messageSubject; }
    public void setMessageSubject(String messageSubject) { this.messageSubject = messageSubject; }
    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
    public String getDeliverySummary() { return deliverySummary; }
    public void setDeliverySummary(String deliverySummary) { this.deliverySummary = deliverySummary; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
