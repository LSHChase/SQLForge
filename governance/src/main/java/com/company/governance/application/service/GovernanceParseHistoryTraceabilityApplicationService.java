package com.company.governance.application.service;

import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceParseHistoryTraceabilityApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "PARSE_HISTORY_TRACEABILITY_BASELINE";
    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;

    public GovernanceParseHistoryTraceabilityApplicationService(
        GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
        ExecutionResultMapper executionResultMapper,
        QueryHistoryMapper queryHistoryMapper
    ) {
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
    }

    public GovernanceParseHistoryWriteResponse writeParseHistory(GovernanceParseHistoryWriteRequest request) {
        if (request == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "request must not be null"
            );
        }
        String tenantId = requireContext("tenantId", RequestContext.getTenantId());
        String userId = requireContext("userId", RequestContext.getUserId());
        String requestId = requireContext("requestId", RequestContext.getRequestId());
        String traceId = requireContext("traceId", RequestContext.getTraceId());
        String parseTaskId = requireText(request.getParseTaskId(), "parseTaskId");
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        String resultStatus = requireText(request.getResultStatus(), "resultStatus");
        String sanitizedTaskId = sanitizeKey(parseTaskId);
        String resultId = "result-parse-" + sanitizedTaskId;
        String historyId = "history-parse-" + sanitizedTaskId;
        String sagaId = "parse-" + sanitizedTaskId;
        Instant submittedAt = parseInstant(request.getSubmittedAt(), Instant.now());

        persistOrUpdateExecutionResult(request, tenantId, requestId, traceId, sagaId, resultId, submittedAt, resultStatus);
        persistQueryHistoryIfMissing(request, tenantId, userId, requestId, traceId, sagaId, historyId, resultId, submittedAt, sqlFingerprint);

        GovernanceParseHistoryWriteResponse response = new GovernanceParseHistoryWriteResponse();
        response.setHistoryId(historyId);
        response.setResultId(resultId);
        response.setTraceId(traceId);
        response.setRequestId(requestId);
        response.setSagaId(sagaId);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private void persistOrUpdateExecutionResult(GovernanceParseHistoryWriteRequest request,
                                                String tenantId,
                                                String requestId,
                                                String traceId,
                                                String sagaId,
                                                String resultId,
                                                Instant submittedAt,
                                                String resultStatus) {
        ExecutionResultRecord record = new ExecutionResultRecord();
        record.setResultId(resultId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.SQL_OPTIMIZATION);
        record.setTaskId(request.getParseTaskId());
        record.setTaskType("SQL_PARSE");
        record.setResultStatus(resultStatus);
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setAccessChannel("PAGE");
        record.setTargetEngine(request.getDatasourceType());
        record.setResultSummary(request.getResultSummaryJson());
        record.setResultPayload(request.getResultPayloadJson());
        record.setStartedAt(toDatabaseTime(submittedAt));
        record.setFinishedAt(toDatabaseTime(submittedAt));
        record.setCreateTime(toDatabaseTime(submittedAt));
        if (executionResultMapper.selectById(resultId) == null) {
            governanceProtectedPersistenceService.saveExecutionResult(record);
            return;
        }
        governanceProtectedPersistenceService.updateExecutionResult(record);
    }

    private void persistQueryHistoryIfMissing(GovernanceParseHistoryWriteRequest request,
                                              String tenantId,
                                              String userId,
                                              String requestId,
                                              String traceId,
                                              String sagaId,
                                              String historyId,
                                              String resultId,
                                              Instant submittedAt,
                                              String sqlFingerprint) {
        if (queryHistoryMapper.selectById(historyId) != null) {
            return;
        }
        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setHistoryId(historyId);
        record.setResultId(resultId);
        record.setTenantId(tenantId);
        record.setHistoryType("SQL_PARSE");
        record.setSqlFingerprint(sqlFingerprint);
        record.setDatasourceCode(request.getDatasourceCode());
        record.setDatasourceType(request.getDatasourceType());
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setAccessChannel("PAGE");
        record.setParameterizedSqlFlag(Boolean.valueOf(StringUtils.hasText(request.getSqlTemplateText())));
        record.setBindingMode(request.getBindingMode());
        record.setBindingRenderStatus("CAPTURED");
        record.setCommentContext(request.getQueryContextJson());
        record.setLogicalObjectHits(request.getLogicalObjectHitsJson());
        record.setQueryContext(request.getQueryContextJson());
        record.setSubmittedBy(userId);
        record.setSubmittedAt(toDatabaseTime(submittedAt));
        record.setCreateTime(toDatabaseTime(submittedAt));
        governanceProtectedPersistenceService.saveQueryHistoryWithSqlSurfaces(
            record,
            request.getSqlText(),
            request.getSqlTemplateText(),
            null
        );
    }

    private String requireContext(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Protected request context is missing " + fieldName
            );
        }
        return value;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " must not be blank"
            );
        }
        return value.trim();
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Instant.parse(value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private LocalDateTime toDatabaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private String sanitizeKey(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "-");
    }
}
