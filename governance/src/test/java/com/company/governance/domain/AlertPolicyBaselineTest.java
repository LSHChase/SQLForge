package com.company.governance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertPolicy;
import com.company.governance.domain.alert.AlertPolicyBaseline;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertPolicyBaselineTest {

    @Test
    void shouldGeneratePoliciesForAllAlertTypes() {
        Instant now = Instant.parse("2026-04-27T13:00:00Z");

        List<AlertPolicy> policies = AlertPolicyBaseline.defaultPoliciesForTenant("tenant-a", "operator-001", now);

        assertEquals(AlertEvent.AlertType.values().length, policies.size());
        AlertPolicy datasourcePolicy = findPolicy(policies, AlertEvent.AlertType.DATASOURCE_UNAVAILABLE);
        assertEquals(AlertEvent.AlertLevel.CRITICAL, datasourcePolicy.getDefaultLevel());
        assertEquals(300, datasourcePolicy.getDedupeWindowSeconds());
        assertEquals(AlertPolicy.DedupeStrategy.TENANT_ALERT_TYPE_TARGET, datasourcePolicy.getDedupeStrategy());
        assertEquals(AlertPolicy.NotifyChannel.SIMULATED_EMAIL, datasourcePolicy.getNotifyChannel());
        assertEquals(AlertEvent.NotifyStatus.SIMULATED_PENDING_NOTIFY, datasourcePolicy.getInitialNotifyStatus());
        assertTrue(datasourcePolicy.getRuleConfigJson().contains("\"baselineType\":\"DATASOURCE_UNAVAILABLE\""));
    }

    @Test
    void shouldMatchPolicyToSameTenantAndAlertType() {
        Instant now = Instant.parse("2026-04-27T13:00:00Z");
        AlertPolicy policy = findPolicy(
            AlertPolicyBaseline.defaultPoliciesForTenant("tenant-a", "operator-001", now),
            AlertEvent.AlertType.REPORT_SQL_RESOLVE_FAILURE
        );
        AlertEvent matchingEvent = AlertEvent.builder()
            .alertId("alert-004")
            .tenantId("tenant-a")
            .alertType(AlertEvent.AlertType.REPORT_SQL_RESOLVE_FAILURE)
            .reportCode("RPT_SALES_DAILY")
            .summary("Report SQL resolver failed")
            .createdAt(now)
            .build();

        assertTrue(policy.appliesTo(matchingEvent));
        assertEquals("TENANT_ADMIN", policy.getOwnerRole());
    }

    private AlertPolicy findPolicy(List<AlertPolicy> policies, AlertEvent.AlertType type) {
        for (AlertPolicy policy : policies) {
            if (policy.getAlertType() == type) {
                return policy;
            }
        }
        throw new IllegalStateException("Missing policy for " + type);
    }
}
