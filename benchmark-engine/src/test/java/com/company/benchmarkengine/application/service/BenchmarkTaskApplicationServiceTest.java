package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.sqlforge.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class BenchmarkTaskApplicationServiceTest {

    @Test
    void shouldLogSubmitLifecycleForPlaceholderSuccess(CapturedOutput output) {
        BenchmarkTaskApplicationService service = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            new InMemoryBenchmarkTaskRepository()
        );

        service.submitTask(baseRequest("SELECT * FROM orders"));

        assertTrue(output.getOut().contains("operation=BENCHMARK_TASK_SUBMIT"));
        assertTrue(output.getOut().contains("status=START"));
        assertTrue(output.getOut().contains("from=REQUEST_ACCEPTED to=TASK_QUEUED"));
        assertTrue(output.getOut().contains("to=PLACEHOLDER_SUCCEEDED"));
        assertTrue(output.getOut().contains("status=END resultStatus=SUCCEEDED"));
    }

    @Test
    void shouldLogExceptionForMissingTaskStatusQuery(CapturedOutput output) {
        BenchmarkTaskApplicationService service = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            new InMemoryBenchmarkTaskRepository()
        );

        BizException ex = assertThrows(BizException.class, () -> service.getTaskStatus("missing-task"));

        assertEquals(23001, ex.getCode());
        assertTrue(output.getOut().contains("operation=BENCHMARK_TASK_STATUS_QUERY"));
        assertTrue(output.getOut().contains("status=FAILED phase=EXCEPTION"));
        assertTrue(output.getOut().contains("reason=Benchmark task does not exist"));
    }

    private BenchmarkTaskSubmitRequest baseRequest(String sqlText) {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText(sqlText);
        request.setTaskContext(new BenchmarkTaskContextDTO());
        return request;
    }
}
