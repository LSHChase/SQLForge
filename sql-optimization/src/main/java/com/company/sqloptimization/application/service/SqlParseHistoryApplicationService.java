package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.SqlParseHistoryExportRequest;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusVO;
import com.company.sqloptimization.application.controller.vo.SqlParseHistoryDetailVO;
import com.company.sqloptimization.application.controller.vo.SqlParseHistoryExportVO;
import com.company.sqloptimization.application.controller.vo.SqlParseHistoryPageVO;
import com.company.sqloptimization.application.controller.vo.SqlParseHistorySummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseQueryDateSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistory;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistoryFilter;
import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SqlParseHistoryApplicationService {

    public static final String SOURCE_STRUCTURE_PARSE = "STRUCTURE_PARSE";
    public static final String SOURCE_COMBINED_PARSE = "COMBINED_PARSE";
    public static final String SOURCE_PARSE_BATCH = "PARSE_BATCH";
    public static final String SOURCE_REPORT_BATCH = "REPORT_BATCH";
    public static final String SOURCE_END_OF_DAY_SLOW_SQL = "END_OF_DAY_SLOW_SQL";
    private static final String HISTORY_TYPE_SQL_PARSE_RECORD = "SQL_PARSE_RECORD";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() {
        };

    private final SqlParseHistoryRepository sqlParseHistoryRepository;

    public SqlParseHistoryApplicationService(SqlParseHistoryRepository sqlParseHistoryRepository) {
        this.sqlParseHistoryRepository = sqlParseHistoryRepository;
    }

    public SqlParseHistoryWriteResult writeStructureHistory(StructureParseResponseVO structureParse,
                                                            AccessParseResponseVO accessParse,
                                                            StructureParseRequest request,
                                                            String resultStatus) {
        return writeStructureHistory(
            structureParse,
            accessParse,
            request,
            resultStatus,
            SOURCE_STRUCTURE_PARSE,
            null,
            null
        );
    }

    public SqlParseHistoryWriteResult writeStructureHistory(StructureParseResponseVO structureParse,
                                                            AccessParseResponseVO accessParse,
                                                            StructureParseRequest request,
                                                            String resultStatus,
                                                            String sourceType,
                                                            String sourceId,
                                                            String batchKey) {
        if (structureParse == null || request == null) {
            return new SqlParseHistoryWriteResult(null, Boolean.FALSE, "WRITE_SKIPPED");
        }
        LinkedHashMap<String, Object> resultSummary = buildResultSummary(structureParse, accessParse, resultStatus);
        LinkedHashMap<String, Object> resultPayload = new LinkedHashMap<String, Object>();
        resultPayload.put("structureParse", structureParse);
        if (accessParse != null) {
            resultPayload.put("accessParse", accessParse);
        }
        return writeHistory(
            structureParse,
            accessParse,
            request,
            normalizeResultStatus(resultStatus),
            normalizeSourceType(sourceType),
            sourceId,
            batchKey,
            resultSummary,
            resultPayload
        );
    }

    public SqlParseHistoryWriteResult writeCombinedHistory(CombinedParseStatusVO status,
                                                           StructureParseRequest request,
                                                           String resultStatus) {
        if (status == null || status.getStructureParse() == null || request == null) {
            return new SqlParseHistoryWriteResult(null, Boolean.FALSE, "WRITE_SKIPPED");
        }
        return writeHistory(
            status.getStructureParse(),
            status.getAccessParse(),
            request,
            normalizeResultStatus(resultStatus),
            SOURCE_COMBINED_PARSE,
            status.getParseTaskId(),
            null,
            status.getConclusion(),
            status
        );
    }

    public SqlParseHistoryWriteResult writeEndOfDaySlowSqlHistory(StructureParseResponseVO structureParse,
                                                                  StructureParseRequest request,
                                                                  String batchKey,
                                                                  String sourceId,
                                                                  String resultStatus) {
        if (!StringUtils.hasText(batchKey) || structureParse == null || request == null) {
            return new SqlParseHistoryWriteResult(null, Boolean.FALSE, "WRITE_SKIPPED");
        }
        String sqlFingerprint = resolveSqlFingerprint(structureParse, request);
        SqlParseHistory existing = sqlParseHistoryRepository.findByBatchKeyAndSqlFingerprint(batchKey, sqlFingerprint);
        if (existing != null) {
            return new SqlParseHistoryWriteResult(existing.getParseHistoryId(), Boolean.TRUE, "IDEMPOTENT_REUSED");
        }
        return writeStructureHistory(
            structureParse,
            null,
            request,
            resultStatus,
            SOURCE_END_OF_DAY_SLOW_SQL,
            sourceId,
            batchKey
        );
    }

    public SqlParseHistoryPageVO findPage(String tenantId,
                                          String sourceType,
                                          String reportCode,
                                          String datasourceCode,
                                          String stage,
                                          String bizDate,
                                          String queryDateStart,
                                          String queryDateEnd,
                                          String status,
                                          String logicalObjectType,
                                          String accessChannel,
                                          String engine,
                                          String submittedBy,
                                          String traceId,
                                          String parseTaskId,
                                          String submittedStart,
                                          String submittedEnd,
                                          String sortBy,
                                          String sortOrder,
                                          Integer pageNo,
                                          Integer pageSize) {
        String effectiveTenantId = resolveTenantId(tenantId);
        int resolvedPageNo = normalizePageNo(pageNo);
        int resolvedPageSize = normalizePageSize(pageSize);
        SqlParseHistoryFilter filter = new SqlParseHistoryFilter();
        filter.setTenantId(effectiveTenantId);
        filter.setSourceType(normalizeSourceTypeFilter(sourceType));
        filter.setReportCode(trimToNull(reportCode));
        filter.setDatasourceCode(trimToNull(datasourceCode));
        filter.setStageCode(trimToNull(stage));
        filter.setBizDate(trimToNull(bizDate));
        filter.setQueryDateStart(trimToNull(queryDateStart));
        filter.setQueryDateEnd(trimToNull(queryDateEnd));
        filter.setStatus(normalizeResultStatusFilter(status));
        filter.setLogicalObjectType(trimToNull(logicalObjectType));
        filter.setAccessChannel(trimToNull(accessChannel));
        filter.setEngine(trimToNull(engine));
        filter.setSubmittedBy(trimToNull(submittedBy));
        filter.setTraceId(trimToNull(traceId));
        filter.setParseTaskId(trimToNull(parseTaskId));
        filter.setSubmittedStart(parseWindowValue(submittedStart, "submittedStart"));
        filter.setSubmittedEnd(parseWindowValue(submittedEnd, "submittedEnd"));
        filter.setOrderByClause(resolveOrderBy(sortBy, sortOrder));
        filter.setOffset((resolvedPageNo - 1) * resolvedPageSize);
        filter.setLimit(resolvedPageSize + 1);
        List<SqlParseHistory> rows = sqlParseHistoryRepository.findPage(filter);
        boolean hasMore = rows.size() > resolvedPageSize;
        if (hasMore) {
            rows = new ArrayList<SqlParseHistory>(rows.subList(0, resolvedPageSize));
        }
        filter.setLimit(resolvedPageSize);
        int totalCount = sqlParseHistoryRepository.count(filter);
        List<SqlParseHistorySummaryVO> items = new ArrayList<SqlParseHistorySummaryVO>(rows.size());
        for (SqlParseHistory row : rows) {
            items.add(toSummary(row));
        }
        return new SqlParseHistoryPageVO(
            items,
            Integer.valueOf(resolvedPageNo),
            Integer.valueOf(resolvedPageSize),
            Integer.valueOf(totalCount),
            Integer.valueOf(pageCount(totalCount, resolvedPageSize)),
            Boolean.valueOf(hasMore),
            buildClassificationSummary(items)
        );
    }

    public SqlParseHistoryDetailVO findDetail(String tenantId, String parseHistoryId) {
        String effectiveTenantId = resolveTenantId(tenantId);
        String normalizedId = requireText(parseHistoryId, "parseHistoryId");
        SqlParseHistory history = sqlParseHistoryRepository.findByParseHistoryId(normalizedId);
        if (history == null || !effectiveTenantId.equals(history.getTenantId())) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.NOT_FOUND,
                "SQL 解析历史记录不存在"
            );
        }
        return toDetail(history);
    }

    public SqlParseHistoryExportVO exportHistory(String tenantId, SqlParseHistoryExportRequest request) {
        String parseHistoryId = trimToNull(request == null ? null : request.getParseHistoryId());
        if (!StringUtils.hasText(parseHistoryId)) {
            parseHistoryId = trimToNull(request == null ? null : request.getHistoryId());
        }
        SqlParseHistoryDetailVO detail = findDetail(tenantId, parseHistoryId);
        String exportFormat = requireSupportedExportFormat(request == null ? null : request.getExportFormat());
        String exportId = "sql-parse-history-export-" + detail.getParseHistoryId() + "-"
            + UUID.randomUUID().toString().replace("-", "");
        Instant exportedAt = Instant.now();
        SqlParseHistoryExportVO response = new SqlParseHistoryExportVO();
        response.setExportId(exportId);
        response.setParseHistoryId(detail.getParseHistoryId());
        response.setHistoryId(detail.getHistoryId());
        response.setExportFormat(exportFormat);
        response.setExportStatus("GENERATED");
        response.setFileName("sql-parse-history-" + detail.getParseHistoryId() + "." + exportFormat.toLowerCase(Locale.ROOT));
        response.setContentType(resolveExportContentType(exportFormat));
        response.setStorageType("INLINE_RESPONSE");
        response.setStorageUri("inline://sql-optimization/parse-history/" + exportId);
        response.setExportedAt(exportedAt);
        LinkedHashMap<String, Object> auditReference = new LinkedHashMap<String, Object>();
        auditReference.put("serviceCode", "SQL_OPTIMIZATION");
        auditReference.put("operationCode", "SQL_PARSE_HISTORY_EXPORT");
        auditReference.put("parseHistoryId", detail.getParseHistoryId());
        auditReference.put("exportedAt", exportedAt.toString());
        response.setAuditReference(auditReference);
        response.setPayload(JsonUtils.toJson(detail));
        return response;
    }

    private SqlParseHistoryWriteResult writeHistory(StructureParseResponseVO structureParse,
                                                    AccessParseResponseVO accessParse,
                                                    StructureParseRequest request,
                                                    String resultStatus,
                                                    String sourceType,
                                                    String sourceId,
                                                    String batchKey,
                                                    Object resultSummary,
                                                    Object resultPayload) {
        try {
            Instant now = Instant.now();
            String sqlFingerprint = resolveSqlFingerprint(structureParse, request);
            String parseTaskId = requireText(structureParse.getParseTaskId(), "parseTaskId");
            String parseHistoryId = "parse-history-" + sanitizeKey(parseTaskId);
            SqlParseHistory existing = sqlParseHistoryRepository.findByParseHistoryId(parseHistoryId);
            SqlParseHistory history = new SqlParseHistory();
            history.setParseHistoryId(parseHistoryId);
            history.setTenantId(resolveTenantId(null));
            history.setSourceType(sourceType);
            history.setSourceId(trimToNull(sourceId));
            history.setBatchKey(trimToNull(batchKey));
            history.setParseTaskId(parseTaskId);
            history.setSqlFingerprint(sqlFingerprint);
            history.setDatasourceCode(trimToNull(request.getDatasourceCode()));
            history.setDatasourceType("AUTO");
            populateContextFields(history, request, structureParse, accessParse, sourceType);
            history.setParserMode(trimToNull(request.getParserMode()));
            history.setSqlText(request.getSqlText());
            history.setSqlTemplateText(trimToNull(request.getSqlTemplateText()));
            history.setBindingMode(trimToNull(request.getBindingMode()));
            history.setParameterizedSqlFlag(Boolean.valueOf(StringUtils.hasText(request.getSqlTemplateText())));
            history.setResultStatus(resultStatus);
            history.setTargetEngine("AUTO");
            history.setStructureParseSummaryJson(toJson(structureParse));
            history.setAccessParseSummaryJson(accessParse == null ? null : toJson(accessParse));
            history.setResultSummaryJson(toJson(resultSummary));
            history.setResultPayloadJson(toJson(resultPayload));
            history.setQueryContextJson(buildQueryContextJson(history, request, structureParse, accessParse, resultSummary));
            history.setCommentContextJson(toJsonOrNull(request.getCommentContext()));
            history.setBindingSummaryJson(buildBindingSummaryJson(request, sqlFingerprint));
            history.setLogicalObjectHitsJson(toJsonOrNull(structureParse.getLogicalObjectHits()));
            history.setIssueScenesJson(toJsonOrNull(extractIssueScenes(structureParse)));
            history.setLogicalObjectKeysJson(toJsonOrNull(extractLogicalObjectKeys(structureParse)));
            history.setTraceId(trimToNull(RequestContext.getTraceId()));
            history.setRequestId(trimToNull(RequestContext.getRequestId()));
            history.setSagaId("parse-history-" + sanitizeKey(parseTaskId));
            history.setSubmittedBy(trimToNull(RequestContext.getUserId()));
            history.setSubmittedAt(existing == null || existing.getSubmittedAt() == null ? now : existing.getSubmittedAt());
            history.setCreatedAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt());
            history.setUpdatedAt(now);
            sqlParseHistoryRepository.save(history);
            return new SqlParseHistoryWriteResult(parseHistoryId, Boolean.TRUE, existing == null ? "SAVED" : "UPSERTED");
        } catch (RuntimeException ex) {
            return new SqlParseHistoryWriteResult(null, Boolean.FALSE, "WRITE_FAILED");
        }
    }

    private void populateContextFields(SqlParseHistory history,
                                       StructureParseRequest request,
                                       StructureParseResponseVO structureParse,
                                       AccessParseResponseVO accessParse,
                                       String sourceType) {
        Map<String, Object> commentContext = request.getCommentContext() == null
            ? Collections.<String, Object>emptyMap()
            : request.getCommentContext();
        history.setReportCode(firstText(commentContext, "report_code", "reportCode"));
        history.setStageCode(firstText(commentContext, "stage", "stageCode"));
        history.setBizDate(firstText(commentContext, "biz_date", "bizDate"));
        StructureParseQueryDateSummaryVO queryDateSummary = structureParse.getQueryDateSummary();
        if (queryDateSummary != null) {
            history.setQueryDateStart(queryDateSummary.getQueryDateStart());
            history.setQueryDateEnd(queryDateSummary.getQueryDateEnd());
            history.setQueryDateStatus(queryDateSummary.getQueryDateStatus());
        }
        history.setAccessChannel(resolveAccessChannel(sourceType, accessParse));
    }

    private String buildQueryContextJson(SqlParseHistory history,
                                         StructureParseRequest request,
                                         StructureParseResponseVO structureParse,
                                         AccessParseResponseVO accessParse,
                                         Object resultSummary) {
        LinkedHashMap<String, Object> queryContext = new LinkedHashMap<String, Object>();
        queryContext.put("parseTaskId", history.getParseTaskId());
        queryContext.put("sourceType", history.getSourceType());
        queryContext.put("sourceId", history.getSourceId());
        queryContext.put("batchKey", history.getBatchKey());
        queryContext.put("datasourceCode", history.getDatasourceCode());
        queryContext.put("datasourceType", history.getDatasourceType());
        queryContext.put("sqlFingerprint", history.getSqlFingerprint());
        queryContext.put("accessChannel", history.getAccessChannel());
        if (request.getCommentContext() != null && !request.getCommentContext().isEmpty()) {
            queryContext.put("commentContext", request.getCommentContext());
        }
        if (structureParse.getQueryDateSummary() != null) {
            queryContext.put("queryDateSummary", structureParse.getQueryDateSummary());
        }
        queryContext.put("queryDateStart", history.getQueryDateStart());
        queryContext.put("queryDateEnd", history.getQueryDateEnd());
        queryContext.put("queryDateStatus", history.getQueryDateStatus());
        queryContext.put("bindingSummary", parseJsonMap(buildBindingSummaryJson(request, history.getSqlFingerprint())));
        queryContext.put("logicalObjectHits", structureParse.getLogicalObjectHits());
        queryContext.put("structureParseSummary", structureParse);
        if (accessParse != null) {
            queryContext.put("accessParseSummary", accessParse);
        }
        if (structureParse.getPlanAnalysis() != null) {
            queryContext.put("planAnalysis", structureParse.getPlanAnalysis());
        }
        queryContext.put("analysisStatus", structureParse.getAnalysisStatus());
        queryContext.put("structureAnalysisStatus", structureParse.getStructureAnalysisStatus());
        queryContext.put("resultSummary", resultSummary);
        return toJson(queryContext);
    }

    private LinkedHashMap<String, Object> buildResultSummary(StructureParseResponseVO structureParse,
                                                             AccessParseResponseVO accessParse,
                                                             String resultStatus) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("overallStatus", normalizeResultStatus(resultStatus));
        summary.put("analysisStatus", structureParse.getAnalysisStatus());
        summary.put("structureAnalysisStatus", structureParse.getStructureAnalysisStatus());
        summary.put("structureAvailable", Boolean.TRUE);
        summary.put("accessAvailable", Boolean.valueOf(accessParse != null
            && "AVAILABLE".equals(accessParse.getServiceStatus())
            && "CONNECTED".equals(accessParse.getConnectionStatus())));
        summary.put("planStatus", structureParse.getPlanAnalysis() == null ? null : structureParse.getPlanAnalysis().getStatus());
        summary.put("planAvailable", Boolean.valueOf(structureParse.getPlanAnalysis() != null
            && "SUCCESS".equals(structureParse.getPlanAnalysis().getStatus())));
        summary.put("degradeReason", resolveSummaryDegradeReason(structureParse, accessParse));
        return summary;
    }

    private String resolveSummaryDegradeReason(StructureParseResponseVO structureParse,
                                               AccessParseResponseVO accessParse) {
        if (accessParse != null && StringUtils.hasText(accessParse.getDegradeReason())) {
            return accessParse.getDegradeReason();
        }
        if (structureParse != null && StringUtils.hasText(structureParse.getFailureReason())) {
            return structureParse.getFailureReason();
        }
        if (structureParse != null
            && structureParse.getPlanAnalysis() != null
            && StringUtils.hasText(structureParse.getPlanAnalysis().getFailureReason())) {
            return structureParse.getPlanAnalysis().getFailureReason();
        }
        return null;
    }

    private String buildBindingSummaryJson(StructureParseRequest request, String sqlFingerprint) {
        LinkedHashMap<String, Object> binding = new LinkedHashMap<String, Object>();
        binding.put("parameterizedSqlFlag", Boolean.valueOf(StringUtils.hasText(request.getSqlTemplateText())));
        binding.put("bindingMode", trimToNull(request.getBindingMode()));
        binding.put("bindingRenderStatus", "CAPTURED");
        binding.put("sqlFingerprint", sqlFingerprint);
        return toJson(binding);
    }

    private SqlParseHistorySummaryVO toSummary(SqlParseHistory history) {
        SqlParseHistorySummaryVO vo = new SqlParseHistorySummaryVO();
        vo.setParseHistoryId(history.getParseHistoryId());
        vo.setHistoryId(history.getParseHistoryId());
        vo.setTenantId(history.getTenantId());
        vo.setHistoryType(HISTORY_TYPE_SQL_PARSE_RECORD);
        vo.setSourceType(history.getSourceType());
        vo.setSourceId(history.getSourceId());
        vo.setBatchKey(history.getBatchKey());
        vo.setParseTaskId(history.getParseTaskId());
        vo.setSqlFingerprint(history.getSqlFingerprint());
        vo.setDatasourceCode(history.getDatasourceCode());
        vo.setDatasourceType(history.getDatasourceType());
        vo.setReportCode(history.getReportCode());
        vo.setStageCode(history.getStageCode());
        vo.setBizDate(history.getBizDate());
        vo.setQueryDateStart(history.getQueryDateStart());
        vo.setQueryDateEnd(history.getQueryDateEnd());
        vo.setQueryDateStatus(history.getQueryDateStatus());
        vo.setAccessChannel(history.getAccessChannel());
        vo.setParserMode(history.getParserMode());
        vo.setBindingMode(history.getBindingMode());
        vo.setParameterizedSqlFlag(history.getParameterizedSqlFlag());
        vo.setResultStatus(history.getResultStatus());
        vo.setTargetEngine(history.getTargetEngine());
        vo.setTraceId(history.getTraceId());
        vo.setRequestId(history.getRequestId());
        vo.setSagaId(history.getSagaId());
        vo.setSubmittedBy(history.getSubmittedBy());
        vo.setSubmittedAt(history.getSubmittedAt());
        vo.setCreatedAt(history.getCreatedAt());
        vo.setUpdatedAt(history.getUpdatedAt());
        return vo;
    }

    private SqlParseHistoryDetailVO toDetail(SqlParseHistory history) {
        SqlParseHistoryDetailVO detail = new SqlParseHistoryDetailVO();
        copySummary(toSummary(history), detail);
        detail.setSqlText(history.getSqlText());
        detail.setSqlTemplateText(history.getSqlTemplateText());
        LinkedHashMap<String, Object> sqlState = new LinkedHashMap<String, Object>();
        sqlState.put("sqlFingerprint", history.getSqlFingerprint());
        sqlState.put("bindingMode", history.getBindingMode());
        sqlState.put("bindingRenderStatus", "CAPTURED");
        sqlState.put("parameterizedSqlFlag", history.getParameterizedSqlFlag());
        detail.setSqlState(sqlState);
        detail.setStructureParseSummary(parseJsonValue(history.getStructureParseSummaryJson()));
        detail.setAccessParseSummary(parseJsonValue(history.getAccessParseSummaryJson()));
        detail.setExecutionSummary(parseJsonValue(history.getResultSummaryJson()));
        detail.setQueryContext(parseJsonValue(history.getQueryContextJson()));
        detail.setCommentContext(parseJsonValue(history.getCommentContextJson()));
        detail.setBindingSummary(parseJsonValue(history.getBindingSummaryJson()));
        detail.setLogicalObjectHits(parseJsonValue(history.getLogicalObjectHitsJson()));
        detail.setIssueScenes(parseJsonValue(history.getIssueScenesJson()));
        detail.setLogicalObjectKeys(parseJsonValue(history.getLogicalObjectKeysJson()));
        detail.setRecommendationRefs(Collections.<Map<String, Object>>emptyList());
        detail.setBenchmarkRefs(Collections.<Map<String, Object>>emptyList());
        detail.setAlertRefs(Collections.<Map<String, Object>>emptyList());
        detail.setAuditRefs(Collections.<Map<String, Object>>emptyList());
        return detail;
    }

    private void copySummary(SqlParseHistorySummaryVO source, SqlParseHistorySummaryVO target) {
        target.setParseHistoryId(source.getParseHistoryId());
        target.setHistoryId(source.getHistoryId());
        target.setTenantId(source.getTenantId());
        target.setHistoryType(source.getHistoryType());
        target.setSourceType(source.getSourceType());
        target.setSourceId(source.getSourceId());
        target.setBatchKey(source.getBatchKey());
        target.setParseTaskId(source.getParseTaskId());
        target.setSqlFingerprint(source.getSqlFingerprint());
        target.setDatasourceCode(source.getDatasourceCode());
        target.setDatasourceType(source.getDatasourceType());
        target.setReportCode(source.getReportCode());
        target.setStageCode(source.getStageCode());
        target.setBizDate(source.getBizDate());
        target.setQueryDateStart(source.getQueryDateStart());
        target.setQueryDateEnd(source.getQueryDateEnd());
        target.setQueryDateStatus(source.getQueryDateStatus());
        target.setAccessChannel(source.getAccessChannel());
        target.setParserMode(source.getParserMode());
        target.setBindingMode(source.getBindingMode());
        target.setParameterizedSqlFlag(source.getParameterizedSqlFlag());
        target.setResultStatus(source.getResultStatus());
        target.setTargetEngine(source.getTargetEngine());
        target.setTraceId(source.getTraceId());
        target.setRequestId(source.getRequestId());
        target.setSagaId(source.getSagaId());
        target.setSubmittedBy(source.getSubmittedBy());
        target.setSubmittedAt(source.getSubmittedAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private Map<String, Object> buildClassificationSummary(List<SqlParseHistorySummaryVO> items) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        LinkedHashMap<String, Integer> statusCounts = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, Integer> accessChannelCounts = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, Integer> sourceTypeCounts = new LinkedHashMap<String, Integer>();
        int success = 0;
        int nonSuccess = 0;
        for (SqlParseHistorySummaryVO item : items) {
            String status = item.getResultStatus() == null ? "UNKNOWN" : item.getResultStatus();
            statusCounts.put(status, Integer.valueOf(statusCounts.getOrDefault(status, Integer.valueOf(0)).intValue() + 1));
            if ("SUCCESS".equals(status)) {
                success += 1;
            } else {
                nonSuccess += 1;
            }
            String channel = item.getAccessChannel() == null ? "UNKNOWN" : item.getAccessChannel();
            accessChannelCounts.put(channel, Integer.valueOf(accessChannelCounts.getOrDefault(channel, Integer.valueOf(0)).intValue() + 1));
            String sourceType = item.getSourceType() == null ? "UNKNOWN" : item.getSourceType();
            sourceTypeCounts.put(sourceType, Integer.valueOf(sourceTypeCounts.getOrDefault(sourceType, Integer.valueOf(0)).intValue() + 1));
        }
        summary.putAll(statusCounts);
        summary.put("succeeded", Integer.valueOf(success));
        summary.put("nonSuccess", Integer.valueOf(nonSuccess));
        summary.put("statusCounts", statusCounts);
        summary.put("accessChannelCounts", accessChannelCounts);
        summary.put("sourceTypeCounts", sourceTypeCounts);
        return summary;
    }

    private List<String> extractIssueScenes(StructureParseResponseVO structureParse) {
        if (structureParse.getIssues() == null || structureParse.getIssues().isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (StructureParseIssueVO issue : structureParse.getIssues()) {
            String issueScene = trimToNull(issue.getIssueScene());
            values.add(issueScene == null ? issue.getIssueCode() : issueScene);
        }
        return new ArrayList<String>(values);
    }

    private List<String> extractLogicalObjectKeys(StructureParseResponseVO structureParse) {
        if (structureParse.getLogicalObjectHits() == null || structureParse.getLogicalObjectHits().isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (Object hit : structureParse.getLogicalObjectHits()) {
            if (hit == null) {
                continue;
            }
            try {
                Object mappedPhysicalTargets = hit.getClass().getMethod("getMappedPhysicalTargets").invoke(hit);
                if (mappedPhysicalTargets instanceof Iterable) {
                    for (Object target : (Iterable<?>) mappedPhysicalTargets) {
                        addTableKey(values, target == null ? null : String.valueOf(target));
                    }
                }
                Object objectKey = hit.getClass().getMethod("getObjectKey").invoke(hit);
                addTableKey(values, objectKey == null ? null : String.valueOf(objectKey));
            } catch (Exception ignored) {
                // LogicalObjectSurface 通过 bean getter 暴露 objectKey；忽略非预期结构。
            }
        }
        if (!values.isEmpty()) {
            return new ArrayList<String>(values);
        }
        for (Object hit : structureParse.getLogicalObjectHits()) {
            if (hit == null) {
                continue;
            }
            try {
                Object objectKey = hit.getClass().getMethod("getObjectKey").invoke(hit);
                if (objectKey != null) {
                    values.add(String.valueOf(objectKey));
                }
            } catch (Exception ignored) {
                // 保留非预期逻辑对象结构的既有兜底行为。
            }
        }
        return new ArrayList<String>(values);
    }

    private void addTableKey(LinkedHashSet<String> values, String candidate) {
        String normalized = trimToNull(candidate);
        if (normalized != null && normalized.toUpperCase(Locale.ROOT).startsWith("TABLE:")) {
            values.add(normalized);
        }
    }

    private String resolveAccessChannel(String sourceType, AccessParseResponseVO accessParse) {
        if (SOURCE_PARSE_BATCH.equals(sourceType)) {
            return "PARSE_BATCH";
        }
        if (SOURCE_REPORT_BATCH.equals(sourceType)) {
            return "REPORT_BATCH";
        }
        if (SOURCE_END_OF_DAY_SLOW_SQL.equals(sourceType)) {
            return "END_OF_DAY_BATCH";
        }
        return accessParse == null ? "PAGE_STRUCTURE" : "PAGE_COMBINED";
    }

    private String resolveSqlFingerprint(StructureParseResponseVO structureParse, StructureParseRequest request) {
        if (StringUtils.hasText(structureParse.getSqlFingerprint())) {
            return structureParse.getSqlFingerprint();
        }
        return SqlFingerprintUtils.fingerprint(request.getSqlText());
    }

    private String resolveOrderBy(String sortBy, String sortOrder) {
        String normalizedSortBy = trimToNull(sortBy);
        String column = "submitted_at";
        if ("createdAt".equals(normalizedSortBy)) {
            column = "created_at";
        } else if ("updatedAt".equals(normalizedSortBy)) {
            column = "updated_at";
        } else if ("resultStatus".equals(normalizedSortBy)) {
            column = "result_status";
        } else if ("reportCode".equals(normalizedSortBy)) {
            column = "report_code";
        }
        String direction = "ASC".equalsIgnoreCase(trimToNull(sortOrder)) ? "ASC" : "DESC";
        return column + " " + direction + ", parse_history_id DESC";
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = trimToNull(sourceType);
        return normalized == null ? SOURCE_STRUCTURE_PARSE : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeSourceTypeFilter(String sourceType) {
        String normalized = trimToNull(sourceType);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeResultStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "PARTIAL";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("SUCCEEDED".equals(upper) || "VALID".equals(upper) || upper.endsWith("_SUCCEEDED")) {
            return "SUCCESS";
        }
        if (upper.contains("PARTIAL") || upper.contains("WAITING") || upper.contains("PARSING")) {
            return "PARTIAL";
        }
        if (upper.contains("FAILED") || upper.contains("INVALID")) {
            return "FAILED";
        }
        return upper;
    }

    private String normalizeResultStatusFilter(String status) {
        String normalized = trimToNull(status);
        return normalized == null ? null : normalizeResultStatus(normalized);
    }

    private String requireSupportedExportFormat(String exportFormat) {
        String normalized = trimToNull(exportFormat);
        if (normalized == null) {
            return "JSON";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!"JSON".equals(upper) && !"CSV".equals(upper)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "exportFormat 必须为 JSON 或 CSV"
            );
        }
        return upper;
    }

    private String resolveExportContentType(String exportFormat) {
        return "CSV".equals(exportFormat) ? "text/csv" : "application/json";
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo.intValue() <= 0 ? DEFAULT_PAGE_NO : pageNo.intValue();
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize.intValue() <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(MAX_PAGE_SIZE, pageSize.intValue());
    }

    private int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    private LocalDateTime parseWindowValue(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized.length() == 10 ? normalized + "T00:00:00" : normalized.replace(' ', 'T'));
        } catch (DateTimeParseException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 必须是 ISO 日期或日期时间"
            );
        }
    }

    private String resolveTenantId(String tenantId) {
        String normalized = trimToNull(tenantId);
        if (normalized != null) {
            return normalized;
        }
        normalized = trimToNull(RequestContext.getTenantId());
        if (normalized != null) {
            return normalized;
        }
        return trimToNull(TenantContext.get());
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return normalized;
    }

    private String firstText(Map<String, Object> source, String firstKey, String secondKey) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Object value = source.get(firstKey);
        if (value == null) {
            value = source.get(secondKey);
        }
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String sanitizeKey(String value) {
        return requireText(value, "key").replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        return JsonUtils.toJson(value);
    }

    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return JsonUtils.toJson(value);
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
}
