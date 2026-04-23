package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryOptimizationTaskRepository;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class OptimizationTaskApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldLogSubmitLifecycleForQueuedSubmit(CapturedOutput output) {
        OptimizationTaskApplicationService service = new OptimizationTaskApplicationService(
            new OptimizationTaskModelApplicationService(),
            new InMemoryOptimizationTaskRepository(),
            mockGovernanceClient()
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        OptimizationTaskSubmitResponse response = service.submitTask(baseRequest("SELECT * FROM orders"));

        assertEquals("QUEUED", response.getStatus().name());
        assertTrue(output.getOut().contains("operation=OPTIMIZATION_TASK_SUBMIT"));
        assertTrue(output.getOut().contains("status=START"));
        assertTrue(output.getOut().contains("from=REQUEST_ACCEPTED to=TASK_QUEUED"));
        assertTrue(output.getOut().contains("status=END resultStatus=QUEUED"));
    }

    @Test
    void shouldLogExceptionForMissingTaskStatusQuery(CapturedOutput output) {
        OptimizationTaskApplicationService service = new OptimizationTaskApplicationService(
            new OptimizationTaskModelApplicationService(),
            new InMemoryOptimizationTaskRepository(),
            mockGovernanceClient()
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BizException ex = assertThrows(BizException.class, () -> service.getTaskStatus("missing-task"));

        assertEquals(22001, ex.getCode());
        assertTrue(output.getOut().contains("operation=OPTIMIZATION_TASK_STATUS_QUERY"));
        assertTrue(output.getOut().contains("status=FAILED phase=EXCEPTION"));
        assertTrue(output.getOut().contains("reason=Optimization task does not exist"));
    }

    @Test
    void shouldCallGovernanceChecksAndAuditOnSubmitAndStatusQuery() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        OptimizationTaskApplicationService service = new OptimizationTaskApplicationService(
            new OptimizationTaskModelApplicationService(),
            new InMemoryOptimizationTaskRepository(),
            governanceCapabilityClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        OptimizationTaskSubmitResponse response = service.submitTask(baseRequest("SELECT * FROM orders"));
        service.getTaskStatus(response.getTaskId());

        verify(governanceCapabilityClient).assertAuthorization(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.eq(DataSourceTypeEnum.HETU),
            org.mockito.Mockito.eq("SQL_OPTIMIZATION_TASK"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.Mockito.eq("OPTIMIZATION_TASK_SUBMIT")
        );
        verify(governanceCapabilityClient).assertAuthorization(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.eq(DataSourceTypeEnum.HETU),
            org.mockito.Mockito.eq("SQL_OPTIMIZATION_TASK"),
            org.mockito.Mockito.eq(response.getTaskId()),
            org.mockito.Mockito.eq("OPTIMIZATION_TASK_STATUS_QUERY")
        );
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).writeAudit(any());
    }

    private OptimizationTaskSubmitRequest baseRequest(String sqlText) {
        OptimizationTaskSubmitRequest request = new OptimizationTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(com.company.sqloptimization.domain.task.OptimizationTaskType.REWRITE);
        request.setSqlText(sqlText);
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setTaskContext(new OptimizationTaskContextDTO());
        return request;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }
}
