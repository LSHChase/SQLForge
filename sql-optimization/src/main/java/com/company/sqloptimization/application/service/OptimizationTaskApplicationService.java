package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.access.AccessAuditContract;
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
 * 基于已持久化的优化任务载体提供提交/轮询 API。
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
    private final OptimizationMetricsRecorder optimizationMetricsRecorder;

    public OptimizationTaskApplicationService(
        OptimizationTaskModelApplicationService optimizationTaskModelApplicationService,
        OptimizationTaskRepository optimizationTaskRepository,
        GovernanceCapabilityClient governanceCapabilityClient,
        OptimizationMetricsRecorder optimizationMetricsRecorder
    ) {
        this.optimizationTaskModelApplicationService = optimizationTaskModelApplicationService;
        this.optimizationTaskRepository = optimizationTaskRepository;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.optimizationMetricsRecorder = optimizationMetricsRecorder;
    }

    public OptimizationTaskSubmitResponse submitTask(OptimizationTaskSubmitRequest request) {
        return submitTask(request, UUID.randomUUID().toString(), false);
    }

    public OptimizationTaskSubmitResponse submitInternalTaskIfAbsent(OptimizationTaskSubmitRequest request, String taskId) {
        return submitTask(request, taskId, true);
    }

    private OptimizationTaskSubmitResponse submitTask(OptimizationTaskSubmitRequest request,
                                                      String requestedTaskId,
                                                      boolean idempotent) {
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
            String taskId = hasText(requestedTaskId) ? requestedTaskId.trim() : UUID.randomUUID().toString();
            if (idempotent) {
                OptimizationTask existing = optimizationTaskRepository.findByTaskId(taskId);
                if (existing != null) {
                    verifyTenantAccess(existing.getTenantId());
                    OptimizationTaskSubmitResponse response = optimizationTaskModelApplicationService.buildSubmitResponse(
                        existing,
                        existing.getSubmittedAt() == null
                            ? Instant.now().plusSeconds(READY_ESTIMATE_SECONDS)
                            : existing.getSubmittedAt().plusSeconds(READY_ESTIMATE_SECONDS)
                    );
                    logEnd(SUBMIT_OPERATION, existing.getTaskId(), existing.getTenantId(), start, existing.getStatus().name());
                    return response;
                }
            }
            Instant submittedAt = Instant.now();
            OptimizationTask task = optimizationTaskModelApplicationService.createQueuedTask(
                request,
                taskId,
                submittedAt
            );
            optimizationTaskRepository.save(task);
            optimizationMetricsRecorder.recordTaskSubmitted(task);
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
        LOGGER.info("操作日志 operation={} entity={} status=START", QUERY_OPERATION, taskId);
        try {
            OptimizationTask task = optimizationTaskRepository.findByTaskId(taskId);
            if (task == null) {
                throw new BizException(
                    ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "优化任务不存在，taskId=" + taskId
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
                "callbackUrl 必须以 http:// 或 https:// 开头"
            );
        }
    }

    private String normalizeFingerprint(OptimizationTaskSubmitRequest request) {
        if (request.getSqlFingerprint() != null && request.getSqlFingerprint().trim().length() > 0) {
            return request.getSqlFingerprint().trim();
        }
        return SqlFingerprintUtils.fingerprint(request.getSqlText().trim());
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (requestTenantId != null && requestTenantId.trim().length() > 0
            && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("当前认证租户无权访问该优化任务");
        }
    }

    private void logSubmitStart(OptimizationTaskSubmitRequest request, String sqlFingerprint) {
        LOGGER.info(
            "操作日志 operation={} entity={} tenantId={} taskType={} datasourceType={} status=START",
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
            "操作日志 operation={} entity={} tenantId={} taskType={} status=STATE_CHANGE from={} to={} resultStatus={} note={}",
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
            "操作日志 operation={} entity={} tenantId={} costMs={} status=END resultStatus={}",
            operation,
            entity,
            tenantId,
            System.currentTimeMillis() - start,
            resultStatus
        );
    }

    private void logFailure(String operation, String entity, String tenantId, long start, RuntimeException ex) {
        LOGGER.error(
            "操作日志 operation={} entity={} tenantId={} costMs={} status=FAILED phase=EXCEPTION reason={}",
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
        AccessAuditContract accessAuditContract = AccessAuditContract.capture();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("accessChannel", accessAuditContract.getAccessChannel().name());
        payload.put("authSource", accessAuditContract.getAuthSource());
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", request.getTenantId());
        payload.put("taskType", request.getTaskType() == null ? null : request.getTaskType().name());
        payload.put("datasourceType", request.getDatasourceType() == null ? null : request.getDatasourceType().name());
        payload.put("sqlFingerprint", sqlFingerprint);
        return JsonUtils.toJson(payload);
    }

    private String buildStatusRequestParams(OptimizationTask task) {
        AccessAuditContract accessAuditContract = AccessAuditContract.capture();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("accessChannel", accessAuditContract.getAccessChannel().name());
        payload.put("authSource", accessAuditContract.getAuthSource());
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", task.getTenantId());
        payload.put("taskId", task.getTaskId());
        payload.put("taskType", task.getTaskType().name());
        payload.put("datasourceType", task.getDatasourceType() == null ? null : task.getDatasourceType().name());
        payload.put("sqlFingerprint", task.getSqlFingerprint());
        return JsonUtils.toJson(payload);
    }

    private String buildMissingStatusRequestParams(String taskId) {
        AccessAuditContract accessAuditContract = AccessAuditContract.capture();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("accessChannel", accessAuditContract.getAccessChannel().name());
        payload.put("authSource", accessAuditContract.getAuthSource());
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
