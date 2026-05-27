package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.utils.DateUtils;
import com.company.sqloptimization.domain.plan.AccelerationPlan;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationPlanRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationPlanMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisAccelerationPlanRepositoryTest {

    @Test
    void shouldConvertInstantTimestampsToLocalDateTimeWhenSaving() {
        AccelerationPlanMapper mapper = org.mockito.Mockito.mock(AccelerationPlanMapper.class);
        when(mapper.selectByPlanId(anyString())).thenReturn(null);
        MybatisAccelerationPlanRepository repository = new MybatisAccelerationPlanRepository(mapper);
        AccelerationPlan plan = samplePlan("plan-db-001");

        plan.attachGovernanceTrace("cfg-001", "result-001", "history-001");
        plan.markActivated("{\"runtimeStatus\":\"ACTIVE\"}", "user-001", Instant.parse("2026-04-25T05:00:10Z"));

        repository.save(plan);

        ArgumentCaptor<AccelerationPlanRecord> captor = ArgumentCaptor.forClass(AccelerationPlanRecord.class);
        verify(mapper).insert(captor.capture());
        AccelerationPlanRecord record = captor.getValue();
        assertEquals(
            DateUtils.toBeijingDateTime(Instant.parse("2026-04-25T05:00:00Z")),
            record.getCreatedAt());
        assertEquals(
            DateUtils.toBeijingDateTime(Instant.parse("2026-04-25T05:00:10Z")),
            record.getActivatedAt());
        assertEquals("cfg-001", record.getConfigSnapshotId());
        assertTrue(record.getStatusHistoryJson().contains("PLAN_ACTIVATED"));
    }

    @Test
    void shouldRestoreStructuredLifecycleFieldsFromDatabaseRecord() {
        AccelerationPlanMapper mapper = org.mockito.Mockito.mock(AccelerationPlanMapper.class);
        AccelerationPlanRecord record = new AccelerationPlanRecord();
        record.setPlanId("plan-db-002");
        record.setTenantId("tenant-a");
        record.setSourceTaskId("task-001");
        record.setSqlText("SELECT * FROM orders");
        record.setSqlFingerprint("fp-001");
        record.setDatasourceType("HETU");
        record.setSelectedSuggestionTypesJson("[\"PRECOMPUTE\",\"PARTITION\"]");
        record.setPlanStatus("PAUSED");
        record.setPlanSummary("active plan");
        record.setPrimaryRecommendation("activate governed runtime binding");
        record.setPlanPayloadJson("{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"dt\"}");
        record.setBenefitsJson("[{\"category\":\"PLAN_SIMPLIFICATION\",\"estimatedImprovementPercent\":35,\"summary\":\"smaller plan\"}]");
        record.setCostsJson("[{\"category\":\"VALIDATION\",\"level\":\"MEDIUM\",\"summary\":\"verify output\"}]");
        record.setRisksJson("[{\"level\":\"MEDIUM\",\"category\":\"DATA_DRIFT\",\"summary\":\"refresh lag\",\"mitigation\":\"verify\"}]");
        record.setConfigSnapshotId("cfg-002");
        record.setResultId("result-002");
        record.setHistoryId("history-002");
        record.setActivationEvidenceJson("{\"runtimeStatus\":\"ACTIVE\"}");
        record.setActivatedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 20));
        record.setActivatedBy("user-001");
        record.setPauseEvidenceJson("{\"runtimeStatus\":\"PAUSED\"}");
        record.setPausedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 30));
        record.setPausedBy("user-001");
        record.setStatusHistoryJson("["
            + "{\"previousStatus\":null,\"currentStatus\":\"READY\",\"occurredAt\":\"2026-04-25T05:00:00Z\",\"note\":\"PLAN_SUBMITTED\"},"
            + "{\"previousStatus\":\"READY\",\"currentStatus\":\"ACTIVE\",\"occurredAt\":\"2026-04-25T05:00:20Z\",\"note\":\"PLAN_ACTIVATED\"},"
            + "{\"previousStatus\":\"ACTIVE\",\"currentStatus\":\"PAUSED\",\"occurredAt\":\"2026-04-25T05:00:30Z\",\"note\":\"PLAN_PAUSED\"}"
            + "]");
        record.setCreatedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 30));
        when(mapper.selectByPlanId("plan-db-002")).thenReturn(record);

        MybatisAccelerationPlanRepository repository = new MybatisAccelerationPlanRepository(mapper);
        AccelerationPlan restored = repository.findByPlanId("plan-db-002");

        assertNotNull(restored);
        assertEquals(DateUtils.toInstant(LocalDateTime.of(2026, 4, 25, 5, 0, 0)), restored.getCreatedAt());
        assertEquals(DataSourceTypeEnum.HETU, restored.getDatasourceType());
        assertEquals(AccelerationSuggestionType.PRECOMPUTE, restored.getSelectedSuggestionTypes().get(0));
        assertEquals("user-001", restored.getPausedBy());
        assertEquals("PLAN_PAUSED", restored.getStatusHistory().get(2).getNote());
    }

    private AccelerationPlan samplePlan(String planId) {
        return AccelerationPlan.submit(
            planId,
            "tenant-a",
            "task-001",
            "SELECT * FROM orders",
            "fp-001",
            DataSourceTypeEnum.HETU,
            Arrays.asList(AccelerationSuggestionType.PRECOMPUTE, AccelerationSuggestionType.PARTITION),
            "ready plan",
            "activate governed runtime binding",
            "{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"dt\"}",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Instant.parse("2026-04-25T05:00:00Z")
        );
    }
}
