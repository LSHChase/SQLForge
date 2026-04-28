package com.company.governance.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class AlertPolicyRecord {

    private String policyId;
    private String tenantId;
    private String policyName;
    private String alertType;
    private String defaultLevel;
    private String dedupeStrategy;
    private Integer dedupeWindowSeconds;
    private String notifyChannel;
    private String initialNotifyStatus;
    private String ownerRole;
    private Boolean enabled;
    private String ruleConfigJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getDefaultLevel() { return defaultLevel; }
    public void setDefaultLevel(String defaultLevel) { this.defaultLevel = defaultLevel; }
    public String getDedupeStrategy() { return dedupeStrategy; }
    public void setDedupeStrategy(String dedupeStrategy) { this.dedupeStrategy = dedupeStrategy; }
    public Integer getDedupeWindowSeconds() { return dedupeWindowSeconds; }
    public void setDedupeWindowSeconds(Integer dedupeWindowSeconds) { this.dedupeWindowSeconds = dedupeWindowSeconds; }
    public String getNotifyChannel() { return notifyChannel; }
    public void setNotifyChannel(String notifyChannel) { this.notifyChannel = notifyChannel; }
    public String getInitialNotifyStatus() { return initialNotifyStatus; }
    public void setInitialNotifyStatus(String initialNotifyStatus) { this.initialNotifyStatus = initialNotifyStatus; }
    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String ownerRole) { this.ownerRole = ownerRole; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getRuleConfigJson() { return ruleConfigJson; }
    public void setRuleConfigJson(String ruleConfigJson) { this.ruleConfigJson = ruleConfigJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
