package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceAccelerationPlanTraceabilityApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldPersistAccelerationPlanTraceabilityChain() {
        GovernanceProtectedPersistenceService protectedPersistenceService = org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        ConfigSnapshotMapper configSnapshotMapper = org.mockito.Mockito.mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = org.mockito.Mockito.mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = org.mockito.Mockito.mock(QueryHistoryMapper.class);
        GovernanceAccelerationPlanTraceabilityApplicationService service =
            new GovernanceAccelerationPlanTraceabilityApplicationService(
                protectedPersistenceService,
                configSnapshotMapper,
                executionResultMapper,
                queryHistoryMapper
            );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(configSnapshotMapper.selectById("cfg-acceleration-plan-plan-001")).thenReturn(null);
        when(executionResultMapper.selectById("result-acceleration-plan-plan-001")).thenReturn(null);
        when(queryHistoryMapper.selectById("history-acceleration-plan-plan-001")).thenReturn(null);

        GovernanceAccelerationPlanTraceRequest request = new GovernanceAccelerationPlanTraceRequest();
        request.setPlanId("plan-001");
        request.setSourceTaskId("task-001");
        request.setSqlFingerprint("fp-001");
        request.setDatasourceType("HETU");
        request.setSqlText("SELECT * FROM orders");
        request.setPlanStatus("READY");
        request.setSnapshotPayloadJson("{\"selectedSuggestionTypes\":[\"PRECOMPUTE\"]}");
        request.setResultSummaryJson("{\"status\":\"READY\"}");
        request.setResultPayloadJson("{\"planPayload\":{\"PRECOMPUTE\":\"reason\"}}");
        request.setQueryContextJson("{\"sourceTaskId\":\"task-001\"}");
        request.setCreatedAt("2026-04-25T10:00:00Z");
        request.setUpdatedAt("2026-04-25T10:00:00Z");

        GovernanceAccelerationPlanTraceResponse response = service.writeAccelerationPlanTrace(request);

        assertEquals("cfg-acceleration-plan-plan-001", response.getConfigSnapshotId());
        assertEquals("result-acceleration-plan-plan-001", response.getResultId());
        assertEquals("history-acceleration-plan-plan-001", response.getHistoryId());
        assertEquals("ACCELERATION_PLAN_TRACEABILITY_BASELINE", response.getImplementationStage());

        ArgumentCaptor<ConfigSnapshotRecord> configCaptor = ArgumentCaptor.forClass(ConfigSnapshotRecord.class);
        verify(protectedPersistenceService).saveConfigSnapshot(configCaptor.capture());
        Map snapshotPayload = JsonUtils.fromJson(configCaptor.getValue().getSnapshotPayload(), Map.class);
        assertEquals(Arrays.asList("PRECOMPUTE"), snapshotPayload.get("selectedSuggestionTypes"));

        ArgumentCaptor<ExecutionResultRecord> resultCaptor = ArgumentCaptor.forClass(ExecutionResultRecord.class);
        verify(protectedPersistenceService).saveExecutionResult(resultCaptor.capture());
        assertEquals("ACCELERATION_PLAN", resultCaptor.getValue().getTaskType());
        Map resultSummary = JsonUtils.fromJson(resultCaptor.getValue().getResultSummary(), Map.class);
        assertEquals("READY", resultSummary.get("status"));

        ArgumentCaptor<QueryHistoryRecord> historyCaptor = ArgumentCaptor.forClass(QueryHistoryRecord.class);
        verify(protectedPersistenceService).saveQueryHistoryWithSqlText(historyCaptor.capture(), org.mockito.Mockito.eq("SELECT * FROM orders"));
        assertEquals("ACCELERATION_PLAN", historyCaptor.getValue().getHistoryType());
        assertNotNull(historyCaptor.getValue().getQueryContext());
    }

    @Test
    void shouldUpdateExecutionResultWhenAccelerationPlanTraceAlreadyExists() {
        GovernanceProtectedPersistenceService protectedPersistenceService = org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        ConfigSnapshotMapper configSnapshotMapper = org.mockito.Mockito.mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = org.mockito.Mockito.mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = org.mockito.Mockito.mock(QueryHistoryMapper.class);
        GovernanceAccelerationPlanTraceabilityApplicationService service =
            new GovernanceAccelerationPlanTraceabilityApplicationService(
                protectedPersistenceService,
                configSnapshotMapper,
                executionResultMapper,
                queryHistoryMapper
            );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-002", "trace-002", "header", 1L, 2L);
        when(configSnapshotMapper.selectById("cfg-acceleration-plan-plan-002")).thenReturn(new ConfigSnapshotRecord());
        when(executionResultMapper.selectById("result-acceleration-plan-plan-002")).thenReturn(new ExecutionResultRecord());
        when(queryHistoryMapper.selectById("history-acceleration-plan-plan-002")).thenReturn(new QueryHistoryRecord());

        GovernanceAccelerationPlanTraceRequest request = new GovernanceAccelerationPlanTraceRequest();
        request.setPlanId("plan-002");
        request.setSourceTaskId("task-002");
        request.setSqlFingerprint("fp-002");
        request.setDatasourceType("HETU");
        request.setSqlText("SELECT 1");
        request.setPlanStatus("ACTIVE");
        request.setSnapshotPayloadJson("{\"selectedSuggestionTypes\":[\"PARTITION\"]}");
        request.setResultSummaryJson("{\"status\":\"ACTIVE\"}");
        request.setResultPayloadJson("{\"activationEvidenceJson\":{\"runtimeStatus\":\"ACTIVE\"}}");
        request.setQueryContextJson("{\"sourceTaskId\":\"task-002\"}");
        request.setCreatedAt("2026-04-25T10:00:00Z");
        request.setUpdatedAt("2026-04-25T10:05:00Z");

        service.writeAccelerationPlanTrace(request);

        verify(protectedPersistenceService, times(0)).saveConfigSnapshot(any(ConfigSnapshotRecord.class));
        verify(protectedPersistenceService, times(1)).updateExecutionResult(any(ExecutionResultRecord.class));
        verify(protectedPersistenceService, times(0)).saveQueryHistoryWithSqlText(any(QueryHistoryRecord.class), any(String.class));
    }
}
