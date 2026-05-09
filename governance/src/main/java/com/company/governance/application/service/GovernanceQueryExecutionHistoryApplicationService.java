package com.company.governance.application.service;

import com.company.governance.domain.trace.entity.AuditLogRecord;
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
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GovernanceQueryExecutionHistoryApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "QUERY_EXECUTION_HISTORY_PERSISTENCE_BASELINE";
    private static final String HISTORY_TYPE = "QUERY_EXECUTION";
    private static final String OPERATION = "QUERY_EXECUTE_SYNC";
    private static final String TARGET_TYPE = "QUERY_EXECUTION_QUERY";
    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final GovernanceProtectedPersistenceService protectedPersistenceService;
    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;

    public GovernanceQueryExecutionHistoryApplicationService(
        GovernanceProtectedPersistenceService protectedPersistenceService,
        ConfigSnapshotMapper configSnapshotMapper,
        ExecutionResultMapper executionResultMapper,
        QueryHistoryMapper queryHistoryMapper
    ) {
        this.protectedPersistenceService = protectedPersistenceService;
        this.configSnapshotMapper = configSnapshotMapper;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
    }

    @Transactional
    public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
        GovernanceQueryExecutionHistoryWriteRequest request
    ) {
        if (request == null) {
            throw invalidArgument("request must not be null");
        }
        String contextTenantId = requireContext("tenantId", RequestContext.getTenantId());
        String contextUserId = requireContext("userId", RequestContext.getUserId());
        String contextRequestId = requireContext("requestId", RequestContext.getRequestId());
        String contextTraceId = requireContext("traceId", RequestContext.getTraceId());
        String tenantId = requireText(request.getTenantId(), "tenantId");
        if (!contextTenantId.equals(tenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "request tenantId does not match protected tenant context"
            );
        }
        if (!HISTORY_TYPE.equals(requireText(request.getHistoryType(), "historyType"))) {
            throw invalidArgument("historyType must be QUERY_EXECUTION");
        }
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        String resultStatus = requireText(request.getResultStatus(), "resultStatus");
        String datasourceType = requireText(request.getDatasourceType(), "datasourceType");
        String stableKey = stableHash(
            firstText(request.getRequestId(), contextRequestId),
            firstText(request.getTraceId(), contextTraceId),
            tenantId,
            sqlFingerprint
        );
        String configSnapshotId = firstText(request.getConfigSnapshotId(), "cfg-qe-" + stableKey);
        String resultId = firstText(request.getResultId(), "result-qe-" + stableKey);
        String historyId = firstText(request.getHistoryId(), "history-qe-" + stableKey);
        String requestId = firstText(request.getRequestId(), contextRequestId);
        String traceId = firstText(request.getTraceId(), contextTraceId);
        String sagaId = firstText(request.getSagaId(), "query-execution-" + stableKey);
        String submittedBy = firstText(request.getSubmittedBy(), contextUserId);
        Instant finishedAt = parseInstant(request.getFinishedAt(), Instant.now());
        Instant startedAt = parseInstant(request.getStartedAt(), finishedAt);

        saveConfigSnapshotIfMissing(request, tenantId, submittedBy, configSnapshotId, requestId, traceId, sagaId, startedAt);
        saveOrUpdateExecutionResult(
            request,
            tenantId,
            resultId,
            configSnapshotId,
            resultStatus,
            requestId,
            traceId,
            sagaId,
            startedAt,
            finishedAt
        );
        saveOrUpdateQueryHistory(
            request,
            tenantId,
            historyId,
            resultId,
            sqlFingerprint,
            datasourceType,
            requestId,
            traceId,
            sagaId,
            submittedBy,
            startedAt
        );
        AuditLogRecord audit = saveAuditLog(
            request,
            tenantId,
            configSnapshotId,
            resultId,
            historyId,
            sqlFingerprint,
            resultStatus,
            requestId,
            traceId,
            sagaId,
            finishedAt
        );

        GovernanceQueryExecutionHistoryWriteResponse response =
            new GovernanceQueryExecutionHistoryWriteResponse();
        response.setConfigSnapshotId(configSnapshotId);
        response.setResultId(resultId);
        response.setHistoryId(historyId);
        response.setAuditId(audit.getId());
        response.setTraceId(traceId);
        response.setRequestId(requestId);
        response.setSagaId(sagaId);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private void saveConfigSnapshotIfMissing(GovernanceQueryExecutionHistoryWriteRequest request,
                                             String tenantId,
                                             String submittedBy,
                                             String configSnapshotId,
                                             String requestId,
                                             String traceId,
                                             String sagaId,
                                             Instant createdAt) {
        if (configSnapshotMapper.selectById(configSnapshotId) != null) {
            return;
        }
        ConfigSnapshotRecord record = new ConfigSnapshotRecord();
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.QUERY_EXECUTION);
        record.setSourceConfigType(HISTORY_TYPE);
        record.setSourceConfigId(firstText(request.getRequestId(), request.getHistoryId(), request.getSqlFingerprint()));
        record.setSourceVersion(request.getStartedAt());
        record.setSnapshotStatus("CAPTURED");
        record.setSnapshotReason("Query execution history persistence");
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setSnapshotPayload(toJson(snapshotPayload(request)));
        record.setCreatedBy(submittedBy);
        record.setCreateTime(toDatabaseTime(createdAt));
        protectedPersistenceService.saveConfigSnapshot(record);
    }

    private void saveOrUpdateExecutionResult(GovernanceQueryExecutionHistoryWriteRequest request,
                                             String tenantId,
                                             String resultId,
                                             String configSnapshotId,
                                             String resultStatus,
                                             String requestId,
                                             String traceId,
                                             String sagaId,
                                             Instant startedAt,
                                             Instant finishedAt) {
        ExecutionResultRecord record = new ExecutionResultRecord();
        record.setResultId(resultId);
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.QUERY_EXECUTION);
        record.setTaskId(firstText(requestId, request.getHistoryId(), request.getSqlFingerprint()));
        record.setTaskType(HISTORY_TYPE);
        record.setResultStatus(resultStatus);
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setAccessChannel(request.getAccessChannel());
        record.setTargetEngine(request.getTargetEngine());
        record.setReturnedRowCount(request.getReturnedRowCount());
        record.setCacheHit(request.getCacheHit());
        record.setRewriteApplied(request.getRewriteApplied());
        record.setAccelerationApplied(request.getAccelerationApplied());
        record.setHitTableSummary(request.getLogicalObjectHits());
        record.setRouteSummary(request.getRouteSummary());
        record.setCacheSummary(request.getCacheSummary());
        record.setResultSummary(toJson(resultSummary(request)));
        record.setResultPayload(toJson(resultPayload(request)));
        record.setErrorCode(request.getErrorCode() == null ? null : String.valueOf(request.getErrorCode()));
        record.setErrorMessage(request.getErrorMessage());
        record.setStartedAt(toDatabaseTime(startedAt));
        record.setFinishedAt(toDatabaseTime(finishedAt));
        record.setCreateTime(toDatabaseTime(startedAt));
        if (executionResultMapper.selectById(resultId) == null) {
            protectedPersistenceService.saveExecutionResult(record);
            return;
        }
        protectedPersistenceService.updateExecutionResult(record);
    }

    private void saveOrUpdateQueryHistory(GovernanceQueryExecutionHistoryWriteRequest request,
                                          String tenantId,
                                          String historyId,
                                          String resultId,
                                          String sqlFingerprint,
                                          String datasourceType,
                                          String requestId,
                                          String traceId,
                                          String sagaId,
                                          String submittedBy,
                                          Instant submittedAt) {
        Map<String, Object> queryDateSummary = parseJsonMap(request.getQueryDateSummary());
        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setHistoryId(historyId);
        record.setResultId(resultId);
        record.setTenantId(tenantId);
        record.setHistoryType(HISTORY_TYPE);
        record.setSqlFingerprint(sqlFingerprint);
        record.setDatasourceCode(request.getDatasourceCode());
        record.setDatasourceType(datasourceType);
        record.setQueryDateStart(readDate(queryDateSummary, "queryDateStart"));
        record.setQueryDateEnd(readDate(queryDateSummary, "queryDateEnd"));
        record.setQueryDateStatus(readText(queryDateSummary, "queryDateStatus"));
        record.setAccessChannel(request.getAccessChannel());
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setCommentContext(request.getCommentContext());
        record.setBindingSummary(request.getBindingSummary());
        record.setLogicalObjectHits(request.getLogicalObjectHits());
        record.setRouteSummary(request.getRouteSummary());
        record.setCacheSummary(request.getCacheSummary());
        record.setQueryContext(request.getQueryContext());
        record.setSubmittedBy(submittedBy);
        record.setSubmittedAt(toDatabaseTime(submittedAt));
        record.setCreateTime(toDatabaseTime(submittedAt));
        if (queryHistoryMapper.selectById(historyId) == null) {
            protectedPersistenceService.saveQueryHistoryWithSqlSurfaces(
                record,
                request.getSqlText(),
                request.getSqlTemplate(),
                request.getBoundSql()
            );
            return;
        }
        protectedPersistenceService.updateQueryHistoryWithSqlSurfaces(
            record,
            request.getSqlText(),
            request.getSqlTemplate(),
            request.getBoundSql()
        );
    }

    private AuditLogRecord saveAuditLog(GovernanceQueryExecutionHistoryWriteRequest request,
                                        String tenantId,
                                        String configSnapshotId,
                                        String resultId,
                                        String historyId,
                                        String sqlFingerprint,
                                        String resultStatus,
                                        String requestId,
                                        String traceId,
                                        String sagaId,
                                        Instant createdAt) {
        AuditLogRecord record = new AuditLogRecord();
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.QUERY_EXECUTION);
        record.setOperationType(OPERATION);
        record.setTargetType(TARGET_TYPE);
        record.setTargetId(sqlFingerprint);
        record.setRequestId(requestId);
        record.setTraceId(traceId);
        record.setSagaId(sagaId);
        record.setConfigSnapshotId(configSnapshotId);
        record.setResultId(resultId);
        record.setHistoryId(historyId);
        record.setRequestParams(toJson(auditRequestParams(request)));
        record.setResponseSummary(toJson(auditResponseSummary(request)));
        record.setStatus(resultStatus);
        record.setCostMs(request.getElapsedMs());
        record.setCreateTime(toDatabaseTime(createdAt));
        protectedPersistenceService.saveAuditLog(record);
        return record;
    }

    private Map<String, Object> snapshotPayload(GovernanceQueryExecutionHistoryWriteRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("historyType", request.getHistoryType());
        payload.put("sqlFingerprint", request.getSqlFingerprint());
        payload.put("datasourceCode", request.getDatasourceCode());
        payload.put("datasourceType", request.getDatasourceType());
        payload.put("targetEngine", request.getTargetEngine());
        payload.put("accessChannel", request.getAccessChannel());
        payload.put("queryContext", parseJsonMap(request.getQueryContext()));
        return payload;
    }

    private Map<String, Object> resultSummary(GovernanceQueryExecutionHistoryWriteRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", request.getResultStatus());
        payload.put("targetEngine", request.getTargetEngine());
        payload.put("returnedRowCount", request.getReturnedRowCount());
        payload.put("cacheHit", request.getCacheHit());
        payload.put("rewriteApplied", request.getRewriteApplied());
        payload.put("accelerationApplied", request.getAccelerationApplied());
        payload.put("elapsedMs", request.getElapsedMs());
        payload.put("errorCode", request.getErrorCode());
        payload.put("errorMessage", request.getErrorMessage());
        payload.put("routeSummary", parseJsonMap(request.getRouteSummary()));
        payload.put("cacheSummary", parseJsonMap(request.getCacheSummary()));
        return payload;
    }

    private Map<String, Object> resultPayload(GovernanceQueryExecutionHistoryWriteRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("commentContext", parseJsonMap(request.getCommentContext()));
        payload.put("queryDateSummary", parseJsonMap(request.getQueryDateSummary()));
        payload.put("bindingSummary", parseJsonMap(request.getBindingSummary()));
        payload.put("logicalObjectHits", parseJsonValue(request.getLogicalObjectHits()));
        payload.put("queryContext", parseJsonMap(request.getQueryContext()));
        return payload;
    }

    private Map<String, Object> auditRequestParams(GovernanceQueryExecutionHistoryWriteRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tenantId", request.getTenantId());
        payload.put("historyType", request.getHistoryType());
        payload.put("sqlFingerprint", request.getSqlFingerprint());
        payload.put("datasourceCode", request.getDatasourceCode());
        payload.put("datasourceType", request.getDatasourceType());
        payload.put("targetEngine", request.getTargetEngine());
        payload.put("accessChannel", request.getAccessChannel());
        payload.put("traceId", firstText(request.getTraceId(), RequestContext.getTraceId()));
        payload.put("requestId", firstText(request.getRequestId(), RequestContext.getRequestId()));
        return payload;
    }

    private Map<String, Object> auditResponseSummary(GovernanceQueryExecutionHistoryWriteRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", request.getResultStatus());
        payload.put("returnedRowCount", request.getReturnedRowCount());
        payload.put("cacheHit", request.getCacheHit());
        payload.put("rewriteApplied", request.getRewriteApplied());
        payload.put("accelerationApplied", request.getAccelerationApplied());
        payload.put("elapsedMs", request.getElapsedMs());
        payload.put("errorCode", request.getErrorCode());
        payload.put("errorMessage", request.getErrorMessage());
        return payload;
    }

    private Map<String, Object> parseJsonMap(String json) {
        Object value = parseJsonValue(json);
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return Collections.emptyMap();
    }

    private Object parseJsonValue(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtils.objectMapper().readValue(json, Object.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate readDate(Map<String, Object> source, String fieldName) {
        String value = readText(source, fieldName);
        return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
    }

    private String readText(Map<String, Object> source, String fieldName) {
        Object value = source == null ? null : source.get(fieldName);
        return value == null ? null : String.valueOf(value);
    }

    private Instant parseInstant(String value, Instant fallback) {
        return StringUtils.hasText(value) ? Instant.parse(value.trim()) : fallback;
    }

    private LocalDateTime toDatabaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private String toJson(Object payload) {
        return payload == null ? null : JsonUtils.toJson(payload);
    }

    private String requireContext(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Protected request context is missing " + fieldName
            );
        }
        return value.trim();
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw invalidArgument(fieldName + " must not be empty");
        }
        return value.trim();
    }

    private BizException invalidArgument(String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, message);
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String stableHash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String part : parts) {
                    digest.update((part == null ? "" : part).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) '|');
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i += 1) {
                builder.append(String.format("%02x", Integer.valueOf(hash[i] & 0xff)));
            }
            return builder.toString();
        } catch (Exception ex) {
            return String.valueOf(Math.abs(join(parts).hashCode()));
        }
    }

    private String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                builder.append(part == null ? "" : part).append('|');
            }
        }
        return builder.toString();
    }
}
