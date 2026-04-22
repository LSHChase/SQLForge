package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqloptimization.config.OptimizationTaskExecutionProperties;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public OptimizationTaskWorker(OptimizationTaskRepository optimizationTaskRepository,
                                  OptimizationTaskExecutionProperties executionProperties) {
        this.optimizationTaskRepository = optimizationTaskRepository;
        this.executionProperties = executionProperties;
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
                        "SQL optimization worker failed before producing a suggestion payload",
                        "Inspect the database-backed worker pipeline and retry after the carrier is healthy.",
                        true
                    ),
                    Instant.now()
                );
                optimizationTaskRepository.save(task);
                logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_FAILED, task.getError().getMessage());
                logEnd(task, start);
                return;
            }
            advancePhases(task);
            delay();
            task.markSucceeded(buildSummary(task), Instant.now());
            optimizationTaskRepository.save(task);
            logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_SUCCEEDED, task.getSummary());
            logEnd(task, start);
        } catch (RuntimeException ex) {
            LOGGER.error("operation={} entity={} tenantId={} costMs={} status=FAILED phase=EXCEPTION reason={}",
                OPERATION,
                task.getTaskId(),
                task.getTenantId(),
                System.currentTimeMillis() - start,
                ex.getMessage(),
                ex);
            throw ex;
        }
    }

    private void advancePhases(OptimizationTask task) {
        if (task.getTaskType() == OptimizationTaskType.PARSE) {
            delay();
            task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "WORKER_PARSE_SUMMARY_READY");
            optimizationTaskRepository.save(task);
            return;
        }
        if (task.getTaskType() == OptimizationTaskType.REWRITE) {
            delay();
            task.advancePhase(OptimizationTaskPhase.SQL_REWRITING, 45, "WORKER_REWRITE_RULES_APPLIED");
            optimizationTaskRepository.save(task);
            delay();
            task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "WORKER_REWRITE_SUMMARY_READY");
            optimizationTaskRepository.save(task);
            return;
        }
        delay();
        task.advancePhase(OptimizationTaskPhase.COST_ESTIMATING, 35, "WORKER_COST_BASELINE_READY");
        optimizationTaskRepository.save(task);
        delay();
        task.advancePhase(OptimizationTaskPhase.ACCELERATION_PLANNING, 70, "WORKER_ACCELERATION_PLAN_READY");
        optimizationTaskRepository.save(task);
        delay();
        task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 90, "WORKER_ACCELERATION_SUMMARY_READY");
        optimizationTaskRepository.save(task);
    }

    private String buildSummary(OptimizationTask task) {
        if (task.getTaskType() == OptimizationTaskType.PARSE) {
            return "Deep parse placeholder completed for sqlFingerprint=" + task.getSqlFingerprint();
        }
        if (task.getTaskType() == OptimizationTaskType.REWRITE) {
            return "Rewrite suggestion placeholder completed for sqlFingerprint=" + task.getSqlFingerprint();
        }
        return "Acceleration suggestion placeholder completed for sqlFingerprint=" + task.getSqlFingerprint();
    }

    private boolean shouldForceFailure(OptimizationTask task) {
        return containsFailureMarker(task.getSqlText()) || containsFailureMarker(task.getSqlFingerprint());
    }

    private boolean containsFailureMarker(String value) {
        return value != null && value.toUpperCase().contains(FAILURE_MARKER);
    }

    private void logStateChange(OptimizationTask task, String from, String to, String note) {
        LOGGER.info(
            "operation={} entity={} tenantId={} taskType={} status=STATE_CHANGE from={} to={} resultStatus={} note={}",
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
            "operation={} entity={} tenantId={} costMs={} status=END resultStatus={}",
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
            throw new IllegalStateException("Optimization worker interrupted", ex);
        }
    }
}
