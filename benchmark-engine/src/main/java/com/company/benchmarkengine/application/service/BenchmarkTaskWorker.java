package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
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
public class BenchmarkTaskWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkTaskWorker.class);

    private static final String OPERATION = "BENCHMARK_TASK_WORKER";
    private static final String STATE_TASK_QUEUED = "TASK_QUEUED";
    private static final String STATE_WORKER_RUNNING = "WORKER_RUNNING";
    private static final String STATE_WORKER_SUCCEEDED = "WORKER_SUCCEEDED";
    private static final String STATE_WORKER_FAILED = "WORKER_FAILED";
    private static final String FAILURE_MARKER = "FAIL_BENCHMARK";

    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkIsolatedExecutionService benchmarkIsolatedExecutionService;
    private final BenchmarkReportExportService benchmarkReportExportService;
    private final BenchmarkArtifactStorageService benchmarkArtifactStorageService;
    private final BenchmarkGovernanceTraceService benchmarkGovernanceTraceService;
    private final BenchmarkTaskRepository benchmarkTaskRepository;
    private final BenchmarkTaskExecutionProperties executionProperties;
    private final BenchmarkMetricsRecorder benchmarkMetricsRecorder;

    public BenchmarkTaskWorker(BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                               BenchmarkIsolatedExecutionService benchmarkIsolatedExecutionService,
                               BenchmarkReportExportService benchmarkReportExportService,
                               BenchmarkArtifactStorageService benchmarkArtifactStorageService,
                               BenchmarkGovernanceTraceService benchmarkGovernanceTraceService,
                               BenchmarkTaskRepository benchmarkTaskRepository,
                               BenchmarkTaskExecutionProperties executionProperties,
                               BenchmarkMetricsRecorder benchmarkMetricsRecorder) {
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkIsolatedExecutionService = benchmarkIsolatedExecutionService;
        this.benchmarkReportExportService = benchmarkReportExportService;
        this.benchmarkArtifactStorageService = benchmarkArtifactStorageService;
        this.benchmarkGovernanceTraceService = benchmarkGovernanceTraceService;
        this.benchmarkTaskRepository = benchmarkTaskRepository;
        this.executionProperties = executionProperties;
        this.benchmarkMetricsRecorder = benchmarkMetricsRecorder;
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
            logStateChange(task, STATE_TASK_QUEUED, STATE_WORKER_RUNNING, task.getCurrentPhase().name());
            if (shouldForceFailure(task)) {
                delay();
                task.markFailed(
                    new BenchmarkTaskError(
                        ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_PIPELINE_NOT_READY,
                        "Benchmark worker failed before report write-back completed",
                        "Inspect the benchmark task table, report table, and worker pipeline before retrying.",
                        true
                    ),
                    Instant.now()
                );
                benchmarkTaskRepository.saveTask(task);
                logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_FAILED, task.getError().getMessage());
                benchmarkMetricsRecorder.recordWorkerTerminal(task, System.currentTimeMillis() - start);
                logEnd(task, start);
                return;
            }
            advanceWorkerPhases(task);
            delay();
            Instant generatedAt = Instant.now();
            BenchmarkIsolatedExecutionResult executionResult =
                benchmarkIsolatedExecutionService.execute(task, generatedAt);
            BenchmarkReport report = benchmarkTaskModelApplicationService.buildExecutedReport(task, executionResult, generatedAt);
            BenchmarkReportResponse reportResponse = benchmarkTaskModelApplicationService.buildReportResponse(report);
            BenchmarkReportRawDataResponse rawDataResponse = benchmarkTaskModelApplicationService.buildRawDataResponse(report);
            List<BenchmarkReportArtifact> artifacts =
                benchmarkReportExportService.buildArtifacts(reportResponse);
            artifacts.add(benchmarkReportExportService.buildRawDataArtifact(rawDataResponse));
            artifacts = benchmarkArtifactStorageService.externalize(
                report.getReportId(),
                report.getTenantId(),
                report.getGeneratedAt(),
                artifacts
            );
            artifacts = benchmarkGovernanceTraceService.registerTrace(task, report, reportResponse, rawDataResponse, artifacts);
            report = report.withExportArtifacts(artifacts);
            benchmarkTaskRepository.saveReport(report);
            task.markSucceeded(report.getReportId(), Instant.now());
            benchmarkTaskRepository.saveTask(task);
            logStateChange(task, STATE_WORKER_RUNNING, STATE_WORKER_SUCCEEDED, report.getReportId());
            benchmarkMetricsRecorder.recordReportGenerated(report);
            benchmarkMetricsRecorder.recordWorkerTerminal(task, System.currentTimeMillis() - start);
            logEnd(task, start);
        } catch (RuntimeException ex) {
            benchmarkMetricsRecorder.recordWorkerException(task, System.currentTimeMillis() - start);
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

    private void advanceWorkerPhases(BenchmarkTask task) {
        if (task.getTaskType() == BenchmarkTaskType.BASELINE) {
            delay();
            task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 35, "WORKER_WARMUP_READY");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "WORKER_RUN_STARTED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "WORKER_RUN_FINISHED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "WORKER_REPORT_ASSEMBLING");
            benchmarkTaskRepository.saveTask(task);
            return;
        }
        if (task.getTaskType() == BenchmarkTaskType.COMPARISON) {
            delay();
            task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 25, "WORKER_SHADOW_ENVIRONMENT_VALIDATED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 45, "WORKER_WARMUP_READY");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.EXECUTING, 65, "WORKER_COMPARISON_RUN_STARTED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 82, "WORKER_COMPARISON_RUN_FINISHED");
            benchmarkTaskRepository.saveTask(task);
            delay();
            task.advancePhase(BenchmarkTaskPhase.REPORTING, 96, "WORKER_REPORT_ASSEMBLING");
            benchmarkTaskRepository.saveTask(task);
            return;
        }
        delay();
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 55, "WORKER_REGRESSION_RUN_STARTED");
        benchmarkTaskRepository.saveTask(task);
        delay();
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 82, "WORKER_REGRESSION_RUN_FINISHED");
        benchmarkTaskRepository.saveTask(task);
        delay();
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 96, "WORKER_REPORT_ASSEMBLING");
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
            throw new IllegalStateException("Benchmark worker interrupted", ex);
        }
    }
}
