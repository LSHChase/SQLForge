package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
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
        plan.approve("approve", "approver-001", Instant.parse("2026-04-25T05:00:10Z"));

        repository.save(plan);

        ArgumentCaptor<AccelerationPlanRecord> captor = ArgumentCaptor.forClass(AccelerationPlanRecord.class);
        verify(mapper).insert(captor.capture());
        AccelerationPlanRecord record = captor.getValue();
        assertEquals(LocalDateTime.of(2026, 4, 25, 5, 0, 0), record.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 4, 25, 5, 0, 10), record.getApprovedAt());
        assertEquals("cfg-001", record.getConfigSnapshotId());
        assertTrue(record.getStatusHistoryJson().contains("PLAN_APPROVED"));
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
        record.setPlanStatus("VERIFIED");
        record.setPlanSummary("approved plan");
        record.setPrimaryRecommendation("apply governed runtime binding");
        record.setPlanPayloadJson("{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"dt\"}");
        record.setBenefitsJson("[{\"category\":\"PLAN_SIMPLIFICATION\",\"estimatedImprovementPercent\":35,\"summary\":\"smaller plan\"}]");
        record.setCostsJson("[{\"category\":\"VALIDATION\",\"level\":\"MEDIUM\",\"summary\":\"verify output\"}]");
        record.setRisksJson("[{\"level\":\"MEDIUM\",\"category\":\"DATA_DRIFT\",\"summary\":\"refresh lag\",\"mitigation\":\"verify\"}]");
        record.setConfigSnapshotId("cfg-002");
        record.setResultId("result-002");
        record.setHistoryId("history-002");
        record.setReviewNote("approve");
        record.setApprovedBy("approver-001");
        record.setApprovedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 10));
        record.setRuntimeBindingJson("{\"runtimeStatus\":\"APPLIED\"}");
        record.setRuntimeBindingAt(LocalDateTime.of(2026, 4, 25, 5, 0, 20));
        record.setRuntimeBindingBy("operator-001");
        record.setVerificationEvidenceJson("{\"runtimeStatus\":\"VERIFIED\"}");
        record.setVerifiedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 30));
        record.setVerifiedBy("operator-001");
        record.setStatusHistoryJson("["
            + "{\"previousStatus\":null,\"currentStatus\":\"PENDING_APPROVAL\",\"occurredAt\":\"2026-04-25T05:00:00Z\",\"note\":\"PLAN_SUBMITTED\"},"
            + "{\"previousStatus\":\"PENDING_APPROVAL\",\"currentStatus\":\"APPROVED\",\"occurredAt\":\"2026-04-25T05:00:10Z\",\"note\":\"PLAN_APPROVED\"},"
            + "{\"previousStatus\":\"APPROVED\",\"currentStatus\":\"APPLIED\",\"occurredAt\":\"2026-04-25T05:00:20Z\",\"note\":\"PLAN_APPLIED\"},"
            + "{\"previousStatus\":\"APPLIED\",\"currentStatus\":\"VERIFIED\",\"occurredAt\":\"2026-04-25T05:00:30Z\",\"note\":\"PLAN_VERIFIED\"}"
            + "]");
        record.setCreatedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 4, 25, 5, 0, 30));
        when(mapper.selectByPlanId("plan-db-002")).thenReturn(record);

        MybatisAccelerationPlanRepository repository = new MybatisAccelerationPlanRepository(mapper);
        AccelerationPlan restored = repository.findByPlanId("plan-db-002");

        assertNotNull(restored);
        assertEquals(Instant.parse("2026-04-25T05:00:00Z"), restored.getCreatedAt());
        assertEquals(DataSourceTypeEnum.HETU, restored.getDatasourceType());
        assertEquals(AccelerationSuggestionType.PRECOMPUTE, restored.getSelectedSuggestionTypes().get(0));
        assertEquals("operator-001", restored.getVerifiedBy());
        assertEquals("PLAN_VERIFIED", restored.getStatusHistory().get(3).getNote());
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
            "approved plan",
            "apply governed runtime binding",
            "{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"dt\"}",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Instant.parse("2026-04-25T05:00:00Z")
        );
    }
}
