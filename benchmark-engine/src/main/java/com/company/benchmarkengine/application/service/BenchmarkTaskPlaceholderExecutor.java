package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkTaskPlaceholderExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkTaskPlaceholderExecutor.class);

    private static final String OPERATION = "BENCHMARK_TASK_EXECUTOR";
    private static final String STATE_TASK_QUEUED = "TASK_QUEUED";
    private static final String STATE_PLACEHOLDER_RUNNING = "PLACEHOLDER_RUNNING";
    private static final String STATE_PLACEHOLDER_SUCCEEDED = "PLACEHOLDER_SUCCEEDED";
    private static final String STATE_PLACEHOLDER_FAILED = "PLACEHOLDER_FAILED";
    private static final String FAILURE_MARKER = "FAIL_BENCHMARK";

    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkTaskRepository benchmarkTaskRepository;
    private final BenchmarkTaskExecutionProperties executionProperties;

    public BenchmarkTaskPlaceholderExecutor(BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                                            BenchmarkTaskRepository benchmarkTaskRepository,
                                            BenchmarkTaskExecutionProperties executionProperties) {
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkTaskRepository = benchmarkTaskRepository;
        this.executionProperties = executionProperties;
    }

    @Scheduled(fixedDelayString = "${benchmark-engine.task-execution.poll-interval-ms:25}")
    public void processQueuedTasks() {
        Instant visibleBefore = Instant.now().minusMillis(executionProperties.getQueueVisibilityDelayMs());
        List<BenchmarkTask> queuedTasks = benchmarkTaskRepository.findQueuedTasksSubmittedBefore(visibleBefore);
        for (BenchmarkTask task : queuedTasks) {
            processTask(task);
        }
    }

    private void processTask(BenchmarkTask task) {
        long start = System.currentTimeMillis();
        try {
            task.markRunning(Instant.now());
            benchmarkTaskRepository.saveTask(task);
            logStateChange(task, STATE_TASK_QUEUED, STATE_PLACEHOLDER_RUNNING, task.getCurrentPhase().name());
            if (shouldForceFailure(task)) {
                delay();
                task.markFailed(
                    new BenchmarkTaskError(
                        ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_PIPELINE_NOT_READY,
                        ErrorCodeConstants.BENCHMARK_ENGINE_PIPELINE_NOT_READY_MESSAGE,
                        "Remove the FAIL_BENCHMARK marker or wait for the real execution pipeline in a later task.",
                        true
                    ),
                    Instant.now()
                );
                benchmarkTaskRepository.saveTask(task);
                logStateChange(task, STATE_PLACEHOLDER_RUNNING, STATE_PLACEHOLDER_FAILED, task.getError().getMessage());
                logEnd(task, start);
                return;
            }
            advancePlaceholderPhases(task);
            delay();
            BenchmarkReport report = benchmarkTaskModelApplicationService.buildPlaceholderReport(task, Instant.now());
            benchmarkTaskRepository.saveReport(report);
            task.markSucceeded(report.getReportId(), Instant.now());
            benchmarkTaskRepository.saveTask(task);
            logStateChange(task, STATE_PLACEHOLDER_RUNNING, STATE_PLACEHOLDER_SUCCEEDED, report.getReportId());
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

    private void advancePlaceholderPhases(BenchmarkTask task) {
        if (task.getTaskType() == BenchmarkTaskType.BASELINE) {
            delay();
            task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 35, "PLACEHOLDER_WARMUP_READY");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "PLACEHOLDER_RUN_STARTED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "PLACEHOLDER_RUN_FINISHED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "PLACEHOLDER_REPORT_ASSEMBLING");
            benchmarkTaskRepository.saveTask(task);
            return;
        }
        if (task.getTaskType() == BenchmarkTaskType.COMPARISON) {
            delay();
            task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 25, "PLACEHOLDER_SHADOW_ENVIRONMENT_VALIDATED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 45, "PLACEHOLDER_WARMUP_READY");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.EXECUTING, 65, "PLACEHOLDER_COMPARISON_RUN_STARTED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 82, "PLACEHOLDER_COMPARISON_RUN_FINISHED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.REPORTING, 96, "PLACEHOLDER_REPORT_ASSEMBLING");
            benchmarkTaskRepository.saveTask(task);
            return;
        }
        delay();
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 55, "PLACEHOLDER_REGRESSION_RUN_STARTED");
        benchmarkTaskRepository.saveTask(task);
        delay();
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 82, "PLACEHOLDER_REGRESSION_RUN_FINISHED");
        benchmarkTaskRepository.saveTask(task);
        delay();
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 96, "PLACEHOLDER_REPORT_ASSEMBLING");
        benchmarkTaskRepository.saveTask(task);
    }

    private boolean shouldForceFailure(BenchmarkTask task) {
        return containsFailureMarker(task.getSqlText()) || containsFailureMarker(task.getSqlFingerprint());
    }

    private boolean containsFailureMarker(String value) {
        return value != null && value.toUpperCase().contains(FAILURE_MARKER);
    }

    private void logStateChange(BenchmarkTask task, String from, String to, String note) {
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

    private void logEnd(BenchmarkTask task, long start) {
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
            throw new IllegalStateException("Benchmark placeholder executor interrupted", ex);
        }
    }
}
