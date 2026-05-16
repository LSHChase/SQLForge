package com.company.sqloptimization.domain.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class AccelerationPlanTest {

    @Test
    void shouldAdvanceAcrossGovernedLifecycle() {
        Instant baseTime = Instant.parse("2026-04-25T00:00:00Z");
        AccelerationPlan plan = AccelerationPlan.submit(
            "plan-001",
            "tenant-a",
            "task-001",
            "SELECT * FROM orders",
            "fp-001",
            DataSourceTypeEnum.HETU,
            Arrays.asList(AccelerationSuggestionType.PRECOMPUTE, AccelerationSuggestionType.PARTITION),
            "approved plan",
            "use governed runtime apply",
            "{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"dt\"}",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            baseTime
        );

        plan.approve("looks safe", "approver-001", baseTime.plusSeconds(10));
        plan.markApplied("{\"runtimeStatus\":\"APPLIED\"}", "operator-001", baseTime.plusSeconds(20));
        plan.markVerified("{\"runtimeStatus\":\"VERIFIED\"}", "operator-001", baseTime.plusSeconds(30));
        plan.markRolledBack("{\"runtimeStatus\":\"ROLLED_BACK\"}", "operator-001", baseTime.plusSeconds(40));

        assertEquals(AccelerationPlanStatus.ROLLED_BACK, plan.getStatus());
        assertEquals("approver-001", plan.getApprovedBy());
        assertEquals("operator-001", plan.getRolledBackBy());
        assertEquals(5, plan.getStatusHistory().size());
        assertEquals("PLAN_SUBMITTED", plan.getStatusHistory().get(0).getNote());
        assertEquals("PLAN_APPROVED", plan.getStatusHistory().get(1).getNote());
        assertEquals("PLAN_APPLIED", plan.getStatusHistory().get(2).getNote());
        assertEquals("PLAN_VERIFIED", plan.getStatusHistory().get(3).getNote());
        assertEquals("PLAN_ROLLED_BACK", plan.getStatusHistory().get(4).getNote());
    }

    @Test
    void shouldRejectVerificationBeforeApply() {
        Instant baseTime = Instant.parse("2026-04-25T00:00:00Z");
        AccelerationPlan plan = AccelerationPlan.submit(
            "plan-002",
            "tenant-a",
            "task-002",
            "SELECT * FROM orders",
            "fp-002",
            DataSourceTypeEnum.HETU,
            Collections.singletonList(AccelerationSuggestionType.PRECOMPUTE),
            "approved plan",
            "use governed runtime apply",
            "{\"PRECOMPUTE\":\"mv_orders\"}",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            baseTime
        );

        plan.approve("looks safe", "approver-001", baseTime.plusSeconds(10));

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> plan.markVerified("{\"runtimeStatus\":\"VERIFIED\"}", "operator-001", baseTime.plusSeconds(20))
        );

        assertEquals("加速方案校验前必须已应用。", ex.getMessage());
    }
}
