package com.company.governance.application.service;

import com.company.governance.application.controller.dto.GovernanceQueryHistoryExportRequest;
import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryExportVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryPageVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryRewriteRecordVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryRewriteRecordsVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistorySummaryVO;
import com.company.governance.application.controller.vo.GovernanceTraceLookupPageVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
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
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationTarget;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceHistoryApplicationService extends GovernanceHistoryApplicationServiceSupport {

    @Autowired
    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic,
                                               GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient,
                                               GovernanceSqlOptimizationClient governanceSqlOptimizationClient,
                                               GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                               SensitiveDataCryptoService sensitiveDataCryptoService) {
        super(
            auditLogMapper,
            governanceHistoryLookupIndexMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            governanceBenchmarkEngineClient,
            governanceSqlOptimizationClient,
            governanceProtectedPersistenceService,
            sensitiveDataCryptoService
        );
    }

    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic) {
        this(auditLogMapper, null, queryHistoryMapper, exportRecordMapper, tenantAccessLogic, null, null, null, null);
    }

    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic) {
        this(
            auditLogMapper,
            governanceHistoryLookupIndexMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            null,
            null,
            null,
            null
        );
    }

    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic,
                                               GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient) {
        this(
            auditLogMapper,
            governanceHistoryLookupIndexMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            governanceBenchmarkEngineClient,
            null,
            null,
            null
        );
    }

    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic,
                                               GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient,
                                               GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                               SensitiveDataCryptoService sensitiveDataCryptoService) {
        this(
            auditLogMapper,
            governanceHistoryLookupIndexMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            governanceBenchmarkEngineClient,
            null,
            governanceProtectedPersistenceService,
            sensitiveDataCryptoService
        );
    }

    public List<GovernanceTraceSummaryVO> findRecentTraces(String tenantId, Integer limit) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        int resolvedLimit = normalizeLimit(limit);
        int recentSourceScanLimit = resolveRecentSourceScanLimit(resolvedLimit);
        List<TraceAggregate> aggregates = loadRecentAggregates(effectiveTenantId, recentSourceScanLimit);

        List<GovernanceTraceSummaryVO> summaries = new ArrayList<GovernanceTraceSummaryVO>();
        List<String> displayGroupKeys = new ArrayList<String>();
        for (TraceAggregate aggregate : aggregates) {
            if (!aggregate.shouldDisplayInRecentList()) {
                continue;
            }
            String displayGroupKey = aggregate.toDisplayGroupKey();
            if (displayGroupKeys.contains(displayGroupKey)) {
                continue;
            }
            displayGroupKeys.add(displayGroupKey);
            summaries.add(aggregate.toSummaryVO());
            if (summaries.size() >= resolvedLimit) {
                break;
            }
        }
        LOGGER.info("已加载治理追溯摘要，tenantId={}, count={}, traceIdSample={}",
            effectiveTenantId,
            Integer.valueOf(summaries.size()),
            summaries.isEmpty() ? "-" : summaries.get(0).getTraceId());
        return summaries;
    }

    public GovernanceQueryHistoryPageVO findQueryHistoryPage(String tenantId,
                                                             String historyType,
                                                             String reportCode,
                                                             String datasourceCode,
                                                             String stageCode,
                                                             String bizDate,
                                                             String queryDateStart,
                                                             String queryDateEnd,
                                                             String status,
                                                             Boolean cacheHit,
                                                             Boolean rewriteApplied,
                                                             Boolean accelerationApplied,
                                                             Boolean parameterizedSql,
                                                             String logicalObjectType,
                                                             String accessChannel,
                                                             String engine,
                                                             String submittedBy,
                                                             String submittedStart,
                                                             String submittedEnd,
                                                             Boolean hasRewriteRecord,
                                                             String rewriteValidationStatus,
                                                             String rewriteSourceType,
                                                             String recommendationId,
                                                             String sortBy,
                                                             String sortOrder,
                                                             Integer pageNo,
                                                             Integer pageSize) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        int resolvedPageNo = normalizePageNo(pageNo);
        int resolvedPageSize = normalizeLimit(pageSize);
        int offset = (resolvedPageNo - 1) * resolvedPageSize;
        String normalizedReportCode = trimToNull(reportCode);
        String normalizedHistoryType = normalizeHistoryTypeFilter(historyType);
        String normalizedDatasourceCode = trimToNull(datasourceCode);
        String normalizedStageCode = trimToNull(stageCode);
        LocalDate parsedBizDate = parseDateValue(bizDate, "bizDate");
        LocalDate parsedQueryDateStart = parseDateValue(queryDateStart, "queryDateStart");
        LocalDate parsedQueryDateEnd = parseDateValue(queryDateEnd, "queryDateEnd");
        String normalizedStatus = trimToNull(status);
        String normalizedLogicalObjectType = trimToNull(logicalObjectType);
        String normalizedAccessChannel = trimToNull(accessChannel);
        String normalizedEngine = trimToNull(engine);
        String normalizedSubmittedBy = trimToNull(submittedBy);
        LocalDateTime parsedSubmittedStart = parseWindowValue(submittedStart, "submittedStart");
        LocalDateTime parsedSubmittedEnd = parseWindowValue(submittedEnd, "submittedEnd");
        RewriteHistoryScope rewriteHistoryScope = resolveRewriteHistoryScope(
            effectiveTenantId,
            hasRewriteRecord,
            rewriteValidationStatus,
            rewriteSourceType,
            recommendationId
        );
        if (rewriteHistoryScope.isEmptyInclude()) {
            return emptyQueryHistoryPage(resolvedPageNo, resolvedPageSize);
        }
        List<GovernanceQueryHistoryProjection> rows = queryHistoryMapper.selectHistoryPage(
            effectiveTenantId,
            normalizedHistoryType,
            normalizedReportCode,
            normalizedDatasourceCode,
            normalizedStageCode,
            parsedBizDate,
            parsedQueryDateStart,
            parsedQueryDateEnd,
            normalizedStatus,
            cacheHit,
            rewriteApplied,
            accelerationApplied,
            parameterizedSql,
            normalizedLogicalObjectType,
            normalizedAccessChannel,
            normalizedEngine,
            normalizedSubmittedBy,
            parsedSubmittedStart,
            parsedSubmittedEnd,
            rewriteHistoryScope.includeHistoryIds,
            rewriteHistoryScope.excludeHistoryIds,
            resolveHistoryOrderBy(sortBy, sortOrder),
            offset,
            resolvedPageSize + LOOKUP_PAGE_FETCH_OVERFLOW
        );
        int totalCount = queryHistoryMapper.countHistoryPage(
            effectiveTenantId,
            normalizedHistoryType,
            normalizedReportCode,
            normalizedDatasourceCode,
            normalizedStageCode,
            parsedBizDate,
            parsedQueryDateStart,
            parsedQueryDateEnd,
            normalizedStatus,
            cacheHit,
            rewriteApplied,
            accelerationApplied,
            parameterizedSql,
            normalizedLogicalObjectType,
            normalizedAccessChannel,
            normalizedEngine,
            normalizedSubmittedBy,
            parsedSubmittedStart,
            parsedSubmittedEnd,
            rewriteHistoryScope.includeHistoryIds,
            rewriteHistoryScope.excludeHistoryIds
        );
        boolean hasMore = rows.size() > resolvedPageSize;
        if (hasMore) {
            rows = new ArrayList<GovernanceQueryHistoryProjection>(rows.subList(0, resolvedPageSize));
        }
        List<GovernanceQueryHistorySummaryVO> items = new ArrayList<GovernanceQueryHistorySummaryVO>(rows.size());
        for (GovernanceQueryHistoryProjection row : rows) {
            items.add(toQueryHistorySummary(row));
        }
        return new GovernanceQueryHistoryPageVO(
            items,
            Integer.valueOf(resolvedPageNo),
            Integer.valueOf(resolvedPageSize),
            Integer.valueOf(totalCount),
            Integer.valueOf(pageCount(totalCount, resolvedPageSize)),
            Boolean.valueOf(hasMore),
            buildHistoryClassificationSummary(items)
        );
    }

    private RewriteHistoryScope resolveRewriteHistoryScope(String tenantId,
                                                           Boolean hasRewriteRecord,
                                                           String rewriteValidationStatus,
                                                           String rewriteSourceType,
                                                           String recommendationId) {
        String normalizedValidationStatus = trimToNull(rewriteValidationStatus);
        String normalizedSourceType = trimToNull(rewriteSourceType);
        String normalizedRecommendationId = trimToNull(recommendationId);
        boolean hasRewriteFilters = hasRewriteRecord != null
            || StringUtils.hasText(normalizedValidationStatus)
            || StringUtils.hasText(normalizedSourceType)
            || StringUtils.hasText(normalizedRecommendationId);
        if (!hasRewriteFilters) {
            return RewriteHistoryScope.unfiltered();
        }
        if (governanceSqlOptimizationClient == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "governance 的 sql-optimization 客户端未配置"
            );
        }
        if (Boolean.FALSE.equals(hasRewriteRecord)) {
            List<SqlOptimizationRewriteRecordResponse> records =
                governanceSqlOptimizationClient.listRewriteRecords(null, null, null, null);
            return RewriteHistoryScope.exclude(extractRewriteHistoryIds(records, tenantId));
        }
        List<SqlOptimizationRewriteRecordResponse> records =
            governanceSqlOptimizationClient.listRewriteRecords(
                null,
                normalizedRecommendationId,
                normalizedValidationStatus,
                normalizedSourceType
            );
        return RewriteHistoryScope.include(extractRewriteHistoryIds(records, tenantId));
    }

    private GovernanceQueryHistoryPageVO emptyQueryHistoryPage(int pageNo, int pageSize) {
        List<GovernanceQueryHistorySummaryVO> items = Collections.emptyList();
        return new GovernanceQueryHistoryPageVO(
            items,
            Integer.valueOf(pageNo),
            Integer.valueOf(pageSize),
            Integer.valueOf(0),
            Integer.valueOf(0),
            Boolean.FALSE,
            buildHistoryClassificationSummary(items)
        );
    }

    private List<String> extractRewriteHistoryIds(List<SqlOptimizationRewriteRecordResponse> records, String tenantId) {
        LinkedHashSet<String> historyIds = new LinkedHashSet<String>();
        if (records == null) {
            return new ArrayList<String>(historyIds);
        }
        for (SqlOptimizationRewriteRecordResponse record : records) {
            String historyId = trimToNull(record == null ? null : record.getHistoryId());
            if (StringUtils.hasText(historyId) && record != null && tenantId.equals(record.getTenantId())) {
                historyIds.add(historyId);
            }
        }
        return new ArrayList<String>(historyIds);
    }

    public static String allowedHistoryTypeMessage() {
        return "historyType 必须是以下值之一：" + String.join("/", ALLOWED_HISTORY_TYPES);
    }

    public static String normalizeHistoryTypeFilter(String historyType) {
        String normalizedHistoryType = trimToNull(historyType);
        if (!StringUtils.hasText(normalizedHistoryType)) {
            return null;
        }
        String upperHistoryType = normalizedHistoryType.toUpperCase();
        if (!ALLOWED_HISTORY_TYPES.contains(upperHistoryType)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                allowedHistoryTypeMessage()
            );
        }
        return upperHistoryType;
    }

    public GovernanceQueryHistoryDetailVO findQueryHistoryDetail(String tenantId, String historyId) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        if (!StringUtils.hasText(historyId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "historyId 不能为空"
            );
        }
        GovernanceQueryHistoryProjection row =
            queryHistoryMapper.selectHistoryDetail(effectiveTenantId, historyId.trim());
        if (row == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.NOT_FOUND,
                "查询历史记录不存在"
            );
        }
        QueryHistoryRecord historyRecord = queryHistoryMapper.selectById(historyId.trim());
        GovernanceQueryHistoryDetailVO detailVO = toQueryHistoryDetail(row, historyRecord);
        if (StringUtils.hasText(row.getTraceId())) {
            detailVO.setTraceDetail(findTraceDetail(effectiveTenantId, row.getTraceId(), Integer.valueOf(10)));
        }
        populateReferenceSurfaces(detailVO);
        return detailVO;
    }

    public GovernanceQueryHistoryRewriteRecordsVO findQueryHistoryRewriteRecords(String tenantId, String historyId) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        String normalizedHistoryId = trimToNull(historyId);
        if (!StringUtils.hasText(normalizedHistoryId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "historyId 不能为空"
            );
        }
        GovernanceQueryHistoryProjection row =
            queryHistoryMapper.selectHistoryDetail(effectiveTenantId, normalizedHistoryId);
        if (row == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.NOT_FOUND,
                "查询历史记录不存在"
            );
        }
        if (governanceSqlOptimizationClient == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "governance 的 sql-optimization 客户端未配置"
            );
        }
        List<SqlOptimizationRewriteRecordResponse> records =
            governanceSqlOptimizationClient.listRewriteRecordsByHistoryId(normalizedHistoryId);
        List<GovernanceQueryHistoryRewriteRecordVO> items =
            new ArrayList<GovernanceQueryHistoryRewriteRecordVO>();
        for (SqlOptimizationRewriteRecordResponse record : records) {
            if (sameRewriteRecordScope(record, effectiveTenantId, normalizedHistoryId)) {
                items.add(toQueryHistoryRewriteRecord(record));
            }
        }

        GovernanceQueryHistoryRewriteRecordsVO response = new GovernanceQueryHistoryRewriteRecordsVO();
        response.setTenantId(effectiveTenantId);
        response.setHistoryId(normalizedHistoryId);
        response.setRewriteRecordCount(Integer.valueOf(items.size()));
        response.setItems(items);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(REWRITE_RECORD_AGGREGATION_STAGE);
        return response;
    }

    public GovernanceQueryHistoryExportVO exportQueryHistory(String tenantId, GovernanceQueryHistoryExportRequest request) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        String historyId = trimToNull(request == null ? null : request.getHistoryId());
        if (!StringUtils.hasText(historyId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "historyId 不能为空"
            );
        }
        String exportFormat = requireSupportedExportFormat(request == null ? null : request.getExportFormat());
        GovernanceQueryHistoryProjection row = queryHistoryMapper.selectHistoryDetail(effectiveTenantId, historyId);
        if (row == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.NOT_FOUND,
                "查询历史记录不存在"
            );
        }
        QueryHistoryRecord historyRecord = queryHistoryMapper.selectById(historyId);
        GovernanceQueryHistoryDetailVO detail = toQueryHistoryDetail(row, historyRecord);
        if (Boolean.TRUE.equals(request == null ? null : request.getIncludeTraceDetail()) && StringUtils.hasText(row.getTraceId())) {
            detail.setTraceDetail(findTraceDetail(effectiveTenantId, row.getTraceId(), Integer.valueOf(10)));
        }
        populateReferenceSurfaces(detail);

        String payload = renderExportPayload(exportFormat, detail);
        LocalDateTime exportedAt = DateUtils.now();
        String exportId = "query-history-export-" + historyId + "-" + UUID.randomUUID().toString().replace("-", "");
        String fileName = buildExportFileName(historyId, exportFormat);
        String contentType = resolveExportContentType(exportFormat);
        String storageUri = "inline://governance/query-history/" + exportId;

        persistExportRecord(
            exportId,
            effectiveTenantId,
            row,
            request,
            exportFormat,
            fileName,
            contentType,
            storageUri,
            exportedAt
        );
        persistExportAudit(exportId, row, effectiveTenantId, request, exportFormat, fileName, contentType, exportedAt);

        GovernanceQueryHistoryExportVO response = new GovernanceQueryHistoryExportVO();
        response.setExportId(exportId);
        response.setHistoryId(row.getHistoryId());
        response.setResultId(row.getResultId());
        response.setTraceId(row.getTraceId());
        response.setExportFormat(exportFormat);
        response.setExportStatus(DEFAULT_EXPORT_STATUS);
        response.setFileName(fileName);
        response.setContentType(contentType);
        response.setStorageType(DEFAULT_EXPORT_STORAGE_TYPE);
        response.setStorageUri(storageUri);
        response.setExportedAt(exportedAt);
        response.setAuditReference(buildAuditReference(exportId, row, exportFormat, exportedAt));
        response.setPayload(payload);
        return response;
    }

    public GovernanceTraceLookupPageVO lookupTraces(String tenantId,
                                                    String traceId,
                                                    String taskId,
                                                    String reportId,
                                                    String windowStart,
                                                    String windowEnd,
                                                    String cursor,
                                                    Integer limit) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        String normalizedTraceId = trimToNull(traceId);
        String normalizedTaskId = trimToNull(taskId);
        String normalizedReportId = trimToNull(reportId);
        LocalDateTime windowStartAt = parseWindowValue(windowStart, "windowStart");
        LocalDateTime windowEndAt = parseWindowValue(windowEnd, "windowEnd");
        if (!StringUtils.hasText(normalizedTraceId)
            && !StringUtils.hasText(normalizedTaskId)
            && !StringUtils.hasText(normalizedReportId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "必须提供 traceId、taskId 或 reportId 中的一个"
            );
        }
        if (windowStartAt != null && windowEndAt != null && windowStartAt.isAfter(windowEndAt)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "windowStart 不能晚于 windowEnd"
            );
        }

        int resolvedLimit = normalizeLimit(limit);
        if (StringUtils.hasText(normalizedTraceId)) {
            TraceAggregate aggregate = loadTraceAggregate(effectiveTenantId, normalizedTraceId, TRACE_LOOKUP_SOURCE_LIMIT);
            List<GovernanceTraceSummaryVO> items = new ArrayList<GovernanceTraceSummaryVO>();
            if (aggregate.shouldDisplayInRecentList()
                && aggregate.matchesLookupCriteria(normalizedTraceId, normalizedTaskId, normalizedReportId)) {
                items.add(aggregate.toSummaryVO());
            }
            LOGGER.info("已加载治理追溯查询结果，tenantId={}, traceId={}, taskId={}, reportId={}, count={}, mode=TRACE",
                effectiveTenantId,
                normalizedTraceId,
                StringUtils.hasText(normalizedTaskId) ? normalizedTaskId : "-",
                StringUtils.hasText(normalizedReportId) ? normalizedReportId : "-",
                Integer.valueOf(items.size()));
            return new GovernanceTraceLookupPageVO(items, Boolean.FALSE, null);
        }

        LookupCursor lookupCursor = parseLookupCursor(cursor);
        List<TraceLookupHitRecord> hits = loadIndexedTraceHits(
            effectiveTenantId,
            normalizedTaskId,
            normalizedReportId,
            windowStartAt,
            windowEndAt,
            lookupCursor,
            resolvedLimit + LOOKUP_PAGE_FETCH_OVERFLOW
        );
        if (hits.isEmpty()) {
            LOGGER.info("已加载治理追溯索引查询结果，tenantId={}, traceId=-, taskId={}, reportId={}, count=0, mode=INDEXED",
                effectiveTenantId,
                StringUtils.hasText(normalizedTaskId) ? normalizedTaskId : "-",
                StringUtils.hasText(normalizedReportId) ? normalizedReportId : "-");
            return new GovernanceTraceLookupPageVO(Collections.<GovernanceTraceSummaryVO>emptyList(), Boolean.FALSE, null);
        }

        List<String> traceIds = new ArrayList<String>();
        for (TraceLookupHitRecord hit : hits) {
            if (StringUtils.hasText(hit.getTraceId()) && !traceIds.contains(hit.getTraceId())) {
                traceIds.add(hit.getTraceId());
            }
        }

        List<TraceAggregate> aggregates = loadAggregatesByTraceIds(effectiveTenantId, traceIds);
        Map<String, TraceAggregate> aggregateByTraceId = new HashMap<String, TraceAggregate>();
        for (TraceAggregate aggregate : aggregates) {
            aggregateByTraceId.put(aggregate.traceId, aggregate);
        }

        List<GovernanceTraceSummaryVO> matches = new ArrayList<GovernanceTraceSummaryVO>();
        TraceLookupHitRecord lastReturnedHit = null;
        boolean hasMore = false;
        for (TraceLookupHitRecord hit : hits) {
            TraceAggregate aggregate = aggregateByTraceId.get(hit.getTraceId());
            if (aggregate == null || !aggregate.shouldDisplayInRecentList()) {
                continue;
            }
            if (!aggregate.matchesLookupCriteria(null, normalizedTaskId, normalizedReportId)) {
                continue;
            }
            if (matches.size() < resolvedLimit) {
                matches.add(aggregate.toSummaryVO());
                lastReturnedHit = hit;
            } else {
                hasMore = true;
                break;
            }
        }

        String nextCursorValue = hasMore ? encodeCursor(lastReturnedHit) : null;
        LOGGER.info("已加载治理追溯索引查询结果，tenantId={}, traceId=-, taskId={}, reportId={}, count={}, hasMore={}, mode=INDEXED",
            effectiveTenantId,
            StringUtils.hasText(normalizedTaskId) ? normalizedTaskId : "-",
            StringUtils.hasText(normalizedReportId) ? normalizedReportId : "-",
            Integer.valueOf(matches.size()),
            Boolean.valueOf(hasMore));
        return new GovernanceTraceLookupPageVO(matches, Boolean.valueOf(hasMore), nextCursorValue);
    }

    public GovernanceTraceDetailVO findTraceDetail(String tenantId, String traceId, Integer limit) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        if (!StringUtils.hasText(traceId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "traceId 不能为空"
            );
        }

        int resolvedLimit = normalizeLimit(limit);
        TraceAggregate aggregate = new TraceAggregate(traceId.trim());
        mergeAuditLogs(aggregate, auditLogMapper.selectByTraceId(effectiveTenantId, traceId.trim(), resolvedLimit));
        mergeQueryHistories(aggregate, queryHistoryMapper.selectByTraceId(effectiveTenantId, traceId.trim(), resolvedLimit));
        mergeExportRecords(aggregate, exportRecordMapper.selectByTraceId(effectiveTenantId, traceId.trim(), resolvedLimit));

        LOGGER.info("已加载治理追溯详情，tenantId={}, traceId={}, auditEvents={}, histories={}, exports={}",
            effectiveTenantId,
            traceId,
            Integer.valueOf(aggregate.getAuditEventCount()),
            Integer.valueOf(aggregate.getQueryHistoryCount()),
            Integer.valueOf(aggregate.getExportRecordCount()));
        return aggregate.toDetailVO();
    }

    public GovernanceBenchmarkArtifactOperationResponse operateArtifact(
        GovernanceBenchmarkArtifactOperationRequest request
    ) {
        if (governanceBenchmarkEngineClient == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                "治理侧 benchmark-engine 客户端不可用"
            );
        }
        String effectiveTenantId = resolveAuthorizedTenantId(trimToNull(request == null ? null : request.getTenantId()));
        assertArtifactOperationRole();
        GovernanceBenchmarkArtifactOperationRequest internalRequest = new GovernanceBenchmarkArtifactOperationRequest();
        internalRequest.setTenantId(effectiveTenantId);
        internalRequest.setReportId(trimToNull(request == null ? null : request.getReportId()));
        internalRequest.setArtifactKey(trimToNull(request == null ? null : request.getArtifactKey()));
        internalRequest.setOperationType(trimToNull(request == null ? null : request.getOperationType()));
        internalRequest.setOperationReason(trimToNull(request == null ? null : request.getOperationReason()));
        internalRequest.setCleanupScope(trimToNull(request == null ? null : request.getCleanupScope()));
        internalRequest.setOrchestrationType(trimToNull(request == null ? null : request.getOrchestrationType()));
        internalRequest.setBatchId(trimToNull(request == null ? null : request.getBatchId()));
        internalRequest.setBatchIndex(request == null ? null : request.getBatchIndex());
        internalRequest.setBatchSize(request == null ? null : request.getBatchSize());
        return governanceBenchmarkEngineClient.operateArtifact(internalRequest);
    }

    public GovernanceBenchmarkArtifactBatchOperationResponse operateArtifactBatch(
        GovernanceBenchmarkArtifactBatchOperationRequest request
    ) {
        if (governanceBenchmarkEngineClient == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                "治理侧 benchmark-engine 客户端不可用"
            );
        }
        String effectiveTenantId = resolveAuthorizedTenantId(trimToNull(request == null ? null : request.getTenantId()));
        assertArtifactOperationRole();
        String batchOperationType = normalizeBatchOperationType(request == null ? null : request.getOperationType());
        List<GovernanceBenchmarkArtifactBatchOperationTarget> targets =
            request == null ? null : request.getTargets();
        if (targets == null || targets.isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "产物批处理目标不能为空"
            );
        }

        String batchId = resolveBatchId(request == null ? null : request.getBatchId());
        String itemOperationType = mapBatchItemOperationType(batchOperationType);
        String cleanupScope = resolveBatchCleanupScope(batchOperationType, request == null ? null : request.getCleanupScope());
        List<GovernanceBenchmarkArtifactOperationResponse> items =
            new ArrayList<GovernanceBenchmarkArtifactOperationResponse>(targets.size());
        Set<String> seenTargets = new LinkedHashSet<String>();
        int succeededItems = 0;
        int failedItems = 0;
        int skippedItems = 0;
        int batchSize = targets.size();
        int batchIndex = 0;

        for (GovernanceBenchmarkArtifactBatchOperationTarget target : targets) {
            batchIndex += 1;
            String reportId = trimToNull(target == null ? null : target.getReportId());
            String artifactKey = trimToNull(target == null ? null : target.getArtifactKey());
            if (!StringUtils.hasText(reportId) || !StringUtils.hasText(artifactKey)) {
                failedItems += 1;
                items.add(buildBatchFailureItem(
                    effectiveTenantId,
                    reportId,
                    artifactKey,
                    itemOperationType,
                    batchOperationType,
                    batchId,
                    batchIndex,
                    batchSize,
                    null,
                    "reportId 和 artifactKey 不能为空"
                ));
                continue;
            }
            String dedupeKey = reportId + "::" + artifactKey;
            if (!seenTargets.add(dedupeKey)) {
                skippedItems += 1;
                items.add(buildBatchSkippedItem(
                    effectiveTenantId,
                    reportId,
                    artifactKey,
                    itemOperationType,
                    batchOperationType,
                    batchId,
                    batchIndex,
                    batchSize,
                    "治理批处理编排已忽略重复目标"
                ));
                continue;
            }

            GovernanceBenchmarkArtifactOperationRequest internalRequest = new GovernanceBenchmarkArtifactOperationRequest();
            internalRequest.setTenantId(effectiveTenantId);
            internalRequest.setReportId(reportId);
            internalRequest.setArtifactKey(artifactKey);
            internalRequest.setOperationType(itemOperationType);
            internalRequest.setOperationReason(trimToNull(request == null ? null : request.getOperationReason()));
            internalRequest.setCleanupScope(cleanupScope);
            internalRequest.setOrchestrationType(batchOperationType);
            internalRequest.setBatchId(batchId);
            internalRequest.setBatchIndex(Integer.valueOf(batchIndex));
            internalRequest.setBatchSize(Integer.valueOf(batchSize));
            try {
                GovernanceBenchmarkArtifactOperationResponse itemResponse =
                    governanceBenchmarkEngineClient.operateArtifact(internalRequest);
                if (itemResponse == null) {
                    failedItems += 1;
                    items.add(buildBatchFailureItem(
                        effectiveTenantId,
                        reportId,
                        artifactKey,
                        itemOperationType,
                        batchOperationType,
                        batchId,
                        batchIndex,
                        batchSize,
                        null,
                        "压测引擎产物操作未返回响应"
                    ));
                    continue;
                }
                populateBatchMetadata(itemResponse, batchOperationType, batchId, batchIndex, batchSize);
                items.add(itemResponse);
                if ("FAILED".equalsIgnoreCase(itemResponse.getOperationStatus())) {
                    failedItems += 1;
                } else {
                    succeededItems += 1;
                }
            } catch (BizException ex) {
                failedItems += 1;
                items.add(buildBatchFailureItem(
                    effectiveTenantId,
                    reportId,
                    artifactKey,
                    itemOperationType,
                    batchOperationType,
                    batchId,
                    batchIndex,
                    batchSize,
                    Integer.valueOf(ex.getCode()),
                    ex.getMessage()
                ));
            } catch (RuntimeException ex) {
                failedItems += 1;
                items.add(buildBatchFailureItem(
                    effectiveTenantId,
                    reportId,
                    artifactKey,
                    itemOperationType,
                    batchOperationType,
                    batchId,
                    batchIndex,
                    batchSize,
                    null,
                    ex.getMessage()
                ));
            }
        }

        GovernanceBenchmarkArtifactBatchOperationResponse response = new GovernanceBenchmarkArtifactBatchOperationResponse();
        response.setTenantId(effectiveTenantId);
        response.setBatchId(batchId);
        response.setOperationType(batchOperationType);
        response.setTotalItems(Integer.valueOf(targets.size()));
        response.setSucceededItems(Integer.valueOf(succeededItems));
        response.setFailedItems(Integer.valueOf(failedItems));
        response.setSkippedItems(Integer.valueOf(skippedItems));
        response.setPartialFailure(Boolean.valueOf(failedItems > 0));
        response.setItems(items);
        response.setOperationStatus(resolveBatchOperationStatus(succeededItems, failedItems, skippedItems));
        response.setBatchOperationSurface(buildBatchOperationSurface(
            batchId,
            batchOperationType,
            itemOperationType,
            cleanupScope,
            targets.size(),
            succeededItems,
            failedItems,
            skippedItems
        ));
        return response;
    }

}
