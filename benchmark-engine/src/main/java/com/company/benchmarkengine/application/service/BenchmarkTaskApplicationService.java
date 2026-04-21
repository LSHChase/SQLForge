package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkTaskApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkTaskApplicationService.class);

    private static final String SUBMIT_OPERATION = "BENCHMARK_TASK_SUBMIT";
    private static final String QUERY_OPERATION = "BENCHMARK_TASK_STATUS_QUERY";
    private static final String STATE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    private static final String STATE_TASK_QUEUED = "TASK_QUEUED";
    private static final String STATE_PLACEHOLDER_RUNNING = "PLACEHOLDER_RUNNING";
    private static final String STATE_PLACEHOLDER_SUCCEEDED = "PLACEHOLDER_SUCCEEDED";
    private static final String STATE_PLACEHOLDER_FAILED = "PLACEHOLDER_FAILED";
    private static final String FAILURE_MARKER = "FAIL_BENCHMARK";
    private static final long PLACEHOLDER_ESTIMATE_SECONDS = 45L;

    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkTaskRepository benchmarkTaskRepository;

    public BenchmarkTaskApplicationService(BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                                           BenchmarkTaskRepository benchmarkTaskRepository) {
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkTaskRepository = benchmarkTaskRepository;
    }

    public BenchmarkTaskSubmitResponse submitTask(BenchmarkTaskSubmitRequest request) {
        long start = System.currentTimeMillis();
        String normalizedFingerprint = normalizeFingerprint(request);
        logSubmitStart(request, normalizedFingerprint);
        try {
            validateIsolationPolicy(request);
            request.setSqlFingerprint(normalizedFingerprint);
            Instant submittedAt = Instant.now();
            BenchmarkTask task = benchmarkTaskModelApplicationService.createQueuedTask(
                request,
                UUID.randomUUID().toString(),
                submittedAt
            );
            benchmarkTaskRepository.saveTask(task);
            logStateChange(
                SUBMIT_OPERATION,
                task.getTaskId(),
                request.getTenantId(),
                task.getTaskType().name(),
                STATE_REQUEST_ACCEPTED,
                STATE_TASK_QUEUED,
                task.getStatus().name(),
                null
            );
            BenchmarkTaskSubmitResponse response = benchmarkTaskModelApplicationService.buildSubmitResponse(
                task,
                submittedAt.plusSeconds(PLACEHOLDER_ESTIMATE_SECONDS)
            );
            processPlaceholderLifecycle(task);
            logEnd(SUBMIT_OPERATION, task.getTaskId(), request.getTenantId(), start, task.getStatus().name());
            return response;
        } catch (RuntimeException ex) {
            logFailure(SUBMIT_OPERATION, normalizedFingerprint, request.getTenantId(), start, ex);
            throw ex;
        }
    }

    public BenchmarkTaskStatusResponse getTaskStatus(String taskId) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} status=START", QUERY_OPERATION, taskId);
        try {
            BenchmarkTask task = benchmarkTaskRepository.findTaskByTaskId(taskId);
            if (task == null) {
                throw new BizException(
                    ErrorCodeConstants.BENCHMARK_TASK_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Benchmark task does not exist for taskId=" + taskId
                );
            }
            BenchmarkTaskStatusResponse response = benchmarkTaskModelApplicationService.buildStatusResponse(task);
            logEnd(QUERY_OPERATION, taskId, task.getTenantId(), start, task.getStatus().name());
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, taskId, null, start, ex);
            throw ex;
        }
    }

    private void processPlaceholderLifecycle(BenchmarkTask task) {
        task.markRunning(Instant.now());
        benchmarkTaskRepository.saveTask(task);
        logStateChange(
            SUBMIT_OPERATION,
            task.getTaskId(),
            task.getTenantId(),
            task.getTaskType().name(),
            STATE_TASK_QUEUED,
            STATE_PLACEHOLDER_RUNNING,
            task.getStatus().name(),
            task.getCurrentPhase().name()
        );

        if (shouldForceFailure(task)) {
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
            logStateChange(
                SUBMIT_OPERATION,
                task.getTaskId(),
                task.getTenantId(),
                task.getTaskType().name(),
                STATE_PLACEHOLDER_RUNNING,
                STATE_PLACEHOLDER_FAILED,
                task.getStatus().name(),
                task.getError().getMessage()
            );
            return;
        }

        advancePlaceholderPhases(task);
        BenchmarkReport report = benchmarkTaskModelApplicationService.buildPlaceholderReport(task, Instant.now());
        benchmarkTaskRepository.saveReport(report);
        task.markSucceeded(report.getReportId(), Instant.now());
        benchmarkTaskRepository.saveTask(task);
        logStateChange(
            SUBMIT_OPERATION,
            task.getTaskId(),
            task.getTenantId(),
            task.getTaskType().name(),
            STATE_PLACEHOLDER_RUNNING,
            STATE_PLACEHOLDER_SUCCEEDED,
            task.getStatus().name(),
            report.getReportId()
        );
    }

    private void advancePlaceholderPhases(BenchmarkTask task) {
        if (task.getTaskType() == BenchmarkTaskType.BASELINE) {
            task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 35, "PLACEHOLDER_WARMUP_READY");
            task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "PLACEHOLDER_RUN_STARTED");
            task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "PLACEHOLDER_RUN_FINISHED");
            task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "PLACEHOLDER_REPORT_ASSEMBLING");
            return;
        }
        if (task.getTaskType() == BenchmarkTaskType.COMPARISON) {
            task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 25, "PLACEHOLDER_SHADOW_ENVIRONMENT_VALIDATED");
            task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 45, "PLACEHOLDER_WARMUP_READY");
            task.advancePhase(BenchmarkTaskPhase.EXECUTING, 65, "PLACEHOLDER_COMPARISON_RUN_STARTED");
            task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 82, "PLACEHOLDER_COMPARISON_RUN_FINISHED");
            task.advancePhase(BenchmarkTaskPhase.REPORTING, 96, "PLACEHOLDER_REPORT_ASSEMBLING");
            return;
        }
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 55, "PLACEHOLDER_REGRESSION_RUN_STARTED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 82, "PLACEHOLDER_REGRESSION_RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 96, "PLACEHOLDER_REPORT_ASSEMBLING");
    }

    private boolean shouldForceFailure(BenchmarkTask task) {
        return containsFailureMarker(task.getSqlText()) || containsFailureMarker(task.getSqlFingerprint());
    }

    private boolean containsFailureMarker(String value) {
        return value != null && value.toUpperCase().contains(FAILURE_MARKER);
    }

    private void validateIsolationPolicy(BenchmarkTaskSubmitRequest request) {
        if (request.getTaskContext() == null) {
            return;
        }
        if (Boolean.FALSE.equals(request.getTaskContext().getReadonlyRequired())) {
            throw new BizException(
                ErrorCodeConstants.BENCHMARK_ISOLATION_POLICY_REJECTED,
                HttpStatus.BAD_REQUEST,
                "readonlyRequired must remain true in the current benchmark-engine baseline"
            );
        }
        if (request.getTaskContext().getShadowEnvironmentMode() == ShadowEnvironmentMode.DISABLED) {
            throw new BizException(
                ErrorCodeConstants.BENCHMARK_ISOLATION_POLICY_REJECTED,
                HttpStatus.BAD_REQUEST,
                "shadowEnvironmentMode=DISABLED is not allowed in the current benchmark-engine baseline"
            );
        }
    }

    private String normalizeFingerprint(BenchmarkTaskSubmitRequest request) {
        if (request.getSqlFingerprint() != null && request.getSqlFingerprint().trim().length() > 0) {
            return request.getSqlFingerprint().trim();
        }
        return SqlFingerprintUtils.fingerprint(request.getSqlText().trim());
    }

    private void logSubmitStart(BenchmarkTaskSubmitRequest request, String sqlFingerprint) {
        LOGGER.info(
            "operation={} entity={} tenantId={} taskType={} status=START",
            SUBMIT_OPERATION,
            sqlFingerprint,
            request.getTenantId(),
            request.getTaskType()
        );
    }

    private void logStateChange(String operation,
                                String entity,
                                String tenantId,
                                String taskType,
                                String from,
                                String to,
                                String resultStatus,
                                String note) {
        LOGGER.info(
            "operation={} entity={} tenantId={} taskType={} status=STATE_CHANGE from={} to={} resultStatus={} note={}",
            operation,
            entity,
            tenantId,
            taskType,
            from,
            to,
            resultStatus,
            note
        );
    }

    private void logEnd(String operation, String entity, String tenantId, long start, String resultStatus) {
        LOGGER.info(
            "operation={} entity={} tenantId={} costMs={} status=END resultStatus={}",
            operation,
            entity,
            tenantId,
            System.currentTimeMillis() - start,
            resultStatus
        );
    }

    private void logFailure(String operation, String entity, String tenantId, long start, RuntimeException ex) {
        LOGGER.error(
            "operation={} entity={} tenantId={} costMs={} status=FAILED phase=EXCEPTION reason={}",
            operation,
            entity,
            tenantId,
            System.currentTimeMillis() - start,
            ex.getMessage(),
            ex
        );
    }
}
