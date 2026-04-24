package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BenchmarkTaskWorkerTest {

    @Test
    void shouldAdvanceQueuedTaskAndGenerateReport() {
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = modelService.createQueuedTask(baseRequest(), "benchmark-task-async-001", Instant.now().minusSeconds(1L));
        repository.saveTask(task);

        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BenchmarkTaskWorker worker = new BenchmarkTaskWorker(
            modelService,
            repository,
            properties,
            new BenchmarkMetricsRecorder(meterRegistry)
        );

        worker.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findTaskByTaskId("benchmark-task-async-001").getStatus().name());
        assertEquals("report-benchmark-task-async-001", repository.findTaskByTaskId("benchmark-task-async-001").getReportId());
        assertEquals("tenant-a", repository.findReportByTaskId("benchmark-task-async-001").getTenantId());
        assertEquals(1.0D, meterRegistry.get("sqlforge.benchmark.engine.tasks.terminal").tags(
            "task_type", "BASELINE",
            "result_status", "SUCCEEDED"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.benchmark.engine.reports.generated").tags(
            "task_type", "BASELINE"
        ).counter().count());
    }

    private BenchmarkTaskSubmitRequest baseRequest() {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        return request;
    }
}
