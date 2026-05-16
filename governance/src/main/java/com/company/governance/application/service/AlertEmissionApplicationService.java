package com.company.governance.application.service;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertPolicy;
import com.company.governance.domain.alert.AlertPolicyBaseline;
import com.company.governance.domain.alert.AlertSignalSnapshot;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.entity.AlertPolicyRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertNotificationLogMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertPolicyMapper;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertEmissionApplicationService {

    private static final String DEFAULT_OPERATOR = "alert-emitter";
    private static final String SIMULATED_EMAIL_TEMPLATE = "governance-alert-simulated-email-v1";
    private static final String DEDUPE_TEMPLATE = "governance-alert-dedupe-suppressed-v1";

    private final AlertRuleApplicationService alertRuleApplicationService;
    private final AlertEventMapper alertEventMapper;
    private final AlertPolicyMapper alertPolicyMapper;
    private final AlertNotificationLogMapper alertNotificationLogMapper;
    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;

    public AlertEmissionApplicationService(AlertRuleApplicationService alertRuleApplicationService,
                                           AlertEventMapper alertEventMapper,
                                           AlertPolicyMapper alertPolicyMapper,
                                           AlertNotificationLogMapper alertNotificationLogMapper,
                                           GovernanceProtectedPersistenceService governanceProtectedPersistenceService) {
        this.alertRuleApplicationService = alertRuleApplicationService;
        this.alertEventMapper = alertEventMapper;
        this.alertPolicyMapper = alertPolicyMapper;
        this.alertNotificationLogMapper = alertNotificationLogMapper;
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
    }

    public AlertEmissionResult emit(AlertSignalSnapshot snapshot) {
        return emit(snapshot, DEFAULT_OPERATOR, Instant.now());
    }

    @Transactional
    public AlertEmissionResult emit(AlertSignalSnapshot snapshot, String operator, Instant emittedAt) {
        requireSnapshot(snapshot);
        String effectiveOperator = normalize(operator, DEFAULT_OPERATOR);
        Instant effectiveEmittedAt = emittedAt == null ? Instant.now() : emittedAt;
        List<AlertPolicy> policies = resolvePolicies(snapshot.getTenantId(), effectiveOperator, effectiveEmittedAt);
        Map<AlertEvent.AlertType, AlertPolicy> policyByType = indexPolicies(policies);
        List<AlertEvent> evaluatedAlerts = alertRuleApplicationService.evaluate(
            snapshot,
            policies,
            effectiveOperator,
            effectiveEmittedAt
        );
        List<AlertEvent> emittedAlerts = new ArrayList<AlertEvent>();
        List<AlertNotificationLogRecord> notificationLogs = new ArrayList<AlertNotificationLogRecord>();
        int dedupeSuppressedCount = 0;
        for (AlertEvent alert : evaluatedAlerts) {
            AlertPolicy policy = resolvePolicy(policyByType, alert, effectiveOperator, effectiveEmittedAt);
            AlertEventRecord dedupeSource = findDedupeSource(alert, policy, effectiveEmittedAt);
            if (dedupeSource != null) {
                AlertNotificationLogRecord dedupeLog = buildDedupeSuppressedLog(
                    dedupeSource,
                    alert,
                    policy,
                    effectiveOperator,
                    effectiveEmittedAt
                );
                alertNotificationLogMapper.insert(dedupeLog);
                governanceProtectedPersistenceService.saveAuditLog(buildAuditLog(
                    dedupeSource.getTenantId(),
                    dedupeSource.getAlertId(),
                    dedupeLog.getNotificationLogId(),
                    "ALERT_DEDUPE_SUPPRESSED",
                    dedupeLog.getPayloadJson(),
                    dedupeLog.getDeliverySummary(),
                    effectiveEmittedAt
                ));
                notificationLogs.add(dedupeLog);
                dedupeSuppressedCount++;
                continue;
            }
            alertEventMapper.insert(toRecord(alert));
            AlertNotificationLogRecord notifyLog = buildSimulatedNotifyLog(alert, policy, effectiveOperator, effectiveEmittedAt);
            alertNotificationLogMapper.insert(notifyLog);
            alert.markNotified(effectiveEmittedAt, notifyLog.getDeliverySummary());
            alertEventMapper.update(toRecord(alert));
            governanceProtectedPersistenceService.saveAuditLog(buildAuditLog(
                alert.getTenantId(),
                alert.getAlertId(),
                notifyLog.getNotificationLogId(),
                "ALERT_NOTIFY_SIMULATED",
                notifyLog.getPayloadJson(),
                notifyLog.getDeliverySummary(),
                effectiveEmittedAt
            ));
            emittedAlerts.add(alert);
            notificationLogs.add(notifyLog);
        }
        return new AlertEmissionResult(
            evaluatedAlerts.size(),
            emittedAlerts.size(),
            dedupeSuppressedCount,
            emittedAlerts,
            notificationLogs
        );
    }

    private List<AlertPolicy> resolvePolicies(String tenantId, String operator, Instant emittedAt) {
        List<AlertPolicyRecord> records = alertPolicyMapper.selectEnabledByTenantId(tenantId);
        if (records == null || records.isEmpty()) {
            return AlertPolicyBaseline.defaultPoliciesForTenant(tenantId, operator, emittedAt);
        }
        List<AlertPolicy> policies = new ArrayList<AlertPolicy>();
        for (AlertPolicyRecord record : records) {
            AlertPolicy policy = toPolicy(record);
            if (policy != null) {
                policies.add(policy);
            }
        }
        return policies.isEmpty()
            ? AlertPolicyBaseline.defaultPoliciesForTenant(tenantId, operator, emittedAt)
            : policies;
    }

    private Map<AlertEvent.AlertType, AlertPolicy> indexPolicies(List<AlertPolicy> policies) {
        Map<AlertEvent.AlertType, AlertPolicy> index = new LinkedHashMap<AlertEvent.AlertType, AlertPolicy>();
        for (AlertPolicy policy : policies) {
            index.put(policy.getAlertType(), policy);
        }
        return index;
    }

    private AlertPolicy resolvePolicy(Map<AlertEvent.AlertType, AlertPolicy> policyByType,
                                      AlertEvent alert,
                                      String operator,
                                      Instant emittedAt) {
        AlertPolicy policy = policyByType.get(alert.getAlertType());
        if (policy != null) {
            return policy;
        }
        return AlertPolicyBaseline.defaultPoliciesForTenant(alert.getTenantId(), operator, emittedAt)
            .stream()
            .filter(candidate -> candidate.getAlertType() == alert.getAlertType())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("缺少告警策略：" + alert.getAlertType()));
    }

    private AlertEventRecord findDedupeSource(AlertEvent alert, AlertPolicy policy, Instant emittedAt) {
        List<AlertEventRecord> existingRecords = alertEventMapper.selectByTenantIdAndDedupeKey(
            alert.getTenantId(),
            alert.getDedupeKey()
        );
        if (existingRecords == null || existingRecords.isEmpty()) {
            return null;
        }
        Instant dedupeCutoff = emittedAt.minusSeconds(policy.getDedupeWindowSeconds());
        for (AlertEventRecord record : existingRecords) {
            Instant recordCreatedAt = toInstant(record.getCreatedAt());
            if (recordCreatedAt != null && !recordCreatedAt.isBefore(dedupeCutoff)) {
                return record;
            }
        }
        return null;
    }

    private AlertNotificationLogRecord buildSimulatedNotifyLog(AlertEvent alert,
                                                               AlertPolicy policy,
                                                               String operator,
                                                               Instant emittedAt) {
        String subject = "[SQLForge][" + alert.getAlertLevel().name() + "] " + alert.getAlertType().name();
        String body = "通知已模拟发送\n"
            + "tenant=" + alert.getTenantId() + "\n"
            + "alertId=" + alert.getAlertId() + "\n"
            + "policyId=" + normalize(policy.getPolicyId(), "baseline-policy") + "\n"
            + "摘要=" + alert.getSummary() + "\n"
            + "dedupeKey=" + alert.getDedupeKey();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("mode", "通知已模拟发送");
        payload.put("alertId", alert.getAlertId());
        payload.put("alertType", alert.getAlertType().name());
        payload.put("alertLevel", alert.getAlertLevel().name());
        payload.put("dedupeKey", alert.getDedupeKey());
        payload.put("policyId", policy.getPolicyId());
        payload.put("notifyChannel", policy.getNotifyChannel().name());
        payload.put("summary", alert.getSummary());
        payload.put("evidenceJson", alert.getEvidenceJson());

        AlertNotificationLogRecord record = new AlertNotificationLogRecord();
        record.setNotificationLogId("alert-notify-" + alert.getAlertId());
        record.setTenantId(alert.getTenantId());
        record.setAlertId(alert.getAlertId());
        record.setSourceAlertId(null);
        record.setDedupeKey(alert.getDedupeKey());
        record.setNotifyChannel(policy.getNotifyChannel().name());
        record.setDeliveryStatus("SIMULATED_SENT");
        record.setTemplateCode(SIMULATED_EMAIL_TEMPLATE);
        record.setMessageSubject(subject);
        record.setMessageBody(body);
        record.setDeliverySummary("通知已通过模拟方式发送：" + policy.getNotifyChannel().name() + "，模板=" + SIMULATED_EMAIL_TEMPLATE);
        record.setPayloadJson(JsonUtils.toJson(payload));
        record.setCreatedBy(operator);
        record.setCreatedAt(toLocalDateTime(emittedAt));
        return record;
    }

    private AlertNotificationLogRecord buildDedupeSuppressedLog(AlertEventRecord source,
                                                                AlertEvent suppressed,
                                                                AlertPolicy policy,
                                                                String operator,
                                                                Instant emittedAt) {
        String subject = "[SQLForge][DEDUPE_SUPPRESSED] " + suppressed.getAlertType().name();
        String body = "去重抑制已触发\n"
            + "tenant=" + suppressed.getTenantId() + "\n"
            + "sourceAlertId=" + source.getAlertId() + "\n"
            + "suppressedAlertType=" + suppressed.getAlertType().name() + "\n"
            + "摘要=" + suppressed.getSummary() + "\n"
            + "dedupeKey=" + suppressed.getDedupeKey();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("mode", "去重抑制");
        payload.put("sourceAlertId", source.getAlertId());
        payload.put("dedupeKey", suppressed.getDedupeKey());
        payload.put("suppressedAlertType", suppressed.getAlertType().name());
        payload.put("suppressedSummary", suppressed.getSummary());
        payload.put("suppressedEvidenceJson", suppressed.getEvidenceJson());
        payload.put("notifyChannel", policy.getNotifyChannel().name());
        payload.put("dedupeWindowSeconds", Integer.valueOf(policy.getDedupeWindowSeconds()));

        AlertNotificationLogRecord record = new AlertNotificationLogRecord();
        record.setNotificationLogId("alert-dedupe-" + source.getAlertId() + "-" + compactInstant(emittedAt));
        record.setTenantId(source.getTenantId());
        record.setAlertId(source.getAlertId());
        record.setSourceAlertId(source.getAlertId());
        record.setDedupeKey(suppressed.getDedupeKey());
        record.setNotifyChannel(policy.getNotifyChannel().name());
        record.setDeliveryStatus("DEDUPE_SUPPRESSED");
        record.setTemplateCode(DEDUPE_TEMPLATE);
        record.setMessageSubject(subject);
        record.setMessageBody(body);
        record.setDeliverySummary("去重抑制已触发，sourceAlertId=" + source.getAlertId());
        record.setPayloadJson(JsonUtils.toJson(payload));
        record.setCreatedBy(operator);
        record.setCreatedAt(toLocalDateTime(emittedAt));
        return record;
    }

    private AuditLogRecord buildAuditLog(String tenantId,
                                         String alertId,
                                         String notificationLogId,
                                         String operationType,
                                         String requestParams,
                                         String responseSummary,
                                         Instant emittedAt) {
        AuditLogRecord record = new AuditLogRecord();
        record.setTenantId(tenantId);
        record.setServiceCode("GOVERNANCE");
        record.setOperationType(operationType);
        record.setTargetType("ALERT_EVENT");
        record.setTargetId(alertId);
        record.setRequestId(notificationLogId);
        record.setTraceId(alertId);
        record.setRequestParams(requestParams);
        record.setResponseSummary(responseSummary);
        record.setStatus("SUCCESS");
        record.setCostMs(Long.valueOf(0L));
        record.setCreateTime(toLocalDateTime(emittedAt));
        return record;
    }

    private AlertEventRecord toRecord(AlertEvent event) {
        AlertEventRecord record = new AlertEventRecord();
        record.setAlertId(event.getAlertId());
        record.setTenantId(event.getTenantId());
        record.setAlertType(event.getAlertType().name());
        record.setAlertLevel(event.getAlertLevel().name());
        record.setAlertStatus(event.getAlertStatus().name());
        record.setNotifyStatus(event.getNotifyStatus().name());
        record.setPolicyId(event.getPolicyId());
        record.setDedupeKey(event.getDedupeKey());
        record.setSourceService(event.getSourceService());
        record.setSummary(event.getSummary());
        record.setHistoryId(event.getHistoryId());
        record.setParseTaskId(event.getParseTaskId());
        record.setBatchId(event.getBatchId());
        record.setRouteDecisionId(event.getRouteDecisionId());
        record.setRecommendationId(event.getRecommendationId());
        record.setDispatchEventId(event.getDispatchEventId());
        record.setReportCode(event.getReportCode());
        record.setLogicalObjectKey(event.getLogicalObjectKey());
        record.setDatasourceId(event.getDatasourceId());
        record.setSqlFingerprint(event.getSqlFingerprint());
        record.setEvidenceJson(event.getEvidenceJson());
        record.setNotifyMessage(event.getNotifyMessage());
        record.setNotifiedAt(toLocalDateTime(event.getNotifiedAt()));
        record.setAckedBy(event.getAckedBy());
        record.setAckedAt(toLocalDateTime(event.getAckedAt()));
        record.setCreatedBy(event.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(event.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(event.getUpdatedAt()));
        return record;
    }

    private AlertPolicy toPolicy(AlertPolicyRecord record) {
        if (record == null) {
            return null;
        }
        try {
            return AlertPolicy.builder()
                .policyId(record.getPolicyId())
                .tenantId(record.getTenantId())
                .policyName(record.getPolicyName())
                .alertType(AlertEvent.AlertType.valueOf(record.getAlertType()))
                .defaultLevel(record.getDefaultLevel() == null ? null : AlertEvent.AlertLevel.valueOf(record.getDefaultLevel()))
                .dedupeStrategy(record.getDedupeStrategy() == null
                    ? null
                    : AlertPolicy.DedupeStrategy.valueOf(record.getDedupeStrategy()))
                .dedupeWindowSeconds(record.getDedupeWindowSeconds() == null ? 0 : record.getDedupeWindowSeconds().intValue())
                .notifyChannel(record.getNotifyChannel() == null
                    ? null
                    : AlertPolicy.NotifyChannel.valueOf(record.getNotifyChannel()))
                .initialNotifyStatus(record.getInitialNotifyStatus() == null
                    ? null
                    : AlertEvent.NotifyStatus.valueOf(record.getInitialNotifyStatus()))
                .ownerRole(record.getOwnerRole())
                .enabled(record.getEnabled())
                .ruleConfigJson(record.getRuleConfigJson())
                .createdBy(record.getCreatedBy())
                .createdAt(toInstant(record.getCreatedAt()))
                .updatedAt(toInstant(record.getUpdatedAt()))
                .build();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void requireSnapshot(AlertSignalSnapshot snapshot) {
        if (snapshot == null || trimToNull(snapshot.getTenantId()) == null) {
            throw new IllegalArgumentException("snapshot.tenantId 为必填项");
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private String normalize(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String compactInstant(Instant instant) {
        return instant.toString().replace(":", "").replace("-", "");
    }

    public static final class AlertEmissionResult {
        private final int candidateCount;
        private final int emittedCount;
        private final int dedupeSuppressedCount;
        private final List<AlertEvent> emittedAlerts;
        private final List<AlertNotificationLogRecord> notificationLogs;

        AlertEmissionResult(int candidateCount,
                            int emittedCount,
                            int dedupeSuppressedCount,
                            List<AlertEvent> emittedAlerts,
                            List<AlertNotificationLogRecord> notificationLogs) {
            this.candidateCount = candidateCount;
            this.emittedCount = emittedCount;
            this.dedupeSuppressedCount = dedupeSuppressedCount;
            this.emittedAlerts = Collections.unmodifiableList(new ArrayList<AlertEvent>(emittedAlerts));
            this.notificationLogs = Collections.unmodifiableList(new ArrayList<AlertNotificationLogRecord>(notificationLogs));
        }

        public int getCandidateCount() { return candidateCount; }
        public int getEmittedCount() { return emittedCount; }
        public int getDedupeSuppressedCount() { return dedupeSuppressedCount; }
        public List<AlertEvent> getEmittedAlerts() { return emittedAlerts; }
        public List<AlertNotificationLogRecord> getNotificationLogs() { return notificationLogs; }
    }
}
