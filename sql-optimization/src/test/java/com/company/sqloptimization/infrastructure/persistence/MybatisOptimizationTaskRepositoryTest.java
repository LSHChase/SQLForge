package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.service.OptimizationTaskModelApplicationService;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.infrastructure.persistence.entity.OptimizationTaskRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.OptimizationTaskMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisOptimizationTaskRepositoryTest {

    @Test
    void shouldConvertInstantTimestampsToLocalDateTimeWhenSaving() {
        OptimizationTaskMapper mapper = org.mockito.Mockito.mock(OptimizationTaskMapper.class);
        when(mapper.selectByTaskId(anyString())).thenReturn(null);
        MybatisOptimizationTaskRepository repository = new MybatisOptimizationTaskRepository(mapper);

        OptimizationTask task = new OptimizationTaskModelApplicationService().createQueuedTask(
            baseRequest(),
            "task-db-001",
            Instant.parse("2026-04-22T05:00:00Z")
        );

        repository.save(task);

        ArgumentCaptor<OptimizationTaskRecord> captor = ArgumentCaptor.forClass(OptimizationTaskRecord.class);
        verify(mapper).insert(captor.capture());
        OptimizationTaskRecord record = captor.getValue();
        assertEquals(LocalDateTime.of(2026, 4, 22, 5, 0, 0), record.getSubmittedAt());
        assertNotNull(record.getStatusHistoryJson());
        assertTrue(record.getStatusHistoryJson().contains("TASK_SUBMITTED"));
    }

    @Test
    void shouldRestoreInstantTimestampsFromDatabaseRecord() {
        OptimizationTaskMapper mapper = org.mockito.Mockito.mock(OptimizationTaskMapper.class);
        OptimizationTaskRecord record = new OptimizationTaskRecord();
        record.setTaskId("task-db-002");
        record.setTenantId("tenant-a");
        record.setTaskType("REWRITE");
        record.setSqlText("SELECT * FROM orders");
        record.setSqlFingerprint("abc123");
        record.setDatasourceType("HETU");
        record.setPriority("NORMAL");
        record.setParseDepth("LIGHT");
        record.setRequestedSuggestionTypesJson("[]");
        record.setStatus("QUEUED");
        record.setCurrentPhase("SUBMITTED");
        record.setProgressPercent(Integer.valueOf(5));
        record.setStatusHistoryJson("[{\"previousStatus\":null,\"currentStatus\":\"QUEUED\",\"previousPhase\":null,"
            + "\"currentPhase\":\"SUBMITTED\",\"occurredAt\":\"2026-04-22T05:00:00Z\",\"note\":\"TASK_SUBMITTED\"}]");
        record.setSubmittedAt(LocalDateTime.of(2026, 4, 22, 5, 0, 0));
        when(mapper.selectByTaskId("task-db-002")).thenReturn(record);

        MybatisOptimizationTaskRepository repository = new MybatisOptimizationTaskRepository(mapper);
        OptimizationTask restored = repository.findByTaskId("task-db-002");

        assertNotNull(restored);
        assertEquals(Instant.parse("2026-04-22T05:00:00Z"), restored.getSubmittedAt());
        assertEquals(Instant.parse("2026-04-22T05:00:00Z"), restored.getStatusHistory().get(0).getOccurredAt());
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
