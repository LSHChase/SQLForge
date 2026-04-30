package com.company.governance.application.service;

import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceParseHistoryTraceabilityApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "PARSE_HISTORY_TRACEABILITY_BASELINE";
    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;

    public GovernanceParseHistoryTraceabilityApplicationService(
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
        String configSnapshotId = "cfg-parse-" + sanitizedTaskId;
        String sagaId = "parse-" + sanitizedTaskId;
        Instant submittedAt = parseInstant(request.getSubmittedAt(), Instant.now());

        persistConfigSnapshotIfMissing(request, tenantId, userId, requestId, traceId, sagaId, configSnapshotId, submittedAt);
        persistOrUpdateExecutionResult(request, tenantId, requestId, traceId, sagaId, configSnapshotId, resultId, submittedAt, resultStatus);
        persistOrUpdateQueryHistory(request, tenantId, userId, requestId, traceId, sagaId, historyId, resultId, submittedAt, sqlFingerprint);

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

    private void persistConfigSnapshotIfMissing(GovernanceParseHistoryWriteRequest request,
                                                String tenantId,
                                                String userId,
                                                String requestId,
                                                String traceId,
                                                String sagaId,
                                                String configSnapshotId,
                                                Instant submittedAt) {
        if (configSnapshotMapper.selectById(configSnapshotId) != null) {
            return;
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        putIfPresent(payload, "parseTaskId", request.getParseTaskId());
        putIfPresent(payload, "datasourceCode", request.getDatasourceCode());
        putIfPresent(payload, "datasourceType", request.getDatasourceType());
        putIfPresent(payload, "sqlFingerprint", request.getSqlFingerprint());
        putIfPresent(payload, "bindingMode", request.getBindingMode());
        ConfigSnapshotRecord record = new ConfigSnapshotRecord();
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.SQL_OPTIMIZATION);
        record.setSourceConfigType("SQL_PARSE_HISTORY");
        record.setSourceConfigId(request.getParseTaskId());
        record.setSourceVersion("v1");
        record.setSnapshotStatus("CAPTURED");
        record.setSnapshotReason("SQL parse history traceability snapshot");
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setSnapshotPayload(JsonUtils.toJson(payload));
        record.setCreatedBy(userId);
        record.setCreateTime(toDatabaseTime(submittedAt));
        governanceProtectedPersistenceService.saveConfigSnapshot(record);
    }

    private void persistOrUpdateExecutionResult(GovernanceParseHistoryWriteRequest request,
                                                String tenantId,
                                                String requestId,
                                                String traceId,
                                                String sagaId,
                                                String configSnapshotId,
                                                String resultId,
                                                Instant submittedAt,
                                                String resultStatus) {
        ExecutionResultRecord record = new ExecutionResultRecord();
        record.setResultId(resultId);
        record.setConfigSnapshotId(configSnapshotId);
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

    private void persistOrUpdateQueryHistory(GovernanceParseHistoryWriteRequest request,
                                             String tenantId,
                                             String userId,
                                             String requestId,
                                             String traceId,
                                             String sagaId,
                                             String historyId,
                                             String resultId,
                                             Instant submittedAt,
                                             String sqlFingerprint) {
        QueryHistoryRecord existing = queryHistoryMapper.selectById(historyId);
        NormalizedHistoryContext normalizedContext = normalizeHistoryContext(request);
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
        record.setCommentContext(toJsonOrNull(normalizedContext.commentContext));
        record.setLogicalObjectHits(normalizedContext.logicalObjectHitsJson);
        record.setQueryContext(JsonUtils.toJson(normalizedContext.queryContext));
        record.setSubmittedBy(userId);
        record.setSubmittedAt(toDatabaseTime(submittedAt));
        record.setCreateTime(existing == null ? toDatabaseTime(submittedAt) : existing.getCreateTime());
        if (existing == null) {
            governanceProtectedPersistenceService.saveQueryHistoryWithSqlSurfaces(
                record,
                request.getSqlText(),
                request.getSqlTemplateText(),
                null
            );
            return;
        }
        governanceProtectedPersistenceService.updateQueryHistoryWithSqlSurfaces(
            record,
            request.getSqlText(),
            request.getSqlTemplateText(),
            null
        );
    }

    private NormalizedHistoryContext normalizeHistoryContext(GovernanceParseHistoryWriteRequest request) {
        Map<String, Object> rawQueryContext = parseJsonMap(request.getQueryContextJson());
        Map<String, Object> resultPayload = parseJsonMap(request.getResultPayloadJson());
        Map<String, Object> resultSummary = parseJsonMap(request.getResultSummaryJson());
        Map<String, Object> commentContext = readMap(rawQueryContext, "commentContext");
        if (commentContext.isEmpty() && looksLikeFlatCommentContext(rawQueryContext)) {
            commentContext = new LinkedHashMap<String, Object>(rawQueryContext);
        }

        Map<String, Object> structureParse = firstMap(
            readMap(resultPayload, "structureParse"),
            looksLikeStructureParse(resultPayload) ? resultPayload : Collections.<String, Object>emptyMap(),
            readMap(rawQueryContext, "structureParseSummary"),
            readMap(rawQueryContext, "structureParse")
        );
        Map<String, Object> accessParse = firstMap(
            readMap(resultPayload, "accessParse"),
            looksLikeAccessParse(resultPayload) ? resultPayload : Collections.<String, Object>emptyMap(),
            readMap(rawQueryContext, "accessParseSummary"),
            readMap(rawQueryContext, "accessParse")
        );
        Map<String, Object> queryDateSummary = firstMap(
            readMap(structureParse, "queryDateSummary"),
            readMap(rawQueryContext, "queryDateSummary")
        );

        Object logicalObjectHits = parseJsonValue(request.getLogicalObjectHitsJson());
        if (logicalObjectHits == null) {
            logicalObjectHits = firstNonNull(
                structureParse.get("logicalObjectHits"),
                firstNonNull(rawQueryContext.get("logicalObjectHits"), rawQueryContext.get("logicalObjects"))
            );
        }
        String logicalObjectHitsJson = toJsonOrNull(logicalObjectHits);

        LinkedHashMap<String, Object> normalized = new LinkedHashMap<String, Object>();
        if (!rawQueryContext.isEmpty() && !looksLikeFlatCommentContext(rawQueryContext)) {
            normalized.putAll(rawQueryContext);
        }
        putIfPresent(normalized, "parseTaskId", request.getParseTaskId());
        putIfPresent(normalized, "datasourceCode", request.getDatasourceCode());
        putIfPresent(normalized, "datasourceType", request.getDatasourceType());
        putIfPresent(normalized, "sqlFingerprint", request.getSqlFingerprint());
        putIfPresent(normalized, "accessChannel", "PAGE");
        if (!commentContext.isEmpty()) {
            normalized.put("commentContext", commentContext);
        }
        if (!queryDateSummary.isEmpty()) {
            normalized.put("queryDateSummary", queryDateSummary);
        }
        putIfPresent(normalized, "queryDateStart", firstText(readText(rawQueryContext, "queryDateStart"), readText(queryDateSummary, "queryDateStart")));
        putIfPresent(normalized, "queryDateEnd", firstText(readText(rawQueryContext, "queryDateEnd"), readText(queryDateSummary, "queryDateEnd")));
        putIfPresent(normalized, "queryDateStatus", firstText(readText(rawQueryContext, "queryDateStatus"), readText(queryDateSummary, "queryDateStatus")));

        Map<String, Object> bindingSummary = normalizeBindingSummary(rawQueryContext, request);
        if (!bindingSummary.isEmpty()) {
            normalized.put("bindingSummary", bindingSummary);
        }
        if (logicalObjectHits != null) {
            normalized.put("logicalObjectHits", logicalObjectHits);
        }
        if (!structureParse.isEmpty()) {
            normalized.put("structureParseSummary", structureParse);
        }
        if (!accessParse.isEmpty()) {
            normalized.put("accessParseSummary", accessParse);
        }
        if (!resultSummary.isEmpty()) {
            normalized.put("resultSummary", resultSummary);
        }
        return new NormalizedHistoryContext(normalized, commentContext, logicalObjectHitsJson);
    }

    private Map<String, Object> normalizeBindingSummary(Map<String, Object> rawQueryContext,
                                                        GovernanceParseHistoryWriteRequest request) {
        LinkedHashMap<String, Object> bindingSummary = new LinkedHashMap<String, Object>();
        bindingSummary.putAll(readMap(rawQueryContext, "bindingSummary"));
        putIfAbsent(bindingSummary, "parameterizedSqlFlag", Boolean.valueOf(StringUtils.hasText(request.getSqlTemplateText())));
        putIfAbsent(bindingSummary, "bindingMode", trimToNull(request.getBindingMode()));
        putIfAbsent(bindingSummary, "bindingRenderStatus", "CAPTURED");
        putIfAbsent(bindingSummary, "sqlFingerprint", trimToNull(request.getSqlFingerprint()));
        return bindingSummary;
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return JsonUtils.objectMapper().readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
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

    private Map<String, Object> readMap(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || !StringUtils.hasText(key)) {
            return Collections.emptyMap();
        }
        Object value = source.get(key);
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>();
        copy.putAll((Map<String, Object>) value);
        return copy;
    }

    private String readText(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private boolean looksLikeFlatCommentContext(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (String normalizedKey : Arrays.asList(
            "commentContext",
            "bindingSummary",
            "structureParseSummary",
            "accessParseSummary",
            "queryDateSummary",
            "logicalObjectHits",
            "queryDateStart",
            "queryDateEnd",
            "queryDateStatus"
        )) {
            if (value.containsKey(normalizedKey)) {
                return false;
            }
        }
        for (String commentKey : Arrays.asList("report_code", "stage", "biz_date", "tenant_id", "datasource", "priority", "engine_hint")) {
            if (value.containsKey(commentKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeStructureParse(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return "STRUCTURE".equals(readText(value, "parseType"))
            || value.containsKey("syntaxStatus")
            || value.containsKey("queryDateSummary")
            || value.containsKey("logicalObjectHits");
    }

    private boolean looksLikeAccessParse(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return "ACCESS".equals(readText(value, "parseType"))
            || value.containsKey("serviceStatus")
            || value.containsKey("connectionStatus")
            || value.containsKey("objectResolutionStatus");
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || !StringUtils.hasText(key) || value == null) {
            return;
        }
        if (value instanceof String && !StringUtils.hasText((String) value)) {
            return;
        }
        target.put(key, value);
    }

    private void putIfAbsent(Map<String, Object> target, String key, Object value) {
        if (target == null || target.containsKey(key)) {
            return;
        }
        putIfPresent(target, key, value);
    }

    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
            return null;
        }
        return JsonUtils.toJson(value);
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
            try {
                return Instant.ofEpochMilli(Long.parseLong(value.trim()));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
    }

    private LocalDateTime toDatabaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private String sanitizeKey(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private Map<String, Object> firstMap(Map<String, Object>... values) {
        if (values == null) {
            return Collections.emptyMap();
        }
        for (Map<String, Object> value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return Collections.emptyMap();
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private static final class NormalizedHistoryContext {

        private final Map<String, Object> queryContext;
        private final Map<String, Object> commentContext;
        private final String logicalObjectHitsJson;

        private NormalizedHistoryContext(Map<String, Object> queryContext,
                                         Map<String, Object> commentContext,
                                         String logicalObjectHitsJson) {
            this.queryContext = queryContext == null ? Collections.<String, Object>emptyMap() : queryContext;
            this.commentContext = commentContext == null ? Collections.<String, Object>emptyMap() : commentContext;
            this.logicalObjectHitsJson = logicalObjectHitsJson;
        }
    }
}
