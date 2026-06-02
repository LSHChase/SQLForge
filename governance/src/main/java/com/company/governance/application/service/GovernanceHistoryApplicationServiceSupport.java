package com.company.governance.application.service;

import com.company.governance.application.controller.dto.GovernanceQueryHistoryExportRequest;
import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryRewriteRecordVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistorySummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.GovernanceQueryHistoryProjection;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.domain.trace.entity.TraceLookupHitRecord;
import com.company.governance.infrastructure.benchmarkengine.GovernanceBenchmarkEngineClient;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.GovernanceHistoryLookupIndexMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.governance.infrastructure.sqloptimization.GovernanceSqlOptimizationClient;
import com.company.governance.infrastructure.sqloptimization.SqlOptimizationRewriteRecordResponse;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

abstract class GovernanceHistoryApplicationServiceSupport extends GovernanceHistoryModelSupport {

    protected final AuditLogMapper auditLogMapper;
    protected final GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper;
    protected final QueryHistoryMapper queryHistoryMapper;
    protected final ExportRecordMapper exportRecordMapper;
    protected final TenantAccessLogic tenantAccessLogic;
    protected final GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient;
    protected final GovernanceSqlOptimizationClient governanceSqlOptimizationClient;
    protected final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    protected final SensitiveDataCryptoService sensitiveDataCryptoService;

    public GovernanceHistoryApplicationServiceSupport(AuditLogMapper auditLogMapper,
                                               GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic,
                                               GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient,
                                               GovernanceSqlOptimizationClient governanceSqlOptimizationClient,
                                               GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                               SensitiveDataCryptoService sensitiveDataCryptoService) {
        this.auditLogMapper = auditLogMapper;
        this.governanceHistoryLookupIndexMapper = governanceHistoryLookupIndexMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.tenantAccessLogic = tenantAccessLogic;
        this.governanceBenchmarkEngineClient = governanceBenchmarkEngineClient;
        this.governanceSqlOptimizationClient = governanceSqlOptimizationClient;
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
        this.sensitiveDataCryptoService = sensitiveDataCryptoService;
    }

    protected void mergeAuditLogs(Map<String, TraceAggregate> aggregateByTraceId, List<AuditLogRecord> records) {
        if (records == null) {
            return;
        }
        for (AuditLogRecord record : records) {
            String traceKey = trimToNull(record == null ? null : record.getTraceId());
            if (!StringUtils.hasText(traceKey)) {
                continue;
            }
            TraceAggregate aggregate = aggregateByTraceId.get(traceKey);
            if (aggregate == null) {
                aggregate = new TraceAggregate(traceKey);
                aggregateByTraceId.put(traceKey, aggregate);
            }
            aggregate.applyAudit(record, parseJsonObject(record.getRequestParams()), parseJsonObject(record.getResponseSummary()));
        }
    }

    protected void mergeAuditLogs(TraceAggregate aggregate, List<AuditLogRecord> records) {
        if (records == null) {
            return;
        }
        for (AuditLogRecord record : records) {
            aggregate.applyAudit(record, parseJsonObject(record.getRequestParams()), parseJsonObject(record.getResponseSummary()));
        }
    }

    protected void mergeQueryHistories(Map<String, TraceAggregate> aggregateByTraceId, List<QueryHistoryRecord> records) {
        if (records == null) {
            return;
        }
        for (QueryHistoryRecord record : records) {
            String traceKey = trimToNull(record == null ? null : record.getTraceId());
            if (!StringUtils.hasText(traceKey)) {
                continue;
            }
            TraceAggregate aggregate = aggregateByTraceId.get(traceKey);
            if (aggregate == null) {
                aggregate = new TraceAggregate(traceKey);
                aggregateByTraceId.put(traceKey, aggregate);
            }
            aggregate.applyQueryHistory(record);
        }
    }

    protected void mergeQueryHistories(TraceAggregate aggregate, List<QueryHistoryRecord> records) {
        if (records == null) {
            return;
        }
        for (QueryHistoryRecord record : records) {
            aggregate.applyQueryHistory(record);
        }
    }

    protected void mergeExportRecords(Map<String, TraceAggregate> aggregateByTraceId, List<ExportRecord> records) {
        if (records == null) {
            return;
        }
        for (ExportRecord record : records) {
            String traceKey = trimToNull(record == null ? null : record.getTraceId());
            if (!StringUtils.hasText(traceKey)) {
                continue;
            }
            TraceAggregate aggregate = aggregateByTraceId.get(traceKey);
            if (aggregate == null) {
                aggregate = new TraceAggregate(traceKey);
                aggregateByTraceId.put(traceKey, aggregate);
            }
            aggregate.applyExportRecord(record);
        }
    }

    protected void mergeExportRecords(TraceAggregate aggregate, List<ExportRecord> records) {
        if (records == null) {
            return;
        }
        for (ExportRecord record : records) {
            aggregate.applyExportRecord(record);
        }
    }

    protected String resolveAuthorizedTenantId(String tenantId) {
        String currentTenantId = TenantContext.get();
        if (!StringUtils.hasText(currentTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "租户上下文缺失"
            );
        }

        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId.trim() : currentTenantId;
        if (!currentTenantId.equals(effectiveTenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
        if (!tenantAccessLogic.validateDataSourceAccess(currentTenantId, DEFAULT_DATA_SOURCE_ID, "READ")) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }
        return effectiveTenantId;
    }

    protected void assertArtifactOperationRole() {
        if (SYSTEM_TENANT_ID.equals(RequestContext.getTenantId())) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
            HttpStatus.FORBIDDEN,
            "当前请求不允许触发产物清理或恢复"
        );
    }

    protected int normalizeLimit(Integer limit) {
        if (limit == null || limit.intValue() <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(MAX_LIMIT, limit.intValue());
    }

    protected int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo.intValue() <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo.intValue();
    }

    protected int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    protected int resolveRecentSourceScanLimit(int resolvedLimit) {
        int scaledLimit = resolvedLimit * RECENT_SOURCE_SCAN_MULTIPLIER;
        if (scaledLimit < resolvedLimit) {
            return resolvedLimit;
        }
        return Math.min(MAX_RECENT_SOURCE_SCAN_LIMIT, scaledLimit);
    }

    protected int resolveLookupSourceScanLimit(int resolvedLimit) {
        int scaledLimit = resolvedLimit * LOOKUP_SOURCE_SCAN_MULTIPLIER;
        if (scaledLimit < resolvedLimit) {
            return Math.min(MAX_LOOKUP_SOURCE_SCAN_LIMIT, MIN_LOOKUP_SOURCE_SCAN_LIMIT);
        }
        int candidate = Math.max(MIN_LOOKUP_SOURCE_SCAN_LIMIT, scaledLimit);
        return Math.min(MAX_LOOKUP_SOURCE_SCAN_LIMIT, candidate);
    }

    protected List<TraceAggregate> loadRecentAggregates(String tenantId, int sourceScanLimit) {
        LinkedHashMap<String, TraceAggregate> aggregateByTraceId = new LinkedHashMap<String, TraceAggregate>();
        mergeAuditLogs(
            aggregateByTraceId,
            auditLogMapper.selectRecentBusinessByTenant(tenantId, sourceScanLimit)
        );
        mergeQueryHistories(
            aggregateByTraceId,
            queryHistoryMapper.selectRecentByTenant(tenantId, sourceScanLimit)
        );
        mergeExportRecords(
            aggregateByTraceId,
            exportRecordMapper.selectRecentByTenant(tenantId, sourceScanLimit)
        );

        List<TraceAggregate> aggregates = new ArrayList<TraceAggregate>(aggregateByTraceId.values());
        Collections.sort(aggregates, new Comparator<TraceAggregate>() {
            @Override
            public int compare(TraceAggregate left, TraceAggregate right) {
                LocalDateTime leftTime = left.getLastSeenAt();
                LocalDateTime rightTime = right.getLastSeenAt();
                if (leftTime == null && rightTime == null) {
                    return 0;
                }
                if (leftTime == null) {
                    return 1;
                }
                if (rightTime == null) {
                    return -1;
                }
                return rightTime.compareTo(leftTime);
            }
        });
        return aggregates;
    }

