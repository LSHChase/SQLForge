package com.company.governance.domain.alert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AlertPolicyBaseline {

    private AlertPolicyBaseline() {
    }

    public static List<AlertPolicy> defaultPoliciesForTenant(String tenantId, String actor, Instant occurredAt) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("tenantId 为必填项");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt 为必填项");
        }
        List<AlertPolicy> policies = new ArrayList<AlertPolicy>(AlertEvent.AlertType.values().length);
        for (AlertEvent.AlertType type : AlertEvent.AlertType.values()) {
            String typeCode = type.name().toLowerCase(Locale.ROOT).replace('_', '-');
            policies.add(AlertPolicy.builder()
                .policyId(tenantId.trim() + "-alert-policy-" + typeCode)
                .tenantId(tenantId.trim())
                .policyName("baseline-" + typeCode)
                .alertType(type)
                .defaultLevel(type.defaultLevel())
                .dedupeStrategy(AlertPolicy.DedupeStrategy.TENANT_ALERT_TYPE_TARGET)
                .dedupeWindowSeconds(type.defaultDedupeWindowSeconds())
                .notifyChannel(AlertPolicy.NotifyChannel.SIMULATED_EMAIL)
                .initialNotifyStatus(AlertEvent.NotifyStatus.SIMULATED_PENDING_NOTIFY)
                .ownerScope("TENANT_SCOPE")
                .enabled(Boolean.TRUE)
                .ruleConfigJson(buildRuleConfig(type))
                .createdBy(actor)
                .createdAt(occurredAt)
                .updatedAt(occurredAt)
                .build());
        }
        return policies;
    }

    private static String buildRuleConfig(AlertEvent.AlertType type) {
        return "{"
            + "\"baselineType\":\"" + type.name() + "\","
            + "\"dedupeWindowSeconds\":" + type.defaultDedupeWindowSeconds() + ","
            + "\"notifyChannel\":\"SIMULATED_EMAIL\""
            + "}";
    }
}
