package com.company.governance.domain.alert;

import java.time.Instant;

public class AlertEvent {

    private final String alertId;
    private final String tenantId;
    private final AlertType alertType;
    private final AlertLevel alertLevel;
    private final String policyId;
    private final String dedupeKey;
    private final String sourceService;
    private final String summary;
    private final String historyId;
    private final String parseTaskId;
    private final String batchId;
    private final String routeDecisionId;
    private final String recommendationId;
    private final String dispatchEventId;
    private final String reportCode;
    private final String logicalObjectKey;
    private final String datasourceId;
    private final String sqlFingerprint;
    private final String evidenceJson;
    private final String createdBy;
    private final Instant createdAt;

    private AlertStatus alertStatus;
    private NotifyStatus notifyStatus;
    private String notifyMessage;
    private Instant notifiedAt;
    private String ackedBy;
    private Instant ackedAt;
    private Instant updatedAt;

    private AlertEvent(Builder builder) {
        this.alertId = builder.alertId;
        this.tenantId = builder.tenantId;
        this.alertType = builder.alertType;
        this.alertLevel = resolveAlertLevel(builder);
        this.policyId = trimToNull(builder.policyId);
        this.dedupeKey = trimToNull(builder.dedupeKey) == null ? buildDefaultDedupeKey(builder) : builder.dedupeKey.trim();
        this.sourceService = trimToNull(builder.sourceService);
        this.summary = trimToNull(builder.summary);
        this.historyId = trimToNull(builder.historyId);
        this.parseTaskId = trimToNull(builder.parseTaskId);
        this.batchId = trimToNull(builder.batchId);
        this.routeDecisionId = trimToNull(builder.routeDecisionId);
        this.recommendationId = trimToNull(builder.recommendationId);
        this.dispatchEventId = trimToNull(builder.dispatchEventId);
        this.reportCode = trimToNull(builder.reportCode);
        this.logicalObjectKey = trimToNull(builder.logicalObjectKey);
        this.datasourceId = trimToNull(builder.datasourceId);
        this.sqlFingerprint = trimToNull(builder.sqlFingerprint);
        this.evidenceJson = trimToNull(builder.evidenceJson);
        this.createdBy = trimToNull(builder.createdBy);
        this.createdAt = builder.createdAt;
        this.alertStatus = builder.alertStatus == null ? AlertStatus.OPEN : builder.alertStatus;
        this.notifyStatus = builder.notifyStatus == null ? NotifyStatus.SIMULATED_PENDING_NOTIFY : builder.notifyStatus;
        this.notifyMessage = trimToNull(builder.notifyMessage);
        this.notifiedAt = builder.notifiedAt;
        this.ackedBy = trimToNull(builder.ackedBy);
        this.ackedAt = builder.ackedAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markNotified(Instant occurredAt, String message) {
        requireInstant(occurredAt, "occurredAt");
        this.notifyStatus = NotifyStatus.SIMULATED_NOTIFIED;
        this.notifyMessage = trimToNull(message);
        this.notifiedAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    public void markNotifyFailed(Instant occurredAt, String message) {
        requireInstant(occurredAt, "occurredAt");
        this.notifyStatus = NotifyStatus.SIMULATED_NOTIFY_FAILED;
        this.notifyMessage = trimToNull(message);
        this.notifiedAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    public void ack(Instant occurredAt, String operator) {
        requireStatus(AlertStatus.OPEN, "只有 OPEN 状态告警可以确认");
        requireInstant(occurredAt, "occurredAt");
        requireText(operator, "operator");
        this.alertStatus = AlertStatus.ACKED;
        this.ackedBy = operator.trim();
        this.ackedAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    private void validate() {
        requireText(alertId, "alertId");
        requireText(tenantId, "tenantId");
        if (alertType == null) {
            throw new IllegalArgumentException("alertType 为必填项");
        }
        requireText(summary, "summary");
        requireText(dedupeKey, "dedupeKey");
        requireInstant(createdAt, "createdAt");
    }

    private void requireStatus(AlertStatus expected, String message) {
        if (alertStatus != expected) {
            throw new IllegalStateException(message + "，当前状态=" + alertStatus);
        }
    }

    private static String buildDefaultDedupeKey(Builder builder) {
        if (builder.alertType == null) {
            throw new IllegalArgumentException("alertType 为必填项");
        }
        StringBuilder value = new StringBuilder();
        value.append(sanitize(builder.tenantId));
        value.append('|');
        value.append(builder.alertType.name());
        int baseLength = value.length();
        appendSegment(value, "datasource", builder.datasourceId);
        appendSegment(value, "service", builder.sourceService);
        appendSegment(value, "report", builder.reportCode);
        appendSegment(value, "logical", builder.logicalObjectKey);
        appendSegment(value, "history", builder.historyId);
        appendSegment(value, "parse", builder.parseTaskId);
        appendSegment(value, "batch", builder.batchId);
        appendSegment(value, "route", builder.routeDecisionId);
        appendSegment(value, "recommendation", builder.recommendationId);
        appendSegment(value, "dispatch", builder.dispatchEventId);
        appendSegment(value, "fingerprint", builder.sqlFingerprint);
        if (value.length() == baseLength) {
            value.append("|scope=GLOBAL");
        }
        return value.toString();
    }

    private static AlertLevel resolveAlertLevel(Builder builder) {
        if (builder.alertLevel != null) {
            return builder.alertLevel;
        }
        if (builder.alertType == null) {
            return null;
        }
        return builder.alertType.defaultLevel();
    }

    private static void appendSegment(StringBuilder target, String name, String rawValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return;
        }
        target.append('|');
        target.append(name);
        target.append('=');
        target.append(sanitize(normalized));
    }

    private static String sanitize(String value) {
        return value == null ? null : value.trim().replace('|', '_');
    }

    private static void requireText(String value, String field) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    private static void requireInstant(Instant value, String field) {
        if (value == null) {
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

    public String getAlertId() { return alertId; }
    public String getTenantId() { return tenantId; }
    public AlertType getAlertType() { return alertType; }
    public AlertLevel getAlertLevel() { return alertLevel; }
    public String getPolicyId() { return policyId; }
    public String getDedupeKey() { return dedupeKey; }
    public String getSourceService() { return sourceService; }
    public String getSummary() { return summary; }
    public String getHistoryId() { return historyId; }
    public String getParseTaskId() { return parseTaskId; }
    public String getBatchId() { return batchId; }
    public String getRouteDecisionId() { return routeDecisionId; }
    public String getRecommendationId() { return recommendationId; }
    public String getDispatchEventId() { return dispatchEventId; }
    public String getReportCode() { return reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public String getDatasourceId() { return datasourceId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public String getEvidenceJson() { return evidenceJson; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public AlertStatus getAlertStatus() { return alertStatus; }
    public NotifyStatus getNotifyStatus() { return notifyStatus; }
    public String getNotifyMessage() { return notifyMessage; }
    public Instant getNotifiedAt() { return notifiedAt; }
    public String getAckedBy() { return ackedBy; }
    public Instant getAckedAt() { return ackedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum AlertType {
        ACCELERATION_TABLE_DROPPED(AlertLevel.CRITICAL, 300),
        ACCELERATION_TABLE_INVALIDATED(AlertLevel.HIGH, 900),
        SQL_EXECUTION_MASS_FAILURE(AlertLevel.CRITICAL, 300),
        DATASOURCE_UNAVAILABLE(AlertLevel.CRITICAL, 300),
        DEPENDENCY_SERVICE_UNAVAILABLE(AlertLevel.CRITICAL, 300),
        REDIS_RULE_SOURCE_UNAVAILABLE(AlertLevel.HIGH, 600),
        REPORT_SQL_RESOLVE_FAILURE(AlertLevel.HIGH, 900),
        BATCH_PARSE_FAILURE(AlertLevel.HIGH, 900),
        ACCESS_PARSE_SERVICE_UNAVAILABLE(AlertLevel.CRITICAL, 300),
        SQL_REWRITE_RESULT_DIVERGENCE(AlertLevel.HIGH, 1800),
        BENCHMARK_REGRESSION_FAILED(AlertLevel.HIGH, 1800),
        DISPATCH_COORDINATION_FAILED(AlertLevel.HIGH, 900),
        AUDIT_WRITE_EXCEPTION(AlertLevel.CRITICAL, 300);

        private final AlertLevel defaultLevel;
        private final int defaultDedupeWindowSeconds;

        AlertType(AlertLevel defaultLevel, int defaultDedupeWindowSeconds) {
            this.defaultLevel = defaultLevel;
            this.defaultDedupeWindowSeconds = defaultDedupeWindowSeconds;
        }

        public AlertLevel defaultLevel() {
            return defaultLevel;
        }

        public int defaultDedupeWindowSeconds() {
            return defaultDedupeWindowSeconds;
        }
    }

    public enum AlertLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AlertStatus {
        OPEN,
        ACKED
    }

    public enum NotifyStatus {
        SIMULATED_PENDING_NOTIFY,
        SIMULATED_NOTIFIED,
        SIMULATED_NOTIFY_FAILED
    }

    public static final class Builder {
        private String alertId;
        private String tenantId;
        private AlertType alertType;
        private AlertLevel alertLevel;
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
        private String createdBy;
        private Instant createdAt;
        private AlertStatus alertStatus;
        private NotifyStatus notifyStatus;
        private String notifyMessage;
        private Instant notifiedAt;
        private String ackedBy;
        private Instant ackedAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder alertId(String alertId) { this.alertId = alertId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder alertType(AlertType alertType) { this.alertType = alertType; return this; }
        public Builder alertLevel(AlertLevel alertLevel) { this.alertLevel = alertLevel; return this; }
        public Builder policyId(String policyId) { this.policyId = policyId; return this; }
        public Builder dedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; return this; }
        public Builder sourceService(String sourceService) { this.sourceService = sourceService; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder historyId(String historyId) { this.historyId = historyId; return this; }
        public Builder parseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; return this; }
        public Builder batchId(String batchId) { this.batchId = batchId; return this; }
        public Builder routeDecisionId(String routeDecisionId) { this.routeDecisionId = routeDecisionId; return this; }
        public Builder recommendationId(String recommendationId) { this.recommendationId = recommendationId; return this; }
        public Builder dispatchEventId(String dispatchEventId) { this.dispatchEventId = dispatchEventId; return this; }
        public Builder reportCode(String reportCode) { this.reportCode = reportCode; return this; }
        public Builder logicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; return this; }
        public Builder datasourceId(String datasourceId) { this.datasourceId = datasourceId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder evidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder alertStatus(AlertStatus alertStatus) { this.alertStatus = alertStatus; return this; }
        public Builder notifyStatus(NotifyStatus notifyStatus) { this.notifyStatus = notifyStatus; return this; }
        public Builder notifyMessage(String notifyMessage) { this.notifyMessage = notifyMessage; return this; }
        public Builder notifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; return this; }
        public Builder ackedBy(String ackedBy) { this.ackedBy = ackedBy; return this; }
        public Builder ackedAt(Instant ackedAt) { this.ackedAt = ackedAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public AlertEvent build() {
            return new AlertEvent(this);
        }
    }
}
