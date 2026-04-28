package com.company.governance.application.service;

import com.company.governance.application.controller.vo.GovernanceAlertDetailVO;
import com.company.governance.application.controller.vo.GovernanceAlertNotificationLogVO;
import com.company.governance.application.controller.vo.GovernanceAlertPageVO;
import com.company.governance.application.controller.vo.GovernanceAlertSummaryVO;
import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertNotificationLogMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceAlertApplicationService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_OPERATOR = "alert-api";

    private final AlertEventMapper alertEventMapper;
    private final AlertNotificationLogMapper alertNotificationLogMapper;
    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;

    public GovernanceAlertApplicationService(AlertEventMapper alertEventMapper,
                                             AlertNotificationLogMapper alertNotificationLogMapper,
                                             GovernanceProtectedPersistenceService governanceProtectedPersistenceService) {
        this.alertEventMapper = alertEventMapper;
        this.alertNotificationLogMapper = alertNotificationLogMapper;
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
    }

    public GovernanceAlertPageVO findAlertPage(String tenantId,
                                               String alertStatus,
                                               String alertType,
                                               String notifyStatus,
                                               Integer pageNo,
                                               Integer pageSize) {
        String effectiveTenantId = requireTenantId(tenantId);
        int resolvedPageNo = normalizePageNo(pageNo);
        int resolvedPageSize = normalizePageSize(pageSize);
        List<AlertEventRecord> records = alertEventMapper.selectByTenantIdFiltered(
            effectiveTenantId,
            normalizeEnumFilter(alertStatus),
            normalizeEnumFilter(alertType),
            normalizeEnumFilter(notifyStatus)
        );
        List<AlertEventRecord> safeRecords = records == null ? Collections.<AlertEventRecord>emptyList() : records;
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, safeRecords.size());
        List<GovernanceAlertSummaryVO> items = new ArrayList<GovernanceAlertSummaryVO>();
        for (AlertEventRecord record : safeRecords.subList(fromIndex, toIndex)) {
            items.add(toSummary(record));
        }
        return new GovernanceAlertPageVO(
            items,
            Integer.valueOf(resolvedPageNo),
            Integer.valueOf(resolvedPageSize),
            Integer.valueOf(safeRecords.size()),
            Boolean.valueOf(toIndex < safeRecords.size())
        );
    }

    public GovernanceAlertDetailVO findAlertDetail(String tenantId, String alertId) {
        String effectiveTenantId = requireTenantId(tenantId);
        AlertEventRecord record = requireAlertForTenant(effectiveTenantId, alertId);
        return toDetail(record, alertNotificationLogMapper.selectByAlertId(record.getAlertId()));
    }

    @Transactional
    public GovernanceAlertDetailVO ackAlert(String tenantId, String alertId, String operator, Instant ackedAt) {
        String effectiveTenantId = requireTenantId(tenantId);
        AlertEventRecord current = requireAlertForTenant(effectiveTenantId, alertId);
        AlertEvent event = toDomain(current);
        Instant effectiveAckedAt = ackedAt == null ? Instant.now() : ackedAt;
        String effectiveOperator = trimToNull(operator) == null ? DEFAULT_OPERATOR : operator.trim();
        try {
            event.ack(effectiveAckedAt, effectiveOperator);
        } catch (IllegalStateException ex) {
            throw new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
        AlertEventRecord updated = toRecord(event, current);
        alertEventMapper.update(updated);
        governanceProtectedPersistenceService.saveAuditLog(buildAckAuditLog(updated, effectiveOperator, effectiveAckedAt));
        return toDetail(updated, alertNotificationLogMapper.selectByAlertId(updated.getAlertId()));
    }

    private AuditLogRecord buildAckAuditLog(AlertEventRecord record, String operator, Instant ackedAt) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("alertId", record.getAlertId());
        payload.put("alertStatus", record.getAlertStatus());
        payload.put("notifyStatus", record.getNotifyStatus());
        payload.put("ackedBy", operator);
        payload.put("ackedAt", ackedAt.toString());
        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setTenantId(record.getTenantId());
        auditLogRecord.setServiceCode("GOVERNANCE");
        auditLogRecord.setOperationType("ALERT_ACK");
        auditLogRecord.setTargetType("ALERT_EVENT");
        auditLogRecord.setTargetId(record.getAlertId());
        auditLogRecord.setRequestId("alert-ack-" + record.getAlertId());
        auditLogRecord.setTraceId(record.getAlertId());
        auditLogRecord.setRequestParams(JsonUtils.toJson(payload));
        auditLogRecord.setResponseSummary("Alert acknowledged");
        auditLogRecord.setStatus("SUCCESS");
        auditLogRecord.setCostMs(Long.valueOf(0L));
        auditLogRecord.setCreateTime(toLocalDateTime(ackedAt));
        return auditLogRecord;
    }

    private GovernanceAlertSummaryVO toSummary(AlertEventRecord record) {
        GovernanceAlertSummaryVO vo = new GovernanceAlertSummaryVO();
        vo.setAlertId(record.getAlertId());
        vo.setAlertType(record.getAlertType());
        vo.setAlertLevel(record.getAlertLevel());
        vo.setAlertStatus(record.getAlertStatus());
        vo.setNotifyStatus(record.getNotifyStatus());
        vo.setPolicyId(record.getPolicyId());
        vo.setDedupeKey(record.getDedupeKey());
        vo.setSourceService(record.getSourceService());
        vo.setSummary(record.getSummary());
        vo.setNotifyMessage(record.getNotifyMessage());
        vo.setNotifiedAt(record.getNotifiedAt());
        vo.setAckedBy(record.getAckedBy());
        vo.setAckedAt(record.getAckedAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private GovernanceAlertDetailVO toDetail(AlertEventRecord record, List<AlertNotificationLogRecord> notificationLogs) {
        GovernanceAlertDetailVO vo = new GovernanceAlertDetailVO();
        GovernanceAlertSummaryVO summary = toSummary(record);
        vo.setAlertId(summary.getAlertId());
        vo.setAlertType(summary.getAlertType());
        vo.setAlertLevel(summary.getAlertLevel());
        vo.setAlertStatus(summary.getAlertStatus());
        vo.setNotifyStatus(summary.getNotifyStatus());
        vo.setPolicyId(summary.getPolicyId());
        vo.setDedupeKey(summary.getDedupeKey());
        vo.setSourceService(summary.getSourceService());
        vo.setSummary(summary.getSummary());
        vo.setNotifyMessage(summary.getNotifyMessage());
        vo.setNotifiedAt(summary.getNotifiedAt());
        vo.setAckedBy(summary.getAckedBy());
        vo.setAckedAt(summary.getAckedAt());
        vo.setCreatedAt(summary.getCreatedAt());
        vo.setUpdatedAt(summary.getUpdatedAt());
        vo.setTenantId(record.getTenantId());
        vo.setHistoryId(record.getHistoryId());
        vo.setParseTaskId(record.getParseTaskId());
        vo.setBatchId(record.getBatchId());
        vo.setRouteDecisionId(record.getRouteDecisionId());
        vo.setRecommendationId(record.getRecommendationId());
        vo.setDispatchEventId(record.getDispatchEventId());
        vo.setReportCode(record.getReportCode());
        vo.setLogicalObjectKey(record.getLogicalObjectKey());
        vo.setDatasourceId(record.getDatasourceId());
        vo.setSqlFingerprint(record.getSqlFingerprint());
        vo.setEvidence(parseJsonObject(record.getEvidenceJson()));
        List<GovernanceAlertNotificationLogVO> logItems = new ArrayList<GovernanceAlertNotificationLogVO>();
        for (AlertNotificationLogRecord item : notificationLogs == null ? Collections.<AlertNotificationLogRecord>emptyList() : notificationLogs) {
            logItems.add(toNotificationLog(item));
        }
        vo.setNotificationLogs(logItems);
        return vo;
    }

    private GovernanceAlertNotificationLogVO toNotificationLog(AlertNotificationLogRecord record) {
        GovernanceAlertNotificationLogVO vo = new GovernanceAlertNotificationLogVO();
        vo.setNotificationLogId(record.getNotificationLogId());
        vo.setAlertId(record.getAlertId());
        vo.setSourceAlertId(record.getSourceAlertId());
        vo.setNotifyChannel(record.getNotifyChannel());
        vo.setDeliveryStatus(record.getDeliveryStatus());
        vo.setTemplateCode(record.getTemplateCode());
        vo.setMessageSubject(record.getMessageSubject());
        vo.setMessageBody(record.getMessageBody());
        vo.setDeliverySummary(record.getDeliverySummary());
        vo.setPayload(parseJsonObject(record.getPayloadJson()));
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private AlertEventRecord requireAlertForTenant(String tenantId, String alertId) {
        String effectiveAlertId = trimToNull(alertId);
        if (effectiveAlertId == null) {
            throw new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, "alertId must not be empty");
        }
        AlertEventRecord record = alertEventMapper.selectByAlertId(effectiveAlertId);
        if (record == null || !tenantId.equals(record.getTenantId())) {
            throw new BizException(ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Alert not found");
        }
        return record;
    }

    private AlertEvent toDomain(AlertEventRecord record) {
        return AlertEvent.builder()
            .alertId(record.getAlertId())
            .tenantId(record.getTenantId())
            .alertType(AlertEvent.AlertType.valueOf(record.getAlertType()))
            .alertLevel(record.getAlertLevel() == null ? null : AlertEvent.AlertLevel.valueOf(record.getAlertLevel()))
            .policyId(record.getPolicyId())
            .dedupeKey(record.getDedupeKey())
            .sourceService(record.getSourceService())
            .summary(record.getSummary())
            .historyId(record.getHistoryId())
            .parseTaskId(record.getParseTaskId())
            .batchId(record.getBatchId())
            .routeDecisionId(record.getRouteDecisionId())
            .recommendationId(record.getRecommendationId())
            .dispatchEventId(record.getDispatchEventId())
            .reportCode(record.getReportCode())
            .logicalObjectKey(record.getLogicalObjectKey())
            .datasourceId(record.getDatasourceId())
            .sqlFingerprint(record.getSqlFingerprint())
            .evidenceJson(record.getEvidenceJson())
            .createdBy(record.getCreatedBy())
            .createdAt(toInstant(record.getCreatedAt()))
            .alertStatus(record.getAlertStatus() == null ? null : AlertEvent.AlertStatus.valueOf(record.getAlertStatus()))
            .notifyStatus(record.getNotifyStatus() == null ? null : AlertEvent.NotifyStatus.valueOf(record.getNotifyStatus()))
            .notifyMessage(record.getNotifyMessage())
            .notifiedAt(toInstant(record.getNotifiedAt()))
            .ackedBy(record.getAckedBy())
            .ackedAt(toInstant(record.getAckedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private AlertEventRecord toRecord(AlertEvent event, AlertEventRecord base) {
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
        record.setCreatedBy(base.getCreatedBy());
        record.setCreatedAt(base.getCreatedAt());
        record.setUpdatedAt(toLocalDateTime(event.getUpdatedAt()));
        return record;
    }

    private String requireTenantId(String tenantId) {
        String effectiveTenantId = trimToNull(tenantId);
        if (effectiveTenantId == null) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED, "Tenant context is missing");
        }
        return effectiveTenantId;
    }

    private String normalizeEnumFilter(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private int normalizePageNo(Integer pageNo) {
        int value = pageNo == null ? DEFAULT_PAGE_NO : pageNo.intValue();
        return value <= 0 ? DEFAULT_PAGE_NO : value;
    }

    private int normalizePageSize(Integer pageSize) {
        int value = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize.intValue();
        if (value <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, MAX_PAGE_SIZE);
    }

    private Map<String, Object> parseJsonObject(String json) {
        String normalized = trimToNull(json);
        if (normalized == null) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> parsed = JsonUtils.fromJson(normalized, Map.class);
            return parsed == null ? Collections.<String, Object>emptyMap() : parsed;
        } catch (RuntimeException ex) {
            Map<String, Object> fallback = new LinkedHashMap<String, Object>();
            fallback.put("raw", normalized);
            return fallback;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
