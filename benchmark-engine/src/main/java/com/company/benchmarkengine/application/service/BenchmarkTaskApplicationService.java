package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
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
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
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
            verifyTenantAccess(task.getTenantId(), "Authenticated tenant cannot access this benchmark task");
            BenchmarkTaskStatusResponse response = benchmarkTaskModelApplicationService.buildStatusResponse(task);
            logEnd(QUERY_OPERATION, taskId, task.getTenantId(), start, task.getStatus().name());
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, taskId, null, start, ex);
            throw ex;
        }
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

    private void verifyTenantAccess(String resourceTenantId, String message) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException(message);
        }
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