    protected TraceAggregate loadTraceAggregate(String tenantId, String traceId, int sourceLimit) {
        TraceAggregate aggregate = new TraceAggregate(traceId);
        mergeAuditLogs(aggregate, auditLogMapper.selectByTraceId(tenantId, traceId, sourceLimit));
        mergeQueryHistories(aggregate, queryHistoryMapper.selectByTraceId(tenantId, traceId, sourceLimit));
        mergeExportRecords(aggregate, exportRecordMapper.selectByTraceId(tenantId, traceId, sourceLimit));
        return aggregate;
    }

    protected List<TraceAggregate> loadAggregatesByTraceIds(String tenantId, List<String> traceIds) {
        if (traceIds == null || traceIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, TraceAggregate> aggregateByTraceId = new LinkedHashMap<String, TraceAggregate>();
        for (String traceId : traceIds) {
            if (StringUtils.hasText(traceId) && !aggregateByTraceId.containsKey(traceId)) {
                aggregateByTraceId.put(traceId, new TraceAggregate(traceId));
            }
        }
        mergeAuditLogs(aggregateByTraceId, auditLogMapper.selectByTraceIds(tenantId, traceIds));
        mergeQueryHistories(aggregateByTraceId, queryHistoryMapper.selectByTraceIds(tenantId, traceIds));
        mergeExportRecords(aggregateByTraceId, exportRecordMapper.selectByTraceIds(tenantId, traceIds));
        return new ArrayList<TraceAggregate>(aggregateByTraceId.values());
    }

    protected List<TraceLookupHitRecord> loadIndexedTraceHits(String tenantId,
                                                            String taskId,
                                                            String reportId,
                                                            LocalDateTime windowStart,
                                                            LocalDateTime windowEnd,
                                                            LookupCursor cursor,
                                                            int limit) {
        if (governanceHistoryLookupIndexMapper != null) {
            if (StringUtils.hasText(taskId)) {
                return governanceHistoryLookupIndexMapper.selectTraceHitsByLookupId(
                    tenantId,
                    LOOKUP_TYPE_TASK,
                    taskId,
                    windowStart,
                    windowEnd,
                    cursor == null ? null : cursor.getCreatedAt(),
                    cursor == null ? null : cursor.getAuditId(),
                    limit
                );
            }
            return governanceHistoryLookupIndexMapper.selectTraceHitsByLookupId(
                tenantId,
                LOOKUP_TYPE_REPORT,
                reportId,
                windowStart,
                windowEnd,
                cursor == null ? null : cursor.getCreatedAt(),
                cursor == null ? null : cursor.getAuditId(),
                limit
            );
        }
        if (StringUtils.hasText(taskId)) {
            return auditLogMapper.selectTraceHitsByTargetId(
                tenantId,
                taskId,
                TASK_TARGET_TYPES,
                cursor == null ? null : cursor.getCreatedAt(),
                cursor == null ? null : cursor.getAuditId(),
                limit
            );
        }
        return auditLogMapper.selectTraceHitsByTargetId(
            tenantId,
            reportId,
            REPORT_TARGET_TYPES,
            cursor == null ? null : cursor.getCreatedAt(),
            cursor == null ? null : cursor.getAuditId(),
            limit
        );
    }

    protected LocalDateTime parseWindowValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 无效"
            );
        }
    }

    protected LocalDate parseDateValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 无效"
            );
        }
    }

    protected String resolveHistoryOrderBy(String sortBy, String sortOrder) {
        String normalizedSortBy = trimToNull(sortBy);
        String normalizedSortOrder = trimToNull(sortOrder);
        String direction = "ASC".equalsIgnoreCase(normalizedSortOrder) ? "ASC" : "DESC";
        if (!StringUtils.hasText(normalizedSortBy) || "submittedAt".equalsIgnoreCase(normalizedSortBy)) {
            return "qh.submitted_at " + direction + ", qh.history_id DESC";
        }
        if ("createTime".equalsIgnoreCase(normalizedSortBy)) {
            return "qh.create_time " + direction + ", qh.history_id DESC";
        }
        if ("rowCount".equalsIgnoreCase(normalizedSortBy) || "returnedRowCount".equalsIgnoreCase(normalizedSortBy)) {
            return "er.returned_row_count " + direction + ", qh.history_id DESC";
        }
        if ("finishedAt".equalsIgnoreCase(normalizedSortBy)) {
            return "er.finished_at " + direction + ", qh.history_id DESC";
        }
        if ("status".equalsIgnoreCase(normalizedSortBy)) {
            return "er.result_status " + direction + ", qh.history_id DESC";
        }
        return "qh.submitted_at DESC, qh.history_id DESC";
    }

    protected LookupCursor parseLookupCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        String[] parts = cursor.trim().split("\\|", 2);
        if (parts.length != 2) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "lookup cursor 无效"
            );
        }
        try {
            return new LookupCursor(LocalDateTime.parse(parts[0]), Long.valueOf(parts[1]));
        } catch (Exception ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "lookup cursor 无效"
            );
        }
    }

    protected String encodeCursor(TraceLookupHitRecord hit) {
        if (hit == null || hit.getLastSeenAt() == null || hit.getLastAuditId() == null) {
            return null;
        }
        return hit.getLastSeenAt().toString() + "|" + hit.getLastAuditId();
    }

    protected static Map<String, Object> parseJsonObject(String content) {
        if (!StringUtils.hasText(content)) {
            return Collections.emptyMap();
        }
        try {
            ObjectMapper objectMapper = JsonUtils.objectMapper();
            return objectMapper.readValue(content, MAP_TYPE);
        } catch (Exception ex) {
            LinkedHashMap<String, Object> fallback = new LinkedHashMap<String, Object>();
            fallback.put("raw", content);
            return fallback;
        }
    }

    protected static Object parseJsonValue(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            return JsonUtils.objectMapper().readValue(content, Object.class);
        } catch (Exception ex) {
            return content;
        }
    }

    protected GovernanceQueryHistorySummaryVO toQueryHistorySummary(GovernanceQueryHistoryProjection row) {
        GovernanceQueryHistorySummaryVO item = new GovernanceQueryHistorySummaryVO();
        item.setHistoryId(row.getHistoryId());
        item.setResultId(row.getResultId());
        item.setTraceId(row.getTraceId());
        item.setTenantId(row.getTenantId());
        item.setHistoryType(row.getHistoryType());
        item.setReportCode(row.getReportCode());
        item.setDatasourceCode(row.getDatasourceCode());
        item.setDatasourceType(row.getDatasourceType());
        item.setStageCode(row.getStageCode());
        item.setBizDate(row.getBizDate());
        item.setQueryDateStart(row.getQueryDateStart());
        item.setQueryDateEnd(row.getQueryDateEnd());
        item.setQueryDateStatus(row.getQueryDateStatus());
        item.setAccessChannel(row.getAccessChannel());
        item.setParameterizedSqlFlag(row.getParameterizedSqlFlag());
        item.setBindingMode(row.getBindingMode());
        item.setBindingRenderStatus(row.getBindingRenderStatus());
        item.setSqlFingerprint(row.getSqlFingerprint());
        item.setSqlTemplateFingerprint(row.getSqlTemplateFingerprint());
        item.setBoundSqlFingerprint(row.getBoundSqlFingerprint());
        item.setResultStatus(row.getResultStatus());
        item.setTargetEngine(row.getTargetEngine());
        item.setReturnedRowCount(row.getReturnedRowCount());
        item.setCacheHit(row.getCacheHit());
        item.setRewriteApplied(normalizeRewriteApplied(row.getRewriteApplied()));
        item.setAccelerationApplied(row.getAccelerationApplied());
        item.setSubmittedBy(row.getSubmittedBy());
        item.setSubmittedAt(row.getSubmittedAt() == null ? row.getCreateTime() : row.getSubmittedAt());
        item.setErrorCode(row.getErrorCode());
        List<LogicalObjectSurface> logicalObjectHits = normalizeLogicalObjectHits(row.getLogicalObjectHits());
        item.setLogicalObjectHits(logicalObjectHits);
        item.setLogicalObjectTypes(extractLogicalObjectTypes(logicalObjectHits));
        return item;
    }

    protected GovernanceQueryHistoryDetailVO toQueryHistoryDetail(GovernanceQueryHistoryProjection row,
                                                                QueryHistoryRecord historyRecord) {
        GovernanceQueryHistoryDetailVO detail = new GovernanceQueryHistoryDetailVO();
        detail.setHistoryId(row.getHistoryId());
        detail.setResultId(row.getResultId());
        detail.setTraceId(row.getTraceId());
        detail.setHistoryType(row.getHistoryType());
        detail.setReportCode(row.getReportCode());
        detail.setDatasourceCode(row.getDatasourceCode());
        detail.setDatasourceType(row.getDatasourceType());
        detail.setStageCode(row.getStageCode());
        detail.setBizDate(row.getBizDate());
        detail.setSqlText(decryptSqlText(historyRecord == null ? null : historyRecord.getSqlTextCipher()));
        detail.setSqlTemplateText(decryptSqlText(historyRecord == null ? null : historyRecord.getSqlTemplateCipher()));
        detail.setBoundSqlText(decryptSqlText(historyRecord == null ? null : historyRecord.getBoundSqlTextCipher()));
        detail.setSqlState(buildSqlState(row));
        detail.setCommentContext(parseJsonObject(row.getCommentContext()));
        detail.setQueryDateSummary(buildQueryDateSummary(row));
        detail.setLogicalObjectHits(normalizeLogicalObjectHits(row.getLogicalObjectHits()));
        detail.setExecutionSummary(buildExecutionSummary(row));
        detail.setStructureParseSummary(buildParseSummary(parseJsonObject(row.getQueryContext()), "structureParseSummary"));
        detail.setAccessParseSummary(buildParseSummary(parseJsonObject(row.getQueryContext()), "accessParseSummary"));
        detail.setRouteDecision(selectFirstNonEmptyMap(parseJsonObject(row.getRouteSummary()), parseJsonObject(row.getResultSummary()), "routeSummary"));
        detail.setCacheSummary(selectFirstNonEmptyMap(parseJsonObject(row.getCacheSummary()), parseJsonObject(row.getResultSummary()), "cacheSummary"));
        detail.setBindingSummary(selectFirstNonEmptyMap(parseJsonObject(row.getBindingSummary()), parseJsonObject(row.getQueryContext()), "bindingSummary"));
        detail.setQueryContext(parseJsonObject(row.getQueryContext()));
        detail.setRewriteAudit(buildRewriteAudit(row, detail));
        detail.setRecommendationRefs(readNestedList(detail.getQueryContext(), "recommendationRefs"));
        detail.setBenchmarkRefs(readNestedList(detail.getQueryContext(), "benchmarkRefs"));
        detail.setAuditRefs(Collections.<Map<String, Object>>emptyList());
        detail.setAlertRefs(readNestedList(detail.getQueryContext(), "alertRefs"));
        detail.setSubmittedAt(row.getSubmittedAt() == null ? row.getCreateTime() : row.getSubmittedAt());
        detail.setSubmittedBy(row.getSubmittedBy());
        return detail;
    }

    protected void populateReferenceSurfaces(GovernanceQueryHistoryDetailVO detail) {
        if (detail == null) {
            return;
        }
        detail.setAuditRefs(buildAuditRefs(detail.getTraceDetail()));
        if ((detail.getBenchmarkRefs() == null || detail.getBenchmarkRefs().isEmpty()) && detail.getTraceDetail() != null) {
            detail.setBenchmarkRefs(buildBenchmarkRefs(detail.getTraceDetail()));
        }
        if (detail.getRecommendationRefs() == null) {
            detail.setRecommendationRefs(Collections.<Map<String, Object>>emptyList());
        }
        if (detail.getAlertRefs() == null) {
            detail.setAlertRefs(Collections.<Map<String, Object>>emptyList());
        }
    }

    protected boolean sameRewriteRecordScope(SqlOptimizationRewriteRecordResponse record,
                                           String tenantId,
                                           String historyId) {
        return record != null
            && tenantId.equals(record.getTenantId())
            && historyId.equals(record.getHistoryId());
    }

    protected GovernanceQueryHistoryRewriteRecordVO toQueryHistoryRewriteRecord(
        SqlOptimizationRewriteRecordResponse record
    ) {
        GovernanceQueryHistoryRewriteRecordVO item = new GovernanceQueryHistoryRewriteRecordVO();
        item.setRewriteRecordId(record.getRewriteRecordId());
        item.setTenantId(record.getTenantId());
        item.setRecommendationId(record.getRecommendationId());
        item.setOptimizationTaskId(record.getOptimizationTaskId());
        item.setSourceType(record.getSourceType());
        item.setSourceKind(record.getSourceKind());
        item.setSourceId(record.getSourceId());
        item.setEvidenceLevel(record.getEvidenceLevel());
        item.setHistoryId(record.getHistoryId());
        item.setParseHistoryId(record.getParseHistoryId());
        item.setSqlFingerprint(record.getSqlFingerprint());
        item.setDatasourceCode(record.getDatasourceCode());
        item.setStatus(record.getStatus());
        item.setValidationStatus(record.getValidationStatus());
        item.setAutoApplyAllowed(record.getAutoApplyAllowed());
        item.setManualReviewRequired(record.getManualReviewRequired());
        item.setValidationPolicyId(record.getValidationPolicyId());
        item.setLastValidationRunId(record.getLastValidationRunId());
        item.setLastComparedAt(record.getLastComparedAt());
        item.setAlertStatus(record.getAlertStatus());
        item.setOriginalSqlText(record.getOriginalSqlText());
        item.setRecommendedSqlText(record.getRecommendedSqlText());
        item.setExecutedSqlText(record.getExecutedSqlText());
        item.setCreatedBy(record.getCreatedBy());
        item.setCreatedAt(record.getCreatedAt());
        item.setUpdatedAt(record.getUpdatedAt());
        item.setRuleChain(record.getRuleChain() == null
            ? Collections.<Map<String, Object>>emptyList()
            : record.getRuleChain());
        item.setDiffSummary(record.getDiffSummary() == null
            ? Collections.<String, Object>emptyMap()
            : record.getDiffSummary());
        item.setRisk(record.getRisk() == null
            ? Collections.<String, Object>emptyMap()
            : record.getRisk());
        item.setTraceRefs(record.getTraceRefs() == null
            ? Collections.<String, Object>emptyMap()
            : record.getTraceRefs());
        item.setAlertRefs(buildRewriteRecordAlertRefs(record));
        return item;
    }

    protected List<Map<String, Object>> buildRewriteRecordAlertRefs(SqlOptimizationRewriteRecordResponse record) {
        List<Map<String, Object>> alertRefs = readNestedList(record.getTraceRefs(), "alertRefs");
        if (!alertRefs.isEmpty()) {
            return alertRefs;
        }
        if (!StringUtils.hasText(record.getAlertStatus()) || "NONE".equals(record.getAlertStatus())) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, Object> alertRef = new LinkedHashMap<String, Object>();
        alertRef.put("source", "sql_rewrite_record");
        alertRef.put("rewriteRecordId", record.getRewriteRecordId());
        alertRef.put("alertStatus", record.getAlertStatus());
        alertRef.put("validationStatus", record.getValidationStatus());
        alertRef.put("lastValidationRunId", record.getLastValidationRunId());
        return Collections.<Map<String, Object>>singletonList(alertRef);
    }

    protected Map<String, Object> buildHistoryClassificationSummary(List<GovernanceQueryHistorySummaryVO> items) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("totalItems", Integer.valueOf(items == null ? 0 : items.size()));
        summary.put("statusCounts", bucketCount(items, "status"));
        summary.put("historyTypeCounts", bucketCount(items, "historyType"));
        summary.put("accessChannelCounts", bucketCount(items, "accessChannel"));
        return summary;
    }

    protected Map<String, Integer> bucketCount(List<GovernanceQueryHistorySummaryVO> items, String bucketType) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        if (items == null) {
            return counts;
        }
        for (GovernanceQueryHistorySummaryVO item : items) {
            String key;
            if ("status".equals(bucketType)) {
                key = firstNonBlank(item.getResultStatus(), "UNKNOWN");
            } else if ("accessChannel".equals(bucketType)) {
                key = firstNonBlank(item.getAccessChannel(), "UNKNOWN");
            } else {
                key = firstNonBlank(item.getHistoryType(), "UNKNOWN");
            }
            Integer count = counts.get(key);
            counts.put(key, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        return counts;
    }

    protected List<String> extractLogicalObjectTypes(List<LogicalObjectSurface> logicalObjectHits) {
        LinkedHashSet<String> types = new LinkedHashSet<String>();
        collectLogicalObjectTypes(logicalObjectHits, types);
        return new ArrayList<String>(types);
    }

    protected void collectLogicalObjectTypes(Object parsed, Set<String> types) {
        if (parsed == null || types == null) {
            return;
        }
        if (parsed instanceof List) {
            for (Object item : (List<?>) parsed) {
                collectLogicalObjectTypes(item, types);
            }
            return;
        }
        if (parsed instanceof LogicalObjectSurface) {
            String type = ((LogicalObjectSurface) parsed).getObjectType();
            if (StringUtils.hasText(type)) {
                types.add(type);
            }
            return;
        }
        if (parsed instanceof Map) {
            Object type = ((Map<?, ?>) parsed).get("type");
            if (type == null) {
                type = ((Map<?, ?>) parsed).get("objectType");
            }
            if (type != null) {
                types.add(String.valueOf(type));
            }
            return;
        }
        String raw = String.valueOf(parsed);
        if (raw.contains(LogicalObjectType.BUSINESS_VIEW.name())) {
            types.add(LogicalObjectType.BUSINESS_VIEW.name());
        }
        if (raw.contains(LogicalObjectType.DB_VIEW.name())) {
            types.add(LogicalObjectType.DB_VIEW.name());
        }
        if (raw.contains(LogicalObjectType.TABLE.name())) {
            types.add(LogicalObjectType.TABLE.name());
        }
        if (types.isEmpty() && StringUtils.hasText(raw)) {
            types.add("RAW");
        }
    }

    protected static List<LogicalObjectSurface> normalizeLogicalObjectHits(String logicalObjectHitsJson) {
        return normalizeLogicalObjectHits(parseJsonValue(logicalObjectHitsJson));
    }

    protected static List<LogicalObjectSurface> normalizeLogicalObjectHits(Object logicalObjectHits) {
        if (logicalObjectHits == null) {
            return Collections.emptyList();
        }
        List<LogicalObjectSurface> surfaces = new ArrayList<LogicalObjectSurface>();
        collectLogicalObjectSurfaces(logicalObjectHits, surfaces);
        return surfaces;
    }

    protected static void collectLogicalObjectSurfaces(Object rawValue, List<LogicalObjectSurface> surfaces) {
        if (rawValue == null || surfaces == null) {
            return;
        }
        if (rawValue instanceof List) {
            for (Object item : (List<?>) rawValue) {
                collectLogicalObjectSurfaces(item, surfaces);
            }
            return;
        }
        if (rawValue instanceof Map) {
            LogicalObjectSurface surface = toLogicalObjectSurface((Map<?, ?>) rawValue);
            if (surface != null) {
                surfaces.add(surface);
            }
            return;
        }
        LogicalObjectSurface surface = fromRawLogicalObject(String.valueOf(rawValue));
        if (surface != null) {
            surfaces.add(surface);
        }
    }

    protected static LogicalObjectSurface toLogicalObjectSurface(Map<?, ?> rawMap) {
        if (rawMap == null || rawMap.isEmpty()) {
            return null;
        }
        LogicalObjectSurface surface = new LogicalObjectSurface();
        String objectType = firstText(rawMap.get("objectType"), rawMap.get("type"));
        String objectKey = firstText(rawMap.get("objectKey"), rawMap.get("key"));
        String objectName = firstText(rawMap.get("objectName"), rawMap.get("name"));
        String catalogName = firstText(rawMap.get("catalogName"), rawMap.get("catalog"));
        String schemaName = firstText(rawMap.get("schemaName"), rawMap.get("schema"));
        String matchSource = firstText(rawMap.get("matchSource"), rawMap.get("source"));
        Boolean resolved = toBooleanValue(rawMap.get("resolved"));
        List<String> mappedTargets = normalizeMappedPhysicalTargets(rawMap.get("mappedPhysicalTargets"));
        if (mappedTargets.isEmpty()) {
            mappedTargets = normalizeMappedPhysicalTargets(rawMap.get("physicalTargets"));
        }

        if (!StringUtils.hasText(objectType) && StringUtils.hasText(objectKey)) {
            objectType = extractLogicalObjectType(objectKey);
        }
        if (!StringUtils.hasText(objectKey) && StringUtils.hasText(objectType) && StringUtils.hasText(objectName)) {
            LogicalObjectType logicalObjectType = resolveLogicalObjectType(objectType);
            if (logicalObjectType != null) {
                objectKey = LogicalObjectRef.buildObjectKey(logicalObjectType, objectName);
            }
        }
        if (!StringUtils.hasText(objectName) && StringUtils.hasText(objectKey) && objectKey.contains(":")) {
            objectName = objectKey.substring(objectKey.indexOf(':') + 1);
        }

        surface.setObjectType(StringUtils.hasText(objectType) ? objectType : "RAW");
        surface.setObjectKey(objectKey);
        surface.setObjectName(objectName);
        surface.setCatalogName(catalogName);
        surface.setSchemaName(schemaName);
        surface.setMatchSource(matchSource);
        surface.setResolved(resolved);
        surface.setMappedPhysicalTargets(mappedTargets);
        return surface;
    }

    protected static LogicalObjectSurface fromRawLogicalObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalizedRaw = raw.trim();
        LogicalObjectSurface surface = new LogicalObjectSurface();
        String objectType = extractLogicalObjectType(normalizedRaw);
        surface.setObjectType(StringUtils.hasText(objectType) ? objectType : "RAW");
        if (StringUtils.hasText(objectType)) {
            surface.setObjectKey(normalizedRaw);
            surface.setObjectName(normalizedRaw.contains(":") ? normalizedRaw.substring(normalizedRaw.indexOf(':') + 1) : normalizedRaw);
        } else {
            surface.setObjectName(normalizedRaw);
            LogicalObjectType fallbackType = inferLogicalObjectTypeFromName(normalizedRaw);
            if (fallbackType != null) {
                surface.setObjectType(fallbackType.name());
                surface.setObjectKey(LogicalObjectRef.buildObjectKey(fallbackType, normalizedRaw));
            }
        }
        surface.setResolved(Boolean.FALSE);
        surface.setMappedPhysicalTargets(Collections.<String>emptyList());
        return surface;
    }

    protected static List<String> normalizeMappedPhysicalTargets(Object rawTargets) {
        if (rawTargets == null) {
            return Collections.emptyList();
        }
        List<String> targets = new ArrayList<String>();
        if (rawTargets instanceof List) {
            for (Object item : (List<?>) rawTargets) {
                String normalized = normalizeTargetEntry(item);
                if (StringUtils.hasText(normalized)) {
                    targets.add(normalized);
                }
            }
            return targets;
        }
        String normalized = normalizeTargetEntry(rawTargets);
        if (StringUtils.hasText(normalized)) {
            targets.add(normalized);
        }
        return targets;
    }

    protected static String normalizeTargetEntry(Object rawTarget) {
        if (rawTarget == null) {
            return null;
        }
        if (rawTarget instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) rawTarget;
            return firstText(rawMap.get("targetObjectKey"), rawMap.get("objectKey"), rawMap.get("name"));
        }
        return trimToNull(String.valueOf(rawTarget));
    }

    protected static String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            String normalized = trimToNull(value == null ? null : String.valueOf(value));
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    protected static Boolean toBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    protected static String extractLogicalObjectType(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        for (LogicalObjectType type : LogicalObjectType.values()) {
            if (normalized.startsWith(type.name() + ":") || normalized.equals(type.name())) {
                return type.name();
            }
        }
        return null;
    }

    protected static LogicalObjectType resolveLogicalObjectType(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return LogicalObjectType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    protected static LogicalObjectType inferLogicalObjectTypeFromName(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.startsWith("vw_") || normalized.contains(" view")) {
            return LogicalObjectType.DB_VIEW;
        }
        if (normalized.contains(".")) {
            return LogicalObjectType.TABLE;
        }
        return null;
    }

    protected static String joinLogicalObjectKeys(List<LogicalObjectSurface> logicalObjectHits) {
        if (logicalObjectHits == null || logicalObjectHits.isEmpty()) {
            return null;
        }
        List<String> objectKeys = new ArrayList<String>();
        for (LogicalObjectSurface hit : logicalObjectHits) {
            if (hit == null) {
                continue;
            }
            String value = firstNonBlank(hit.getObjectKey(), hit.getObjectName());
            if (StringUtils.hasText(value) && !objectKeys.contains(value)) {
                objectKeys.add(value);
            }
        }
        return objectKeys.isEmpty() ? null : String.join("|", objectKeys);
    }

    protected Map<String, Object> buildSqlState(GovernanceQueryHistoryProjection row) {
        LinkedHashMap<String, Object> sqlState = new LinkedHashMap<String, Object>();
        sqlState.put("sqlFingerprint", row.getSqlFingerprint());
        sqlState.put("sqlTemplateFingerprint", row.getSqlTemplateFingerprint());
        sqlState.put("boundSqlFingerprint", row.getBoundSqlFingerprint());
        sqlState.put("parameterizedSqlFlag", row.getParameterizedSqlFlag());
        sqlState.put("bindingMode", row.getBindingMode());
        sqlState.put("bindingRenderStatus", row.getBindingRenderStatus());
        return sqlState;
    }

    protected Map<String, Object> buildQueryDateSummary(GovernanceQueryHistoryProjection row) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("bizDate", row.getBizDate());
        summary.put("queryDateStart", row.getQueryDateStart());
        summary.put("queryDateEnd", row.getQueryDateEnd());
        summary.put("queryDateStatus", row.getQueryDateStatus());
        return summary;
    }

    protected Map<String, Object> buildExecutionSummary(GovernanceQueryHistoryProjection row) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("status", row.getResultStatus());
        summary.put("targetEngine", row.getTargetEngine());
        summary.put("returnedRowCount", row.getReturnedRowCount());
        summary.put("cacheHit", row.getCacheHit());
        summary.put("rewriteApplied", normalizeRewriteApplied(row.getRewriteApplied()));
        summary.put("accelerationApplied", row.getAccelerationApplied());
        summary.put("hitTableSummary", parseJsonValue(row.getHitTableSummary()));
        summary.put("resultSummary", parseJsonObject(row.getResultSummary()));
        summary.put("errorCode", row.getErrorCode());
        summary.put("errorMessage", row.getErrorMessage());
        summary.put("startedAt", row.getStartedAt());
        summary.put("finishedAt", row.getFinishedAt());
        return summary;
    }

    protected Map<String, Object> buildRewriteAudit(GovernanceQueryHistoryProjection row,
                                                  GovernanceQueryHistoryDetailVO detail) {
        Map<String, Object> bindingSummary = detail.getBindingSummary() == null
            ? Collections.<String, Object>emptyMap()
            : detail.getBindingSummary();
        Map<String, Object> queryContext = detail.getQueryContext() == null
            ? Collections.<String, Object>emptyMap()
            : detail.getQueryContext();
        Map<String, Object> nestedAudit = readNestedMap(queryContext, "rewriteAudit");
        String rewriteRecordId = firstNonBlank(
            row.getRewriteRecordId(),
            readText(bindingSummary, "rewriteRecordId"),
            readText(queryContext, "rewriteRecordId"),
            readText(nestedAudit, "rewriteRecordId")
        );
        LinkedHashMap<String, Object> audit = new LinkedHashMap<String, Object>();
        audit.put("auditSource", "BACKEND_HISTORY_FIELDS");
        audit.put("rewriteApplied", normalizeRewriteApplied(row.getRewriteApplied()));
        audit.put("originalSqlText", firstNonBlank(detail.getSqlTemplateText(), detail.getSqlText()));
        audit.put("actualSqlText", firstNonBlank(detail.getBoundSqlText(), detail.getSqlText()));
        audit.put("originalSqlFingerprint", firstNonBlank(
            row.getSqlTemplateFingerprint(),
            row.getSqlFingerprint(),
            readText(bindingSummary, "sqlTemplateFingerprint"),
            readText(nestedAudit, "originalSqlFingerprint")
        ));
        audit.put("actualSqlFingerprint", firstNonBlank(
            row.getBoundSqlFingerprint(),
            readText(bindingSummary, "actualSqlFingerprint"),
            readText(bindingSummary, "boundSqlFingerprint"),
            readText(nestedAudit, "actualSqlFingerprint")
        ));
        audit.put("rewriteRecordId", rewriteRecordId);
        audit.put("runtimeBindingId", firstNonBlank(
            row.getRuntimeBindingId(),
            readText(bindingSummary, "runtimeBindingId"),
            readText(queryContext, "runtimeBindingId"),
            readText(nestedAudit, "runtimeBindingId")
        ));
        audit.put("ruleVersion", firstNonNull(
            row.getRewriteRuleVersion(),
            firstNonNull(
                bindingSummary.get("ruleVersion"),
                firstNonNull(queryContext.get("ruleVersion"), nestedAudit.get("ruleVersion"))
            )
        ));
        audit.put("runtimeRuleVersion", firstNonBlank(
            row.getRuntimeRuleVersion(),
            readText(bindingSummary, "runtimeRuleVersion"),
            readText(queryContext, "runtimeRuleVersion"),
            readText(nestedAudit, "runtimeRuleVersion")
        ));
        audit.put("runtimeRewriteStatus", firstNonBlank(
            row.getRuntimeRewriteStatus(),
            readText(bindingSummary, "runtimeRewriteStatus"),
            readText(queryContext, "runtimeRewriteStatus"),
            readText(nestedAudit, "runtimeRewriteStatus")
        ));
        audit.put("activationStatusSnapshot", firstNonBlank(
            row.getRewriteActivationStatusSnapshot(),
            readText(bindingSummary, "rewriteActivationStatusSnapshot"),
            readText(queryContext, "rewriteActivationStatusSnapshot"),
            readText(nestedAudit, "activationStatusSnapshot")
        ));
        audit.put("rewriteFallbackReason", firstNonBlank(
            row.getRewriteFallbackReason(),
            readText(bindingSummary, "rewriteFallbackReason"),
            readText(queryContext, "rewriteFallbackReason"),
            readText(nestedAudit, "rewriteFallbackReason")
        ));
        if (!StringUtils.hasText(String.valueOf(audit.get("activationStatusSnapshot")))
            || "null".equals(String.valueOf(audit.get("activationStatusSnapshot")))) {
            audit.put("activationStatusSnapshot", StringUtils.hasText(rewriteRecordId) ? "UNKNOWN" : "INACTIVE");
        }
        return audit;
    }

    protected List<Map<String, Object>> buildAuditRefs(GovernanceTraceDetailVO traceDetail) {
        if (traceDetail == null || traceDetail.getAuditEvents() == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> refs = new ArrayList<Map<String, Object>>();
        for (GovernanceTraceDetailVO.AuditEventVO auditEvent : traceDetail.getAuditEvents()) {
            LinkedHashMap<String, Object> ref = new LinkedHashMap<String, Object>();
            ref.put("auditId", auditEvent.getId());
            ref.put("serviceCode", auditEvent.getServiceCode());
            ref.put("operationType", auditEvent.getOperationType());
            ref.put("status", auditEvent.getStatus());
            ref.put("createTime", auditEvent.getCreateTime());
            ref.put("exportId", auditEvent.getExportId());
            refs.add(ref);
        }
        return refs;
    }

    protected List<Map<String, Object>> buildBenchmarkRefs(GovernanceTraceDetailVO traceDetail) {
        if (traceDetail == null || traceDetail.getExportRecords() == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> refs = new ArrayList<Map<String, Object>>();
        for (GovernanceTraceDetailVO.ExportRecordVO exportRecordVO : traceDetail.getExportRecords()) {
            LinkedHashMap<String, Object> ref = new LinkedHashMap<String, Object>();
            ref.put("exportId", exportRecordVO.getExportId());
            ref.put("exportFormat", exportRecordVO.getExportFormat());
            ref.put("exportStatus", exportRecordVO.getExportStatus());
            ref.put("storageType", exportRecordVO.getStorageType());
            ref.put("createTime", exportRecordVO.getCreateTime());
            refs.add(ref);
        }
        return refs;
    }

    protected List<Map<String, Object>> readNestedList(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        Object value = source.get(key);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> refs = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                refs.add(new LinkedHashMap<String, Object>((Map<String, Object>) item));
            }
        }
        return refs;
    }

    protected String decryptSqlText(byte[] cipher) {
        if (cipher == null || cipher.length == 0 || sensitiveDataCryptoService == null) {
            return null;
        }
        return new String(sensitiveDataCryptoService.decryptBytes(cipher), java.nio.charset.StandardCharsets.UTF_8);
    }

    protected String requireSupportedExportFormat(String exportFormat) {
        String normalized = trimToNull(exportFormat);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "exportFormat 不能为空"
            );
        }
        if ("CSV".equalsIgnoreCase(normalized)
            || "EXCEL".equalsIgnoreCase(normalized)
            || "JSON".equalsIgnoreCase(normalized)
            || "SQL_TEXT".equalsIgnoreCase(normalized)
            || "PDF_REPORT".equalsIgnoreCase(normalized)) {
            return normalized.toUpperCase();
        }
        throw new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            "不支持的 exportFormat"
        );
    }

    protected String renderExportPayload(String exportFormat, GovernanceQueryHistoryDetailVO detail) {
        String logicalObjectSummary = joinLogicalObjectKeys(detail.getLogicalObjectHits());
        if ("JSON".equals(exportFormat)) {
            return JsonUtils.toJson(detail);
        }
        if ("SQL_TEXT".equals(exportFormat)) {
            StringBuilder builder = new StringBuilder();
            builder.append("-- 历史 ID history_id=").append(detail.getHistoryId()).append('\n');
            builder.append("-- 报表编码 report_code=").append(firstNonBlank(detail.getReportCode(), "-")).append('\n');
            builder.append("-- 数据源 datasource=").append(firstNonBlank(detail.getDatasourceCode(), "-")).append('\n');
            builder.append("-- 逻辑对象键 logical_object_keys=").append(firstNonBlank(logicalObjectSummary, "-")).append('\n');
            builder.append("-- 是否应用改写 rewrite_applied=").append(rewriteAuditText(detail, "rewriteApplied", "false")).append('\n');
            builder.append("-- 改写记录 ID rewrite_record_id=").append(rewriteAuditText(detail, "rewriteRecordId", "-")).append('\n');
            builder.append("-- 运行时绑定 ID runtime_binding_id=").append(rewriteAuditText(detail, "runtimeBindingId", "-")).append('\n');
            builder.append("-- 规则版本 rule_version=").append(rewriteAuditText(detail, "ruleVersion", "-")).append('\n');
            builder.append("-- 运行时规则版本 runtime_rule_version=").append(rewriteAuditText(detail, "runtimeRuleVersion", "-")).append('\n');
            builder.append("-- 激活状态快照 activation_status_snapshot=").append(rewriteAuditText(detail, "activationStatusSnapshot", "-")).append('\n');
            builder.append('\n').append("-- SQL 文本 sql_text").append('\n').append(firstNonBlank(detail.getSqlText(), "-- 不可用"));
            builder.append('\n').append('\n').append("-- SQL 模板文本 sql_template_text").append('\n')
                .append(firstNonBlank(detail.getSqlTemplateText(), "-- 不可用"));
            builder.append('\n').append('\n').append("-- 绑定后 SQL 文本 bound_sql_text").append('\n')
                .append(firstNonBlank(detail.getBoundSqlText(), "-- 不可用"));
            return builder.toString();
        }
        if ("CSV".equals(exportFormat)) {
            return "historyId,reportCode,datasourceCode,stageCode,resultStatus,targetEngine,returnedRowCount,cacheHit,rewriteApplied,rewriteRecordId,runtimeBindingId,ruleVersion,runtimeRuleVersion,activationStatusSnapshot,rewriteFallbackReason,accelerationApplied,logicalObjectKeys\n"
                + csvCell(detail.getHistoryId()) + ","
                + csvCell(detail.getReportCode()) + ","
                + csvCell(detail.getDatasourceCode()) + ","
                + csvCell(detail.getStageCode()) + ","
                + csvCell(String.valueOf(detail.getExecutionSummary().get("status"))) + ","
                + csvCell(String.valueOf(detail.getExecutionSummary().get("targetEngine"))) + ","
                + csvCell(String.valueOf(detail.getExecutionSummary().get("returnedRowCount"))) + ","
                + csvCell(String.valueOf(detail.getExecutionSummary().get("cacheHit"))) + ","
                + csvCell(rewriteAuditText(detail, "rewriteApplied", "false")) + ","
                + csvCell(rewriteAuditText(detail, "rewriteRecordId", "")) + ","
                + csvCell(rewriteAuditText(detail, "runtimeBindingId", "")) + ","
                + csvCell(rewriteAuditText(detail, "ruleVersion", "")) + ","
                + csvCell(rewriteAuditText(detail, "runtimeRuleVersion", "")) + ","
                + csvCell(rewriteAuditText(detail, "activationStatusSnapshot", "")) + ","
                + csvCell(rewriteAuditText(detail, "rewriteFallbackReason", "")) + ","
                + csvCell(String.valueOf(detail.getExecutionSummary().get("accelerationApplied"))) + ","
                + csvCell(logicalObjectSummary);
        }
        if ("EXCEL".equals(exportFormat)) {
            return "historyId\treportCode\tdatasourceCode\tstageCode\tresultStatus\ttargetEngine\treturnedRowCount\tcacheHit\trewriteApplied\trewriteRecordId\truntimeBindingId\truleVersion\truntimeRuleVersion\tactivationStatusSnapshot\trewriteFallbackReason\taccelerationApplied\tlogicalObjectKeys\n"
                + firstNonBlank(detail.getHistoryId(), "") + "\t"
                + firstNonBlank(detail.getReportCode(), "") + "\t"
                + firstNonBlank(detail.getDatasourceCode(), "") + "\t"
                + firstNonBlank(detail.getStageCode(), "") + "\t"
                + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("status")), "") + "\t"
                + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("targetEngine")), "") + "\t"
                + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("returnedRowCount")), "") + "\t"
                + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("cacheHit")), "") + "\t"
                + rewriteAuditText(detail, "rewriteApplied", "false") + "\t"
                + rewriteAuditText(detail, "rewriteRecordId", "") + "\t"
                + rewriteAuditText(detail, "runtimeBindingId", "") + "\t"
                + rewriteAuditText(detail, "ruleVersion", "") + "\t"
                + rewriteAuditText(detail, "runtimeRuleVersion", "") + "\t"
                + rewriteAuditText(detail, "activationStatusSnapshot", "") + "\t"
                + rewriteAuditText(detail, "rewriteFallbackReason", "") + "\t"
                + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("accelerationApplied")), "") + "\t"
                + firstNonBlank(logicalObjectSummary, "");
        }
        return "SQL 历史证据报告\n"
            + "历史 ID historyId: " + firstNonBlank(detail.getHistoryId(), "-") + "\n"
            + "报表编码 reportCode: " + firstNonBlank(detail.getReportCode(), "-") + "\n"
            + "数据源编码 datasourceCode: " + firstNonBlank(detail.getDatasourceCode(), "-") + "\n"
            + "结果状态 resultStatus: " + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("status")), "-") + "\n"
            + "目标引擎 targetEngine: " + firstNonBlank(String.valueOf(detail.getExecutionSummary().get("targetEngine")), "-") + "\n"
            + "是否应用改写 rewriteApplied: " + rewriteAuditText(detail, "rewriteApplied", "false") + "\n"
            + "改写记录 ID rewriteRecordId: " + rewriteAuditText(detail, "rewriteRecordId", "-") + "\n"
            + "运行时绑定 ID runtimeBindingId: " + rewriteAuditText(detail, "runtimeBindingId", "-") + "\n"
            + "规则版本 ruleVersion: " + rewriteAuditText(detail, "ruleVersion", "-") + "\n"
            + "运行时规则版本 runtimeRuleVersion: " + rewriteAuditText(detail, "runtimeRuleVersion", "-") + "\n"
            + "激活状态快照 activationStatusSnapshot: " + rewriteAuditText(detail, "activationStatusSnapshot", "-") + "\n"
            + "逻辑对象键 logicalObjectKeys: " + firstNonBlank(logicalObjectSummary, "-") + "\n"
            + "查询日期状态 queryDateStatus: " + firstNonBlank(String.valueOf(detail.getQueryDateSummary().get("queryDateStatus")), "-") + "\n"
            + "备注：阶段 1 的 PDF 基线以内联文本证据载荷形式输出。\n";
    }

    protected String rewriteAuditText(GovernanceQueryHistoryDetailVO detail, String key, String fallback) {
        if (detail == null || detail.getRewriteAudit() == null) {
            return fallback;
        }
        Object value = detail.getRewriteAudit().get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) && !"null".equals(text) ? text : fallback;
    }

    protected String csvCell(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    protected String buildExportFileName(String historyId, String exportFormat) {
        if ("EXCEL".equals(exportFormat)) {
            return historyId + ".xlsx";
        }
        if ("JSON".equals(exportFormat)) {
            return historyId + ".json";
        }
        if ("SQL_TEXT".equals(exportFormat)) {
            return historyId + ".sql";
        }
        if ("PDF_REPORT".equals(exportFormat)) {
            return historyId + ".pdf";
        }
        return historyId + ".csv";
    }

    protected String resolveExportContentType(String exportFormat) {
        if ("EXCEL".equals(exportFormat)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if ("JSON".equals(exportFormat)) {
            return "application/json";
        }
        if ("SQL_TEXT".equals(exportFormat)) {
            return "text/sql";
        }
        if ("PDF_REPORT".equals(exportFormat)) {
            return "application/pdf";
        }
        return "text/csv";
    }

    protected void persistExportRecord(String exportId,
                                     String tenantId,
                                     GovernanceQueryHistoryProjection row,
                                     GovernanceQueryHistoryExportRequest request,
                                     String exportFormat,
                                     String fileName,
                                     String contentType,
                                     String storageUri,
                                     LocalDateTime exportedAt) {
        if (governanceProtectedPersistenceService == null) {
            return;
        }
        ExportRecord exportRecord = new ExportRecord();
        exportRecord.setExportId(exportId);
        exportRecord.setHistoryId(row.getHistoryId());
        exportRecord.setResultId(row.getResultId());
        exportRecord.setTenantId(tenantId);
        exportRecord.setExportFormat(exportFormat);
        exportRecord.setExportStatus(DEFAULT_EXPORT_STATUS);
        exportRecord.setTraceId(row.getTraceId());
        exportRecord.setRequestId(RequestContext.getRequestId());
        exportRecord.setSagaId(RequestContext.getRequestId());
        exportRecord.setStorageType(DEFAULT_EXPORT_STORAGE_TYPE);
        exportRecord.setStorageUri(storageUri);
        exportRecord.setExportOptions(JsonUtils.toJson(buildExportOptions(request, exportFormat, fileName, contentType)));
        exportRecord.setCreatedBy(RequestContext.getUserId());
        exportRecord.setCreateTime(exportedAt);
        exportRecord.setFinishedAt(exportedAt);
        governanceProtectedPersistenceService.saveExportRecord(exportRecord);
    }

    protected Map<String, Object> buildExportOptions(GovernanceQueryHistoryExportRequest request,
                                                   String exportFormat,
                                                   String fileName,
                                                   String contentType) {
        LinkedHashMap<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("fileName", fileName);
        options.put("contentType", contentType);
        options.put("includeTraceDetail", Boolean.valueOf(Boolean.TRUE.equals(request == null ? null : request.getIncludeTraceDetail())));
        options.put("exportReason", trimToNull(request == null ? null : request.getExportReason()));
        options.put("inline", Boolean.TRUE);
        options.put("renderMode", "PDF_REPORT".equals(exportFormat) ? "SIMULATED_TEXTUAL_PDF_BASELINE" : "INLINE_BASELINE");
        return options;
    }

    protected void persistExportAudit(String exportId,
                                    GovernanceQueryHistoryProjection row,
                                    String tenantId,
                                    GovernanceQueryHistoryExportRequest request,
                                    String exportFormat,
                                    String fileName,
                                    String contentType,
                                    LocalDateTime exportedAt) {
        if (governanceProtectedPersistenceService == null) {
            return;
        }
        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setTenantId(tenantId);
        auditLogRecord.setServiceCode(ServiceCodeConstants.GOVERNANCE);
        auditLogRecord.setOperationType(OPERATION_QUERY_HISTORY_EXPORT);
        auditLogRecord.setTargetType(TARGET_QUERY_HISTORY);
        auditLogRecord.setTargetId(row.getHistoryId());
        auditLogRecord.setRequestId(RequestContext.getRequestId());
        auditLogRecord.setTraceId(firstNonBlank(row.getTraceId(), RequestContext.getTraceId()));
        auditLogRecord.setSagaId(RequestContext.getRequestId());
        auditLogRecord.setResultId(row.getResultId());
        auditLogRecord.setHistoryId(row.getHistoryId());
        auditLogRecord.setExportId(exportId);
        auditLogRecord.setRequestParams(JsonUtils.toJson(buildExportAuditRequestParams(request, exportFormat)));
        auditLogRecord.setResponseSummary(JsonUtils.toJson(buildAuditReference(exportId, row, exportFormat, exportedAt, fileName, contentType)));
        auditLogRecord.setStatus(DEFAULT_EXPORT_STATUS);
        auditLogRecord.setCostMs(Long.valueOf(0L));
        auditLogRecord.setCreateTime(exportedAt);
        governanceProtectedPersistenceService.saveAuditLog(auditLogRecord);
    }

    protected Map<String, Object> buildExportAuditRequestParams(GovernanceQueryHistoryExportRequest request,
                                                              String exportFormat) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("historyId", request == null ? null : request.getHistoryId());
        params.put("exportFormat", exportFormat);
        params.put("includeTraceDetail", request == null ? null : request.getIncludeTraceDetail());
        params.put("exportReason", request == null ? null : request.getExportReason());
        return params;
    }

    protected Map<String, Object> buildAuditReference(String exportId,
                                                    GovernanceQueryHistoryProjection row,
                                                    String exportFormat,
                                                    LocalDateTime exportedAt) {
        return buildAuditReference(exportId, row, exportFormat, exportedAt, null, null);
    }

    protected Map<String, Object> buildAuditReference(String exportId,
                                                    GovernanceQueryHistoryProjection row,
                                                    String exportFormat,
                                                    LocalDateTime exportedAt,
                                                    String fileName,
                                                    String contentType) {
        LinkedHashMap<String, Object> auditReference = new LinkedHashMap<String, Object>();
        auditReference.put("serviceCode", ServiceCodeConstants.GOVERNANCE);
        auditReference.put("operationType", OPERATION_QUERY_HISTORY_EXPORT);
        auditReference.put("historyId", row.getHistoryId());
        auditReference.put("resultId", row.getResultId());
        auditReference.put("traceId", row.getTraceId());
        auditReference.put("requestId", RequestContext.getRequestId());
        auditReference.put("exportId", exportId);
        auditReference.put("exportFormat", exportFormat);
        auditReference.put("status", DEFAULT_EXPORT_STATUS);
        auditReference.put("exportedAt", exportedAt);
        if (fileName != null) {
            auditReference.put("fileName", fileName);
        }
        if (contentType != null) {
            auditReference.put("contentType", contentType);
        }
        return auditReference;
    }

    protected Map<String, Object> buildParseSummary(Map<String, Object> queryContext, String key) {
        return selectFirstNonEmptyMap(Collections.<String, Object>emptyMap(), queryContext, key);
    }

    protected Map<String, Object> selectFirstNonEmptyMap(Map<String, Object> primary, Map<String, Object> secondary, String nestedKey) {
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        if (secondary != null) {
            Map<String, Object> nested = readNestedMap(secondary, nestedKey);
            if (!nested.isEmpty()) {
                return nested;
            }
        }
        return Collections.emptyMap();
    }

    protected static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    protected String normalizeBatchOperationType(String operationType) {
        String normalized = trimToNull(operationType);
        if (BATCH_OPERATION_RETENTION.equalsIgnoreCase(normalized)) {
            return BATCH_OPERATION_RETENTION;
        }
        if (BATCH_OPERATION_RECOVERY.equalsIgnoreCase(normalized)) {
            return BATCH_OPERATION_RECOVERY;
        }
        throw new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            "不支持的治理产物批处理 operationType：" + operationType
        );
    }

    protected String mapBatchItemOperationType(String batchOperationType) {
        return BATCH_OPERATION_RETENTION.equals(batchOperationType) ? ITEM_OPERATION_CLEANUP : ITEM_OPERATION_RECOVER;
    }

    protected String resolveBatchCleanupScope(String batchOperationType, String cleanupScope) {
        if (BATCH_OPERATION_RETENTION.equals(batchOperationType)) {
            return trimToNull(cleanupScope) == null ? BATCH_DEFAULT_RETENTION_SCOPE : trimToNull(cleanupScope);
        }
        return trimToNull(cleanupScope);
    }

    protected String resolveBatchId(String requestedBatchId) {
        String normalized = trimToNull(requestedBatchId);
        if (normalized != null) {
            return normalized;
        }
        String requestId = trimToNull(RequestContext.getRequestId());
        String traceId = trimToNull(RequestContext.getTraceId());
        String suffix = requestId != null ? requestId : traceId != null ? traceId : String.valueOf(System.currentTimeMillis());
        return "artifact-batch-" + suffix;
    }

    protected GovernanceBenchmarkArtifactOperationResponse buildBatchFailureItem(String tenantId,
                                                                               String reportId,
                                                                               String artifactKey,
                                                                               String itemOperationType,
                                                                               String orchestrationType,
                                                                               String batchId,
                                                                               int batchIndex,
                                                                               int batchSize,
                                                                               Integer errorCode,
                                                                               String errorMessage) {
        GovernanceBenchmarkArtifactOperationResponse response = new GovernanceBenchmarkArtifactOperationResponse();
        response.setTenantId(tenantId);
        response.setReportId(reportId);
        response.setArtifactKey(artifactKey);
        response.setOperationType(itemOperationType);
        response.setOperationStatus("FAILED");
        response.setArtifactRecoveryStatus("FAILED");
        response.setStorageRecoverySource("FAILED");
        response.setStorageReadStatus("FAILED");
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        populateBatchMetadata(response, orchestrationType, batchId, batchIndex, batchSize);
        LinkedHashMap<String, Object> operationSurface = new LinkedHashMap<String, Object>();
        operationSurface.put("operationType", itemOperationType);
        operationSurface.put("operationStatus", "FAILED");
        operationSurface.put("orchestrationType", orchestrationType);
        operationSurface.put("batchId", batchId);
        operationSurface.put("batchIndex", Integer.valueOf(batchIndex));
        operationSurface.put("batchSize", Integer.valueOf(batchSize));
        operationSurface.put("failureReason", errorMessage);
        response.setArtifactOperationSurface(operationSurface);
        return response;
    }

    protected GovernanceBenchmarkArtifactOperationResponse buildBatchSkippedItem(String tenantId,
                                                                               String reportId,
                                                                               String artifactKey,
                                                                               String itemOperationType,
                                                                               String orchestrationType,
                                                                               String batchId,
                                                                               int batchIndex,
                                                                               int batchSize,
                                                                               String message) {
        GovernanceBenchmarkArtifactOperationResponse response = new GovernanceBenchmarkArtifactOperationResponse();
        response.setTenantId(tenantId);
        response.setReportId(reportId);
        response.setArtifactKey(artifactKey);
        response.setOperationType(itemOperationType);
        response.setOperationStatus("SKIPPED_DUPLICATE");
        response.setErrorMessage(message);
        populateBatchMetadata(response, orchestrationType, batchId, batchIndex, batchSize);
        LinkedHashMap<String, Object> operationSurface = new LinkedHashMap<String, Object>();
        operationSurface.put("operationType", itemOperationType);
        operationSurface.put("operationStatus", "SKIPPED_DUPLICATE");
        operationSurface.put("orchestrationType", orchestrationType);
        operationSurface.put("batchId", batchId);
        operationSurface.put("batchIndex", Integer.valueOf(batchIndex));
        operationSurface.put("batchSize", Integer.valueOf(batchSize));
        operationSurface.put("skipReason", message);
        response.setArtifactOperationSurface(operationSurface);
        return response;
    }

    protected void populateBatchMetadata(GovernanceBenchmarkArtifactOperationResponse response,
                                       String orchestrationType,
                                       String batchId,
                                       int batchIndex,
                                       int batchSize) {
        if (response == null) {
            return;
        }
        response.setOrchestrationType(orchestrationType);
        response.setBatchId(batchId);
        response.setBatchIndex(Integer.valueOf(batchIndex));
        response.setBatchSize(Integer.valueOf(batchSize));
    }

    protected String resolveBatchOperationStatus(int succeededItems, int failedItems, int skippedItems) {
        if (failedItems > 0 && succeededItems == 0 && skippedItems == 0) {
            return "BATCH_FAILED";
        }
        if (failedItems > 0) {
            return "PARTIAL_FAILURE";
        }
        return "BATCH_COMPLETED";
    }

    protected Map<String, Object> buildBatchOperationSurface(String batchId,
                                                           String batchOperationType,
                                                           String itemOperationType,
                                                           String cleanupScope,
                                                           int totalItems,
                                                           int succeededItems,
                                                           int failedItems,
                                                           int skippedItems) {
        LinkedHashMap<String, Object> surface = new LinkedHashMap<String, Object>();
        surface.put("batchId", batchId);
        surface.put("requestedOperationType", batchOperationType);
        surface.put("mappedItemOperationType", itemOperationType);
        surface.put("cleanupScope", cleanupScope);
        surface.put("totalItems", Integer.valueOf(totalItems));
        surface.put("succeededItems", Integer.valueOf(succeededItems));
        surface.put("failedItems", Integer.valueOf(failedItems));
        surface.put("skippedItems", Integer.valueOf(skippedItems));
        surface.put("partialFailure", Boolean.valueOf(failedItems > 0));
        surface.put("traceId", trimToNull(RequestContext.getTraceId()));
        surface.put("requestId", trimToNull(RequestContext.getRequestId()));
        return surface;
    }

}
