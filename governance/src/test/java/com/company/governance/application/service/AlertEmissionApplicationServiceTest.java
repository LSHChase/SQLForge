package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertSignalSnapshot;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertNotificationLogMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertPolicyMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlertEmissionApplicationServiceTest {

    @Test
    void shouldPersistAlertAndWriteSimulatedNotificationAudit() {
        AlertRuleApplicationService ruleService = new AlertRuleApplicationService();
        AlertEventMapper alertEventMapper = org.mockito.Mockito.mock(AlertEventMapper.class);
        AlertPolicyMapper alertPolicyMapper = org.mockito.Mockito.mock(AlertPolicyMapper.class);
        AlertNotificationLogMapper alertNotificationLogMapper = org.mockito.Mockito.mock(AlertNotificationLogMapper.class);
        GovernanceProtectedPersistenceService protectedPersistenceService =
            org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        AlertEmissionApplicationService service = new AlertEmissionApplicationService(
            ruleService,
            alertEventMapper,
            alertPolicyMapper,
            alertNotificationLogMapper,
            protectedPersistenceService
        );
        Instant emittedAt = Instant.parse("2026-04-27T15:00:00Z");
        AlertSignalSnapshot snapshot = AlertSignalSnapshot.builder()
            .tenantId("tenant-a")
            .addMassFailureSignal(new AlertSignalSnapshot.MassFailureSignal("query-execution", "5m", 10, 7, 0.5d))
            .build();

        when(alertPolicyMapper.selectEnabledByTenantId("tenant-a")).thenReturn(Collections.emptyList());
        when(alertEventMapper.selectByTenantIdAndDedupeKey("tenant-a", "tenant-a|SQL_EXECUTION_MASS_FAILURE|service=query-execution"))
            .thenReturn(Collections.<AlertEventRecord>emptyList());

        AlertEmissionApplicationService.AlertEmissionResult result =
            service.emit(snapshot, "operator-001", emittedAt);

        assertEquals(1, result.getCandidateCount());
        assertEquals(1, result.getEmittedCount());
        assertEquals(0, result.getDedupeSuppressedCount());
        assertEquals(AlertEvent.NotifyStatus.SIMULATED_NOTIFIED, result.getEmittedAlerts().get(0).getNotifyStatus());

        ArgumentCaptor<AlertEventRecord> insertedEvent = ArgumentCaptor.forClass(AlertEventRecord.class);
        verify(alertEventMapper).insert(insertedEvent.capture());
        assertEquals("tenant-a", insertedEvent.getValue().getTenantId());
        assertEquals("SQL_EXECUTION_MASS_FAILURE", insertedEvent.getValue().getAlertType());
        assertEquals("SIMULATED_PENDING_NOTIFY", insertedEvent.getValue().getNotifyStatus());

        ArgumentCaptor<AlertNotificationLogRecord> notificationLog = ArgumentCaptor.forClass(AlertNotificationLogRecord.class);
        verify(alertNotificationLogMapper).insert(notificationLog.capture());
        assertEquals("SIMULATED_SENT", notificationLog.getValue().getDeliveryStatus());
        assertEquals("SIMULATED_EMAIL", notificationLog.getValue().getNotifyChannel());
        assertEquals("governance-alert-simulated-email-v1", notificationLog.getValue().getTemplateCode());
        assertTrue(notificationLog.getValue().getMessageBody().contains("通知已模拟发送"));
        assertTrue(notificationLog.getValue().getPayloadJson().contains("\"mode\":\"通知已模拟发送\""));

        ArgumentCaptor<AlertEventRecord> updatedEvent = ArgumentCaptor.forClass(AlertEventRecord.class);
        verify(alertEventMapper).update(updatedEvent.capture());
        assertEquals("SIMULATED_NOTIFIED", updatedEvent.getValue().getNotifyStatus());
        assertTrue(updatedEvent.getValue().getNotifyMessage().contains("通知已通过模拟方式发送"));
        assertNotNull(updatedEvent.getValue().getNotifiedAt());

        ArgumentCaptor<AuditLogRecord> auditLog = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(protectedPersistenceService).saveAuditLog(auditLog.capture());
        assertEquals("ALERT_NOTIFY_SIMULATED", auditLog.getValue().getOperationType());
        assertEquals("ALERT_EVENT", auditLog.getValue().getTargetType());
        assertEquals("SUCCESS", auditLog.getValue().getStatus());
    }

    @Test
    void shouldSuppressDedupedAlertsAndOnlyWriteSuppressionLog() {
        AlertRuleApplicationService ruleService = new AlertRuleApplicationService();
        AlertEventMapper alertEventMapper = org.mockito.Mockito.mock(AlertEventMapper.class);
        AlertPolicyMapper alertPolicyMapper = org.mockito.Mockito.mock(AlertPolicyMapper.class);
        AlertNotificationLogMapper alertNotificationLogMapper = org.mockito.Mockito.mock(AlertNotificationLogMapper.class);
        GovernanceProtectedPersistenceService protectedPersistenceService =
            org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        AlertEmissionApplicationService service = new AlertEmissionApplicationService(
            ruleService,
            alertEventMapper,
            alertPolicyMapper,
            alertNotificationLogMapper,
            protectedPersistenceService
        );
        Instant emittedAt = Instant.parse("2026-04-27T15:05:00Z");
        AlertSignalSnapshot snapshot = AlertSignalSnapshot.builder()
            .tenantId("tenant-a")
            .addMassFailureSignal(new AlertSignalSnapshot.MassFailureSignal("query-execution", "5m", 10, 7, 0.5d))
            .build();
        AlertEventRecord existing = new AlertEventRecord();
        existing.setAlertId("alert-existing-001");
        existing.setTenantId("tenant-a");
        existing.setDedupeKey("tenant-a|SQL_EXECUTION_MASS_FAILURE|service=query-execution");
        existing.setCreatedAt(LocalDateTime.ofInstant(emittedAt.minusSeconds(60), ZoneOffset.UTC));

        when(alertPolicyMapper.selectEnabledByTenantId("tenant-a")).thenReturn(Collections.emptyList());
        when(alertEventMapper.selectByTenantIdAndDedupeKey("tenant-a", "tenant-a|SQL_EXECUTION_MASS_FAILURE|service=query-execution"))
            .thenReturn(Collections.singletonList(existing));

        AlertEmissionApplicationService.AlertEmissionResult result =
            service.emit(snapshot, "operator-001", emittedAt);

        assertEquals(1, result.getCandidateCount());
        assertEquals(0, result.getEmittedCount());
        assertEquals(1, result.getDedupeSuppressedCount());

        verify(alertEventMapper, never()).insert(org.mockito.ArgumentMatchers.any(AlertEventRecord.class));
        verify(alertEventMapper, never()).update(org.mockito.ArgumentMatchers.any(AlertEventRecord.class));

        ArgumentCaptor<AlertNotificationLogRecord> notificationLog = ArgumentCaptor.forClass(AlertNotificationLogRecord.class);
        verify(alertNotificationLogMapper).insert(notificationLog.capture());
        assertEquals("DEDUPE_SUPPRESSED", notificationLog.getValue().getDeliveryStatus());
        assertEquals("alert-existing-001", notificationLog.getValue().getAlertId());
        assertEquals("alert-existing-001", notificationLog.getValue().getSourceAlertId());
        assertTrue(notificationLog.getValue().getMessageBody().contains("去重抑制已触发"));

        ArgumentCaptor<AuditLogRecord> auditLog = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(protectedPersistenceService).saveAuditLog(auditLog.capture());
        assertEquals("ALERT_DEDUPE_SUPPRESSED", auditLog.getValue().getOperationType());
        assertEquals("alert-existing-001", auditLog.getValue().getTargetId());
        assertTrue(auditLog.getValue().getRequestParams().contains("\"sourceAlertId\":\"alert-existing-001\""));
    }
}
