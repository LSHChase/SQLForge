package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqloptimization.application.controller.dto.AccelerationPlanActionRequest;
import com.company.sqloptimization.application.controller.dto.AccelerationPlanSubmitRequest;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanStatusResponse;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanSubmitResponse;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionAccelerationPlanClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationPlanRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryOptimizationTaskRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AccelerationPlanApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldSubmitActivateAndPauseGovernedPlan() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        when(runtimeClient.activate(any())).thenReturn(runtimeResponse("ACTIVE", true, "binding is active"));
        when(runtimeClient.pause(any())).thenReturn(runtimeResponse("PAUSED", false, "binding paused"));

        InMemoryOptimizationTaskRepository taskRepository = new InMemoryOptimizationTaskRepository();
        OptimizationTask sourceTask = succeededAccelerationTask("task-001");
        taskRepository.save(sourceTask);

        AccelerationPlanApplicationService service = new AccelerationPlanApplicationService(
            new InMemoryAccelerationPlanRepository(),
            taskRepository,
            governanceClient,
            runtimeClient,
            new AccelerationPlanModelApplicationService()
        );
        setRequestContext("tenant-a");

        AccelerationPlanSubmitResponse submitResponse = service.submitPlan(submitRequest(sourceTask.getTaskId()));
        assertNotNull(submitResponse.getPlanId());
        assertEquals("READY", submitResponse.getStatus().name());
        assertEquals("ACCELERATION_PLAN_GOVERNANCE_BASELINE", submitResponse.getImplementationStage());

        AccelerationPlanActionRequest actionRequest = new AccelerationPlanActionRequest();
        actionRequest.setReason("activate ready plan");
        AccelerationPlanStatusResponse activated = service.activatePlan(submitResponse.getPlanId(), actionRequest);
        assertEquals("ACTIVE", activated.getStatus().name());
        assertEquals("user-001", activated.getActivatedBy());
        assertEquals("cfg-plan-001", activated.getConfigSnapshotId());
        assertTrue(activated.getActivationEvidenceJson().contains("\"runtimeStatus\":\"ACTIVE\""));

        AccelerationPlanStatusResponse paused = service.pausePlan(submitResponse.getPlanId(), actionRequest);
        assertEquals("PAUSED", paused.getStatus().name());
        assertEquals("user-001", paused.getPausedBy());
        assertTrue(paused.getPauseEvidenceJson().contains("\"runtimeStatus\":\"PAUSED\""));

        verify(governanceClient).assertDatasourceAccess("tenant-a", DataSourceTypeEnum.HETU, "SQL_ACCELERATION_PLAN", submitResponse.getPlanId(), "ACCELERATION_PLAN_SUBMIT");
        verify(runtimeClient).activate(any());
        verify(runtimeClient).pause(any());
    }

    @Test
    void shouldRejectPauseBeforeActivationWithoutCallingRuntime() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        InMemoryOptimizationTaskRepository taskRepository = new InMemoryOptimizationTaskRepository();
        OptimizationTask sourceTask = succeededAccelerationTask("task-002");
        taskRepository.save(sourceTask);

        AccelerationPlanApplicationService service = new AccelerationPlanApplicationService(
            new InMemoryAccelerationPlanRepository(),
            taskRepository,
            governanceClient,
            runtimeClient,
            new AccelerationPlanModelApplicationService()
        );
        setRequestContext("tenant-a");

        AccelerationPlanSubmitResponse submitResponse = service.submitPlan(submitRequest(sourceTask.getTaskId()));
        AccelerationPlanActionRequest actionRequest = new AccelerationPlanActionRequest();
        actionRequest.setReason("pause before activation");

        BizException ex = assertThrows(
            BizException.class,
            () -> service.pausePlan(submitResponse.getPlanId(), actionRequest)
        );

        assertEquals(22006, ex.getCode());
        verify(runtimeClient, never()).pause(any());
    }

    @Test
    void shouldNotCopyExactQueryMvArtifactIntoPlanPayload() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        InMemoryOptimizationTaskRepository taskRepository = new InMemoryOptimizationTaskRepository();
        OptimizationTask sourceTask = succeededAccelerationTask(
            "task-exact-artifact",
            Arrays.asList(
                new OptimizationTaskArtifact(
                    "ACCELERATION_PLAN",
                    "recommendedPlan",
                    "{\"PRECOMPUTE\":\"mv_orders\"}"
                ),
                new OptimizationTaskArtifact(
                    "ACCELERATION_ARTIFACT",
                    "accelerationArtifact",
                    "{\"mvType\":\"EXACT_QUERY_MV\",\"artifactStatus\":\"GENERATED\",\"rewriteSql\":\"SELECT * FROM mv_exact\"}"
                )
            )
        );
        taskRepository.save(sourceTask);
        AccelerationPlanApplicationService service = new AccelerationPlanApplicationService(
            new InMemoryAccelerationPlanRepository(),
            taskRepository,
            governanceClient,
            runtimeClient,
            new AccelerationPlanModelApplicationService()
        );
        setRequestContext("tenant-a");

        AccelerationPlanSubmitResponse submitResponse = service.submitPlan(precomputeSubmitRequest(sourceTask.getTaskId()));
        AccelerationPlanStatusResponse status = service.getPlanStatus(submitResponse.getPlanId());

        assertFalse(status.getPlanPayloadJson().contains("EXACT_QUERY_MV"));
        assertFalse(status.getPlanPayloadJson().contains("accelerationArtifact"));
    }

    @Test
    void shouldCopyAllowedAdvancedMvArtifactIntoPlanPayload() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        InMemoryOptimizationTaskRepository taskRepository = new InMemoryOptimizationTaskRepository();
        OptimizationTask sourceTask = succeededAccelerationTask(
            "task-amv-artifact",
            Arrays.asList(
                new OptimizationTaskArtifact(
                    "ACCELERATION_PLAN",
                    "recommendedPlan",
                    "{\"PRECOMPUTE\":\"mv_orders\"}"
                ),
                new OptimizationTaskArtifact(
                    "ACCELERATION_ARTIFACT",
                    "accelerationArtifact",
                    "{\"mvType\":\"PREJOIN_MV\",\"artifactStatus\":\"REVIEW_REQUIRED\",\"grain\":[\"customer_id\"],\"blockingReasons\":[],\"reviewWarnings\":[{\"code\":\"ROW_AMPLIFICATION_METADATA_MISSING\"}],\"rewriteSql\":\"SELECT customer_id FROM mv_prejoin\"}"
                )
            )
        );
        taskRepository.save(sourceTask);
        AccelerationPlanApplicationService service = new AccelerationPlanApplicationService(
            new InMemoryAccelerationPlanRepository(),
            taskRepository,
            governanceClient,
            runtimeClient,
            new AccelerationPlanModelApplicationService()
        );
        setRequestContext("tenant-a");

        AccelerationPlanSubmitResponse submitResponse = service.submitPlan(precomputeSubmitRequest(sourceTask.getTaskId()));
        AccelerationPlanStatusResponse status = service.getPlanStatus(submitResponse.getPlanId());

        assertFalse(status.getPlanPayloadJson().contains("EXACT_QUERY_MV"));
        assertTrue(status.getPlanPayloadJson().contains("PREJOIN_MV"));
        assertTrue(status.getPlanPayloadJson().contains("REVIEW_REQUIRED"));
        assertTrue(status.getPlanPayloadJson().contains("ROW_AMPLIFICATION_METADATA_MISSING"));
        assertTrue(status.getPlanPayloadJson().contains("accelerationArtifact"));
    }

    @Test
    void shouldMarkActivateFailedWhenRuntimeCannotConfirmBinding() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        when(runtimeClient.activate(any())).thenReturn(runtimeResponse("MISSING", false, "binding missing"));

        InMemoryOptimizationTaskRepository taskRepository = new InMemoryOptimizationTaskRepository();
        OptimizationTask sourceTask = succeededAccelerationTask("task-003");
        taskRepository.save(sourceTask);

        AccelerationPlanApplicationService service = new AccelerationPlanApplicationService(
            new InMemoryAccelerationPlanRepository(),
            taskRepository,
            governanceClient,
            runtimeClient,
            new AccelerationPlanModelApplicationService()
        );
        setRequestContext("tenant-a");

        AccelerationPlanSubmitResponse submitResponse = service.submitPlan(submitRequest(sourceTask.getTaskId()));

        BizException ex = assertThrows(BizException.class, () -> service.activatePlan(submitResponse.getPlanId(), null));
        AccelerationPlanStatusResponse activateResponse = service.getPlanStatus(submitResponse.getPlanId());

        assertEquals(13007, ex.getCode());
        assertEquals("ACTIVATE_FAILED", activateResponse.getStatus().name());
        assertEquals(Integer.valueOf(13007), activateResponse.getLastErrorCode());
        assertTrue(activateResponse.getActivationEvidenceJson().contains("\"runtimeStatus\":\"MISSING\""));
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceClient = mock(GovernanceCapabilityClient.class);
        GovernanceAccelerationPlanTraceResponse traceResponse = new GovernanceAccelerationPlanTraceResponse();
        traceResponse.setConfigSnapshotId("cfg-plan-001");
        traceResponse.setResultId("result-plan-001");
        traceResponse.setHistoryId("history-plan-001");
        doNothing().when(governanceClient).assertDatasourceAccess(any(), any(), any(), any(), any());
        doNothing().when(governanceClient).writeAudit(any());
        when(governanceClient.writeAccelerationPlanTrace(any())).thenReturn(traceResponse);
        return governanceClient;
    }

    private QueryExecutionAccelerationPlanResponse runtimeResponse(String status, boolean active, String summary) {
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId("tenant-a");
        response.setPlanId("plan-runtime");
        response.setSqlFingerprint("fp-001");
        response.setTargetEngine("HETU");
        response.setActive(active);
        response.setStatus(status);
        response.setRuntimeSummary(summary);
        response.setRuntimeDetailsJson("{\"bindingState\":\"" + status + "\"}");
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("ACTIVE_ACCELERATION_RUNTIME_BASELINE");
        return response;
    }

    private OptimizationTask succeededAccelerationTask(String taskId) {
        return succeededAccelerationTask(
            taskId,
            Collections.singletonList(
                new OptimizationTaskArtifact(
                    "ACCELERATION_PLAN",
                    "recommendedPlan",
                    "{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"order_date\"}"
                )
            )
        );
    }

    private OptimizationTask succeededAccelerationTask(String taskId, List<OptimizationTaskArtifact> artifacts) {
        OptimizationTask task = new OptimizationTaskModelApplicationService().createQueuedTask(
            optimizationTaskSubmitRequest(),
            taskId,
            Instant.parse("2026-04-25T00:00:00Z")
        );
        task.markRunning(Instant.parse("2026-04-25T00:00:10Z"));
        task.advancePhase(OptimizationTaskPhase.COST_ESTIMATING, 45, "COST_MODEL_READY");
        task.advancePhase(OptimizationTaskPhase.ACCELERATION_PLANNING, 70, "PLAN_OPTIONS_PREPARED");
        task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 90, "PLAN_ARTIFACTS_READY");
        task.markSucceeded(
            new OptimizationTaskSuggestion(
                "governed acceleration is ready",
                "submit a governed plan",
                Integer.valueOf(87),
                artifacts,
                Collections.singletonList(
                    new OptimizationTaskBenefit("PLAN_SIMPLIFICATION", Integer.valueOf(35), "fewer scan paths")
                ),
                Collections.singletonList(
                    new OptimizationTaskCost("VALIDATION", "MEDIUM", "verify plan correctness")
                ),
                Collections.singletonList(
                    new OptimizationTaskRisk("MEDIUM", "DATA_DRIFT", "freshness depends on update lag", "verify after apply")
                )
            ),
            Instant.parse("2026-04-25T00:00:20Z")
        );
        return task;
    }

    private OptimizationTaskSubmitRequest optimizationTaskSubmitRequest() {
        OptimizationTaskSubmitRequest request = new OptimizationTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(OptimizationTaskType.ACCELERATION_SUGGESTION);
        request.setSqlText("SELECT * FROM orders");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setTaskContext(new OptimizationTaskContextDTO());
        return request;
    }

    private AccelerationPlanSubmitRequest submitRequest(String sourceTaskId) {
        AccelerationPlanSubmitRequest request = new AccelerationPlanSubmitRequest();
        request.setTenantId("tenant-a");
        request.setSourceTaskId(sourceTaskId);
        request.setSelectedSuggestionTypes(Arrays.asList(AccelerationSuggestionType.PRECOMPUTE, AccelerationSuggestionType.PARTITION));
        return request;
    }

    private AccelerationPlanSubmitRequest precomputeSubmitRequest(String sourceTaskId) {
        AccelerationPlanSubmitRequest request = new AccelerationPlanSubmitRequest();
        request.setTenantId("tenant-a");
        request.setSourceTaskId(sourceTaskId);
        request.setSelectedSuggestionTypes(Collections.singletonList(AccelerationSuggestionType.PRECOMPUTE));
        return request;
    }

    private void setRequestContext(String tenantId) {
        RequestContext.set(
            tenantId,
            "user-001",
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }
}
