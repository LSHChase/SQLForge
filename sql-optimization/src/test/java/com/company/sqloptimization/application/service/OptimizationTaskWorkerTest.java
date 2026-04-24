package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.config.OptimizationTaskExecutionProperties;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.infrastructure.repository.InMemoryOptimizationTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OptimizationTaskWorkerTest {

    @Test
    void shouldAdvanceQueuedTaskToSuccess() {
        InMemoryOptimizationTaskRepository repository = new InMemoryOptimizationTaskRepository();
        OptimizationTaskModelApplicationService modelService = new OptimizationTaskModelApplicationService();
        OptimizationTask task = modelService.createQueuedTask(baseRequest(), "task-async-001", Instant.now().minusSeconds(1L));
        repository.save(task);

        OptimizationTaskExecutionProperties properties = new OptimizationTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OptimizationTaskWorker worker = new OptimizationTaskWorker(
            repository,
            properties,
            new OptimizationMetricsRecorder(meterRegistry)
        );

        worker.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findByTaskId("task-async-001").getStatus().name());
        assertEquals(1.0D, meterRegistry.get("sqlforge.sql.optimization.tasks.terminal").tags(
            "task_type", "REWRITE",
            "result_status", "SUCCEEDED"
        ).counter().count());
    }

    private OptimizationTaskSubmitRequest baseRequest() {
        OptimizationTaskSubmitRequest request = new OptimizationTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(com.company.sqloptimization.domain.task.OptimizationTaskType.REWRITE);
        request.setSqlText("SELECT * FROM orders");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setTaskContext(new OptimizationTaskContextDTO());
        return request;
    }
}
