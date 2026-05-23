package com.company.governance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.domain.alert.AlertEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertEventTest {

    @Test
    void shouldApplyDefaultLevelNotifyStatusAndDedupeKey() {
        Instant now = Instant.parse("2026-04-27T12:00:00Z");

        AlertEvent event = AlertEvent.builder()
            .alertId("alert-001")
            .tenantId("tenant-a")
            .alertType(AlertEvent.AlertType.DATASOURCE_UNAVAILABLE)
            .datasourceId("hetu-main")
            .summary("Datasource health probe failed")
            .createdBy("user-001")
            .createdAt(now)
            .build();

        assertEquals(AlertEvent.AlertLevel.CRITICAL, event.getAlertLevel());
        assertEquals(AlertEvent.AlertStatus.OPEN, event.getAlertStatus());
        assertEquals(AlertEvent.NotifyStatus.SIMULATED_PENDING_NOTIFY, event.getNotifyStatus());
        assertTrue(event.getDedupeKey().contains("tenant-a|DATASOURCE_UNAVAILABLE"));
        assertTrue(event.getDedupeKey().contains("datasource=hetu-main"));
    }

    @Test
    void shouldSupportSimulatedNotifyAndAckLifecycle() {
        Instant createdAt = Instant.parse("2026-04-27T12:00:00Z");
        Instant notifiedAt = Instant.parse("2026-04-27T12:05:00Z");
        Instant ackedAt = Instant.parse("2026-04-27T12:07:00Z");
        AlertEvent event = AlertEvent.builder()
            .alertId("alert-002")
            .tenantId("tenant-a")
            .alertType(AlertEvent.AlertType.DISPATCH_COORDINATION_FAILED)
            .dispatchEventId("dispatch-001")
            .summary("Dispatch event has not been acked by the external module")
            .createdAt(createdAt)
            .build();

        event.markNotified(notifiedAt, "simulated email queued");
        event.ack(ackedAt, "operator-002");

        assertEquals(AlertEvent.NotifyStatus.SIMULATED_NOTIFIED, event.getNotifyStatus());
        assertEquals("simulated email queued", event.getNotifyMessage());
        assertEquals(AlertEvent.AlertStatus.ACKED, event.getAlertStatus());
        assertEquals("operator-002", event.getAckedBy());
        assertEquals(ackedAt, event.getUpdatedAt());
    }

    @Test
    void shouldRejectDuplicateAck() {
        Instant now = Instant.parse("2026-04-27T12:00:00Z");
        AlertEvent event = AlertEvent.builder()
            .alertId("alert-003")
            .tenantId("tenant-a")
            .alertType(AlertEvent.AlertType.AUDIT_WRITE_EXCEPTION)
            .summary("Audit writer returned error")
            .createdAt(now)
            .build();

        event.ack(now.plusSeconds(30), "operator-003");

        assertThrows(IllegalStateException.class, () -> event.ack(now.plusSeconds(60), "operator-004"));
    }
}
