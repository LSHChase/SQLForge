package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String RESOURCE_TYPE_TASK = "BENCHMARK_ENGINE_TASK";

    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkTaskRepository benchmarkTaskRepository;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final BenchmarkMetricsRecorder benchmarkMetricsRecorder;
    private final BenchmarkTaskQueueService benchmarkTaskQueueService;

    public BenchmarkTaskApplicationService(BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                                           BenchmarkTaskRepository benchmarkTaskRepository,
                                           GovernanceCapabilityClient governanceCapabilityClient,
                                           BenchmarkMetricsRecorder benchmarkMetricsRecorder,
                                           BenchmarkTaskQueueService benchmarkTaskQueueService) {
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkTaskRepository = benchmarkTaskRepository;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.benchmarkMetricsRecorder = benchmarkMetricsRecorder;
        this.benchmarkTaskQueueService = benchmarkTaskQueueService;
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
            assertAuthorization(task.getTenantId(), task.getTaskId(), task.getTargetEngines(), SUBMIT_OPERATION);
            benchmarkTaskRepository.saveTask(task);
            BenchmarkTaskQueueService.BenchmarkQueueDispatch queueDispatch = benchmarkTaskQueueService.dispatch(task);
            benchmarkMetricsRecorder.recordTaskSubmitted(task);
            logStateChange(
                SUBMIT_OPERATION,
                task.getTaskId(),
                request.getTenantId(),
                task.getTaskType().name(),
                STATE_REQUEST_ACCEPTED,
                STATE_TASK_QUEUED,
                task.getStatus().name(),
                queueDispatch.getQueueEvidence()
            );
            BenchmarkTaskSubmitResponse response = benchmarkTaskModelApplicationService.buildSubmitResponse(
                task,
                submittedAt.plusSeconds(PLACEHOLDER_ESTIMATE_SECONDS)
            );
            logEnd(SUBMIT_OPERATION, task.getTaskId(), request.getTenantId(), start, task.getStatus().name());
            writeAuditRecord(
                SUBMIT_OPERATION,
                task.getTaskId(),
                task.getStatus().name(),
                System.currentTimeMillis() - start,
                buildTaskRequestParams(task),
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
                buildMissingTaskRequestParams(normalizedFingerprint),
                buildSubmitResponseSummary(null, ex.getMessage())
            );
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
            assertAuthorization(task.getTenantId(), taskId, task.getTargetEngines(), QUERY_OPERATION);
            BenchmarkTaskStatusResponse response = benchmarkTaskModelApplicationService.buildStatusResponse(task);
            logEnd(QUERY_OPERATION, taskId, task.getTenantId(), start, task.getStatus().name());
            writeAuditRecord(
                QUERY_OPERATION,
                taskId,
                task.getStatus().name(),
                System.currentTimeMillis() - start,
                buildTaskRequestParams(task),
                buildTaskStatusResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, taskId, null, start, ex);
            writeAuditRecord(
                QUERY_OPERATION,
                taskId,
                "FAILED",
                System.currentTimeMillis() - start,
                buildMissingTaskRequestParams(taskId),
                buildTaskStatusResponseSummary(null, ex.getMessage())
            );
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

    private void assertAuthorization(String tenantId,
                                     String resourceId,
                                     List<DataSourceTypeEnum> targetEngines,
                                     String operationCode) {
        if (targetEngines == null || targetEngines.isEmpty()) {
            governanceCapabilityClient.assertAuthorization(
                tenantId,
                DataSourceTypeEnum.HETU,
                RESOURCE_TYPE_TASK,
                resourceId,
                operationCode
            );
            return;
        }
        for (DataSourceTypeEnum targetEngine : targetEngines) {
            governanceCapabilityClient.assertAuthorization(
                tenantId,
                targetEngine,
                RESOURCE_TYPE_TASK,
                resourceId,
                operationCode
            );
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

    private void writeAuditRecord(String operationCode,
                                  String resourceId,
                                  String resultStatus,
                                  long elapsedMs,
                                  String requestParams,
                                  String responseSummary) {
        governanceCapabilityClient.writeAudit(
            new BenchmarkAuditRecord(
                operationCode,
                RESOURCE_TYPE_TASK,
                resourceId,
                resultStatus,
                elapsedMs,
                null,
                null,
                null,
                null,
                null,
                requestParams,
                responseSummary
            )
        );
    }

    private String buildTaskRequestParams(BenchmarkTask task) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", task.getTenantId());
        payload.put("taskId", task.getTaskId());
        payload.put("taskType", task.getTaskType().name());
        payload.put("targetEngines", task.getTargetEngines());
        payload.put("sqlFingerprint", task.getSqlFingerprint());
        payload.put("templateId", task.getTemplateId());
        payload.put("templateType", task.getTemplateType() == null ? null : task.getTemplateType().name());
        payload.put("testSetId", task.getTestSetId());
        payload.put("testSetSource", task.getTestSetSource() == null ? null : task.getTestSetSource().name());
        return JsonUtils.toJson(payload);
    }

    private String buildMissingTaskRequestParams(String resourceId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", RequestContext.getTenantId());
        payload.put("resourceId", resourceId);
        return JsonUtils.toJson(payload);
    }

    private String buildSubmitResponseSummary(BenchmarkTaskSubmitResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getStatus().name());
        payload.put("currentPhase", response == null ? null : response.getCurrentPhase().name());
        payload.put("taskId", response == null ? null : response.getTaskId());
        payload.put("queueMode", response == null ? null : response.getQueueMode());
        payload.put("queueEvidence", response == null ? null : response.getQueueEvidence());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private String buildTaskStatusResponseSummary(BenchmarkTaskStatusResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getStatus().name());
        payload.put("currentPhase", response == null ? null : response.getCurrentPhase().name());
        payload.put("taskId", response == null ? null : response.getTaskId());
        payload.put("reportId", response == null ? null : response.getReportId());
        payload.put("queueMode", response == null ? null : response.getQueueMode());
        payload.put("queueEvidence", response == null ? null : response.getQueueEvidence());
        payload.put("templateId", response == null ? null : response.getTemplateId());
        payload.put("testSetId", response == null ? null : response.getTestSetId());
        payload.put("errorCode", response == null || response.getError() == null ? null : response.getError().getCode());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }
}
