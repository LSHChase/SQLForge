package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanActivationRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanPauseRequest;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.dto.AccelerationPlanActionRequest;
import com.company.sqloptimization.application.controller.dto.AccelerationPlanSubmitRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanStatusResponse;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanSubmitResponse;
import com.company.sqloptimization.domain.plan.AccelerationPlan;
import com.company.sqloptimization.domain.plan.repository.AccelerationPlanRepository;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.governance.OptimizationAuditRecord;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionAccelerationPlanClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccelerationPlanApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccelerationPlanApplicationService.class);

    private static final String RESOURCE_TYPE_PLAN = "SQL_ACCELERATION_PLAN";
    private static final String SUBMIT_OPERATION = "ACCELERATION_PLAN_SUBMIT";
    private static final String QUERY_OPERATION = "ACCELERATION_PLAN_STATUS_QUERY";
    private static final String ACTIVATE_OPERATION = "ACCELERATION_PLAN_ACTIVATE";
    private static final String PAUSE_OPERATION = "ACCELERATION_PLAN_PAUSE";

    private final AccelerationPlanRepository accelerationPlanRepository;
    private final OptimizationTaskRepository optimizationTaskRepository;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final QueryExecutionAccelerationPlanClient queryExecutionAccelerationPlanClient;
    private final AccelerationPlanModelApplicationService accelerationPlanModelApplicationService;
    private final AccelerationArtifactSnapshotService accelerationArtifactSnapshotService;

    public AccelerationPlanApplicationService(AccelerationPlanRepository accelerationPlanRepository,
                                              OptimizationTaskRepository optimizationTaskRepository,
                                              GovernanceCapabilityClient governanceCapabilityClient,
                                              QueryExecutionAccelerationPlanClient queryExecutionAccelerationPlanClient,
                                              AccelerationPlanModelApplicationService accelerationPlanModelApplicationService) {
        this(
            accelerationPlanRepository,
            optimizationTaskRepository,
            governanceCapabilityClient,
            queryExecutionAccelerationPlanClient,
            accelerationPlanModelApplicationService,
            new AccelerationArtifactSnapshotService()
        );
    }

    @Autowired
    public AccelerationPlanApplicationService(AccelerationPlanRepository accelerationPlanRepository,
                                              OptimizationTaskRepository optimizationTaskRepository,
                                              GovernanceCapabilityClient governanceCapabilityClient,
                                              QueryExecutionAccelerationPlanClient queryExecutionAccelerationPlanClient,
                                              AccelerationPlanModelApplicationService accelerationPlanModelApplicationService,
                                              AccelerationArtifactSnapshotService accelerationArtifactSnapshotService) {
        this.accelerationPlanRepository = accelerationPlanRepository;
        this.optimizationTaskRepository = optimizationTaskRepository;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.queryExecutionAccelerationPlanClient = queryExecutionAccelerationPlanClient;
        this.accelerationPlanModelApplicationService = accelerationPlanModelApplicationService;
        this.accelerationArtifactSnapshotService = accelerationArtifactSnapshotService;
    }

    public AccelerationPlanSubmitResponse submitPlan(AccelerationPlanSubmitRequest request) {
        long start = System.currentTimeMillis();
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
        OptimizationTask sourceTask = requireEligibleSourceTask(request.getSourceTaskId(), request.getTenantId());
        String planId = UUID.randomUUID().toString();
        try {
            governanceCapabilityClient.assertAuthorization(
                request.getTenantId(),
                sourceTask.getDatasourceType(),
                RESOURCE_TYPE_PLAN,
                planId,
                SUBMIT_OPERATION
            );
            LinkedHashMap<String, String> recommendedPlan = extractRecommendedPlan(sourceTask);
            List<AccelerationSuggestionType> selectedTypes = resolveSelectedTypes(
                request.getSelectedSuggestionTypes(),
                recommendedPlan
            );
            Map<String, Object> accelerationArtifact = extractAccelerationArtifact(sourceTask);
            AccelerationPlan plan = AccelerationPlan.submit(
                planId,
                request.getTenantId(),
                sourceTask.getTaskId(),
                sourceTask.getSqlText(),
                sourceTask.getSqlFingerprint(),
                sourceTask.getDatasourceType(),
                selectedTypes,
                sourceTask.getSuggestion().getSummary(),
                sourceTask.getSuggestion().getPrimaryRecommendation(),
                JsonUtils.toJson(buildPlanPayload(recommendedPlan, selectedTypes, accelerationArtifact)),
                sourceTask.getSuggestion().getBenefits(),
                sourceTask.getSuggestion().getCosts(),
                sourceTask.getSuggestion().getRisks(),
                Instant.now()
            );
            attachTrace(plan, syncTrace(plan));
            accelerationPlanRepository.save(plan);
            AccelerationPlanSubmitResponse response = accelerationPlanModelApplicationService.buildSubmitResponse(plan);
            writeAuditRecord(
                SUBMIT_OPERATION,
                plan,
                plan.getStatus().name(),
                System.currentTimeMillis() - start,
                buildSubmitRequestParams(sourceTask, plan),
                buildStatusResponseSummary(accelerationPlanModelApplicationService.buildStatusResponse(plan), null)
            );
            return response;
        } catch (RuntimeException ex) {
            logFailure(SUBMIT_OPERATION, planId, request.getTenantId(), start, ex);
            throw ex;
        }
    }

    public AccelerationPlanStatusResponse getPlanStatus(String planId) {
        long start = System.currentTimeMillis();
        AccelerationPlan plan = requirePlan(planId);
        verifyTenantAccess(plan.getTenantId());
        try {
            governanceCapabilityClient.assertAuthorization(
                plan.getTenantId(),
                plan.getDatasourceType(),
                RESOURCE_TYPE_PLAN,
                planId,
                QUERY_OPERATION
            );
            AccelerationPlanStatusResponse response = accelerationPlanModelApplicationService.buildStatusResponse(plan);
            writeAuditRecord(
                QUERY_OPERATION,
                plan,
                plan.getStatus().name(),
                System.currentTimeMillis() - start,
                buildPlanQueryRequestParams(plan),
                buildStatusResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            logFailure(QUERY_OPERATION, planId, plan.getTenantId(), start, ex);
            throw ex;
        }
    }

    public AccelerationPlanStatusResponse activatePlan(String planId, AccelerationPlanActionRequest request) {
        long start = System.currentTimeMillis();
        AccelerationPlan plan = requirePlan(planId);
        verifyTenantAccess(plan.getTenantId());
        QueryExecutionAccelerationPlanResponse runtimeResponse = null;
        try {
            governanceCapabilityClient.assertAuthorization(
                plan.getTenantId(),
                plan.getDatasourceType(),
                RESOURCE_TYPE_PLAN,
                planId,
                ACTIVATE_OPERATION
            );
            requireActivateEligible(plan);
            runtimeResponse = queryExecutionAccelerationPlanClient.activate(buildActivationRequest(plan));
            if (runtimeResponse == null || !runtimeResponse.isActive() || !"ACTIVE".equals(runtimeResponse.getStatus())) {
                throw new BizException(
                    ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE,
                    HttpStatus.CONFLICT,
                    "加速方案未在查询执行运行时变为 ACTIVE"
                );
            }
            plan.markActivated(buildRuntimeEvidence(request, runtimeResponse), RequestContext.getUserId(), Instant.now());
            accelerationPlanRepository.save(plan);
            syncTrace(plan);
            AccelerationPlanStatusResponse response = accelerationPlanModelApplicationService.buildStatusResponse(plan);
            writeAuditRecord(
                ACTIVATE_OPERATION,
                plan,
                plan.getStatus().name(),
                System.currentTimeMillis() - start,
                buildActionRequestParams(plan, request),
                buildStatusResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            if (!isPlanStateInvalid(ex)) {
                compensateActivateFailure(plan, request, runtimeResponse, ex);
            }
            logFailure(ACTIVATE_OPERATION, planId, plan.getTenantId(), start, ex);
            throw ex;
        }
    }

    public AccelerationPlanStatusResponse pausePlan(String planId, AccelerationPlanActionRequest request) {
        long start = System.currentTimeMillis();
        AccelerationPlan plan = requirePlan(planId);
        verifyTenantAccess(plan.getTenantId());
        try {
            governanceCapabilityClient.assertAuthorization(
                plan.getTenantId(),
                plan.getDatasourceType(),
                RESOURCE_TYPE_PLAN,
                planId,
                PAUSE_OPERATION
            );
            requirePauseEligible(plan);
            QueryExecutionAccelerationPlanResponse runtimeResponse =
                queryExecutionAccelerationPlanClient.pause(buildPauseRequest(plan));
            if (runtimeResponse != null && "PAUSED".equals(runtimeResponse.getStatus())) {
                plan.markPaused(buildRuntimeEvidence(request, runtimeResponse), RequestContext.getUserId(), Instant.now());
            } else {
                plan.markPauseFailed(
                    Integer.valueOf(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_ROLLBACK_FAILURE),
                    "查询执行运行时未确认暂停完成",
                    buildRuntimeEvidence(request, runtimeResponse),
                    RequestContext.getUserId(),
                    Instant.now()
                );
            }
            syncTrace(plan);
            accelerationPlanRepository.save(plan);
            AccelerationPlanStatusResponse response = accelerationPlanModelApplicationService.buildStatusResponse(plan);
            writeAuditRecord(
                PAUSE_OPERATION,
                plan,
                plan.getStatus().name(),
                System.currentTimeMillis() - start,
                buildActionRequestParams(plan, request),
                buildStatusResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            logFailure(PAUSE_OPERATION, planId, plan.getTenantId(), start, ex);
            throw ex;
        }
    }

    private void compensateActivateFailure(AccelerationPlan plan,
                                        AccelerationPlanActionRequest request,
                                        QueryExecutionAccelerationPlanResponse runtimeResponse,
                                        RuntimeException ex) {
        String compensationPayload = buildRuntimeEvidence(request, runtimeResponse);
        if (runtimeResponse != null && runtimeResponse.isActive()) {
            try {
                QueryExecutionAccelerationPlanResponse pauseResponse =
                    queryExecutionAccelerationPlanClient.pause(buildPauseRequest(plan));
                compensationPayload = buildCompensationEvidence(compensationPayload, pauseResponse);
            } catch (RuntimeException pauseEx) {
                LOGGER.error("操作日志 operation={} entity={} status=COMPENSATION_FAILED reason={}",
                    ACTIVATE_OPERATION, plan.getPlanId(), pauseEx.getMessage(), pauseEx);
            }
        }
        try {
            plan.markActivateFailed(
                Integer.valueOf(resolveErrorCode(ex, ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE)),
                ex.getMessage(),
                compensationPayload,
                RequestContext.getUserId(),
                Instant.now()
            );
            accelerationPlanRepository.save(plan);
            syncTrace(plan);
            writeAuditRecord(
                ACTIVATE_OPERATION,
                plan,
                plan.getStatus().name(),
                0L,
                buildActionRequestParams(plan, request),
                buildStatusResponseSummary(accelerationPlanModelApplicationService.buildStatusResponse(plan), ex.getMessage())
            );
        } catch (RuntimeException persistenceEx) {
            LOGGER.error("操作日志 operation={} entity={} status=FAILURE_PERSISTENCE_FAILED reason={}",
                ACTIVATE_OPERATION, plan.getPlanId(), persistenceEx.getMessage(), persistenceEx);
        }
    }

    private OptimizationTask requireEligibleSourceTask(String taskId, String tenantId) {
        OptimizationTask task = optimizationTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "优化任务不存在，taskId=" + taskId
            );
        }
        if (!tenantId.equals(task.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权访问该优化任务");
        }
        if (task.getTaskType() != OptimizationTaskType.ACCELERATION_SUGGESTION
            || task.getStatus() != OptimizationTaskStatus.SUCCEEDED
            || task.getSuggestion() == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SUGGESTION_NOT_READY,
                HttpStatus.CONFLICT,
                "加速计划只能从已成功的 ACCELERATION_SUGGESTION 任务创建"
            );
        }
        return task;
    }

    private AccelerationPlan requirePlan(String planId) {
        AccelerationPlan plan = accelerationPlanRepository.findByPlanId(planId);
        if (plan == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_ACCELERATION_PLAN_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "加速方案不存在，planId=" + planId
            );
        }
        return plan;
    }

    private LinkedHashMap<String, String> extractRecommendedPlan(OptimizationTask sourceTask) {
        for (OptimizationTaskArtifact artifact : sourceTask.getSuggestion().getArtifacts()) {
            if ("ACCELERATION_PLAN".equals(artifact.getCategory())) {
                return JsonUtils.fromJson(artifact.getContent(), LinkedHashMap.class);
            }
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SUGGESTION_NOT_READY,
            HttpStatus.CONFLICT,
            "加速建议载荷不包含 ACCELERATION_PLAN 产物"
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAccelerationArtifact(OptimizationTask sourceTask) {
        for (OptimizationTaskArtifact artifact : sourceTask.getSuggestion().getArtifacts()) {
            if ("ACCELERATION_ARTIFACT".equals(artifact.getCategory())) {
                return accelerationArtifactSnapshotService.sanitizeForResponse(
                    JsonUtils.fromJson(artifact.getContent(), LinkedHashMap.class)
                );
            }
        }
        return Collections.emptyMap();
    }

    private List<AccelerationSuggestionType> resolveSelectedTypes(List<AccelerationSuggestionType> requestedTypes,
                                                                  LinkedHashMap<String, String> recommendedPlan) {
        List<AccelerationSuggestionType> availableTypes = new ArrayList<AccelerationSuggestionType>();
        for (String key : recommendedPlan.keySet()) {
            availableTypes.add(AccelerationSuggestionType.valueOf(key));
        }
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return availableTypes;
        }
        for (AccelerationSuggestionType type : requestedTypes) {
            if (!availableTypes.contains(type)) {
                throw new BizException(
                    ErrorCodeConstants.SQL_OPTIMIZATION_ACCELERATION_PLAN_INVALID,
                    HttpStatus.BAD_REQUEST,
                    "请求的建议类型 " + type.name() + " 未出现在来源加速推荐中"
                );
            }
        }
        return requestedTypes;
    }

    private LinkedHashMap<String, String> filterPlanPayload(LinkedHashMap<String, String> recommendedPlan,
                                                            List<AccelerationSuggestionType> selectedTypes) {
        LinkedHashMap<String, String> filtered = new LinkedHashMap<String, String>();
        for (AccelerationSuggestionType type : selectedTypes) {
            filtered.put(type.name(), recommendedPlan.get(type.name()));
        }
        return filtered;
    }

    private Map<String, Object> buildPlanPayload(LinkedHashMap<String, String> recommendedPlan,
                                                 List<AccelerationSuggestionType> selectedTypes,
                                                 Map<String, Object> accelerationArtifact) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("recommendedTypes", filterPlanPayload(recommendedPlan, selectedTypes));
        if (selectedTypes.contains(AccelerationSuggestionType.PRECOMPUTE)
            && accelerationArtifact != null
            && !accelerationArtifact.isEmpty()) {
            payload.put("accelerationArtifact", accelerationArtifact);
        }
        return payload;
    }

    private GovernanceAccelerationPlanTraceResponse syncTrace(AccelerationPlan plan) {
        return governanceCapabilityClient.writeAccelerationPlanTrace(buildTraceRequest(plan));
    }

    private GovernanceAccelerationPlanTraceRequest buildTraceRequest(AccelerationPlan plan) {
        GovernanceAccelerationPlanTraceRequest request = new GovernanceAccelerationPlanTraceRequest();
        request.setPlanId(plan.getPlanId());
        request.setSourceTaskId(plan.getSourceTaskId());
        request.setSqlFingerprint(plan.getSqlFingerprint());
        request.setDatasourceType(plan.getDatasourceType().name());
        request.setSqlText(plan.getSqlText());
        request.setPlanStatus(plan.getStatus().name());
        request.setSnapshotPayloadJson(buildTraceSnapshotPayload(plan));
        request.setResultSummaryJson(buildTraceResultSummary(plan));
        request.setResultPayloadJson(buildTraceResultPayload(plan));
        request.setQueryContextJson(buildTraceQueryContext(plan));
        request.setCreatedAt(plan.getCreatedAt().toString());
        request.setUpdatedAt(plan.getUpdatedAt().toString());
        request.setErrorCode(plan.getLastErrorCode());
        request.setErrorMessage(plan.getLastErrorMessage());
        return request;
    }

    private void attachTrace(AccelerationPlan plan, GovernanceAccelerationPlanTraceResponse traceResponse) {
        plan.attachGovernanceTrace(
            traceResponse.getConfigSnapshotId(),
            traceResponse.getResultId(),
            traceResponse.getHistoryId()
        );
    }

    private String buildTraceSnapshotPayload(AccelerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("planId", plan.getPlanId());
        payload.put("sourceTaskId", plan.getSourceTaskId());
        payload.put("sqlFingerprint", plan.getSqlFingerprint());
        payload.put("datasourceType", plan.getDatasourceType().name());
        payload.put("selectedSuggestionTypes", plan.getSelectedSuggestionTypes());
        payload.put("planSummary", plan.getPlanSummary());
        payload.put("primaryRecommendation", plan.getPrimaryRecommendation());
        payload.put("planPayload", JsonUtils.fromJson(plan.getPlanPayloadJson(), Map.class));
        return JsonUtils.toJson(payload);
    }

    private String buildTraceResultSummary(AccelerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("planId", plan.getPlanId());
        payload.put("status", plan.getStatus().name());
        payload.put("selectedSuggestionTypeCount", Integer.valueOf(plan.getSelectedSuggestionTypes().size()));
        payload.put("lastErrorCode", plan.getLastErrorCode());
        payload.put("activatedAt", plan.getActivatedAt());
        payload.put("pausedAt", plan.getPausedAt());
        return JsonUtils.toJson(payload);
    }

    private String buildTraceResultPayload(AccelerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("planPayload", JsonUtils.fromJson(plan.getPlanPayloadJson(), Map.class));
        payload.put("activationEvidence", parseJsonString(plan.getActivationEvidenceJson()));
        payload.put("pauseEvidence", parseJsonString(plan.getPauseEvidenceJson()));
        return JsonUtils.toJson(payload);
    }

    private String buildTraceQueryContext(AccelerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("planId", plan.getPlanId());
        payload.put("sourceTaskId", plan.getSourceTaskId());
        payload.put("status", plan.getStatus().name());
        payload.put("selectedSuggestionTypes", plan.getSelectedSuggestionTypes());
        payload.put("activatedBy", plan.getActivatedBy());
        payload.put("pausedBy", plan.getPausedBy());
        return JsonUtils.toJson(payload);
    }

    private QueryExecutionAccelerationPlanActivationRequest buildActivationRequest(AccelerationPlan plan) {
        QueryExecutionAccelerationPlanActivationRequest request = new QueryExecutionAccelerationPlanActivationRequest();
        request.setTenantId(plan.getTenantId());
        request.setPlanId(plan.getPlanId());
        request.setSqlFingerprint(plan.getSqlFingerprint());
        request.setDatasourceType(plan.getDatasourceType().name());
        request.setSelectedSuggestionTypes(toSuggestionTypeNames(plan.getSelectedSuggestionTypes()));
        request.setPlanSummary(plan.getPlanSummary());
        request.setPrimaryRecommendation(plan.getPrimaryRecommendation());
        return request;
    }

    private QueryExecutionAccelerationPlanPauseRequest buildPauseRequest(AccelerationPlan plan) {
        QueryExecutionAccelerationPlanPauseRequest request = new QueryExecutionAccelerationPlanPauseRequest();
        request.setTenantId(plan.getTenantId());
        request.setPlanId(plan.getPlanId());
        request.setSqlFingerprint(plan.getSqlFingerprint());
        return request;
    }

    private List<String> toSuggestionTypeNames(List<AccelerationSuggestionType> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<String>(types.size());
        for (AccelerationSuggestionType type : types) {
            names.add(type.name());
        }
        return names;
    }

    private String buildRuntimeEvidence(AccelerationPlanActionRequest actionRequest,
                                        QueryExecutionAccelerationPlanResponse runtimeResponse) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("reason", actionRequest == null ? null : actionRequest.getReason());
        payload.put("runtimeStatus", runtimeResponse == null ? null : runtimeResponse.getStatus());
        payload.put("active", runtimeResponse != null && runtimeResponse.isActive());
        payload.put("targetEngine", runtimeResponse == null ? null : runtimeResponse.getTargetEngine());
        payload.put("runtimeSummary", runtimeResponse == null ? null : runtimeResponse.getRuntimeSummary());
        payload.put("runtimeDetails", runtimeResponse == null ? null : parseJsonString(runtimeResponse.getRuntimeDetailsJson()));
        return JsonUtils.toJson(payload);
    }

    private String buildCompensationEvidence(String activationEvidence, QueryExecutionAccelerationPlanResponse pauseResponse) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("activationEvidence", parseJsonString(activationEvidence));
        payload.put("compensationStatus", pauseResponse == null ? null : pauseResponse.getStatus());
        payload.put("compensationSummary", pauseResponse == null ? null : pauseResponse.getRuntimeSummary());
        payload.put("compensationDetails", pauseResponse == null ? null : parseJsonString(pauseResponse.getRuntimeDetailsJson()));
        return JsonUtils.toJson(payload);
    }

    private Object parseJsonString(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtils.fromJson(json, Map.class);
        } catch (RuntimeException ex) {
            return json;
        }
    }

    private int resolveErrorCode(RuntimeException ex, int fallback) {
        if (ex instanceof BizException) {
            return ((BizException) ex).getCode();
        }
        return fallback;
    }

    private boolean isPlanStateInvalid(RuntimeException ex) {
        return ex instanceof BizException
            && ((BizException) ex).getCode() == ErrorCodeConstants.SQL_OPTIMIZATION_ACCELERATION_PLAN_STATE_INVALID;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("当前认证租户无权访问该加速方案");
        }
    }

    private void requireActivateEligible(AccelerationPlan plan) {
        if (plan.getStatus() == com.company.sqloptimization.domain.plan.AccelerationPlanStatus.READY
            || plan.getStatus() == com.company.sqloptimization.domain.plan.AccelerationPlanStatus.ACTIVATE_FAILED
            || plan.getStatus() == com.company.sqloptimization.domain.plan.AccelerationPlanStatus.PAUSED) {
            return;
        }
        throw invalidPlanState("加速方案激活前必须处于 READY、ACTIVATE_FAILED 或 PAUSED 状态。");
    }

    private void requirePauseEligible(AccelerationPlan plan) {
        if (plan.getStatus() == com.company.sqloptimization.domain.plan.AccelerationPlanStatus.ACTIVE
            || plan.getStatus() == com.company.sqloptimization.domain.plan.AccelerationPlanStatus.PAUSED
            || plan.getStatus() == com.company.sqloptimization.domain.plan.AccelerationPlanStatus.PAUSE_FAILED) {
            return;
        }
        throw invalidPlanState("加速方案暂停前必须处于 ACTIVE、PAUSED 或 PAUSE_FAILED 状态。");
    }

    private BizException invalidPlanState(String message) {
        return new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_ACCELERATION_PLAN_STATE_INVALID,
            HttpStatus.CONFLICT,
            message
        );
    }

    private String buildSubmitRequestParams(OptimizationTask sourceTask, AccelerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", plan.getTenantId());
        payload.put("sourceTaskId", sourceTask.getTaskId());
        payload.put("planId", plan.getPlanId());
        payload.put("sqlFingerprint", plan.getSqlFingerprint());
        payload.put("datasourceType", plan.getDatasourceType().name());
        payload.put("selectedSuggestionTypes", plan.getSelectedSuggestionTypes());
        return JsonUtils.toJson(payload);
    }

    private String buildPlanQueryRequestParams(AccelerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", plan.getTenantId());
        payload.put("planId", plan.getPlanId());
        payload.put("status", plan.getStatus().name());
        return JsonUtils.toJson(payload);
    }

    private String buildActionRequestParams(AccelerationPlan plan, AccelerationPlanActionRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.SQL_OPTIMIZATION);
        payload.put("tenantId", plan.getTenantId());
        payload.put("planId", plan.getPlanId());
        payload.put("reason", request == null ? null : request.getReason());
        return JsonUtils.toJson(payload);
    }

    private String buildStatusResponseSummary(AccelerationPlanStatusResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? null : response.getStatus().name());
        payload.put("configSnapshotId", response == null ? null : response.getConfigSnapshotId());
        payload.put("resultId", response == null ? null : response.getResultId());
        payload.put("historyId", response == null ? null : response.getHistoryId());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private void writeAuditRecord(String operationCode,
                                  AccelerationPlan plan,
                                  String resultStatus,
                                  long elapsedMs,
                                  String requestParams,
                                  String responseSummary) {
        governanceCapabilityClient.writeAudit(
            new OptimizationAuditRecord(
                operationCode,
                RESOURCE_TYPE_PLAN,
                plan.getPlanId(),
                resultStatus,
                elapsedMs,
                requestParams,
                responseSummary,
                "acceleration-plan-" + plan.getPlanId(),
                plan.getConfigSnapshotId(),
                plan.getResultId(),
                plan.getHistoryId()
            )
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
}
