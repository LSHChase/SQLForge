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
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationTarget;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceHistoryApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceHistoryApplicationService.class);
    private static final String SYSTEM_TENANT_ID = "system";
    private static final String BATCH_OPERATION_RETENTION = "EXECUTE_RETENTION_BATCH";
    private static final String BATCH_OPERATION_RECOVERY = "RECOVER_ARTIFACT_BATCH";
    private static final String ITEM_OPERATION_CLEANUP = "CLEANUP_ARTIFACT";
    private static final String ITEM_OPERATION_RECOVER = "RECOVER_ARTIFACT";
    private static final String BATCH_DEFAULT_RETENTION_SCOPE = "MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER";
    private static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";
    private static final String DEFAULT_EXPORT_STATUS = "GENERATED";
    private static final String DEFAULT_EXPORT_STORAGE_TYPE = "INLINE_RESPONSE";
    private static final String OPERATION_QUERY_HISTORY_EXPORT = "QUERY_HISTORY_EXPORT";
    private static final String TARGET_QUERY_HISTORY = "QUERY_HISTORY";
    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String REWRITE_RECORD_AGGREGATION_STAGE = "QUERY_HISTORY_REWRITE_RECORD_AGGREGATION";
    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int RECENT_SOURCE_SCAN_MULTIPLIER = 5;
    private static final int MAX_RECENT_SOURCE_SCAN_LIMIT = 200;
    private static final int LOOKUP_SOURCE_SCAN_MULTIPLIER = 8;
    private static final int MIN_LOOKUP_SOURCE_SCAN_LIMIT = 120;
    private static final int MAX_LOOKUP_SOURCE_SCAN_LIMIT = 400;
    private static final int LOOKUP_PAGE_FETCH_OVERFLOW = 1;
    private static final int TRACE_LOOKUP_SOURCE_LIMIT = 100;
    private static final String LOOKUP_TYPE_TASK = "TASK";
    private static final String LOOKUP_TYPE_REPORT = "REPORT";
    private static final List<String> TASK_TARGET_TYPES = Collections.unmodifiableList(
        java.util.Arrays.asList("TASK", "SQL_OPTIMIZATION_TASK", "BENCHMARK_ENGINE_TASK")
    );
    private static final List<String> REPORT_TARGET_TYPES = Collections.unmodifiableList(
        java.util.Arrays.asList("REPORT", "BENCHMARK_ENGINE_REPORT")
    );
    private static final List<String> ALLOWED_HISTORY_TYPES = Collections.unmodifiableList(
        java.util.Arrays.asList(
            "QUERY_EXECUTION",
            "SQL_PARSE",
            "ACCELERATION_PLAN",
            "BENCHMARK_REPORT_EXPORT",
            "SQL_OPTIMIZATION",
            "BENCHMARK"
        )
    );
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() {
        };

    private final AuditLogMapper auditLogMapper;
    private final GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final TenantAccessLogic tenantAccessLogic;
    private final GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient;
    private final GovernanceSqlOptimizationClient governanceSqlOptimizationClient;
    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final SensitiveDataCryptoService sensitiveDataCryptoService;

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

    private void mergeAuditLogs(Map<String, TraceAggregate> aggregateByTraceId, List<AuditLogRecord> records) {
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

    private void mergeAuditLogs(TraceAggregate aggregate, List<AuditLogRecord> records) {
        if (records == null) {
            return;
        }
        for (AuditLogRecord record : records) {
            aggregate.applyAudit(record, parseJsonObject(record.getRequestParams()), parseJsonObject(record.getResponseSummary()));
        }
    }

    private void mergeQueryHistories(Map<String, TraceAggregate> aggregateByTraceId, List<QueryHistoryRecord> records) {
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

    private void mergeQueryHistories(TraceAggregate aggregate, List<QueryHistoryRecord> records) {
        if (records == null) {
            return;
        }
        for (QueryHistoryRecord record : records) {
            aggregate.applyQueryHistory(record);
        }
    }

    private void mergeExportRecords(Map<String, TraceAggregate> aggregateByTraceId, List<ExportRecord> records) {
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

    private void mergeExportRecords(TraceAggregate aggregate, List<ExportRecord> records) {
        if (records == null) {
            return;
        }
        for (ExportRecord record : records) {
            aggregate.applyExportRecord(record);
        }
    }

    private String resolveAuthorizedTenantId(String tenantId) {
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

    private void assertArtifactOperationRole() {
        if (SYSTEM_TENANT_ID.equals(RequestContext.getTenantId())) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
            HttpStatus.FORBIDDEN,
            "当前请求不允许触发产物清理或恢复"
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit.intValue() <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(MAX_LIMIT, limit.intValue());
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo.intValue() <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo.intValue();
    }

    private int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    private int resolveRecentSourceScanLimit(int resolvedLimit) {
        int scaledLimit = resolvedLimit * RECENT_SOURCE_SCAN_MULTIPLIER;
        if (scaledLimit < resolvedLimit) {
            return resolvedLimit;
        }
        return Math.min(MAX_RECENT_SOURCE_SCAN_LIMIT, scaledLimit);
    }

    private int resolveLookupSourceScanLimit(int resolvedLimit) {
        int scaledLimit = resolvedLimit * LOOKUP_SOURCE_SCAN_MULTIPLIER;
        if (scaledLimit < resolvedLimit) {
            return Math.min(MAX_LOOKUP_SOURCE_SCAN_LIMIT, MIN_LOOKUP_SOURCE_SCAN_LIMIT);
        }
        int candidate = Math.max(MIN_LOOKUP_SOURCE_SCAN_LIMIT, scaledLimit);
        return Math.min(MAX_LOOKUP_SOURCE_SCAN_LIMIT, candidate);
    }

    private List<TraceAggregate> loadRecentAggregates(String tenantId, int sourceScanLimit) {
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

    private TraceAggregate loadTraceAggregate(String tenantId, String traceId, int sourceLimit) {
        TraceAggregate aggregate = new TraceAggregate(traceId);
        mergeAuditLogs(aggregate, auditLogMapper.selectByTraceId(tenantId, traceId, sourceLimit));
        mergeQueryHistories(aggregate, queryHistoryMapper.selectByTraceId(tenantId, traceId, sourceLimit));
        mergeExportRecords(aggregate, exportRecordMapper.selectByTraceId(tenantId, traceId, sourceLimit));
        return aggregate;
    }

    private List<TraceAggregate> loadAggregatesByTraceIds(String tenantId, List<String> traceIds) {
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

    private List<TraceLookupHitRecord> loadIndexedTraceHits(String tenantId,
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

    private LocalDateTime parseWindowValue(String value, String fieldName) {
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

    private LocalDate parseDateValue(String value, String fieldName) {
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

    private String resolveHistoryOrderBy(String sortBy, String sortOrder) {
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

    private LookupCursor parseLookupCursor(String cursor) {
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

    private String encodeCursor(TraceLookupHitRecord hit) {
        if (hit == null || hit.getLastSeenAt() == null || hit.getLastAuditId() == null) {
            return null;
        }
        return hit.getLastSeenAt().toString() + "|" + hit.getLastAuditId();
    }

    private static Map<String, Object> parseJsonObject(String content) {
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

    private static Object parseJsonValue(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            return JsonUtils.objectMapper().readValue(content, Object.class);
        } catch (Exception ex) {
            return content;
        }
    }

    private GovernanceQueryHistorySummaryVO toQueryHistorySummary(GovernanceQueryHistoryProjection row) {
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

    private GovernanceQueryHistoryDetailVO toQueryHistoryDetail(GovernanceQueryHistoryProjection row,
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

    private void populateReferenceSurfaces(GovernanceQueryHistoryDetailVO detail) {
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

    private boolean sameRewriteRecordScope(SqlOptimizationRewriteRecordResponse record,
                                           String tenantId,
                                           String historyId) {
        return record != null
            && tenantId.equals(record.getTenantId())
            && historyId.equals(record.getHistoryId());
    }

    private GovernanceQueryHistoryRewriteRecordVO toQueryHistoryRewriteRecord(
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

    private List<Map<String, Object>> buildRewriteRecordAlertRefs(SqlOptimizationRewriteRecordResponse record) {
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

    private Map<String, Object> buildHistoryClassificationSummary(List<GovernanceQueryHistorySummaryVO> items) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("totalItems", Integer.valueOf(items == null ? 0 : items.size()));
        summary.put("statusCounts", bucketCount(items, "status"));
        summary.put("historyTypeCounts", bucketCount(items, "historyType"));
        summary.put("accessChannelCounts", bucketCount(items, "accessChannel"));
        return summary;
    }

    private Map<String, Integer> bucketCount(List<GovernanceQueryHistorySummaryVO> items, String bucketType) {
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

    private List<String> extractLogicalObjectTypes(List<LogicalObjectSurface> logicalObjectHits) {
        LinkedHashSet<String> types = new LinkedHashSet<String>();
        collectLogicalObjectTypes(logicalObjectHits, types);
        return new ArrayList<String>(types);
    }

    private void collectLogicalObjectTypes(Object parsed, Set<String> types) {
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

    private static List<LogicalObjectSurface> normalizeLogicalObjectHits(String logicalObjectHitsJson) {
        return normalizeLogicalObjectHits(parseJsonValue(logicalObjectHitsJson));
    }

    private static List<LogicalObjectSurface> normalizeLogicalObjectHits(Object logicalObjectHits) {
        if (logicalObjectHits == null) {
            return Collections.emptyList();
        }
        List<LogicalObjectSurface> surfaces = new ArrayList<LogicalObjectSurface>();
        collectLogicalObjectSurfaces(logicalObjectHits, surfaces);
        return surfaces;
    }

    private static void collectLogicalObjectSurfaces(Object rawValue, List<LogicalObjectSurface> surfaces) {
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

    private static LogicalObjectSurface toLogicalObjectSurface(Map<?, ?> rawMap) {
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

    private static LogicalObjectSurface fromRawLogicalObject(String raw) {
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

    private static List<String> normalizeMappedPhysicalTargets(Object rawTargets) {
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

    private static String normalizeTargetEntry(Object rawTarget) {
        if (rawTarget == null) {
            return null;
        }
        if (rawTarget instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) rawTarget;
            return firstText(rawMap.get("targetObjectKey"), rawMap.get("objectKey"), rawMap.get("name"));
        }
        return trimToNull(String.valueOf(rawTarget));
    }

    private static String firstText(Object... values) {
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

    private static Boolean toBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private static String extractLogicalObjectType(String value) {
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

    private static LogicalObjectType resolveLogicalObjectType(String value) {
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

    private static LogicalObjectType inferLogicalObjectTypeFromName(String value) {
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

    private static String joinLogicalObjectKeys(List<LogicalObjectSurface> logicalObjectHits) {
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

    private Map<String, Object> buildSqlState(GovernanceQueryHistoryProjection row) {
        LinkedHashMap<String, Object> sqlState = new LinkedHashMap<String, Object>();
        sqlState.put("sqlFingerprint", row.getSqlFingerprint());
        sqlState.put("sqlTemplateFingerprint", row.getSqlTemplateFingerprint());
        sqlState.put("boundSqlFingerprint", row.getBoundSqlFingerprint());
        sqlState.put("parameterizedSqlFlag", row.getParameterizedSqlFlag());
        sqlState.put("bindingMode", row.getBindingMode());
        sqlState.put("bindingRenderStatus", row.getBindingRenderStatus());
        return sqlState;
    }

    private Map<String, Object> buildQueryDateSummary(GovernanceQueryHistoryProjection row) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("bizDate", row.getBizDate());
        summary.put("queryDateStart", row.getQueryDateStart());
        summary.put("queryDateEnd", row.getQueryDateEnd());
        summary.put("queryDateStatus", row.getQueryDateStatus());
        return summary;
    }

    private Map<String, Object> buildExecutionSummary(GovernanceQueryHistoryProjection row) {
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

    private Map<String, Object> buildRewriteAudit(GovernanceQueryHistoryProjection row,
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

    private List<Map<String, Object>> buildAuditRefs(GovernanceTraceDetailVO traceDetail) {
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

    private List<Map<String, Object>> buildBenchmarkRefs(GovernanceTraceDetailVO traceDetail) {
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

    private List<Map<String, Object>> readNestedList(Map<String, Object> source, String key) {
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

    private String decryptSqlText(byte[] cipher) {
        if (cipher == null || cipher.length == 0 || sensitiveDataCryptoService == null) {
            return null;
        }
        return new String(sensitiveDataCryptoService.decryptBytes(cipher), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String requireSupportedExportFormat(String exportFormat) {
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

    private String renderExportPayload(String exportFormat, GovernanceQueryHistoryDetailVO detail) {
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

    private String rewriteAuditText(GovernanceQueryHistoryDetailVO detail, String key, String fallback) {
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

    private String csvCell(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private String buildExportFileName(String historyId, String exportFormat) {
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

    private String resolveExportContentType(String exportFormat) {
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

    private void persistExportRecord(String exportId,
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

    private Map<String, Object> buildExportOptions(GovernanceQueryHistoryExportRequest request,
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

    private void persistExportAudit(String exportId,
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

    private Map<String, Object> buildExportAuditRequestParams(GovernanceQueryHistoryExportRequest request,
                                                              String exportFormat) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("historyId", request == null ? null : request.getHistoryId());
        params.put("exportFormat", exportFormat);
        params.put("includeTraceDetail", request == null ? null : request.getIncludeTraceDetail());
        params.put("exportReason", request == null ? null : request.getExportReason());
        return params;
    }

    private Map<String, Object> buildAuditReference(String exportId,
                                                    GovernanceQueryHistoryProjection row,
                                                    String exportFormat,
                                                    LocalDateTime exportedAt) {
        return buildAuditReference(exportId, row, exportFormat, exportedAt, null, null);
    }

    private Map<String, Object> buildAuditReference(String exportId,
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

    private Map<String, Object> buildParseSummary(Map<String, Object> queryContext, String key) {
        return selectFirstNonEmptyMap(Collections.<String, Object>emptyMap(), queryContext, key);
    }

    private Map<String, Object> selectFirstNonEmptyMap(Map<String, Object> primary, Map<String, Object> secondary, String nestedKey) {
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

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeBatchOperationType(String operationType) {
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

    private String mapBatchItemOperationType(String batchOperationType) {
        return BATCH_OPERATION_RETENTION.equals(batchOperationType) ? ITEM_OPERATION_CLEANUP : ITEM_OPERATION_RECOVER;
    }

    private String resolveBatchCleanupScope(String batchOperationType, String cleanupScope) {
        if (BATCH_OPERATION_RETENTION.equals(batchOperationType)) {
            return trimToNull(cleanupScope) == null ? BATCH_DEFAULT_RETENTION_SCOPE : trimToNull(cleanupScope);
        }
        return trimToNull(cleanupScope);
    }

    private String resolveBatchId(String requestedBatchId) {
        String normalized = trimToNull(requestedBatchId);
        if (normalized != null) {
            return normalized;
        }
        String requestId = trimToNull(RequestContext.getRequestId());
        String traceId = trimToNull(RequestContext.getTraceId());
        String suffix = requestId != null ? requestId : traceId != null ? traceId : String.valueOf(System.currentTimeMillis());
        return "artifact-batch-" + suffix;
    }

    private GovernanceBenchmarkArtifactOperationResponse buildBatchFailureItem(String tenantId,
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

    private GovernanceBenchmarkArtifactOperationResponse buildBatchSkippedItem(String tenantId,
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

    private void populateBatchMetadata(GovernanceBenchmarkArtifactOperationResponse response,
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

    private String resolveBatchOperationStatus(int succeededItems, int failedItems, int skippedItems) {
        if (failedItems > 0 && succeededItems == 0 && skippedItems == 0) {
            return "BATCH_FAILED";
        }
        if (failedItems > 0) {
            return "PARTIAL_FAILURE";
        }
        return "BATCH_COMPLETED";
    }

    private Map<String, Object> buildBatchOperationSurface(String batchId,
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

    private static String readText(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean readBoolean(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private static Map<String, Object> readNestedMap(Map<String, Object> payload, String key) {
        if (payload == null || !StringUtils.hasText(key)) {
            return Collections.emptyMap();
        }
        Object value = payload.get(key);
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>();
        copy.putAll((Map<String, Object>) value);
        return copy;
    }

    private static Map<String, Object> readEvidenceMap(Object value) {
        if (value instanceof Map) {
            LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.putAll((Map<String, Object>) value);
            return evidence;
        }
        if (!(value instanceof String) || !StringUtils.hasText((String) value)) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        String[] parts = ((String) value).split(";");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator <= 0) {
                evidence.put(part.trim(), Boolean.TRUE);
                continue;
            }
            evidence.put(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
        }
        return evidence;
    }

    private static Map<String, Object> buildCompensationReplayEvidence(Map<String, Object> queryContext) {
        if (queryContext == null || queryContext.isEmpty()) {
            return null;
        }
        Map<String, Object> workloadEvidence = readNestedMap(queryContext, "workloadEvidence");
        Map<String, Object> queryExecution = readNestedMap(workloadEvidence, "queryExecution");
        Map<String, Object> engines = readNestedMap(queryExecution, "engines");
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        putIfPresent(evidence, "workloadSource", queryContext.get("workloadSource"));
        putIfPresent(evidence, "backfillApplied", queryContext.get("backfillApplied"));
        putIfPresent(evidence, "executionMode", workloadEvidence.get("executionMode"));
        putIfPresent(evidence, "implementationStage", queryExecution.get("implementationStage"));
        Object compensationApplied = firstNonNull(queryExecution.get("compensationApplied"), queryContext.get("compensationApplied"));
        Object compensationStrategy = firstNonNull(queryExecution.get("compensationStrategy"), queryContext.get("compensationStrategy"));
        Object compensationSourceEngine = queryContext.get("compensationSourceEngine");
        Object compensationSourceWorkloadDigest = firstNonNull(
            queryContext.get("compensationSourceWorkloadDigest"),
            queryContext.get("compensationSourceDigest")
        );
        for (Map.Entry<String, Object> entry : engines.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> engineEvidence = new LinkedHashMap<String, Object>();
            engineEvidence.putAll((Map<String, Object>) entry.getValue());
            if (compensationApplied == null && engineEvidence.get("compensationApplied") != null) {
                compensationApplied = engineEvidence.get("compensationApplied");
            }
            if (compensationStrategy == null && engineEvidence.get("compensationStrategy") != null) {
                compensationStrategy = engineEvidence.get("compensationStrategy");
            }
            if (compensationSourceEngine == null && engineEvidence.get("compensationSourceEngine") != null) {
                compensationSourceEngine = engineEvidence.get("compensationSourceEngine");
            }
            Object sourceDigest = firstNonNull(
                engineEvidence.get("compensationSourceWorkloadDigest"),
                engineEvidence.get("compensationSourceDigest")
            );
            if (compensationSourceWorkloadDigest == null && sourceDigest != null) {
                compensationSourceWorkloadDigest = sourceDigest;
            }
        }
        putIfPresent(evidence, "compensationApplied", compensationApplied);
        putIfPresent(evidence, "compensationStrategy", compensationStrategy);
        putIfPresent(evidence, "compensationSourceEngine", compensationSourceEngine);
        putIfPresent(evidence, "compensationSourceWorkloadDigest", compensationSourceWorkloadDigest);
        if (!engines.isEmpty()) {
            evidence.put("engines", engines);
        }
        return evidence.isEmpty() ? null : evidence;
    }

    private static Map<String, Object> buildCacheGovernanceSurface(Map<String, Object> responseSummary,
                                                                   Map<String, Object> queryContext) {
        LinkedHashMap<String, Object> surface = new LinkedHashMap<String, Object>();
        putIfPresent(surface, "cacheHit", responseSummary == null ? null : responseSummary.get("cacheHit"));
        putIfPresent(surface, "cacheGovernanceStatus", responseSummary == null ? null : responseSummary.get("cacheGovernanceStatus"));
        Map<String, Object> auditEvidence = readEvidenceMap(responseSummary == null ? null : responseSummary.get("cacheGovernanceEvidence"));
        if (!auditEvidence.isEmpty()) {
            surface.put("cacheGovernanceEvidence", auditEvidence);
        }

        Map<String, Object> workloadEvidence = readNestedMap(queryContext, "workloadEvidence");
        Map<String, Object> queryExecution = readNestedMap(workloadEvidence, "queryExecution");
        Map<String, Object> engines = readNestedMap(queryExecution, "engines");
        LinkedHashMap<String, Object> engineSurfaces = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : engines.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> engineEvidence = new LinkedHashMap<String, Object>();
            engineEvidence.putAll((Map<String, Object>) entry.getValue());
            Object cacheStatus = engineEvidence.get("cacheGovernanceStatus");
            Object cacheHit = engineEvidence.get("cacheHit");
            Object cacheEvidence = engineEvidence.get("cacheGovernanceEvidence");
            if (cacheStatus == null && cacheHit == null && cacheEvidence == null) {
                continue;
            }
            LinkedHashMap<String, Object> engineSurface = new LinkedHashMap<String, Object>();
            putIfPresent(engineSurface, "cacheHit", cacheHit);
            putIfPresent(engineSurface, "cacheGovernanceStatus", cacheStatus);
            if (cacheEvidence != null) {
                engineSurface.put("cacheGovernanceEvidence", normalizeCacheGovernanceEvidence(cacheEvidence));
            }
            engineSurfaces.put(entry.getKey(), engineSurface);
        }
        if (!engineSurfaces.isEmpty()) {
            surface.put("engines", engineSurfaces);
        }
        return surface.isEmpty() ? null : surface;
    }

    private static Object normalizeCacheGovernanceEvidence(Object value) {
        if (value instanceof Map) {
            return value;
        }
        if (!(value instanceof String) || !StringUtils.hasText((String) value)) {
            return value;
        }
        String rawValue = ((String) value).trim();
        if (rawValue.indexOf(';') >= 0 && rawValue.indexOf('=') >= 0) {
            return readEvidenceMap(rawValue);
        }
        if (rawValue.indexOf('|') >= 0 && rawValue.indexOf(':') >= 0) {
            LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
            String[] parts = rawValue.split("\\|");
            for (String part : parts) {
                if (!StringUtils.hasText(part)) {
                    continue;
                }
                int separator = part.indexOf(':');
                if (separator <= 0) {
                    evidence.put(part.trim(), Boolean.TRUE);
                    continue;
                }
                evidence.put(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
            }
            return evidence;
        }
        return rawValue;
    }

    private static Map<String, Object> buildArtifactStorageContract(Map<String, Object> responseSummary,
                                                                    Map<String, Object> exportOptions) {
        Map<String, Object> evidence = mergeEvidenceMaps(
            readEvidenceMap(responseSummary == null ? null : responseSummary.get("artifactStorageEvidence")),
            readEvidenceMap(exportOptions == null ? null : exportOptions.get("storageEvidence"))
        );
        LinkedHashMap<String, Object> contract = new LinkedHashMap<String, Object>();
        putIfPresent(contract, "storageType", firstNonNull(
            responseSummary == null ? null : responseSummary.get("artifactStorageType"),
            exportOptions == null ? null : exportOptions.get("storageType")
        ));
        putIfPresent(contract, "providerMode", firstNonNull(evidence.get("providerMode"), evidence.get("mode")));
        putIfPresent(contract, "primaryProvider", firstNonNull(evidence.get("primaryProvider"), evidence.get("provider")));
        putIfPresent(contract, "primaryProviderContract", firstNonNull(evidence.get("primaryProviderContract"), evidence.get("providerContract")));
        putIfPresent(contract, "recoveryProvider", evidence.get("recoveryProvider"));
        putIfPresent(contract, "recoveryProviderContract", evidence.get("recoveryProviderContract"));
        putIfPresent(contract, "recoveryOrder", evidence.get("recoveryOrder"));
        putIfPresent(contract, "cleanupScope", evidence.get("cleanupScope"));
        putIfPresent(contract, "retentionDays", firstNonNull(
            responseSummary == null ? null : responseSummary.get("artifactRetentionDays"),
            exportOptions == null ? null : exportOptions.get("retentionDays")
        ));
        putIfPresent(contract, "retentionPolicySource", firstNonNull(
            responseSummary == null ? null : responseSummary.get("artifactRetentionPolicySource"),
            exportOptions == null ? null : exportOptions.get("retentionPolicySource")
        ));
        return contract.isEmpty() ? null : contract;
    }

    private static Map<String, Object> buildArtifactRecoverySurface(Map<String, Object> responseSummary,
                                                                    Map<String, Object> exportOptions) {
        Map<String, Object> evidence = mergeEvidenceMaps(
            readEvidenceMap(responseSummary == null ? null : responseSummary.get("artifactStorageEvidence")),
            readEvidenceMap(exportOptions == null ? null : exportOptions.get("storageEvidence"))
        );
        LinkedHashMap<String, Object> recovery = new LinkedHashMap<String, Object>();
        putIfPresent(recovery, "artifactRecoveryStatus", responseSummary == null ? null : responseSummary.get("artifactRecoveryStatus"));
        putIfPresent(recovery, "storageRecoverySource", responseSummary == null ? null : responseSummary.get("artifactStorageRecoverySource"));
        putIfPresent(recovery, "storageReadStatus", responseSummary == null ? null : responseSummary.get("artifactStorageReadStatus"));
        putIfPresent(recovery, "providerWriteStatus", evidence.get("providerWriteStatus"));
        putIfPresent(recovery, "providerRecoveryStatus", evidence.get("providerRecoveryStatus"));
        putIfPresent(recovery, "recoveryProviderWriteStatus", evidence.get("recoveryProviderWriteStatus"));
        putIfPresent(recovery, "recoveryProviderRecoveryStatus", evidence.get("recoveryProviderRecoveryStatus"));
        putIfPresent(recovery, "externalWriteStatus", evidence.get("externalWriteStatus"));
        putIfPresent(recovery, "recoveryVerificationStatus", evidence.get("recoveryVerificationStatus"));
        putIfPresent(recovery, "providerObjectUrl", evidence.get("providerObjectUrl"));
        putIfPresent(recovery, "recoveryProviderObjectUrl", evidence.get("recoveryProviderObjectUrl"));
        putIfPresent(recovery, "externalWritePath", evidence.get("externalWritePath"));
        return recovery.isEmpty() ? null : recovery;
    }

    private static Map<String, Object> buildArtifactOperationSurface(Map<String, Object> responseSummary) {
        if (responseSummary == null || responseSummary.isEmpty()) {
            return null;
        }
        Object nested = responseSummary.get("artifactOperationSurface");
        if (!(nested instanceof Map)) {
            return null;
        }
        LinkedHashMap<String, Object> operationSurface = new LinkedHashMap<String, Object>();
        operationSurface.putAll((Map<String, Object>) nested);
        return operationSurface;
    }

    private static Map<String, Object> mergeEvidenceMaps(Map<String, Object> first, Map<String, Object> second) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<String, Object>();
        if (first != null && !first.isEmpty()) {
            merged.putAll(first);
        }
        if (second != null && !second.isEmpty()) {
            merged.putAll(second);
        }
        return merged;
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private static Boolean normalizeRewriteApplied(Boolean rewriteApplied) {
        return Boolean.valueOf(Boolean.TRUE.equals(rewriteApplied));
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

    private static String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(first, firstNonBlank(second, third));
    }

    private static String firstNonBlank(String first, String second, String third, String fourth) {
        return firstNonBlank(first, firstNonBlank(second, third, fourth));
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || !StringUtils.hasText(key) || value == null) {
            return;
        }
        target.put(key, value);
    }

    private static class TraceAggregate {

        private final String traceId;
        private String requestId;
        private String serviceCode;
        private String operationType;
        private String resourceType;
        private String resourceId;
        private String latestStatus;
        private LocalDateTime lastSeenAt;
        private int auditEventCount;
        private int nonSuccessEventCount;
        private int queryHistoryCount;
        private int exportRecordCount;
        private String taskId;
        private String reportId;
        private String sqlFingerprint;
        private String errorCode;
        private String targetEngine;
        private Boolean degraded;
        private Map<String, Object> compensationReplayEvidence;
        private Map<String, Object> cacheGovernanceSurface;
        private Map<String, Object> artifactStorageContract;
        private Map<String, Object> artifactRecoverySurface;
        private Map<String, Object> artifactOperationSurface;
        private boolean summaryUsesBusinessAudit;
        private boolean hasBusinessAudit;
        private final List<GovernanceTraceDetailVO.AuditEventVO> auditEvents;
        private final List<GovernanceTraceDetailVO.QueryHistoryVO> queryHistories;
        private final List<GovernanceTraceDetailVO.ExportRecordVO> exportRecords;

        private TraceAggregate(String traceId) {
            this.traceId = traceId;
            this.auditEvents = new ArrayList<GovernanceTraceDetailVO.AuditEventVO>();
            this.queryHistories = new ArrayList<GovernanceTraceDetailVO.QueryHistoryVO>();
            this.exportRecords = new ArrayList<GovernanceTraceDetailVO.ExportRecordVO>();
        }

        private void applyAudit(AuditLogRecord record, Map<String, Object> requestParams, Map<String, Object> responseSummary) {
            if (record == null) {
                return;
            }
            boolean businessAudit = isBusinessAudit(record);
            if (businessAudit) {
                this.hasBusinessAudit = true;
            }
            Map<String, Object> compensationEvidence = null;
            Map<String, Object> cacheGovernanceSurface = buildCacheGovernanceSurface(responseSummary, null);
            Map<String, Object> storageContract = buildArtifactStorageContract(responseSummary, null);
            Map<String, Object> recoverySurface = buildArtifactRecoverySurface(responseSummary, null);
            Map<String, Object> operationSurface = buildArtifactOperationSurface(responseSummary);

            GovernanceTraceDetailVO.AuditEventVO event = new GovernanceTraceDetailVO.AuditEventVO();
            event.setId(record.getId());
            event.setServiceCode(record.getServiceCode());
            event.setOperationType(record.getOperationType());
            event.setTargetType(record.getTargetType());
            event.setTargetId(record.getTargetId());
            event.setStatus(record.getStatus());
            event.setRequestId(record.getRequestId());
            event.setTraceId(record.getTraceId());
            event.setHistoryId(record.getHistoryId());
            event.setExportId(record.getExportId());
            event.setResultId(record.getResultId());
            event.setCostMs(record.getCostMs());
            event.setCreateTime(record.getCreateTime());
            event.setRequestParams(requestParams);
            event.setResponseSummary(responseSummary);
            this.auditEvents.add(event);
            this.auditEventCount += 1;
            if (!"SUCCESS".equalsIgnoreCase(String.valueOf(record.getStatus()))) {
                this.nonSuccessEventCount += 1;
            }

            LocalDateTime eventTime = record.getCreateTime();
            if (shouldRefreshAuditSummary(record, eventTime)) {
                this.requestId = firstNonBlank(record.getRequestId(), readText(requestParams, "requestId"), this.requestId);
                this.serviceCode = firstNonBlank(record.getServiceCode(), readText(requestParams, "serviceCode"), this.serviceCode);
                this.operationType = firstNonBlank(record.getOperationType(), this.operationType);
                this.resourceType = firstNonBlank(record.getTargetType(), this.resourceType);
                this.resourceId = firstNonBlank(record.getTargetId(), this.resourceId);
                this.latestStatus = firstNonBlank(record.getStatus(), readText(responseSummary, "resultStatus"), this.latestStatus);
                this.lastSeenAt = eventTime;
                this.sqlFingerprint = firstNonBlank(readText(requestParams, "sqlFingerprint"), this.sqlFingerprint);
                this.taskId = firstNonBlank(readText(responseSummary, "taskId"), readText(requestParams, "taskId"), inferTaskId(record), this.taskId);
                this.reportId = firstNonBlank(readText(responseSummary, "reportId"), readText(requestParams, "reportId"), inferReportId(record), this.reportId);
                this.errorCode = firstNonBlank(readText(responseSummary, "errorCode"), this.errorCode);
                this.targetEngine = firstNonBlank(readText(responseSummary, "targetEngine"), this.targetEngine);
                this.degraded = firstNonNull(readBoolean(responseSummary, "degraded"), this.degraded);
                this.compensationReplayEvidence = selectEvidenceMap(this.compensationReplayEvidence, compensationEvidence, true);
                this.cacheGovernanceSurface = selectEvidenceMap(this.cacheGovernanceSurface, cacheGovernanceSurface, true);
                this.artifactStorageContract = selectEvidenceMap(this.artifactStorageContract, storageContract, true);
                this.artifactRecoverySurface = selectEvidenceMap(this.artifactRecoverySurface, recoverySurface, true);
                this.artifactOperationSurface = selectEvidenceMap(this.artifactOperationSurface, operationSurface, true);
                this.summaryUsesBusinessAudit = businessAudit;
            } else {
                this.compensationReplayEvidence = selectEvidenceMap(this.compensationReplayEvidence, compensationEvidence, false);
                this.cacheGovernanceSurface = selectEvidenceMap(this.cacheGovernanceSurface, cacheGovernanceSurface, false);
                this.artifactStorageContract = selectEvidenceMap(this.artifactStorageContract, storageContract, false);
                this.artifactRecoverySurface = selectEvidenceMap(this.artifactRecoverySurface, recoverySurface, false);
                this.artifactOperationSurface = selectEvidenceMap(this.artifactOperationSurface, operationSurface, false);
            }
        }

        private void applyQueryHistory(QueryHistoryRecord record) {
            if (record == null) {
                return;
            }
            Map<String, Object> queryContext = parseJsonObject(record.getQueryContext());
            Map<String, Object> compensationEvidence = buildCompensationReplayEvidence(queryContext);
            Map<String, Object> cacheGovernanceSurface = buildCacheGovernanceSurface(null, queryContext);

            GovernanceTraceDetailVO.QueryHistoryVO historyVO = new GovernanceTraceDetailVO.QueryHistoryVO();
            historyVO.setHistoryId(record.getHistoryId());
            historyVO.setResultId(record.getResultId());
            historyVO.setHistoryType(record.getHistoryType());
            historyVO.setDatasourceCode(record.getDatasourceCode());
            historyVO.setDatasourceType(record.getDatasourceType());
            historyVO.setReportCode(record.getReportCode());
            historyVO.setStageCode(record.getStageCode());
            historyVO.setBizDate(record.getBizDate());
            historyVO.setQueryDateStart(record.getQueryDateStart());
            historyVO.setQueryDateEnd(record.getQueryDateEnd());
            historyVO.setQueryDateStatus(record.getQueryDateStatus());
            historyVO.setAccessChannel(record.getAccessChannel());
            historyVO.setParameterizedSqlFlag(record.getParameterizedSqlFlag());
            historyVO.setBindingMode(record.getBindingMode());
            historyVO.setBindingRenderStatus(record.getBindingRenderStatus());
            historyVO.setSqlTemplateFingerprint(record.getSqlTemplateFingerprint());
            historyVO.setBoundSqlFingerprint(record.getBoundSqlFingerprint());
            historyVO.setSqlFingerprint(record.getSqlFingerprint());
            historyVO.setRequestId(record.getRequestId());
            historyVO.setSagaId(record.getSagaId());
            historyVO.setSubmittedBy(record.getSubmittedBy());
            historyVO.setSubmittedAt(record.getSubmittedAt());
            historyVO.setCreateTime(record.getCreateTime());
            historyVO.setCommentContext(parseJsonObject(record.getCommentContext()));
            historyVO.setBindingSummary(parseJsonObject(record.getBindingSummary()));
            historyVO.setLogicalObjectHits(normalizeLogicalObjectHits(record.getLogicalObjectHits()));
            historyVO.setRouteSummary(parseJsonObject(record.getRouteSummary()));
            historyVO.setCacheSummary(parseJsonObject(record.getCacheSummary()));
            historyVO.setQueryContext(queryContext);
            this.queryHistories.add(historyVO);
            this.queryHistoryCount += 1;

            LocalDateTime historyTime = firstNonNull(record.getCreateTime(), record.getSubmittedAt());
            if (shouldRefreshSummary(historyTime) || !StringUtils.hasText(this.serviceCode)) {
                this.requestId = firstNonBlank(record.getRequestId(), this.requestId);
                this.serviceCode = firstNonBlank(record.getHistoryType(), this.serviceCode);
                this.operationType = firstNonBlank("TRACE_READINESS", this.operationType);
                this.resourceType = firstNonBlank("QUERY_HISTORY", this.resourceType);
                this.resourceId = firstNonBlank(record.getHistoryId(), this.resourceId);
                this.latestStatus = firstNonBlank(this.latestStatus, "RECORDED");
                this.lastSeenAt = firstNonNull(historyTime, this.lastSeenAt);
                this.sqlFingerprint = firstNonBlank(record.getSqlFingerprint(), this.sqlFingerprint);
                this.compensationReplayEvidence = selectEvidenceMap(this.compensationReplayEvidence, compensationEvidence, true);
                this.cacheGovernanceSurface = selectEvidenceMap(this.cacheGovernanceSurface, cacheGovernanceSurface, true);
                this.summaryUsesBusinessAudit = false;
            } else {
                this.compensationReplayEvidence = selectEvidenceMap(this.compensationReplayEvidence, compensationEvidence, false);
                this.cacheGovernanceSurface = selectEvidenceMap(this.cacheGovernanceSurface, cacheGovernanceSurface, false);
            }
        }

        private void applyExportRecord(ExportRecord record) {
            if (record == null) {
                return;
            }
            Map<String, Object> exportOptions = parseJsonObject(record.getExportOptions());
            Map<String, Object> storageContract = buildArtifactStorageContract(null, exportOptions);
            Map<String, Object> recoverySurface = buildArtifactRecoverySurface(null, exportOptions);

            GovernanceTraceDetailVO.ExportRecordVO exportVO = new GovernanceTraceDetailVO.ExportRecordVO();
            exportVO.setExportId(record.getExportId());
            exportVO.setHistoryId(record.getHistoryId());
            exportVO.setResultId(record.getResultId());
            exportVO.setExportFormat(record.getExportFormat());
            exportVO.setExportStatus(record.getExportStatus());
            exportVO.setRequestId(record.getRequestId());
            exportVO.setStorageType(record.getStorageType());
            exportVO.setStorageUri(record.getStorageUri());
            exportVO.setErrorCode(record.getErrorCode());
            exportVO.setErrorMessage(record.getErrorMessage());
            exportVO.setCreateTime(record.getCreateTime());
            exportVO.setFinishedAt(record.getFinishedAt());
            exportVO.setExportOptions(exportOptions);
            this.exportRecords.add(exportVO);
            this.exportRecordCount += 1;

            LocalDateTime exportTime = firstNonNull(record.getFinishedAt(), record.getCreateTime());
            if (shouldRefreshSummary(exportTime) || !StringUtils.hasText(this.serviceCode)) {
                this.requestId = firstNonBlank(record.getRequestId(), this.requestId);
                this.serviceCode = firstNonBlank(this.serviceCode, "EXPORT_RECORD");
                this.operationType = firstNonBlank(this.operationType, "EXPORT");
                this.resourceType = firstNonBlank(this.resourceType, "EXPORT_RECORD");
                this.resourceId = firstNonBlank(record.getExportId(), this.resourceId);
                this.latestStatus = firstNonBlank(record.getExportStatus(), this.latestStatus);
                this.lastSeenAt = firstNonNull(exportTime, this.lastSeenAt);
                this.reportId = firstNonBlank(this.reportId, record.getExportId());
                this.errorCode = firstNonBlank(record.getErrorCode(), this.errorCode);
                this.artifactStorageContract = selectEvidenceMap(this.artifactStorageContract, storageContract, true);
                this.artifactRecoverySurface = selectEvidenceMap(this.artifactRecoverySurface, recoverySurface, true);
                this.summaryUsesBusinessAudit = false;
            } else {
                this.artifactStorageContract = selectEvidenceMap(this.artifactStorageContract, storageContract, false);
                this.artifactRecoverySurface = selectEvidenceMap(this.artifactRecoverySurface, recoverySurface, false);
            }
        }

        private boolean shouldRefreshAuditSummary(AuditLogRecord record, LocalDateTime candidateTime) {
            boolean candidateBusinessAudit = isBusinessAudit(record);
            if (candidateBusinessAudit && !this.summaryUsesBusinessAudit) {
                return true;
            }
            if (!candidateBusinessAudit && this.summaryUsesBusinessAudit) {
                return false;
            }
            return shouldRefreshSummary(candidateTime);
        }

        private boolean shouldRefreshSummary(LocalDateTime candidateTime) {
            if (candidateTime == null) {
                return !StringUtils.hasText(this.latestStatus);
            }
            return this.lastSeenAt == null || candidateTime.compareTo(this.lastSeenAt) >= 0;
        }

        private String inferTaskId(AuditLogRecord record) {
            if (record == null) {
                return null;
            }
            if ("TASK".equalsIgnoreCase(record.getTargetType())) {
                return record.getTargetId();
            }
            return null;
        }

        private boolean shouldDisplayInRecentList() {
            return this.hasBusinessAudit || this.queryHistoryCount > 0 || this.exportRecordCount > 0;
        }

        private boolean isBusinessAudit(AuditLogRecord record) {
            return record != null
                && StringUtils.hasText(record.getServiceCode())
                && !"GOVERNANCE".equalsIgnoreCase(record.getServiceCode());
        }

        private String toDisplayGroupKey() {
            String businessKey = firstNonBlank(this.taskId, this.reportId, this.resourceId, this.sqlFingerprint, this.traceId);
            return firstNonBlank(this.serviceCode, "TRACE") + "::" + businessKey;
        }

        private boolean matchesLookupCriteria(String candidateTraceId, String candidateTaskId, String candidateReportId) {
            if (StringUtils.hasText(candidateTraceId) && !candidateTraceId.equals(this.traceId)) {
                return false;
            }
            if (StringUtils.hasText(candidateTaskId) && !candidateTaskId.equals(this.taskId)) {
                return false;
            }
            if (StringUtils.hasText(candidateReportId) && !candidateReportId.equals(this.reportId)) {
                return false;
            }
            return true;
        }

        private String inferReportId(AuditLogRecord record) {
            if (record == null) {
                return null;
            }
            if ("REPORT".equalsIgnoreCase(record.getTargetType())) {
                return record.getTargetId();
            }
            return null;
        }

        private GovernanceTraceSummaryVO toSummaryVO() {
            return new GovernanceTraceSummaryVO(
                this.traceId,
                this.requestId,
                this.serviceCode,
                this.operationType,
                this.resourceType,
                this.resourceId,
                this.latestStatus,
                this.lastSeenAt,
                Integer.valueOf(this.auditEventCount),
                Integer.valueOf(this.nonSuccessEventCount),
                Integer.valueOf(this.queryHistoryCount),
                Integer.valueOf(this.exportRecordCount),
                this.taskId,
                this.reportId,
                this.sqlFingerprint,
                this.errorCode,
                this.targetEngine,
                this.degraded,
                this.compensationReplayEvidence,
                this.cacheGovernanceSurface,
                this.artifactStorageContract,
                this.artifactRecoverySurface,
                this.artifactOperationSurface
            );
        }

        private GovernanceTraceDetailVO toDetailVO() {
            GovernanceTraceDetailVO detailVO = new GovernanceTraceDetailVO();
            detailVO.setTraceId(this.traceId);
            detailVO.setRequestId(this.requestId);
            detailVO.setServiceCode(this.serviceCode);
            detailVO.setOperationType(this.operationType);
            detailVO.setResourceType(this.resourceType);
            detailVO.setResourceId(this.resourceId);
            detailVO.setLatestStatus(this.latestStatus);
            detailVO.setLastSeenAt(this.lastSeenAt);
            detailVO.setAuditEventCount(Integer.valueOf(this.auditEventCount));
            detailVO.setNonSuccessEventCount(Integer.valueOf(this.nonSuccessEventCount));
            detailVO.setQueryHistoryCount(Integer.valueOf(this.queryHistoryCount));
            detailVO.setExportRecordCount(Integer.valueOf(this.exportRecordCount));
            detailVO.setTaskId(this.taskId);
            detailVO.setReportId(this.reportId);
            detailVO.setSqlFingerprint(this.sqlFingerprint);
            detailVO.setErrorCode(this.errorCode);
            detailVO.setTargetEngine(this.targetEngine);
            detailVO.setDegraded(this.degraded);
            detailVO.setCompensationReplayEvidence(this.compensationReplayEvidence);
            detailVO.setCacheGovernanceSurface(this.cacheGovernanceSurface);
            detailVO.setArtifactStorageContract(this.artifactStorageContract);
            detailVO.setArtifactRecoverySurface(this.artifactRecoverySurface);
            detailVO.setArtifactOperationSurface(this.artifactOperationSurface);
            detailVO.setAuditEvents(this.auditEvents);
            detailVO.setQueryHistories(this.queryHistories);
            detailVO.setExportRecords(this.exportRecords);
            return detailVO;
        }

        private LocalDateTime getLastSeenAt() {
            return lastSeenAt;
        }

        private int getAuditEventCount() {
            return auditEventCount;
        }

        private int getQueryHistoryCount() {
            return queryHistoryCount;
        }

        private int getExportRecordCount() {
            return exportRecordCount;
        }

        private static <T> T firstNonNull(T first, T second) {
            return first != null ? first : second;
        }

        private static Map<String, Object> selectEvidenceMap(Map<String, Object> current,
                                                             Map<String, Object> candidate,
                                                             boolean forceRefresh) {
            if (candidate == null || candidate.isEmpty()) {
                return current;
            }
            if (forceRefresh || current == null || current.isEmpty()) {
                return candidate;
            }
            return current;
        }

        private static String firstNonBlank(String first, String second) {
            if (StringUtils.hasText(first)) {
                return first.trim();
            }
            if (StringUtils.hasText(second)) {
                return second.trim();
            }
            return null;
        }

        private static String firstNonBlank(String first, String second, String third) {
            return firstNonBlank(first, firstNonBlank(second, third));
        }

        private static String firstNonBlank(String first, String second, String third, String fourth) {
            return firstNonBlank(first, firstNonBlank(second, third, fourth));
        }

        private static String firstNonBlank(String first, String second, String third, String fourth, String fifth) {
            return firstNonBlank(first, firstNonBlank(second, third, fourth, fifth));
        }
    }

    private static class RewriteHistoryScope {

        private final List<String> includeHistoryIds;
        private final List<String> excludeHistoryIds;

        private RewriteHistoryScope(List<String> includeHistoryIds, List<String> excludeHistoryIds) {
            this.includeHistoryIds = includeHistoryIds;
            this.excludeHistoryIds = excludeHistoryIds;
        }

        private static RewriteHistoryScope unfiltered() {
            return new RewriteHistoryScope(null, null);
        }

        private static RewriteHistoryScope include(List<String> historyIds) {
            return new RewriteHistoryScope(historyIds == null ? Collections.<String>emptyList() : historyIds, null);
        }

        private static RewriteHistoryScope exclude(List<String> historyIds) {
            return new RewriteHistoryScope(null, historyIds == null ? Collections.<String>emptyList() : historyIds);
        }

        private boolean isEmptyInclude() {
            return includeHistoryIds != null && includeHistoryIds.isEmpty();
        }
    }

    private static class LookupCursor {

        private final LocalDateTime createdAt;
        private final Long auditId;

        private LookupCursor(LocalDateTime createdAt, Long auditId) {
            this.createdAt = createdAt;
            this.auditId = auditId;
        }

        private LocalDateTime getCreatedAt() {
            return createdAt;
        }

        private Long getAuditId() {
            return auditId;
        }
    }
}
