package com.company.governance.application.controller.vo;

import java.time.LocalDateTime;
import java.util.Map;

public class GovernanceAlertNotificationLogVO {

    private String notificationLogId;
    private String alertId;
    private String sourceAlertId;
    private String notifyChannel;
    private String deliveryStatus;
    private String templateCode;
    private String messageSubject;
    private String messageBody;
    private String deliverySummary;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;

    public String getNotificationLogId() { return notificationLogId; }
    public void setNotificationLogId(String notificationLogId) { this.notificationLogId = notificationLogId; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getSourceAlertId() { return sourceAlertId; }
    public void setSourceAlertId(String sourceAlertId) { this.sourceAlertId = sourceAlertId; }
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
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
