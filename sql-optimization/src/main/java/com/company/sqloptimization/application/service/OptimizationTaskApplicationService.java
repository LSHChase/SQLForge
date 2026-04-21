package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Provides the transitional submit/poll API skeleton before persistence and queue integrations arrive.
 */
@Service
public class OptimizationTaskApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationTaskApplicationService.class);

    private static final String SUBMIT_OPERATION = "OPTIMIZATION_TASK_SUBMIT";
    private static final String QUERY_OPERATION = "OPTIMIZATION_TASK_STATUS_QUERY";
    private static final String STATE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    private static final String STATE_TASK_QUEUED = "TASK_QUEUED";
    private static final String STATE_PLACEHOLDER_RUNNING = "PLACEHOLDER_RUNNING";
    private static final String STATE_PLACEHOLDER_SUCCEEDED = "PLACEHOLDER_SUCCEEDED";
    private static final String STATE_PLACEHOLDER_FAILED = "PLACEHOLDER_FAILED";
    private static final String FAILURE_MARKER = "FAIL_OPTIMIZATION";
    private static final long PLACEHOLDER_ESTIMATE_SECONDS = 30L;

    private final OptimizationTaskModelApplicationService optimizationTaskModelApplicationService;
    private final OptimizationTaskRepository optimizationTaskRepository;

    public OptimizationTaskApplicationService(
        OptimizationTaskModelApplicationService optimizationTaskModelApplicationService,
        OptimizationTaskRepository optimizationTaskRepository
    ) {
        this.optimizationTaskModelApplicationService = optimizationTaskModelApplicationService;
        this.optimizationTaskRepository = optimizationTaskRepository;
    }

    public OptimizationTaskSubmitResponse submitTask(OptimizationTaskSubmitRequest request) {
        long start = System.currentTimeMillis();
        String normalizedFingerprint = normalizeFingerprint(request);
        logSubmitStart(request, normalizedFingerprint);
        try {
            validateCallbackUrl(request);
            request.setSqlFingerprint(normalizedFingerprint);
            Instant submittedAt = Instant.now();
            OptimizationTask task = optimizationTaskModelApplicationService.createQueuedTask(
                request,
                UUID.randomUUID().toString(),
                submittedAt
            );
            optimizationTaskRepository.save(task);
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
            OptimizationTaskSubmitResponse response = optimizationTaskModelApplicationService.buildSubmitResponse(
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

    public OptimizationTaskStatusResponse getTaskStatus(String taskId) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} status=START", QUERY_OPERATION, taskId);
        try {
            OptimizationTask task = optimizationTaskRepository.findByTaskId(taskId);
            if (task == null) {
                throw new BizException(
                    ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Optimization task does not exist for taskId=" + taskId
                );
            }
            OptimizationTaskStatusResponse response = optimizationTaskModelApplicationService.buildStatusResponse(task);
            logEnd(QUERY_OPERATION, taskId, task.getTenantId(), start, task.getStatus().name());
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, taskId, null, start, ex);
            throw ex;
        }
    }

    private void processPlaceholderLifecycle(OptimizationTask task) {
        task.markRunning(Instant.now());
        optimizationTaskRepository.save(task);
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
                new OptimizationTaskError(
                    ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_PIPELINE_NOT_READY,
                    ErrorCodeConstants.SQL_OPTIMIZATION_PIPELINE_NOT_READY_MESSAGE,
                    "Remove the FAIL_OPTIMIZATION marker or wait for the real worker pipeline in a later task.",
                    true
                ),
                Instant.now()
            );
            optimizationTaskRepository.save(task);
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
        task.markSucceeded(buildSummary(task), Instant.now());
        optimizationTaskRepository.save(task);
        logStateChange(
            SUBMIT_OPERATION,
            task.getTaskId(),
            task.getTenantId(),
            task.getTaskType().name(),
            STATE_PLACEHOLDER_RUNNING,
            STATE_PLACEHOLDER_SUCCEEDED,
            task.getStatus().name(),
            task.getSummary()
        );
    }

    private void advancePlaceholderPhases(OptimizationTask task) {
        if (task.getTaskType() == OptimizationTaskType.PARSE) {
            task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "PLACEHOLDER_PARSE_SUMMARY_READY");
            return;
        }
        if (task.getTaskType() == OptimizationTaskType.REWRITE) {
            task.advancePhase(OptimizationTaskPhase.SQL_REWRITING, 45, "PLACEHOLDER_REWRITE_RULES_APPLIED");
            task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 85, "PLACEHOLDER_REWRITE_SUMMARY_READY");
            return;
        }
        task.advancePhase(OptimizationTaskPhase.COST_ESTIMATING, 35, "PLACEHOLDER_COST_BASELINE_READY");
        task.advancePhase(OptimizationTaskPhase.ACCELERATION_PLANNING, 70, "PLACEHOLDER_ACCELERATION_PLAN_READY");
        task.advancePhase(OptimizationTaskPhase.RESULT_ASSEMBLING, 90, "PLACEHOLDER_ACCELERATION_SUMMARY_READY");
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

    private void validateCallbackUrl(OptimizationTaskSubmitRequest request) {
        if (request.getTaskContext() == null || request.getTaskContext().getCallbackUrl() == null) {
            return;
        }
        String callbackUrl = request.getTaskContext().getCallbackUrl().trim();
        if (!(callbackUrl.startsWith("http://") || callbackUrl.startsWith("https://"))) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_INVALID,
                HttpStatus.BAD_REQUEST,
                "callbackUrl must start with http:// or https://"
            );
        }
    }

    private String normalizeFingerprint(OptimizationTaskSubmitRequest request) {
        if (request.getSqlFingerprint() != null && request.getSqlFingerprint().trim().length() > 0) {
            return request.getSqlFingerprint().trim();
        }
        return SqlFingerprintUtils.fingerprint(request.getSqlText().trim());
    }

    private void logSubmitStart(OptimizationTaskSubmitRequest request, String sqlFingerprint) {
        LOGGER.info(
            "operation={} entity={} tenantId={} taskType={} datasourceType={} status=START",
            SUBMIT_OPERATION,
            sqlFingerprint,
            request.getTenantId(),
            request.getTaskType(),
            request.getDatasourceType()
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
