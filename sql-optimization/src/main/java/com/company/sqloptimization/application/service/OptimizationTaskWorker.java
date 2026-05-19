package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqloptimization.config.OptimizationTaskExecutionProperties;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OptimizationTaskWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationTaskWorker.class);

    private static final String OPERATION = "OPTIMIZATION_TASK_WORKER";
    private static final String STATE_TASK_QUEUED = "TASK_QUEUED";
    private static final String STATE_WORKER_RUNNING = "WORKER_RUNNING";
    private static final String STATE_WORKER_SUCCEEDED = "WORKER_SUCCEEDED";
    private static final String STATE_WORKER_FAILED = "WORKER_FAILED";
    private static final String FAILURE_MARKER = "FAIL_OPTIMIZATION";

    private final OptimizationTaskRepository optimizationTaskRepository;
    private final OptimizationTaskExecutionProperties executionProperties;
    private final OptimizationMetricsRecorder optimizationMetricsRecorder;
    private final SqlOptimizationPipelineService sqlOptimizationPipelineService;
    private final ParseTriggeredRewriteRecommendationService parseTriggeredRewriteRecommendationService;

    public OptimizationTaskWorker(OptimizationTaskRepository optimizationTaskRepository,
                                  OptimizationTaskExecutionProperties executionProperties,
                                  OptimizationMetricsRecorder optimizationMetricsRecorder,
                                  SqlOptimizationPipelineService sqlOptimizationPipelineService) {
        this(
            optimizationTaskRepository,
            executionProperties,
            optimizationMetricsRecorder,
            sqlOptimizationPipelineService,
            null
        );
    }

    @Autowired
    public OptimizationTaskWorker(OptimizationTaskRepository optimizationTaskRepository,
                                  OptimizationTaskExecutionProperties executionProperties,
                                  OptimizationMetricsRecorder optimizationMetricsRecorder,
                                  SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                  ParseTriggeredRewriteRecommendationService parseTriggeredRewriteRecommendationService) {
        this.optimizationTaskRepository = optimizationTaskRepository;
        this.executionProperties = executionProperties;
        this.optimizationMetricsRecorder = optimizationMetricsRecorder;
        this.sqlOptimizationPipelineService = sqlOptimizationPipelineService;
        this.parseTriggeredRewriteRecommendationService = parseTriggeredRewriteRecommendationService;
    }

    @Scheduled(fixedDelayString = "${sql-optimization.task-execution.poll-interval-ms:25}")
    public void processQueuedTasks() {
        Instant visibleBefore = Instant.now().minusMillis(executionProperties.getQueueVisibilityDelayMs());
        List<OptimizationTask> queuedTasks = optimizationTaskRepository.findQueuedTasksSubmittedBefore(visibleBefore);
        for (OptimizationTask task : queuedTasks) {
            processTask(task);
        }
    }

    private void processTask(OptimizationTask task) {
        long start = System.currentTimeMillis();
        try {
            task.markRunning(Instant.now());
            optimizationTaskRepository.save(task);
            logStateChange(task, STATE_TASK_QUEUED, STATE_WORKER_RUNNING, task.getCurrentPhase().name());
            if (shouldForceFailure(task)) {
                delay();
                task.markFailed(
                    new OptimizationTaskError(
                        ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_PIPELINE_NOT_READY,
                        "SQL 优化 worker 在生成建议载荷前失败",
                        "请检查数据库支撑的 worker 流水线，并在载体恢复健康后重试。",
                        true,
                        task.getCurrentPhase(),
                        Collections.singletonList(
                            new OptimizationTaskRisk(
                                "MEDIUM",
                                "PIPELINE_READINESS",
                                "由于 worker 在真实执行前被强制进入失败路径，建议输出不可用。",
                                "请检查 worker 载体，并仅在异步流水线恢复健康后重试。"
                            )
                        )
                    ),
                    Instant.now()
                );
                optimizationTaskRepository.save(task);
                logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_FAILED, task.getError().getMessage());
                optimizationMetricsRecorder.recordWorkerTerminal(task, System.currentTimeMillis() - start);
                logEnd(task, start);
                return;
            }
            OptimizationTaskSuggestion suggestion = executeTask(task);
            delay();
            task.markSucceeded(suggestion, Instant.now());
            optimizationTaskRepository.save(task);
            persistRewriteRecommendation(task);
            logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_SUCCEEDED, task.getSummary());
            optimizationMetricsRecorder.recordWorkerTerminal(task, System.currentTimeMillis() - start);
            logEnd(task, start);
        } catch (SqlOptimizationPipelineService.SqlOptimizationExecutionException ex) {
            task.markFailed(
                new OptimizationTaskError(
                    ex.getCode(),
                    ex.getMessage(),
                    ex.getSuggestedAction(),
                    ex.isRetryable(),
                    ex.getFailedPhase(),
                    ex.getRisks()
                ),
                Instant.now()
            );
            optimizationTaskRepository.save(task);
            logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_FAILED, ex.getMessage());
            optimizationMetricsRecorder.recordWorkerTerminal(task, System.currentTimeMillis() - start);
            logEnd(task, start);
        } catch (RuntimeException ex) {
            optimizationMetricsRecorder.recordWorkerException(task, System.currentTimeMillis() - start);
            LOGGER.error("操作日志 operation={} entity={} tenantId={} costMs={} status=FAILED phase=EXCEPTION reason={}",
                OPERATION,
                task.getTaskId(),
                task.getTenantId(),
                System.currentTimeMillis() - start,
                ex.getMessage(),
                ex);
            throw ex;
        }
    }

    private OptimizationTaskSuggestion executeTask(OptimizationTask task) {
        SqlOptimizationPipelineService.ParsedSqlProfile parsedSqlProfile =
            sqlOptimizationPipelineService.analyze(task.getSqlText(), task.getDatasourceType());
        if (task.getTaskType() == OptimizationTaskType.PARSE) {
            delay();
            task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "WORKER_PARSE_ARTIFACTS_READY");
            optimizationTaskRepository.save(task);
            return sqlOptimizationPipelineService.buildParseSuggestion(parsedSqlProfile);
        }
        if (task.getTaskType() == OptimizationTaskType.REWRITE) {
            delay();
            task.advancePhase(OptimizationTaskPhase.SQL_REWRITING, 45, "WORKER_REWRITE_RULES_APPLIED");
            optimizationTaskRepository.save(task);
            OptimizationTaskSuggestion rewriteSuggestion =
                sqlOptimizationPipelineService.buildRewriteSuggestion(parsedSqlProfile);
            delay();
            task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "WORKER_REWRITE_ARTIFACTS_READY");
            optimizationTaskRepository.save(task);
            return rewriteSuggestion;
        }
        delay();
        task.advancePhase(OptimizationTaskPhase.COST_ESTIMATING, 35, "WORKER_COST_SIGNALS_READY");
        optimizationTaskRepository.save(task);
        delay();
        task.advancePhase(OptimizationTaskPhase.ACCELERATION_PLANNING, 70, "WORKER_ACCELERATION_PLAN_READY");
        optimizationTaskRepository.save(task);
        OptimizationTaskSuggestion accelerationSuggestion =
            sqlOptimizationPipelineService.buildAccelerationSuggestion(
                parsedSqlProfile,
                task.getRequestedSuggestionTypes(),
                task.getDatasourceType(),
                task.getSourceContext() == null ? null : task.getSourceContext().getDatasourceCode(),
                task.getSqlFingerprint(),
                task.getSourceContext() == null ? null : task.getSourceContext().getReportCode()
            );
        delay();
        task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 90, "WORKER_ACCELERATION_ARTIFACTS_READY");
        optimizationTaskRepository.save(task);
        return accelerationSuggestion;
    }

    private boolean shouldForceFailure(OptimizationTask task) {
        return containsFailureMarker(task.getSqlText()) || containsFailureMarker(task.getSqlFingerprint());
    }

    private void persistRewriteRecommendation(OptimizationTask task) {
        if (parseTriggeredRewriteRecommendationService == null) {
            return;
        }
        try {
            parseTriggeredRewriteRecommendationService.persistRecommendationFromSucceededRewriteTask(task);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation={} entity={} tenantId={} status=RECOMMENDATION_WRITE_DEGRADED reason={}",
                OPERATION,
                task.getTaskId(),
                task.getTenantId(),
                ex.getMessage()
            );
        }
    }

    private boolean containsFailureMarker(String value) {
        return value != null && value.toUpperCase().contains(FAILURE_MARKER);
    }

    private void logStateChange(OptimizationTask task, String from, String to, String note) {
        LOGGER.info(
            "操作日志 operation={} entity={} tenantId={} taskType={} status=STATE_CHANGE from={} to={} resultStatus={} note={}",
            OPERATION,
            task.getTaskId(),
            task.getTenantId(),
            task.getTaskType().name(),
            from,
            to,
            task.getStatus().name(),
            note
        );
    }

    private void logEnd(OptimizationTask task, long start) {
        LOGGER.info(
            "操作日志 operation={} entity={} tenantId={} costMs={} status=END resultStatus={}",
            OPERATION,
            task.getTaskId(),
            task.getTenantId(),
            System.currentTimeMillis() - start,
            task.getStatus().name()
        );
    }

    private void delay() {
        if (executionProperties.getPhaseDelayMs() <= 0L) {
            return;
        }
        try {
            Thread.sleep(executionProperties.getPhaseDelayMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("优化 worker 被中断", ex);
        }
    }
}
