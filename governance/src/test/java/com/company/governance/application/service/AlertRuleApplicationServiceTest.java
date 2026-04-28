package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertSignalSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertRuleApplicationServiceTest {

    private final AlertRuleApplicationService service = new AlertRuleApplicationService();

    @Test
    void shouldCreateMassFailureDependencyAndDispatchAlerts() {
        AlertSignalSnapshot snapshot = AlertSignalSnapshot.builder()
            .tenantId("tenant-a")
            .addMassFailureSignal(new AlertSignalSnapshot.MassFailureSignal("query-execution", "5m", 10, 7, 0.5d))
            .addServiceAvailabilitySignal(new AlertSignalSnapshot.ServiceAvailabilitySignal("governance", "history-lookup", false, "HTTP_503"))
            .addDispatchCoordinationSignal(new AlertSignalSnapshot.DispatchCoordinationSignal("dispatch-001", "FAILED", "hetu-main", "external loader rejected event", 20, 15))
            .build();

        List<AlertEvent> alerts = service.evaluate(snapshot, null, "operator-001", Instant.parse("2026-04-27T14:00:00Z"));

        assertEquals(3, alerts.size());
        assertHasType(alerts, AlertEvent.AlertType.SQL_EXECUTION_MASS_FAILURE);
        assertHasType(alerts, AlertEvent.AlertType.DEPENDENCY_SERVICE_UNAVAILABLE);
        assertHasType(alerts, AlertEvent.AlertType.DISPATCH_COORDINATION_FAILED);
    }

    @Test
    void shouldCreateDatasourceReportRedisAndAuditAlerts() {
        AlertSignalSnapshot snapshot = AlertSignalSnapshot.builder()
            .tenantId("tenant-a")
            .addDatasourceAvailabilitySignal(new AlertSignalSnapshot.DatasourceAvailabilitySignal("ds-001", "hetu_main", "DEGRADED", "CONNECTION_ENDPOINT_UNREACHABLE", true))
            .addReportResolveSignal(new AlertSignalSnapshot.ReportResolveSignal("cfg-001", "hetu_main", "PROD", "MOCK_FALLBACK", "REPORT_INTERFACE_CONFIG_NOT_FOUND", false))
            .addRedisRuleAvailabilitySignal(new AlertSignalSnapshot.RedisRuleAvailabilitySignal("redis-001", "tenant-rule-source", "DEGRADED", "REDIS_ENDPOINTS_MISSING", true, true))
            .addAuditWriteSignal(new AlertSignalSnapshot.AuditWriteSignal("governance-audit", 2, 1))
            .build();

        List<AlertEvent> alerts = service.evaluate(snapshot, null, "operator-001", Instant.parse("2026-04-27T14:05:00Z"));

        assertEquals(4, alerts.size());
        assertHasType(alerts, AlertEvent.AlertType.DATASOURCE_UNAVAILABLE);
        assertHasType(alerts, AlertEvent.AlertType.REPORT_SQL_RESOLVE_FAILURE);
        assertHasType(alerts, AlertEvent.AlertType.REDIS_RULE_SOURCE_UNAVAILABLE);
        assertHasType(alerts, AlertEvent.AlertType.AUDIT_WRITE_EXCEPTION);
    }

    @Test
    void shouldMapAccessParseOutageToDedicatedAlertType() {
        AlertSignalSnapshot snapshot = AlertSignalSnapshot.builder()
            .tenantId("tenant-a")
            .addServiceAvailabilitySignal(new AlertSignalSnapshot.ServiceAvailabilitySignal("access_parse_service", "access-parse", false, "TIMEOUT"))
            .build();

        List<AlertEvent> alerts = service.evaluate(snapshot, null, "operator-001", Instant.parse("2026-04-27T14:10:00Z"));

        assertEquals(1, alerts.size());
        assertEquals(AlertEvent.AlertType.ACCESS_PARSE_SERVICE_UNAVAILABLE, alerts.get(0).getAlertType());
    }

    @Test
    void shouldDeduplicateEquivalentDispatchSignals() {
        AlertSignalSnapshot snapshot = AlertSignalSnapshot.builder()
            .tenantId("tenant-a")
            .addDispatchCoordinationSignal(new AlertSignalSnapshot.DispatchCoordinationSignal("dispatch-002", "FAILED", "hetu-main", "failure", 20, 15))
            .addDispatchCoordinationSignal(new AlertSignalSnapshot.DispatchCoordinationSignal("dispatch-002", "FAILED", "hetu-main", "failure", 20, 15))
            .build();

        List<AlertEvent> alerts = service.evaluate(snapshot, null, "operator-001", Instant.parse("2026-04-27T14:15:00Z"));

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).getDedupeKey().contains("dispatch=dispatch-002"));
        assertEquals(AlertEvent.NotifyStatus.SIMULATED_PENDING_NOTIFY, alerts.get(0).getNotifyStatus());
    }

    private void assertHasType(List<AlertEvent> alerts, AlertEvent.AlertType type) {
        for (AlertEvent event : alerts) {
            if (event.getAlertType() == type) {
                return;
            }
        }
        throw new AssertionError("Missing alert type " + type);
    }
}
