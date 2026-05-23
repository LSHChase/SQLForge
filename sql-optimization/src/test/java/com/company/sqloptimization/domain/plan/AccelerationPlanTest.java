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
            "ready plan",
            "use governed runtime activation",
            "{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"dt\"}",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            baseTime
        );

        plan.markActivated("{\"runtimeStatus\":\"ACTIVE\"}", "user-001", baseTime.plusSeconds(20));
        plan.markPaused("{\"runtimeStatus\":\"PAUSED\"}", "user-001", baseTime.plusSeconds(40));

        assertEquals(AccelerationPlanStatus.PAUSED, plan.getStatus());
        assertEquals("user-001", plan.getActivatedBy());
        assertEquals("user-001", plan.getPausedBy());
        assertEquals(3, plan.getStatusHistory().size());
        assertEquals("PLAN_SUBMITTED", plan.getStatusHistory().get(0).getNote());
        assertEquals("PLAN_ACTIVATED", plan.getStatusHistory().get(1).getNote());
        assertEquals("PLAN_PAUSED", plan.getStatusHistory().get(2).getNote());
    }

    @Test
    void shouldRejectPauseBeforeActivation() {
        Instant baseTime = Instant.parse("2026-04-25T00:00:00Z");
        AccelerationPlan plan = AccelerationPlan.submit(
            "plan-002",
            "tenant-a",
            "task-002",
            "SELECT * FROM orders",
            "fp-002",
            DataSourceTypeEnum.HETU,
            Collections.singletonList(AccelerationSuggestionType.PRECOMPUTE),
            "ready plan",
            "use governed runtime activation",
            "{\"PRECOMPUTE\":\"mv_orders\"}",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            baseTime
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> plan.markPaused("{\"runtimeStatus\":\"PAUSED\"}", "user-001", baseTime.plusSeconds(20))
        );

        assertEquals("加速方案暂停前必须处于 ACTIVE、PAUSED 或 PAUSE_FAILED 状态。", ex.getMessage());
    }
}
