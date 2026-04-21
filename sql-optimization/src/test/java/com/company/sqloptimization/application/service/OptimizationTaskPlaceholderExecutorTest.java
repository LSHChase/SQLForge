package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.config.OptimizationTaskExecutionProperties;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.infrastructure.repository.InMemoryOptimizationTaskRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OptimizationTaskPlaceholderExecutorTest {

    @Test
    void shouldAdvanceQueuedTaskToSuccess() {
        InMemoryOptimizationTaskRepository repository = new InMemoryOptimizationTaskRepository();
        OptimizationTaskModelApplicationService modelService = new OptimizationTaskModelApplicationService();
        OptimizationTask task = modelService.createQueuedTask(baseRequest(), "task-async-001", Instant.now().minusSeconds(1L));
        repository.save(task);

        OptimizationTaskExecutionProperties properties = new OptimizationTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        OptimizationTaskPlaceholderExecutor executor = new OptimizationTaskPlaceholderExecutor(repository, properties);

        executor.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findByTaskId("task-async-001").getStatus().name());
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
