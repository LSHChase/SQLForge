package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.config.OptimizationTaskExecutionProperties;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryOptimizationTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
            new OptimizationMetricsRecorder(meterRegistry),
            new SqlOptimizationPipelineService()
        );

        worker.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findByTaskId("task-async-001").getStatus().name());
        assertEquals("REWRITTEN_SQL", repository.findByTaskId("task-async-001").getSuggestion().getArtifacts().get(0).getCategory());
        assertEquals(1.0D, meterRegistry.get("sqlforge.sql.optimization.tasks.terminal").tags(
            "task_type", "REWRITE",
            "result_status", "SUCCEEDED"
        ).counter().count());
    }

    @Test
    void shouldPersistParseTriggeredRewriteRecommendationAfterSuccess() {
        InMemoryOptimizationTaskRepository taskRepository = new InMemoryOptimizationTaskRepository();
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        OptimizationTaskModelApplicationService modelService = new OptimizationTaskModelApplicationService();
        OptimizationTaskSubmitRequest request = baseRequest();
        OptimizationTaskContextDTO context = new OptimizationTaskContextDTO();
        context.setSourceType("PARSE_BATCH");
        context.setSourceId("item-001");
        context.setBatchId("batch-001");
        context.setReportCode("RPT_SELECT_STAR");
        context.setHistoryId("parse-history-001");
        context.setParseTaskId("parse-task-001");
        context.setDatasourceCode("hetu_main");
        context.setIssueScenes(Arrays.asList("SELECT_STAR"));
        context.setPriority(OptimizationTaskPriority.NORMAL);
        request.setTaskContext(context);
        OptimizationTask task = modelService.createQueuedTask(request, "task-parse-rewrite-001", Instant.now().minusSeconds(1L));
        taskRepository.save(task);

        OptimizationTaskExecutionProperties properties = new OptimizationTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        ParseTriggeredRewriteRecommendationService recommendationService =
            new ParseTriggeredRewriteRecommendationService(null, recommendationRepository, new SqlOptimizationPipelineService());
        OptimizationTaskWorker worker = new OptimizationTaskWorker(
            taskRepository,
            properties,
            new OptimizationMetricsRecorder(new SimpleMeterRegistry()),
            new SqlOptimizationPipelineService(),
            recommendationService
        );

        worker.processQueuedTasks();
        recommendationService.persistRecommendationFromSucceededRewriteTask(taskRepository.findByTaskId("task-parse-rewrite-001"));

        List<AccelerationRecommendation> recommendations = recommendationRepository.findByTenantId("tenant-a");
        assertEquals(1, recommendations.size());
        AccelerationRecommendation recommendation = recommendations.get(0);
        assertEquals("REWRITE", recommendation.getRecommendationType().name());
        assertEquals("SELECT * FROM orders", recommendation.getSourceSqlText());
        assertEquals("SELECT * FROM orders", recommendation.getRecommendedSqlText());
        assertEquals("parse-history-001", recommendation.getHistoryId());
        assertEquals("parse-task-001", recommendation.getParseTaskId());
        assertEquals("batch-001", recommendation.getBatchId());
        assertEquals("RPT_SELECT_STAR", recommendation.getReportCode());
        assertEquals("PARSE", recommendation.getSourceType().name());
        assertEquals("PARSE_BATCH", recommendation.getSourceKind().name());
        assertEquals("STATIC_PARSE", recommendation.getEvidenceLevel().name());
        assertEquals("SQL_RECOMMENDATION_RULE_MODEL_V1", recommendation.getSchemaVersion());
        assertEquals("SELECT_STAR_EXPANSION", recommendation.getUnappliedRules().get(0).get("rule"));
        assertEquals("NOT_REAL_EXECUTION_GAIN", recommendation.getExpectedBenefit().get("claimBoundary"));
        assertEquals("NOT_VALIDATED", recommendation.getValidationStatus().name());
        assertEquals(false, recommendation.isAutoApplyAllowed());
        assertEquals(true, recommendation.isManualReviewRequired());
    }

    private OptimizationTaskSubmitRequest baseRequest() {
        OptimizationTaskSubmitRequest request = new OptimizationTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(OptimizationTaskType.REWRITE);
        request.setSqlText("SELECT * FROM orders");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setTaskContext(new OptimizationTaskContextDTO());
        return request;
    }
}
