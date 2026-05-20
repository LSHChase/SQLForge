package com.company.governance.application.service;

import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceAccelerationPlanTraceabilityApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "ACCELERATION_PLAN_TRACEABILITY_BASELINE";
    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;

    public GovernanceAccelerationPlanTraceabilityApplicationService(
        GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
        ConfigSnapshotMapper configSnapshotMapper,
        ExecutionResultMapper executionResultMapper,
        QueryHistoryMapper queryHistoryMapper
    ) {
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
        this.configSnapshotMapper = configSnapshotMapper;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
    }

    public GovernanceAccelerationPlanTraceResponse writeAccelerationPlanTrace(
        GovernanceAccelerationPlanTraceRequest request
    ) {
        if (request == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "request 不能为 null"
            );
        }
        String tenantId = requireContext("tenantId", RequestContext.getTenantId());
        String userId = requireContext("userId", RequestContext.getUserId());
        String requestId = requireContext("requestId", RequestContext.getRequestId());
        String traceId = requireContext("traceId", RequestContext.getTraceId());
        String planId = requireText(request.getPlanId(), "planId");
        requireText(request.getSourceTaskId(), "sourceTaskId");
        requireText(request.getSqlFingerprint(), "sqlFingerprint");
        requireText(request.getDatasourceType(), "datasourceType");
        requireText(request.getPlanStatus(), "planStatus");

        String configSnapshotId = "cfg-acceleration-plan-" + sanitizeKey(planId);
        String resultId = "result-acceleration-plan-" + sanitizeKey(planId);
        String historyId = "history-acceleration-plan-" + sanitizeKey(planId);
        String sagaId = "acceleration-plan-" + sanitizeKey(planId);
        Instant createdAt = parseInstant(request.getCreatedAt(), Instant.now());
        Instant updatedAt = parseInstant(request.getUpdatedAt(), createdAt);

        persistConfigSnapshotIfMissing(
            request,
            tenantId,
            userId,
            requestId,
            traceId,
            sagaId,
            configSnapshotId,
            createdAt
        );
        persistOrUpdateExecutionResult(
            request,
            tenantId,
            requestId,
            traceId,
            sagaId,
            configSnapshotId,
            resultId,
            createdAt,
            updatedAt
        );
        persistQueryHistoryIfMissing(
            request,
            tenantId,
            userId,
            requestId,
            traceId,
            sagaId,
            historyId,
            resultId,
            createdAt
        );

        GovernanceAccelerationPlanTraceResponse response = new GovernanceAccelerationPlanTraceResponse();
        response.setConfigSnapshotId(configSnapshotId);
        response.setResultId(resultId);
        response.setHistoryId(historyId);
        response.setTraceId(traceId);
        response.setRequestId(requestId);
        response.setSagaId(sagaId);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private void persistConfigSnapshotIfMissing(GovernanceAccelerationPlanTraceRequest request,
                                                String tenantId,
                                                String userId,
                                                String requestId,
                                                String traceId,
                                                String sagaId,
                                                String configSnapshotId,
                                                Instant createdAt) {
        if (configSnapshotMapper.selectById(configSnapshotId) != null) {
            return;
        }
        ConfigSnapshotRecord record = new ConfigSnapshotRecord();
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.SQL_OPTIMIZATION);
        record.setSourceConfigType("ACCELERATION_PLAN");
        record.setSourceConfigId(request.getPlanId());
        record.setSourceVersion(request.getCreatedAt());
        record.setSnapshotStatus("CAPTURED");
        record.setSnapshotReason("加速方案治理基线");
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setSnapshotPayload(request.getSnapshotPayloadJson());
        record.setCreatedBy(userId);
        record.setCreateTime(toDatabaseTime(createdAt));
        governanceProtectedPersistenceService.saveConfigSnapshot(record);
    }

    private void persistOrUpdateExecutionResult(GovernanceAccelerationPlanTraceRequest request,
                                                String tenantId,
                                                String requestId,
                                                String traceId,
                                                String sagaId,
                                                String configSnapshotId,
                                                String resultId,
                                                Instant createdAt,
                                                Instant updatedAt) {
        ExecutionResultRecord record = new ExecutionResultRecord();
        record.setResultId(resultId);
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.SQL_OPTIMIZATION);
        record.setTaskId(request.getPlanId());
        record.setTaskType("ACCELERATION_PLAN");
        record.setResultStatus(request.getPlanStatus());
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setResultSummary(request.getResultSummaryJson());
        record.setResultPayload(request.getResultPayloadJson());
        record.setErrorCode(request.getErrorCode() == null ? null : String.valueOf(request.getErrorCode()));
        record.setErrorMessage(request.getErrorMessage());
        record.setStartedAt(toDatabaseTime(createdAt));
        record.setFinishedAt(isTerminalStatus(request.getPlanStatus()) ? toDatabaseTime(updatedAt) : null);
        record.setCreateTime(toDatabaseTime(createdAt));
        if (executionResultMapper.selectById(resultId) == null) {
            governanceProtectedPersistenceService.saveExecutionResult(record);
            return;
        }
        governanceProtectedPersistenceService.updateExecutionResult(record);
    }

    private void persistQueryHistoryIfMissing(GovernanceAccelerationPlanTraceRequest request,
                                              String tenantId,
                                              String userId,
                                              String requestId,
                                              String traceId,
                                              String sagaId,
                                              String historyId,
                                              String resultId,
                                              Instant createdAt) {
        if (queryHistoryMapper.selectById(historyId) != null) {
            return;
        }
        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setHistoryId(historyId);
        record.setResultId(resultId);
        record.setTenantId(tenantId);
        record.setHistoryType("ACCELERATION_PLAN");
        record.setSqlFingerprint(request.getSqlFingerprint());
        record.setDatasourceType(request.getDatasourceType());
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setQueryContext(request.getQueryContextJson());
        record.setSubmittedBy(userId);
        record.setSubmittedAt(toDatabaseTime(createdAt));
        record.setCreateTime(toDatabaseTime(createdAt));
        governanceProtectedPersistenceService.saveQueryHistoryWithSqlText(record, request.getSqlText());
    }

    private boolean isTerminalStatus(String status) {
        return "ACTIVE".equals(status)
            || "PAUSED".equals(status)
            || "ACTIVATE_FAILED".equals(status)
            || "PAUSE_FAILED".equals(status);
    }

    private String requireContext(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "受保护请求上下文缺失：" + fieldName
            );
        }
        return value;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return value.trim();
    }

    private String sanitizeKey(String value) {
        return requireText(value, "planId").replaceAll("[^A-Za-z0-9]+", "-");
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return Instant.parse(value.trim());
    }

    private LocalDateTime toDatabaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }
}
