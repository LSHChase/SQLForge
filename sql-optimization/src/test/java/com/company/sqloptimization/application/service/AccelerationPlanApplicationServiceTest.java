package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.company.sqloptimization.application.controller.dto.AccelerationPlanApprovalRequest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AccelerationPlanApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldSubmitApproveApplyVerifyAndRollbackGovernedPlan() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        when(runtimeClient.apply(any())).thenReturn(runtimeResponse("APPLIED", true, "binding is active"));
        when(runtimeClient.verify(any())).thenReturn(runtimeResponse("VERIFIED", true, "binding verified"));
        when(runtimeClient.rollback(any())).thenReturn(runtimeResponse("ROLLED_BACK", false, "binding removed"));

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
        assertEquals("PENDING_APPROVAL", submitResponse.getStatus().name());
        assertEquals("ACCELERATION_PLAN_GOVERNANCE_BASELINE", submitResponse.getImplementationStage());

        AccelerationPlanApprovalRequest approvalRequest = new AccelerationPlanApprovalRequest();
        approvalRequest.setApprove(Boolean.TRUE);
        approvalRequest.setReviewNote("ready for runtime gating");
        AccelerationPlanStatusResponse approved = service.reviewPlan(submitResponse.getPlanId(), approvalRequest);
        assertEquals("APPROVED", approved.getStatus().name());
        assertEquals("operator-001", approved.getApprovedBy());
        assertEquals("cfg-plan-001", approved.getConfigSnapshotId());

        AccelerationPlanActionRequest actionRequest = new AccelerationPlanActionRequest();
        actionRequest.setReason("activate approved plan");
        AccelerationPlanStatusResponse applied = service.applyPlan(submitResponse.getPlanId(), actionRequest);
        assertEquals("APPLIED", applied.getStatus().name());
        assertEquals("operator-001", applied.getRuntimeBindingBy());

        AccelerationPlanStatusResponse verified = service.verifyPlan(submitResponse.getPlanId(), actionRequest);
        assertEquals("VERIFIED", verified.getStatus().name());
        assertEquals("operator-001", verified.getVerifiedBy());

        AccelerationPlanStatusResponse rolledBack = service.rollbackPlan(submitResponse.getPlanId(), actionRequest);
        assertEquals("ROLLED_BACK", rolledBack.getStatus().name());
        assertEquals("operator-001", rolledBack.getRolledBackBy());

        verify(governanceClient).assertAuthorization("tenant-a", DataSourceTypeEnum.HETU, "SQL_ACCELERATION_PLAN", submitResponse.getPlanId(), "ACCELERATION_PLAN_SUBMIT");
        verify(runtimeClient).apply(any());
        verify(runtimeClient).verify(any());
        verify(runtimeClient).rollback(any());
    }

    @Test
    void shouldRejectApplyBeforeApprovalWithoutCallingRuntime() {
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
        actionRequest.setReason("skip approval");

        BizException ex = assertThrows(
            BizException.class,
            () -> service.applyPlan(submitResponse.getPlanId(), actionRequest)
        );

        assertEquals(22006, ex.getCode());
        verify(runtimeClient, never()).apply(any());
    }

    @Test
    void shouldMarkVerifyFailedWhenRuntimeCannotConfirmBinding() {
        GovernanceCapabilityClient governanceClient = mockGovernanceClient();
        QueryExecutionAccelerationPlanClient runtimeClient = mock(QueryExecutionAccelerationPlanClient.class);
        when(runtimeClient.apply(any())).thenReturn(runtimeResponse("APPLIED", true, "binding is active"));
        when(runtimeClient.verify(any())).thenReturn(runtimeResponse("MISSING", false, "binding missing"));

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
        AccelerationPlanApprovalRequest approvalRequest = new AccelerationPlanApprovalRequest();
        approvalRequest.setApprove(Boolean.TRUE);
        approvalRequest.setReviewNote("approve for verify-failure test");
        service.reviewPlan(submitResponse.getPlanId(), approvalRequest);
        service.applyPlan(submitResponse.getPlanId(), null);

        AccelerationPlanStatusResponse verifyResponse = service.verifyPlan(submitResponse.getPlanId(), null);

        assertEquals("VERIFY_FAILED", verifyResponse.getStatus().name());
        assertEquals(Integer.valueOf(13008), verifyResponse.getLastErrorCode());
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceClient = mock(GovernanceCapabilityClient.class);
        GovernanceAccelerationPlanTraceResponse traceResponse = new GovernanceAccelerationPlanTraceResponse();
        traceResponse.setConfigSnapshotId("cfg-plan-001");
        traceResponse.setResultId("result-plan-001");
        traceResponse.setHistoryId("history-plan-001");
        doNothing().when(governanceClient).assertAuthorization(any(), any(), any(), any(), any());
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
        response.setImplementationStage("APPROVED_ACCELERATION_RUNTIME_BASELINE");
        return response;
    }

    private OptimizationTask succeededAccelerationTask(String taskId) {
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
                Collections.singletonList(
                    new OptimizationTaskArtifact(
                        "ACCELERATION_PLAN",
                        "recommendedPlan",
                        "{\"PRECOMPUTE\":\"mv_orders\",\"PARTITION\":\"order_date\"}"
                    )
                ),
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

    private void setRequestContext(String tenantId) {
        RequestContext.set(
            tenantId,
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }
}
