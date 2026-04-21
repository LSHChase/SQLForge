package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.domain.task.OptimizationTask;
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
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
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
            verifyTenantAccess(task.getTenantId());
            OptimizationTaskStatusResponse response = optimizationTaskModelApplicationService.buildStatusResponse(task);
            logEnd(QUERY_OPERATION, taskId, task.getTenantId(), start, task.getStatus().name());
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, taskId, null, start, ex);
            throw ex;
        }
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

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (requestTenantId != null && requestTenantId.trim().length() > 0
            && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this optimization task");
        }
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
