package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.context.RequestContext;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class BenchmarkTaskApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldLogSubmitLifecycleForQueuedSubmit(CapturedOutput output) {
        BenchmarkTaskApplicationService service = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            new InMemoryBenchmarkTaskRepository(),
            mockGovernanceClient()
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        service.submitTask(baseRequest("SELECT * FROM orders"));

        assertTrue(output.getOut().contains("operation=BENCHMARK_TASK_SUBMIT"));
        assertTrue(output.getOut().contains("status=START"));
        assertTrue(output.getOut().contains("from=REQUEST_ACCEPTED to=TASK_QUEUED"));
        assertTrue(output.getOut().contains("status=END resultStatus=QUEUED"));
    }

    @Test
    void shouldLogExceptionForMissingTaskStatusQuery(CapturedOutput output) {
        BenchmarkTaskApplicationService service = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            new InMemoryBenchmarkTaskRepository(),
            mockGovernanceClient()
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BizException ex = assertThrows(BizException.class, () -> service.getTaskStatus("missing-task"));

        assertEquals(23001, ex.getCode());
        assertTrue(output.getOut().contains("operation=BENCHMARK_TASK_STATUS_QUERY"));
        assertTrue(output.getOut().contains("status=FAILED phase=EXCEPTION"));
        assertTrue(output.getOut().contains("reason=Benchmark task does not exist"));
    }

    @Test
    void shouldCallGovernanceChecksAndAuditOnSubmitAndStatusQuery() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        BenchmarkTaskApplicationService service = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            new InMemoryBenchmarkTaskRepository(),
            governanceCapabilityClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        String taskId = service.submitTask(baseRequest("SELECT * FROM orders")).getTaskId();
        service.getTaskStatus(taskId);

        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).assertTenantScope("tenant-a");
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).writeAudit(any());
    }

    private BenchmarkTaskSubmitRequest baseRequest(String sqlText) {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText(sqlText);
        request.setTaskContext(new BenchmarkTaskContextDTO());
        return request;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertTenantScope(any());
        doNothing().when(governanceCapabilityClient).assertDatasourceAccess(any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }
}
