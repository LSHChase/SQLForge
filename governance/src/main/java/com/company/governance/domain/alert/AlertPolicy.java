package com.company.governance.domain.alert;

import java.time.Instant;

public class AlertPolicy {

    private final String policyId;
    private final String tenantId;
    private final String policyName;
    private final AlertEvent.AlertType alertType;
    private final AlertEvent.AlertLevel defaultLevel;
    private final DedupeStrategy dedupeStrategy;
    private final int dedupeWindowSeconds;
    private final NotifyChannel notifyChannel;
    private final AlertEvent.NotifyStatus initialNotifyStatus;
    private final String ownerScope;
    private final boolean enabled;
    private final String ruleConfigJson;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    private AlertPolicy(Builder builder) {
        this.policyId = builder.policyId;
        this.tenantId = builder.tenantId;
        this.policyName = builder.policyName;
        this.alertType = builder.alertType;
        this.defaultLevel = resolveDefaultLevel(builder);
        this.dedupeStrategy = builder.dedupeStrategy == null ? DedupeStrategy.TENANT_ALERT_TYPE_TARGET : builder.dedupeStrategy;
        this.dedupeWindowSeconds = builder.dedupeWindowSeconds <= 0
            ? resolveDefaultDedupeWindowSeconds(builder)
            : builder.dedupeWindowSeconds;
        this.notifyChannel = builder.notifyChannel == null ? NotifyChannel.SIMULATED_EMAIL : builder.notifyChannel;
        this.initialNotifyStatus = builder.initialNotifyStatus == null
            ? AlertEvent.NotifyStatus.SIMULATED_PENDING_NOTIFY
            : builder.initialNotifyStatus;
        this.ownerScope = trimToNull(builder.ownerScope) == null ? "TENANT_SCOPE" : builder.ownerScope.trim();
        this.enabled = builder.enabled == null || builder.enabled.booleanValue();
        this.ruleConfigJson = trimToNull(builder.ruleConfigJson);
        this.createdBy = trimToNull(builder.createdBy);
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean appliesTo(AlertEvent event) {
        return event != null && tenantId.equals(event.getTenantId()) && alertType == event.getAlertType();
    }

    private void validate() {
        requireText(policyId, "policyId");
        requireText(tenantId, "tenantId");
        requireText(policyName, "policyName");
        if (alertType == null) {
            throw new IllegalArgumentException("alertType 为必填项");
        }
        if (dedupeWindowSeconds <= 0) {
            throw new IllegalArgumentException("dedupeWindowSeconds 必须为 positive");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt 为必填项");
        }
    }

    private static void requireText(String value, String field) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static AlertEvent.AlertLevel resolveDefaultLevel(Builder builder) {
        if (builder.defaultLevel != null) {
            return builder.defaultLevel;
        }
        if (builder.alertType == null) {
            return null;
        }
        return builder.alertType.defaultLevel();
    }

    private static int resolveDefaultDedupeWindowSeconds(Builder builder) {
        if (builder.alertType == null) {
            return 0;
        }
        return builder.alertType.defaultDedupeWindowSeconds();
    }

    public String getPolicyId() { return policyId; }
    public String getTenantId() { return tenantId; }
    public String getPolicyName() { return policyName; }
    public AlertEvent.AlertType getAlertType() { return alertType; }
    public AlertEvent.AlertLevel getDefaultLevel() { return defaultLevel; }
    public DedupeStrategy getDedupeStrategy() { return dedupeStrategy; }
    public int getDedupeWindowSeconds() { return dedupeWindowSeconds; }
    public NotifyChannel getNotifyChannel() { return notifyChannel; }
    public AlertEvent.NotifyStatus getInitialNotifyStatus() { return initialNotifyStatus; }
    public String getOwnerScope() { return ownerScope; }
    public boolean isEnabled() { return enabled; }
    public String getRuleConfigJson() { return ruleConfigJson; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum DedupeStrategy {
        TENANT_ALERT_TYPE_TARGET
    }

    public enum NotifyChannel {
        SIMULATED_EMAIL
    }

    public static final class Builder {
        private String policyId;
        private String tenantId;
        private String policyName;
        private AlertEvent.AlertType alertType;
        private AlertEvent.AlertLevel defaultLevel;
        private DedupeStrategy dedupeStrategy;
        private int dedupeWindowSeconds;
        private NotifyChannel notifyChannel;
        private AlertEvent.NotifyStatus initialNotifyStatus;
        private String ownerScope;
        private Boolean enabled;
        private String ruleConfigJson;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder policyId(String policyId) { this.policyId = policyId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder policyName(String policyName) { this.policyName = policyName; return this; }
        public Builder alertType(AlertEvent.AlertType alertType) { this.alertType = alertType; return this; }
        public Builder defaultLevel(AlertEvent.AlertLevel defaultLevel) { this.defaultLevel = defaultLevel; return this; }
        public Builder dedupeStrategy(DedupeStrategy dedupeStrategy) { this.dedupeStrategy = dedupeStrategy; return this; }
        public Builder dedupeWindowSeconds(int dedupeWindowSeconds) { this.dedupeWindowSeconds = dedupeWindowSeconds; return this; }
        public Builder notifyChannel(NotifyChannel notifyChannel) { this.notifyChannel = notifyChannel; return this; }
        public Builder initialNotifyStatus(AlertEvent.NotifyStatus initialNotifyStatus) { this.initialNotifyStatus = initialNotifyStatus; return this; }
        public Builder ownerScope(String ownerScope) { this.ownerScope = ownerScope; return this; }
        public Builder enabled(Boolean enabled) { this.enabled = enabled; return this; }
        public Builder ruleConfigJson(String ruleConfigJson) { this.ruleConfigJson = ruleConfigJson; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public AlertPolicy build() {
            return new AlertPolicy(this);
        }
    }
}
