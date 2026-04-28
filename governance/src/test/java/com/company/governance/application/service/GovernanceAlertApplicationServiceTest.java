package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.GovernanceAlertDetailVO;
import com.company.governance.application.controller.vo.GovernanceAlertPageVO;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertNotificationLogMapper;
import com.company.sqlforge.common.exception.BizException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceAlertApplicationServiceTest {

    @Test
    void shouldPageTenantAlerts() {
        AlertEventMapper alertEventMapper = org.mockito.Mockito.mock(AlertEventMapper.class);
        AlertNotificationLogMapper alertNotificationLogMapper = org.mockito.Mockito.mock(AlertNotificationLogMapper.class);
        GovernanceProtectedPersistenceService protectedPersistenceService =
            org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        GovernanceAlertApplicationService service = new GovernanceAlertApplicationService(
            alertEventMapper,
            alertNotificationLogMapper,
            protectedPersistenceService
        );
        when(alertEventMapper.selectByTenantIdFiltered("tenant-a", "OPEN", null, null)).thenReturn(Arrays.asList(
            alertRecord("alert-001", "tenant-a", "OPEN", null),
            alertRecord("alert-002", "tenant-a", "OPEN", null)
        ));

        GovernanceAlertPageVO page = service.findAlertPage("tenant-a", "open", null, null, Integer.valueOf(2), Integer.valueOf(1));

        assertEquals(Integer.valueOf(2), page.getPageNo());
        assertEquals(Integer.valueOf(1), page.getPageSize());
        assertEquals(Integer.valueOf(2), page.getTotal());
        assertEquals(Boolean.FALSE, page.getHasNext());
        assertEquals("alert-002", page.getItems().get(0).getAlertId());
    }

    @Test
    void shouldAcknowledgeOpenAlertAndPersistAudit() {
        AlertEventMapper alertEventMapper = org.mockito.Mockito.mock(AlertEventMapper.class);
        AlertNotificationLogMapper alertNotificationLogMapper = org.mockito.Mockito.mock(AlertNotificationLogMapper.class);
        GovernanceProtectedPersistenceService protectedPersistenceService =
            org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        GovernanceAlertApplicationService service = new GovernanceAlertApplicationService(
            alertEventMapper,
            alertNotificationLogMapper,
            protectedPersistenceService
        );
        AlertEventRecord existing = alertRecord("alert-001", "tenant-a", "OPEN", null);
        AlertNotificationLogRecord logRecord = new AlertNotificationLogRecord();
        logRecord.setNotificationLogId("log-001");
        logRecord.setAlertId("alert-001");
        logRecord.setDeliveryStatus("SIMULATED_SENT");
        when(alertEventMapper.selectByAlertId("alert-001")).thenReturn(existing);
        when(alertNotificationLogMapper.selectByAlertId("alert-001")).thenReturn(Collections.singletonList(logRecord));

        GovernanceAlertDetailVO detail = service.ackAlert(
            "tenant-a",
            "alert-001",
            "operator-001",
            Instant.parse("2026-04-27T16:00:00Z")
        );

        assertEquals("ACKED", detail.getAlertStatus());
        assertEquals("operator-001", detail.getAckedBy());
        assertEquals(1, detail.getNotificationLogs().size());

        ArgumentCaptor<AlertEventRecord> eventCaptor = ArgumentCaptor.forClass(AlertEventRecord.class);
        verify(alertEventMapper).update(eventCaptor.capture());
        assertEquals("ACKED", eventCaptor.getValue().getAlertStatus());
        assertEquals("operator-001", eventCaptor.getValue().getAckedBy());

        ArgumentCaptor<AuditLogRecord> auditCaptor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(protectedPersistenceService).saveAuditLog(auditCaptor.capture());
        assertEquals("ALERT_ACK", auditCaptor.getValue().getOperationType());
        assertEquals("alert-001", auditCaptor.getValue().getTargetId());
    }

    @Test
    void shouldRejectCrossTenantDetailLookup() {
        AlertEventMapper alertEventMapper = org.mockito.Mockito.mock(AlertEventMapper.class);
        AlertNotificationLogMapper alertNotificationLogMapper = org.mockito.Mockito.mock(AlertNotificationLogMapper.class);
        GovernanceProtectedPersistenceService protectedPersistenceService =
            org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        GovernanceAlertApplicationService service = new GovernanceAlertApplicationService(
            alertEventMapper,
            alertNotificationLogMapper,
            protectedPersistenceService
        );
        when(alertEventMapper.selectByAlertId("alert-001")).thenReturn(alertRecord("alert-001", "tenant-b", "OPEN", null));

        BizException exception = assertThrows(BizException.class, () -> service.findAlertDetail("tenant-a", "alert-001"));

        assertEquals(10004, exception.getCode());
        assertEquals("Alert not found", exception.getMessage());
    }

    private AlertEventRecord alertRecord(String alertId, String tenantId, String status, String ackedBy) {
        AlertEventRecord record = new AlertEventRecord();
        record.setAlertId(alertId);
        record.setTenantId(tenantId);
        record.setAlertType("DISPATCH_COORDINATION_FAILED");
        record.setAlertLevel("HIGH");
        record.setAlertStatus(status);
        record.setNotifyStatus("SIMULATED_NOTIFIED");
        record.setPolicyId("policy-001");
        record.setDedupeKey("tenant-a|DISPATCH_COORDINATION_FAILED|dispatch=dispatch-001");
        record.setSourceService("dispatch-service");
        record.setSummary("Dispatch failed");
        record.setNotifyMessage("notify simulated");
        record.setCreatedBy("alert-emitter");
        record.setCreatedAt(LocalDateTime.ofInstant(Instant.parse("2026-04-27T15:00:00Z"), ZoneOffset.UTC));
        record.setUpdatedAt(LocalDateTime.ofInstant(Instant.parse("2026-04-27T15:05:00Z"), ZoneOffset.UTC));
        record.setAckedBy(ackedBy);
        return record;
    }
}
