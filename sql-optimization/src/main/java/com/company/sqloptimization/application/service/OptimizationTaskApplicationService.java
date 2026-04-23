package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.governance.OptimizationAuditRecord;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Provides the submit/poll API on top of the persisted optimization task carrier.
 */
@Service
public class OptimizationTaskApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationTaskApplicationService.class);

    private static final String SUBMIT_OPERATION = "OPTIMIZATION_TASK_SUBMIT";
    private static final String QUERY_OPERATION = "OPTIMIZATION_TASK_STATUS_QUERY";
    private static final String STATE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    private static final String STATE_TASK_QUEUED = "TASK_QUEUED";
    private static final long READY_ESTIMATE_SECONDS = 30L;
    private static final String RESOURCE_TYPE_TASK = "SQL_OPTIMIZATION_TASK";

    private final OptimizationTaskModelApplicationService optimizationTaskModelApplicationService;
    private final OptimizationTaskRepository optimizationTaskRepository;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public OptimizationTaskApplicationService(
        OptimizationTaskModelApplicationService optimizationTaskModelApplicationService,
        OptimizationTaskRepository optimizationTaskRepository,
        GovernanceCapabilityClient governanceCapabilityClient
    ) {
        this.optimizationTaskModelApplicationService = optimizationTaskModelApplicationService;
        this.optimizationTaskRepository = optimizationTaskRepository;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public OptimizationTaskSubmitResponse submitTask(OptimizationTaskSubmitRequest request) {
        long start = System.currentTimeMillis();
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
        String normalizedFingerprint = normalizeFingerprint(request);
        logSubmitStart(request, normalizedFingerprint);
        try {
            validateCallbackUrl(request);
            request.setSqlFingerprint(normalizedFingerprint);
            governanceCapabilityClient.assertAuthorization(
                request.getTenantId(),
                request.getDatasourceType(),
                RESOURCE_TYPE_TASK,
                normalizedFingerprint,
                SUBMIT_OPERATION
            );
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
                submittedAt.plusSeconds(READY_ESTIMATE_SECONDS)
            );
            logEnd(SUBMIT_OPERATION, task.getTaskId(), request.getTenantId(), start, task.getStatus().name());
            writeAuditRecord(
                SUBMIT_OPERATION,
                task.getTaskId(),
                task.getStatus().name(),
                System.currentTimeMillis() - start,
                buildSubmitRequestParams(request, normalizedFingerprint),
                buildSubmitResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            logFailure(SUBMIT_OPERATION, normalizedFingerprint, request.getTenantId(), start, ex);
            writeAuditRecord(
                SUBMIT_OPERATION,
                normalizedFingerprint,
                "FAILED",
                System.currentTimeMillis() - start,
                buildSubmitRequestParams(request, normalizedFingerprint),
                buildSubmitResponseSummary(null, ex.getMessage())
            );
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
            governanceCapabilityClient.assertAuthorization(
                task.getTenantId(),
                task.getDatasourceType(),
                RESOURCE_TYPE_TASK,
                taskId,
                QUERY_OPERATION
            );
            OptimizationTaskStatusResponse response = optimizationTaskModelApplicationService.buildStatusResponse(task);
            logEnd(QUERY_OPERATION, taskId, task.getTenantId(), start, task.getStatus().name());
            writeAuditRecord(
                QUERY_OPERATION,
                taskId,
                task.getStatus().name(),
                System.currentTimeMillis() - start,
                buildStatusRequestParams(task),
                buildStatusResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, taskId, null, start, ex);
            writeAuditRecord(
                QUERY_OPERATION,
                taskId,
                "FAILED",
                System.currentTimeMillis() - start,
                buildMissingStatusRequestParams(taskId),
                buildStatusResponseSummary(null, ex.getMessage())
            );
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

    private void writeAuditRecord(String operationCode,
                                  String resourceId,
                                  String resultStatus,
                                  long elapsedMs,
                                  String requestParams,
                                  String responseSummary) {
        governanceCapabilityClient.writeAudit(
            new OptimizationAuditRecord(
                operationCode,
                RESOURCE_TYPE_TASK,
                resourceId,
                resultStatus,
                elapsedMs,
                requestParams,
                responseSummary
            )
        );
    }

    private String buildSubmitRequestParams(OptimizationTaskSubmitRequest request, String sqlFingerprint) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", request.getTenantId());
        payload.put("taskType", request.getTaskType() == null ? null : request.getTaskType().name());
        payload.put("datasourceType", request.getDatasourceType() == null ? null : request.getDatasourceType().name());
        payload.put("sqlFingerprint", sqlFingerprint);
        return JsonUtils.toJson(payload);
    }

    private String buildStatusRequestParams(OptimizationTask task) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", task.getTenantId());
        payload.put("taskId", task.getTaskId());
        payload.put("taskType", task.getTaskType().name());
        payload.put("datasourceType", task.getDatasourceType() == null ? null : task.getDatasourceType().name());
        payload.put("sqlFingerprint", task.getSqlFingerprint());
        return JsonUtils.toJson(payload);
    }

    private String buildMissingStatusRequestParams(String taskId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", RequestContext.getTenantId());
        payload.put("taskId", taskId);
        return JsonUtils.toJson(payload);
    }

    private String buildSubmitResponseSummary(OptimizationTaskSubmitResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getStatus().name());
        payload.put("currentPhase", response == null ? null : response.getCurrentPhase().name());
        payload.put("taskId", response == null ? null : response.getTaskId());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private String buildStatusResponseSummary(OptimizationTaskStatusResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getStatus().name());
        payload.put("currentPhase", response == null ? null : response.getCurrentPhase().name());
        payload.put("taskId", response == null ? null : response.getTaskId());
        payload.put("errorCode", response == null || response.getFailure() == null ? null : response.getFailure().getCode());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }
}
