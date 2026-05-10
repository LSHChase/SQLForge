package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestContext.ContextValue;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.BatchPageResponse;
import com.company.sqloptimization.application.controller.vo.ParseBatchStageStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchItemVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusResponse;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.service.report.ReportSqlResolveRequest;
import com.company.sqloptimization.application.service.report.ReportSqlResolveResult;
import com.company.sqloptimization.application.service.report.ReportSqlResolver;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import com.company.sqloptimization.domain.reportbatch.ReportBatchStatusTransition;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchItemRepository;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchRepository;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportBatchApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportBatchApplicationService.class);
    private static final int ITEM_PREVIEW_LIMIT = 500;
    private static final int FAILURE_REASON_LIMIT = 128;
    private static final int DEFAULT_PARSE_CONCURRENCY = 8;
    private static final int MAX_PARSE_CONCURRENCY = 32;
    private static final int DEFAULT_PERSIST_CHUNK_SIZE = 200;
    private static final int COORDINATOR_QUEUE_CAPACITY = 16;
    private static final int DEFAULT_LIST_PAGE_SIZE = 10;
    private static final int MAX_LIST_PAGE_SIZE = 100;
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger(0);
    private static final Pattern REPORT_SQL_START_PATTERN =
        Pattern.compile("(?i)\\b(WITH|SELECT)\\b(?=\\s|/\\*)");

    private final ReportBatchRepository reportBatchRepository;
    private final ReportBatchItemRepository reportBatchItemRepository;
    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;
    private final ReportSqlResolver reportSqlResolver;
    private final ReportBatchParseStatisticsAssembler parseStatisticsAssembler =
        new ReportBatchParseStatisticsAssembler();
    private final Set<String> runningReportBatches = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final ExecutorService reportBatchCoordinatorExecutor = new ThreadPoolExecutor(
        1,
        2,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(COORDINATOR_QUEUE_CAPACITY),
        namedThreadFactory("report-batch-coordinator")
    );

    @Value("${sql-optimization.report-batch.parse-concurrency:8}")
    private int reportBatchParseConcurrency = DEFAULT_PARSE_CONCURRENCY;

    @Value("${sql-optimization.report-batch.persist-chunk-size:200}")
    private int reportBatchPersistChunkSize = DEFAULT_PERSIST_CHUNK_SIZE;

    public ReportBatchApplicationService(ReportBatchRepository reportBatchRepository,
                                         ReportBatchItemRepository reportBatchItemRepository,
                                         StructureParseApplicationService structureParseApplicationService,
                                         AccessParseApplicationService accessParseApplicationService,
                                         ReportSqlResolver reportSqlResolver) {
        this.reportBatchRepository = reportBatchRepository;
        this.reportBatchItemRepository = reportBatchItemRepository;
        this.structureParseApplicationService = structureParseApplicationService;
        this.accessParseApplicationService = accessParseApplicationService;
        this.reportSqlResolver = reportSqlResolver;
    }

    public ReportBatchStatusResponse importBatch(ReportBatchImportRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        String batchName = requireText(request == null ? null : request.getBatchName(), "batchName");
        String reportCodeField = requireText(request == null ? null : request.getReportCodeField(), "reportCodeField");
        byte[] content = decodeBase64(trimToNull(request == null ? null : request.getContentBase64()));
        String fileType = resolveFileType(request, content);
        List<ReportSourceRow> rows = parseSourceRows(
            content,
            request == null ? null : request.getCharset(),
            fileType,
            reportCodeField
        );
        if (rows.isEmpty()) {
            throw invalidArgument("contentBase64", "No report codes were found in the report batch payload");
        }
        Instant now = Instant.now();
        ReportBatch batch = ReportBatch.initialize(
            UUID.randomUUID().toString(),
            tenantId,
            batchName,
            fileType,
            reportCodeField,
            trimToNull(request == null ? null : request.getDatasourceCode()),
            trimToNull(request == null ? null : request.getStage()),
            trimToNull(request == null ? null : request.getPriority()),
            SqlParserMode.resolve(request == null ? null : request.getParserMode()).name(),
            containsInlineSql(rows) ? "WIDE_SQL_IMPORT" : "TXT_MOCK_SOURCE",
            RequestContext.getUserId(),
            now
        );
        List<ReportBatchItem> items = buildItems(batch, rows);
        batch.recordImportedItems(countDistinctReports(items), now);
        reportBatchRepository.save(batch);
        reportBatchItemRepository.saveAll(items);
        return toResponse(batch, items);
    }

    public ReportBatchStatusResponse resolveSqls(String batchId) {
        ReportBatch batch = requireBatch(batchId);
        List<ReportBatchItem> items = reportBatchItemRepository.findByBatchId(batch.getBatchId());
        if (items.isEmpty()) {
            return toResponse(batch, items);
        }
        if (runningReportBatches.contains(batch.getBatchId())) {
            return toResponse(batch, items);
        }
        if (!runningReportBatches.add(batch.getBatchId())) {
            return toResponse(batch, items);
        }

        Instant now = Instant.now();
        batch.transitionTo(ReportBatch.ParseStatus.RESOLVING_SQLS, now, "REPORT_SQL_RESOLUTION_STARTED");
        reportBatchRepository.save(batch);
        ContextValue contextValue = RequestContext.snapshot();
        try {
            reportBatchCoordinatorExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    runReportBatchStructureResolution(batch.getBatchId(), contextValue);
                }
            });
        } catch (RejectedExecutionException ex) {
            runningReportBatches.remove(batch.getBatchId());
            batch.transitionTo(ReportBatch.ParseStatus.FAILED, Instant.now(), "REPORT_SQL_RESOLUTION_START_REJECTED");
            reportBatchRepository.save(batch);
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_PIPELINE_NOT_READY,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Report batch parse executor is saturated"
            );
        }
        return toResponse(batch, items);
    }

    public ReportBatchStatusResponse getBatch(String batchId) {
        return getBatch(batchId, null, null, null);
    }

    public ReportBatchStatusResponse getBatch(String batchId,
                                              Integer pageNumber,
                                              Integer pageSize,
                                              String reportCode) {
        ReportBatch batch = requireBatch(batchId);
        return toResponse(
            batch,
            reportBatchItemRepository.findByBatchId(batch.getBatchId()),
            true,
            PageSelection.from(pageNumber, pageSize, reportCode, ITEM_PREVIEW_LIMIT)
        );
    }

    public ReportBatchParseStatisticsVO getBatchParseStatistics(String batchId) {
        return getBatchParseStatistics(batchId, null, null, null);
    }

    public ReportBatchParseStatisticsVO getBatchParseStatistics(String batchId,
                                                               Integer pageNumber,
                                                               Integer pageSize,
                                                               String reportCode) {
        ReportBatch batch = requireBatch(batchId);
        return parseStatisticsAssembler.build(
            reportBatchItemRepository.findByBatchId(batch.getBatchId()),
            pageNumber,
            pageSize,
            reportCode
        );
    }

    public ReportBatchIssueSceneDetailVO getBatchIssueSceneDetail(String batchId,
                                                                  String issueScene,
                                                                  Integer pageNumber,
                                                                  Integer pageSize,
                                                                  String reportCode,
                                                                  String logicalObjectKey) {
        ReportBatch batch = requireBatch(batchId);
        return parseStatisticsAssembler.buildIssueSceneDetail(
            reportBatchItemRepository.findByBatchId(batch.getBatchId()),
            requireText(issueScene, "issueScene"),
            pageNumber,
            pageSize,
            reportCode,
            logicalObjectKey
        );
    }

    public BatchPageResponse<ReportBatchStatusResponse> listBatches(Integer pageNo, Integer pageSize) {
        String tenantId = requireAuthorizedTenant(null);
        List<ReportBatchStatusResponse> result = new ArrayList<ReportBatchStatusResponse>();
        for (ReportBatch batch : reportBatchRepository.findAll()) {
            if (!tenantId.equals(batch.getTenantId())) {
                continue;
            }
            result.add(toResponse(batch, reportBatchItemRepository.findByBatchId(batch.getBatchId()), false));
        }
        result.sort(new Comparator<ReportBatchStatusResponse>() {
            @Override
            public int compare(ReportBatchStatusResponse left, ReportBatchStatusResponse right) {
                if (left.getCreatedAt() == null && right.getCreatedAt() == null) {
                    return 0;
                }
                if (left.getCreatedAt() == null) {
                    return 1;
                }
                if (right.getCreatedAt() == null) {
                    return -1;
                }
                return right.getCreatedAt().compareTo(left.getCreatedAt());
            }
        });
        int resolvedPageNo = normalizeListPageNo(pageNo);
        int resolvedPageSize = normalizeListPageSize(pageSize);
        int totalCount = result.size();
        int start = Math.min(totalCount, (resolvedPageNo - 1) * resolvedPageSize);
        int end = Math.min(totalCount, start + resolvedPageSize);
        List<ReportBatchStatusResponse> pageItems =
            new ArrayList<ReportBatchStatusResponse>(result.subList(start, end));
        return new BatchPageResponse<ReportBatchStatusResponse>(
            pageItems,
            Integer.valueOf(resolvedPageNo),
            Integer.valueOf(resolvedPageSize),
            Integer.valueOf(totalCount),
            Integer.valueOf(pageCount(totalCount, resolvedPageSize)),
            Boolean.valueOf(end < totalCount)
        );
    }

    private void runReportBatchStructureResolution(String batchId, ContextValue contextValue) {
        RequestContext.restore(contextValue);
        try {
            ReportBatch batch = requireBatch(batchId);
            List<ReportBatchItem> items = reportBatchItemRepository.findByBatchId(batch.getBatchId());
            List<ReportBatchItem> resolvedItems = resolveItemsConcurrently(batch, items, contextValue);
            Instant completedAt = Instant.now();
            recalculate(batch, resolvedItems, completedAt);
            reportBatchRepository.save(batch);
            LOGGER.info(
                "operation=REPORT_BATCH_STRUCTURE_PARSE batchId={} tenantId={} totalSqls={} status={}",
                batch.getBatchId(),
                batch.getTenantId(),
                Integer.valueOf(resolvedItems.size()),
                batch.getStatus()
            );
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "operation=REPORT_BATCH_STRUCTURE_PARSE batchId={} tenantId={} status=FAILED reason={}",
                batchId,
                RequestContext.getTenantId(),
                ex.getMessage()
            );
            markBatchFailed(batchId);
        } finally {
            runningReportBatches.remove(batchId);
            RequestContext.clear();
        }
    }

    private List<ReportBatchItem> resolveItemsConcurrently(ReportBatch batch,
                                                           List<ReportBatchItem> items,
                                                           ContextValue contextValue) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        int concurrency = resolveParseConcurrency();
        ExecutorService itemExecutor = Executors.newFixedThreadPool(
            concurrency,
            namedThreadFactory("report-batch-item-parser")
        );
        CompletionService<ReportBatchItem> completionService =
            new ExecutorCompletionService<ReportBatchItem>(itemExecutor);
        for (ReportBatchItem item : items) {
            completionService.submit(new Callable<ReportBatchItem>() {
                @Override
                public ReportBatchItem call() {
                    RequestContext.restore(contextValue);
                    try {
                        return resolveReportBatchItemComprehensive(batch, item, Instant.now());
                    } finally {
                        RequestContext.clear();
                    }
                }
            });
        }

        List<ReportBatchItem> resolvedItems = new ArrayList<ReportBatchItem>(items.size());
        List<ReportBatchItem> pendingPersist = new ArrayList<ReportBatchItem>(resolvePersistChunkSize());
        try {
            for (int index = 0; index < items.size(); index++) {
                try {
                    Future<ReportBatchItem> future = completionService.take();
                    ReportBatchItem resolvedItem = future.get();
                    resolvedItems.add(resolvedItem);
                    pendingPersist.add(resolvedItem);
                    if (pendingPersist.size() >= resolvePersistChunkSize()) {
                        reportBatchItemRepository.saveAll(new ArrayList<ReportBatchItem>(pendingPersist));
                        pendingPersist.clear();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Report batch structure parse was interrupted", ex);
                } catch (Exception ex) {
                    throw new IllegalStateException("Report batch structure parse failed", ex);
                }
            }
            if (!pendingPersist.isEmpty()) {
                reportBatchItemRepository.saveAll(pendingPersist);
            }
        } finally {
            itemExecutor.shutdownNow();
        }
        resolvedItems.sort(Comparator.comparingInt(ReportBatchItem::getSequenceNumber));
        return resolvedItems;
    }

    private ReportBatchItem resolveReportBatchItemComprehensive(ReportBatch batch,
                                                                ReportBatchItem item,
                                                                Instant now) {
        String resolvedSqlText = null;
        try {
            resolvedSqlText = resolveSqlText(batch, item);
            Map<String, Object> commentContext = buildCommentContext(batch, item);
            StructureParseRequest structureRequest = buildStructureRequest(resolvedSqlText, batch, item, commentContext);
            structureRequest.setHistoryWriteEnabled(Boolean.FALSE);
            StructureParseResponseVO structureParse = structureParseApplicationService.parse(structureRequest);
            List<String> issueScenes = extractIssueScenes(structureParse.getIssues());
            List<String> logicalObjectKeys = extractLogicalObjectKeys(structureParse.getLogicalObjectHits());
            if (!"VALID".equals(structureParse.getSyntaxStatus())) {
                ReportBatchItem.Status status = resolveStructureOnlyStatus(structureParse);
                structureParseApplicationService.writeParseHistoryWithAccess(
                    structureParse,
                    null,
                    structureRequest,
                    resolveHistoryResultStatus(status),
                    SqlParseHistoryApplicationService.SOURCE_REPORT_BATCH,
                    item.getItemId(),
                    batch.getBatchId()
                );
                item.complete(
                    resolvedSqlText,
                    structureParse.getParseTaskId(),
                    structureParse.getSyntaxStatus(),
                    "SKIPPED",
                    "SKIPPED",
                    status,
                    resolveFailureReason(structureParse, null),
                    issueScenes,
                    logicalObjectKeys,
                    now
                );
                recordItemPlanAnalysis(item, structureParse, now);
                item.recordHistory(
                    structureParse.getHistoryId(),
                    structureParse.getHistoryPersisted(),
                    structureParse.getHistoryPersistenceStatus(),
                    now
                );
                return item;
            }

            AccessParseResponseVO accessParse = parseAccessIfPossible(
                resolvedSqlText,
                batch,
                item,
                commentContext,
                structureParse.getParseTaskId()
            );
            ReportBatchItem.Status status = resolveStatus(structureParse, accessParse);
            structureParseApplicationService.writeParseHistoryWithAccess(
                structureParse,
                accessParse,
                structureRequest,
                resolveHistoryResultStatus(status),
                SqlParseHistoryApplicationService.SOURCE_REPORT_BATCH,
                item.getItemId(),
                batch.getBatchId()
            );
            item.complete(
                resolvedSqlText,
                structureParse.getParseTaskId(),
                structureParse.getSyntaxStatus(),
                accessParse == null ? "SKIPPED" : accessParse.getServiceStatus(),
                accessParse == null ? "SKIPPED" : accessParse.getConnectionStatus(),
                status,
                resolveFailureReason(structureParse, accessParse),
                issueScenes,
                logicalObjectKeys,
                now
            );
            recordItemPlanAnalysis(item, structureParse, now);
            item.recordHistory(
                structureParse.getHistoryId(),
                structureParse.getHistoryPersisted(),
                structureParse.getHistoryPersistenceStatus(),
                now
            );
            return item;
        } catch (RuntimeException ex) {
            item.complete(
                firstNonBlank(resolvedSqlText, item.getSqlText()),
                null,
                "FAILED",
                "SKIPPED",
                "SKIPPED",
                ReportBatchItem.Status.FAILED,
                SqlParseDiagnosticSupport.compactDiagnosticText("REPORT_BATCH_STRUCTURE_PARSE_FAILED: " + ex.getMessage(), FAILURE_REASON_LIMIT),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                now
            );
            item.recordHistory(null, Boolean.FALSE, "WRITE_SKIPPED", now);
            return item;
        }
    }

    private void markBatchFailed(String batchId) {
        try {
            ReportBatch batch = reportBatchRepository.findByBatchId(batchId);
            if (batch == null) {
                return;
            }
            List<ReportBatchItem> items = reportBatchItemRepository.findByBatchId(batchId);
            batch.applySummary(
                countDistinctReports(items),
                countFullyResolvedReports(items),
                countReportsWithNonResolvedSql(items),
                ReportBatch.ParseStatus.FAILED,
                Instant.now(),
                "REPORT_SQL_RESOLUTION_FAILED"
            );
            reportBatchRepository.save(batch);
        } catch (RuntimeException ex) {
            LOGGER.warn("operation=REPORT_BATCH_STRUCTURE_PARSE_MARK_FAILED batchId={} status=DEGRADED reason={}", batchId, ex.getMessage());
        }
    }

    private int resolveParseConcurrency() {
        if (reportBatchParseConcurrency <= 0) {
            return DEFAULT_PARSE_CONCURRENCY;
        }
        return Math.min(MAX_PARSE_CONCURRENCY, reportBatchParseConcurrency);
    }

    private int resolvePersistChunkSize() {
        return reportBatchPersistChunkSize <= 0 ? DEFAULT_PERSIST_CHUNK_SIZE : reportBatchPersistChunkSize;
    }

    @PreDestroy
    public void shutdownReportBatchExecutor() {
        reportBatchCoordinatorExecutor.shutdownNow();
    }

    private ReportBatchStatusResponse toResponse(ReportBatch batch, List<ReportBatchItem> items) {
        return toResponse(batch, items, true);
    }

    private ReportBatchStatusResponse toResponse(ReportBatch batch, List<ReportBatchItem> items, boolean includeItems) {
        return toResponse(batch, items, includeItems, PageSelection.from(null, null, null, ITEM_PREVIEW_LIMIT));
    }

    private ReportBatchStatusResponse toResponse(ReportBatch batch,
                                                 List<ReportBatchItem> items,
                                                 boolean includeItems,
                                                 PageSelection pageSelection) {
        List<ReportBatchItem> safeItems = items == null ? Collections.<ReportBatchItem>emptyList() : items;
        List<ReportBatchItem> filteredItems = includeItems
            ? filterItemsByReportCode(safeItems, pageSelection.reportCode)
            : Collections.<ReportBatchItem>emptyList();
        List<ReportBatchItem> pageItems = includeItems
            ? pageItems(filteredItems, pageSelection)
            : Collections.<ReportBatchItem>emptyList();
        ReportBatchStatusResponse response = new ReportBatchStatusResponse();
        response.setBatchId(batch.getBatchId());
        response.setTenantId(batch.getTenantId());
        response.setBatchName(batch.getBatchName());
        response.setFileType(batch.getFileType());
        response.setReportCodeField(batch.getReportCodeField());
        response.setDatasourceCode(batch.getDatasourceCode());
        response.setStage(batch.getStage());
        response.setPriority(batch.getPriority());
        response.setParserMode(batch.getParserMode());
        response.setSourceType(batch.getSourceType());
        response.setStatus(batch.getStatus().name());
        response.setTotalReports(Integer.valueOf(batch.getTotalReports()));
        response.setResolvedReports(Integer.valueOf(batch.getResolvedReports()));
        response.setFailedReports(Integer.valueOf(batch.getFailedReports()));
        response.setTotalSqls(Integer.valueOf(safeItems.isEmpty() ? batch.getTotalReports() : safeItems.size()));
        response.setResolvedSqls(Integer.valueOf(countSqlsByStatus(safeItems, ReportBatchItem.Status.RESOLVED)));
        response.setFailedSqls(Integer.valueOf(countNonResolvedSqls(safeItems)));
        response.setPlanAnalysisStatistics(buildPlanStatistics(safeItems));
        response.setItemPreviewLimit(Integer.valueOf(pageSelection.pageSize));
        response.setItemPreviewTruncated(Boolean.valueOf(includeItems && filteredItems.size() > pageItems.size()));
        response.setOmittedItemCount(Integer.valueOf(includeItems ? Math.max(0, filteredItems.size() - pageItems.size()) : 0));
        response.setItemPageNumber(Integer.valueOf(pageSelection.pageNumber));
        response.setItemPageSize(Integer.valueOf(pageSelection.pageSize));
        response.setItemPageCount(Integer.valueOf(pageCount(filteredItems.size(), pageSelection.pageSize)));
        response.setItemTotalCount(Integer.valueOf(includeItems ? filteredItems.size() : 0));
        response.setItemReportCodeFilter(pageSelection.reportCode);
        response.setParseStatistics(includeItems ? parseStatisticsAssembler.build(safeItems) : null);
        response.setReportItems(includeItems ? toItemVos(pageItems) : Collections.<ReportBatchItemVO>emptyList());
        response.setStatusHistory(toStatusHistory(batch.getStatusHistory()));
        response.setCreatedAt(batch.getCreatedAt());
        response.setUpdatedAt(batch.getUpdatedAt());
        return response;
    }

    private List<ReportBatchItem> filterItemsByReportCode(List<ReportBatchItem> items, String reportCode) {
        if (!StringUtils.hasText(reportCode)) {
            return items;
        }
        List<ReportBatchItem> result = new ArrayList<ReportBatchItem>();
        for (ReportBatchItem item : items) {
            if (reportCode.equals(item.getReportCode())) {
                result.add(item);
            }
        }
        return result;
    }

    private List<ReportBatchItem> pageItems(List<ReportBatchItem> items, PageSelection pageSelection) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        int start = Math.min(items.size(), (pageSelection.pageNumber - 1) * pageSelection.pageSize);
        int end = Math.min(items.size(), start + pageSelection.pageSize);
        return new ArrayList<ReportBatchItem>(items.subList(start, end));
    }

    private int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    private int normalizeListPageNo(Integer pageNo) {
        if (pageNo == null || pageNo.intValue() <= 0) {
            return 1;
        }
        return pageNo.intValue();
    }

    private int normalizeListPageSize(Integer pageSize) {
        if (pageSize == null || pageSize.intValue() <= 0) {
            return DEFAULT_LIST_PAGE_SIZE;
        }
        return Math.min(MAX_LIST_PAGE_SIZE, pageSize.intValue());
    }

    private void recalculate(ReportBatch batch, List<ReportBatchItem> items, Instant now) {
        int failedSqls = 0;
        for (ReportBatchItem item : items) {
            if (item.getStatus() != ReportBatchItem.Status.RESOLVED) {
                failedSqls++;
            }
        }
        ReportBatch.ParseStatus terminalStatus = failedSqls > 0
            ? ReportBatch.ParseStatus.PARTIAL_COMPLETED
            : ReportBatch.ParseStatus.COMPLETED;
        batch.applySummary(
            countDistinctReports(items),
            countFullyResolvedReports(items),
            countReportsWithNonResolvedSql(items),
            terminalStatus,
            now,
            "REPORT_SQL_RESOLUTION_COMPLETED"
        );
    }

    private ReportBatchItem.Status resolveStatus(StructureParseResponseVO structureParse, AccessParseResponseVO accessParse) {
        if ("FAILED".equals(structureParse.getAnalysisStatus())) {
            return ReportBatchItem.Status.FAILED;
        }
        if ("SUCCESS".equals(structureParse.getAnalysisStatus())
            && (accessParse == null
                || ("AVAILABLE".equals(accessParse.getServiceStatus()) && "CONNECTED".equals(accessParse.getConnectionStatus())))) {
            return ReportBatchItem.Status.RESOLVED;
        }
        return ReportBatchItem.Status.PARTIAL_RESOLVED;
    }

    private ReportBatchItem.Status resolveStructureOnlyStatus(StructureParseResponseVO structureParse) {
        if ("SUCCESS".equals(structureParse.getAnalysisStatus())) {
            return ReportBatchItem.Status.RESOLVED;
        }
        if ("PARTIAL_SUCCESS".equals(structureParse.getAnalysisStatus())) {
            return ReportBatchItem.Status.PARTIAL_RESOLVED;
        }
        return ReportBatchItem.Status.FAILED;
    }

    private String resolveFailureReason(StructureParseResponseVO structureParse, AccessParseResponseVO accessParse) {
        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            return buildStructureFailureReason(structureParse);
        }
        if (structureParse.getPlanAnalysis() != null && "FAILED".equals(structureParse.getPlanAnalysis().getStatus())) {
            return SqlParseDiagnosticSupport.compactDiagnosticText(
                structureParse.getPlanAnalysis().getFailureReason(),
                FAILURE_REASON_LIMIT
            );
        }
        if (accessParse == null) {
            return null;
        }
        return "AVAILABLE".equals(accessParse.getServiceStatus()) && "CONNECTED".equals(accessParse.getConnectionStatus())
            ? null
            : accessParse.getDegradeReason();
    }

    private void recordItemPlanAnalysis(ReportBatchItem item, StructureParseResponseVO structureParse, Instant now) {
        item.recordPlanAnalysis(
            structureParse.getPlanAnalysis() == null ? null : structureParse.getPlanAnalysis().getStatus(),
            structureParse.getAnalysisStatus(),
            structureParse.getPlanAnalysis() == null ? null : JsonUtils.toJson(structureParse.getPlanAnalysis()),
            now
        );
    }

    private String buildStructureFailureReason(StructureParseResponseVO structureParse) {
        return SqlParseDiagnosticSupport.buildStructureFailureReason(structureParse, FAILURE_REASON_LIMIT);
    }

    private String resolveHistoryResultStatus(ReportBatchItem.Status status) {
        if (status == ReportBatchItem.Status.RESOLVED) {
            return "SUCCESS";
        }
        if (status == ReportBatchItem.Status.PARTIAL_RESOLVED) {
            return "PARTIAL";
        }
        return "FAILED";
    }

    private String resolveSqlText(ReportBatch batch, ReportBatchItem item) {
        String inlineSql = trimToNull(item.getSqlText());
        if (StringUtils.hasText(inlineSql)) {
            return extractReportImportSql(inlineSql);
        }
        ReportSqlResolveResult resolvedSql = reportSqlResolver.resolve(buildReportSqlResolveRequest(batch, item));
        return extractReportImportSql(resolvedSql.getSqlText());
    }

    private StructureParseRequest buildStructureRequest(String sqlText,
                                                        ReportBatch batch,
                                                        ReportBatchItem item,
                                                        Map<String, Object> commentContext) {
        StructureParseRequest request = new StructureParseRequest();
        request.setSqlText(sqlText);
        request.setDatasourceCode(firstNonBlank(item.getDatasourceCode(), batch.getDatasourceCode()));
        request.setParserMode(batch.getParserMode());
        request.setCommentContext(commentContext);
        return request;
    }

    private AccessParseResponseVO parseAccessIfPossible(String sqlText,
                                                        ReportBatch batch,
                                                        ReportBatchItem item,
                                                        Map<String, Object> commentContext,
                                                        String parseTaskId) {
        AccessParseRequest request = new AccessParseRequest();
        request.setSqlText(sqlText);
        request.setDatasourceCode(firstNonBlank(item.getDatasourceCode(), batch.getDatasourceCode()));
        request.setCommentContext(commentContext);
        request.setConnectionRequired(Boolean.TRUE);
        try {
            return accessParseApplicationService.parseAccess(request, parseTaskId);
        } catch (RuntimeException ex) {
            return buildAccessParseFailure(parseTaskId, ex);
        }
    }

    private AccessParseResponseVO buildAccessParseFailure(String parseTaskId, RuntimeException ex) {
        AccessParseResponseVO response = new AccessParseResponseVO();
        response.setParseTaskId(parseTaskId);
        response.setServiceStatus("FAILED");
        response.setConnectionStatus("FAILED");
        response.setObjectResolutionStatus("UNAVAILABLE");
        response.setCompatibilityStatus("UNKNOWN");
        response.setAvailabilityWarning("Access parse failed after structure parse succeeded.");
        response.setDegradeReason(SqlParseDiagnosticSupport.compactDiagnosticText(
            "ACCESS_PARSE_FAILED: " + (ex == null ? "unknown" : ex.getMessage()),
            FAILURE_REASON_LIMIT
        ));
        return response;
    }

    private boolean containsInlineSql(List<ReportSourceRow> rows) {
        if (rows == null) {
            return false;
        }
        for (ReportSourceRow row : rows) {
            if (row != null && StringUtils.hasText(row.sqlText)) {
                return true;
            }
        }
        return false;
    }

    private int countDistinctReports(List<ReportBatchItem> items) {
        Set<String> reports = new LinkedHashSet<String>();
        if (items != null) {
            for (ReportBatchItem item : items) {
                reports.add(firstNonBlank(item.getReportCode(), item.getItemId(), "UNSPECIFIED"));
            }
        }
        return reports.size();
    }

    private int countFullyResolvedReports(List<ReportBatchItem> items) {
        Map<String, Boolean> reportResolved = new LinkedHashMap<String, Boolean>();
        if (items != null) {
            for (ReportBatchItem item : items) {
                String reportCode = firstNonBlank(item.getReportCode(), item.getItemId(), "UNSPECIFIED");
                Boolean current = reportResolved.get(reportCode);
                boolean resolved = item.getStatus() == ReportBatchItem.Status.RESOLVED;
                reportResolved.put(reportCode, Boolean.valueOf(current == null ? resolved : current.booleanValue() && resolved));
            }
        }
        int count = 0;
        for (Boolean resolved : reportResolved.values()) {
            if (Boolean.TRUE.equals(resolved)) {
                count++;
            }
        }
        return count;
    }

    private int countReportsWithNonResolvedSql(List<ReportBatchItem> items) {
        return countDistinctReports(items) - countFullyResolvedReports(items);
    }

    private int countSqlsByStatus(List<ReportBatchItem> items, ReportBatchItem.Status status) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ReportBatchItem item : items) {
            if (item.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private int countNonResolvedSqls(List<ReportBatchItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ReportBatchItem item : items) {
            if (item.getStatus() == ReportBatchItem.Status.FAILED
                || item.getStatus() == ReportBatchItem.Status.PARTIAL_RESOLVED) {
                count++;
            }
        }
        return count;
    }

    private ParseBatchStageStatisticsVO buildPlanStatistics(List<ReportBatchItem> items) {
        int success = 0;
        int partial = 0;
        int failed = 0;
        List<ReportBatchItem> safeItems = items == null ? Collections.<ReportBatchItem>emptyList() : items;
        for (ReportBatchItem item : safeItems) {
            if ("SUCCESS".equals(item.getPlanAnalysisStatus())) {
                success++;
            } else if ("FAILED".equals(item.getPlanAnalysisStatus())) {
                failed++;
            } else {
                partial++;
            }
        }
        ParseBatchStageStatisticsVO vo = new ParseBatchStageStatisticsVO();
        vo.setSuccessRecords(Integer.valueOf(success));
        vo.setPartialSuccessRecords(Integer.valueOf(partial));
        vo.setFailedRecords(Integer.valueOf(failed));
        vo.setSuccessRate(successRate(success, safeItems.size()));
        return vo;
    }

    private List<ReportBatchItem> buildItems(ReportBatch batch, List<ReportSourceRow> rows) {
        List<ReportBatchItem> items = new ArrayList<ReportBatchItem>(rows.size());
        Map<String, Integer> reportSqlOrdinals = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < rows.size(); i++) {
            ReportSourceRow row = rows.get(i);
            String reportKey = firstNonBlank(row.reportCode, "UNSPECIFIED");
            int ordinal = reportSqlOrdinals.getOrDefault(reportKey, Integer.valueOf(0)).intValue() + 1;
            reportSqlOrdinals.put(reportKey, Integer.valueOf(ordinal));
            ReportBatchItem item = ReportBatchItem.create(
                UUID.randomUUID().toString(),
                batch.getBatchId(),
                i + 1,
                row.reportCode,
                row.reportName,
                firstNonBlank(row.datasourceCode, batch.getDatasourceCode()),
                firstNonBlank(row.stage, batch.getStage()),
                firstNonBlank(row.priority, batch.getPriority()),
                row.rawLine,
                row.sqlColumnName,
                row.sqlOrdinalInReport == null ? Integer.valueOf(ordinal) : row.sqlOrdinalInReport,
                batch.getCreatedAt()
            );
            if (StringUtils.hasText(row.sqlText)) {
                item.complete(
                    row.sqlText,
                    null,
                    null,
                    null,
                    null,
                    ReportBatchItem.Status.PENDING,
                    null,
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    batch.getCreatedAt()
                );
            }
            items.add(item);
        }
        return items;
    }

    private List<ReportSourceRow> parseSourceRows(byte[] content, String charsetName, String fileType, String reportCodeField) {
        if (content == null || content.length == 0) {
            return Collections.emptyList();
        }
        String normalizedFileType = normalizeFileType(fileType);
        if (isWorkbookFileType(normalizedFileType)) {
            return parseWorkbookSource(content, reportCodeField);
        }
        String text = new String(content, resolveCharset(charsetName));
        String normalized = text.replace("\r\n", "\n").trim();
        if (!StringUtils.hasText(normalized)) {
            return Collections.emptyList();
        }
        if ("CSV".equals(normalizedFileType) || looksLikeCsv(normalized)) {
            return parseCsvSource(normalized, reportCodeField);
        }
        List<ReportSourceRow> rows = new ArrayList<ReportSourceRow>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String trimmed = trimToNull(line);
                if (!StringUtils.hasText(trimmed)) {
                    continue;
                }
                ReportSourceRow row = new ReportSourceRow();
                row.rawLine = trimmed;
                String[] parts = trimmed.split("\\|");
                row.reportCode = parts[0].trim();
                row.reportName = parts.length > 1 ? trimToNull(parts[1]) : null;
                row.datasourceCode = parts.length > 2 ? trimToNull(parts[2]) : null;
                row.stage = parts.length > 3 ? trimToNull(parts[3]) : null;
                row.priority = parts.length > 4 ? trimToNull(parts[4]) : null;
                rows.add(row);
            }
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse report catalog payload: " + ex.getMessage());
        }
        return rows;
    }

    private List<ReportSourceRow> parseWorkbookSource(byte[] content, String reportCodeField) {
        try {
            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content));
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                return Collections.emptyList();
            }
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return Collections.emptyList();
            }
            boolean hasHeader = isWorkbookHeaderRow(headerRow, formatter, reportCodeField);
            List<String> headers = hasHeader
                ? readWorkbookRowValues(headerRow, formatter, Math.max(0, headerRow.getLastCellNum()))
                : Collections.<String>emptyList();
            List<ReportSourceRow> rows = new ArrayList<ReportSourceRow>();
            int firstDataRow = hasHeader ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum();
            for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int cellCount = Math.max(headers.size(), Math.max(0, row.getLastCellNum()));
                List<String> effectiveHeaders = effectiveHeaders(headers, cellCount);
                List<String> values = readWorkbookRowValues(row, formatter, cellCount);
                if (hasAnyText(values)) {
                    rows.addAll(expandReportRows(effectiveHeaders, values, values.toString()));
                }
            }
            workbook.close();
            return rows;
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse workbook report payload: " + ex.getMessage());
        }
    }

    private boolean looksLikeCsv(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String firstLine = text.split("\n", 2)[0].toLowerCase(Locale.ROOT);
        return firstLine.contains(",")
            && (firstLine.contains("report_code")
                || firstLine.contains("reportcode")
                || firstLine.contains("报表代码")
                || firstLine.contains("报表编码"));
    }

    private List<ReportSourceRow> parseCsvSource(String text, String reportCodeField) {
        try {
            CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .build()
                .parse(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return Collections.emptyList();
            }
            boolean hasHeader = isCsvHeaderRecord(records.get(0), reportCodeField);
            int cellCount = maxCsvRecordSize(records);
            List<String> headers = hasHeader
                ? effectiveHeaders(csvRecordValues(records.get(0), cellCount), cellCount)
                : effectiveHeaders(Collections.<String>emptyList(), cellCount);
            List<ReportSourceRow> rows = new ArrayList<ReportSourceRow>();
            int startIndex = hasHeader ? 1 : 0;
            for (int recordIndex = startIndex; recordIndex < records.size(); recordIndex++) {
                CSVRecord record = records.get(recordIndex);
                List<String> values = csvRecordValues(record, cellCount);
                if (hasAnyText(values)) {
                    rows.addAll(expandReportRows(headers, values, values.toString()));
                }
            }
            return rows;
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse report catalog CSV payload: " + ex.getMessage());
        }
    }

    private String resolveFileType(ReportBatchImportRequest request, byte[] content) {
        String explicit = normalizeFileType(request == null ? null : request.getFileType());
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        String fileName = trimToNull(request == null ? null : request.getFileName());
        if (StringUtils.hasText(fileName)) {
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".xlsx")) {
                return "XLSX";
            }
            if (lower.endsWith(".xls")) {
                return "XLS";
            }
            if (lower.endsWith(".et")) {
                return "ET";
            }
            if (lower.endsWith(".csv")) {
                return "CSV";
            }
            if (lower.endsWith(".txt")) {
                return "TXT";
            }
        }
        if (looksBinaryWorkbook(content)) {
            return "XLSX";
        }
        String text = new String(content, resolveCharset(request == null ? null : request.getCharset()));
        return looksLikeCsv(text) ? "CSV" : "TXT";
    }

    private boolean looksBinaryWorkbook(byte[] content) {
        if (content == null || content.length < 4) {
            return false;
        }
        return (content[0] == 'P' && content[1] == 'K') || (content[0] == (byte) 0xD0 && content[1] == (byte) 0xCF);
    }

    private boolean isWorkbookFileType(String fileType) {
        return "XLSX".equals(fileType) || "XLS".equals(fileType) || "ET".equals(fileType);
    }

    private String normalizeFileType(String fileType) {
        String normalized = trimToNull(fileType);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeHeader(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeHeaderKey(String value) {
        String normalized = normalizeHeader(value);
        return normalized == null ? "" : normalized.replace("_", "").replace("-", "").replace(" ", "");
    }

    private boolean isReportCodeHeader(String header, String reportCodeField) {
        String key = normalizeHeaderKey(header);
        String expected = normalizeHeaderKey(reportCodeField);
        return "reportcode".equals(key)
            || "报表代码".equals(key)
            || "报表编码".equals(key)
            || (StringUtils.hasText(expected) && expected.equals(key));
    }

    private boolean isCsvHeaderRecord(CSVRecord record, String reportCodeField) {
        return record != null && record.size() > 0 && isReportCodeHeader(record.get(0), reportCodeField);
    }

    private boolean isWorkbookHeaderRow(Row row, DataFormatter formatter, String reportCodeField) {
        if (row == null) {
            return false;
        }
        Cell firstCell = row.getCell(0);
        return isReportCodeHeader(formatter.formatCellValue(firstCell), reportCodeField);
    }

    private List<String> readWorkbookRowValues(Row row, DataFormatter formatter, int cellCount) {
        List<String> values = new ArrayList<String>(cellCount);
        for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            values.add(trimToNull(formatter.formatCellValue(row.getCell(cellIndex))));
        }
        return values;
    }

    private int maxCsvRecordSize(List<CSVRecord> records) {
        int max = 0;
        for (CSVRecord record : records) {
            max = Math.max(max, record == null ? 0 : record.size());
        }
        return max;
    }

    private List<String> csvRecordValues(CSVRecord record, int cellCount) {
        List<String> values = new ArrayList<String>(cellCount);
        for (int index = 0; index < cellCount; index++) {
            values.add(record != null && index < record.size() ? trimToNull(record.get(index)) : null);
        }
        return values;
    }

    private List<String> effectiveHeaders(List<String> rawHeaders, int cellCount) {
        List<String> headers = new ArrayList<String>(cellCount);
        for (int index = 0; index < cellCount; index++) {
            String header = index < rawHeaders.size() ? trimToNull(rawHeaders.get(index)) : null;
            if (StringUtils.hasText(header)) {
                headers.add(normalizeHeader(header));
            } else if (index == 0) {
                headers.add("report_code");
            } else {
                headers.add("sql_" + index);
            }
        }
        return headers;
    }

    private boolean hasAnyText(List<String> values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return true;
            }
        }
        return false;
    }

    private String valueAt(List<String> values, int index) {
        return values != null && index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private String headerAt(List<String> headers, int index) {
        String header = headers != null && index >= 0 && index < headers.size() ? trimToNull(headers.get(index)) : null;
        if (StringUtils.hasText(header)) {
            return header;
        }
        return index == 0 ? "report_code" : "sql_" + index;
    }

    private String resolveMockBizDate() {
        return java.time.LocalDate.now().toString();
    }

    private Map<String, Object> buildCommentContext(ReportBatch batch, ReportBatchItem item) {
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        putIfPresent(context, "report_code", item.getReportCode());
        putIfPresent(context, "stage", firstNonBlank(item.getStage(), batch.getStage()));
        putIfPresent(context, "biz_date", resolveMockBizDate());
        putIfPresent(context, "tenant_id", batch.getTenantId());
        putIfPresent(context, "datasource", firstNonBlank(item.getDatasourceCode(), batch.getDatasourceCode()));
        putIfPresent(context, "priority", firstNonBlank(item.getPriority(), batch.getPriority()));
        putIfPresent(context, "sql_column_name", item.getSqlColumnName());
        if (item.getSqlOrdinalInReport() != null) {
            context.put("sql_ordinal_in_report", item.getSqlOrdinalInReport());
        }
        return context;
    }

    private ReportSqlResolveRequest buildReportSqlResolveRequest(ReportBatch batch, ReportBatchItem item) {
        return new ReportSqlResolveRequest(
            batch.getTenantId(),
            firstNonBlank(item.getDatasourceCode(), batch.getDatasourceCode()),
            firstNonBlank(item.getStage(), batch.getStage()),
            firstNonBlank(item.getPriority(), batch.getPriority()),
            item.getReportCode()
        );
    }

    private List<String> extractIssueScenes(List<StructureParseIssueVO> issues) {
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> scenes = new LinkedHashSet<String>();
        for (StructureParseIssueVO issue : issues) {
            if (issue == null) {
                continue;
            }
            if (StringUtils.hasText(issue.getIssueCode())) {
                scenes.add(issue.getIssueCode());
            } else if (StringUtils.hasText(issue.getIssueScene())) {
                scenes.add(issue.getIssueScene());
            }
        }
        return new ArrayList<String>(scenes);
    }

    private List<String> extractLogicalObjectKeys(List<LogicalObjectSurface> logicalObjectHits) {
        if (logicalObjectHits == null || logicalObjectHits.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> keys = new LinkedHashSet<String>();
        for (LogicalObjectSurface logicalObjectHit : logicalObjectHits) {
            if (logicalObjectHit == null) {
                continue;
            }
            if (logicalObjectHit.getMappedPhysicalTargets() != null) {
                for (String target : logicalObjectHit.getMappedPhysicalTargets()) {
                    addTableKey(keys, target);
                }
            }
            addTableKey(keys, logicalObjectHit.getObjectKey());
        }
        if (!keys.isEmpty()) {
            return new ArrayList<String>(keys);
        }
        for (LogicalObjectSurface logicalObjectHit : logicalObjectHits) {
            if (logicalObjectHit == null) {
                continue;
            }
            String objectKey = trimToNull(logicalObjectHit.getObjectKey());
            if (!StringUtils.hasText(objectKey)) {
                objectKey = trimToNull(logicalObjectHit.getObjectName());
            }
            if (StringUtils.hasText(objectKey)) {
                keys.add(objectKey);
            }
        }
        return new ArrayList<String>(keys);
    }

    private void addTableKey(Set<String> keys, String candidate) {
        String normalized = trimToNull(candidate);
        if (normalized != null && normalized.toUpperCase(Locale.ROOT).startsWith("TABLE:")) {
            keys.add(normalized);
        }
    }

    private List<ReportBatchItemVO> toItemVos(List<ReportBatchItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReportBatchItemVO> vos = new ArrayList<ReportBatchItemVO>(items.size());
        for (ReportBatchItem item : items) {
            ReportBatchItemVO vo = new ReportBatchItemVO();
            vo.setItemId(item.getItemId());
            vo.setSequenceNumber(Integer.valueOf(item.getSequenceNumber()));
            vo.setReportCode(item.getReportCode());
            vo.setReportName(item.getReportName());
            vo.setDatasourceCode(item.getDatasourceCode());
            vo.setStage(item.getStage());
            vo.setPriority(item.getPriority());
            vo.setSourceFileLine(item.getSourceFileLine());
            vo.setSqlColumnName(item.getSqlColumnName());
            vo.setSqlOrdinalInReport(item.getSqlOrdinalInReport());
            vo.setSqlText(item.getSqlText());
            vo.setParseTaskId(item.getParseTaskId());
            vo.setStructureSyntaxStatus(item.getStructureSyntaxStatus());
            vo.setAccessServiceStatus(item.getAccessServiceStatus());
            vo.setAccessConnectionStatus(item.getAccessConnectionStatus());
            vo.setPlanAnalysisStatus(item.getPlanAnalysisStatus());
            vo.setAnalysisStatus(item.getCombinedAnalysisStatus());
            vo.setPlanAnalysis(parseJsonValue(item.getPlanAnalysisJson()));
            vo.setFailureReason(item.getFailureReason());
            vo.setHistoryId(item.getHistoryId());
            vo.setHistoryPersisted(item.getHistoryPersisted());
            vo.setHistoryPersistenceStatus(item.getHistoryPersistenceStatus());
            SqlParseDiagnosticSupport.Diagnostic diagnostic =
                SqlParseDiagnosticSupport.fromFailureReason(item.getFailureReason());
            vo.setFailureLine(diagnostic.getFailureLine());
            vo.setFailureColumn(diagnostic.getFailureColumn());
            vo.setFailureOffset(diagnostic.getFailureOffset());
            vo.setFailureToken(diagnostic.getFailureToken());
            vo.setFailureSnippet(diagnostic.getFailureSnippet());
            vo.setDiagnosticSummary(buildReportItemDiagnosticSummary(item, diagnostic));
            vo.setStatus(item.getStatus() == null ? null : item.getStatus().name());
            vo.setIssueScenes(new ArrayList<String>(item.getIssueScenes()));
            vo.setIssueLocations(ReportBatchIssueLocationSupport.fromItem(item, diagnostic));
            vo.setLogicalObjectKeys(new ArrayList<String>(item.getLogicalObjectKeys()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setUpdatedAt(item.getUpdatedAt());
            vos.add(vo);
        }
        return vos;
    }

    private Double successRate(int success, int total) {
        if (total <= 0) {
            return Double.valueOf(0.0D);
        }
        return Double.valueOf(((double) success) / ((double) total));
    }

    private Object parseJsonValue(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtils.objectMapper().readValue(json, Object.class);
        } catch (Exception ex) {
            return json;
        }
    }

    private String buildReportItemDiagnosticSummary(ReportBatchItem item,
                                                    SqlParseDiagnosticSupport.Diagnostic diagnostic) {
        if (item == null
            || (!StringUtils.hasText(item.getFailureReason())
                && item.getIssueScenes().isEmpty()
                && item.getStatus() == ReportBatchItem.Status.RESOLVED)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendDiagnosticPart(builder, "sequence", String.valueOf(item.getSequenceNumber()));
        appendDiagnosticPart(builder, "report", firstNonBlank(item.getReportCode(), "UNSPECIFIED"));
        appendDiagnosticPart(builder, "sqlColumn", item.getSqlColumnName());
        appendDiagnosticPart(builder, "sqlOrdinal", item.getSqlOrdinalInReport());
        appendDiagnosticPart(builder, "datasource", item.getDatasourceCode());
        appendDiagnosticPart(builder, "parseTask", item.getParseTaskId());
        appendDiagnosticPart(builder, "status", item.getStatus() == null ? null : item.getStatus().name());
        appendDiagnosticPart(builder, "line", diagnostic.getFailureLine());
        appendDiagnosticPart(builder, "col", diagnostic.getFailureColumn());
        appendDiagnosticPart(builder, "offset", diagnostic.getFailureOffset());
        appendDiagnosticPart(builder, "token", diagnostic.getFailureToken());
        appendDiagnosticPart(builder, "near", diagnostic.getFailureSnippet());
        if (!item.getIssueScenes().isEmpty()) {
            appendDiagnosticPart(builder, "issues", String.join(",", item.getIssueScenes()));
        }
        return SqlParseDiagnosticSupport.compactDiagnosticText(builder.toString(), 260);
    }

    private void appendDiagnosticPart(StringBuilder builder, String label, Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(label).append('=').append(value);
    }

    private List<ReportBatchStatusHistoryVO> toStatusHistory(List<ReportBatchStatusTransition> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReportBatchStatusHistoryVO> items = new ArrayList<ReportBatchStatusHistoryVO>(history.size());
        for (ReportBatchStatusTransition transition : history) {
            ReportBatchStatusHistoryVO vo = new ReportBatchStatusHistoryVO();
            vo.setPreviousStatus(transition.getPreviousStatus() == null ? null : transition.getPreviousStatus().name());
            vo.setCurrentStatus(transition.getCurrentStatus() == null ? null : transition.getCurrentStatus().name());
            vo.setOccurredAt(transition.getOccurredAt());
            vo.setNote(transition.getNote());
            items.add(vo);
        }
        return items;
    }

    private ReportBatch requireBatch(String batchId) {
        ReportBatch batch = reportBatchRepository.findByBatchId(requireText(batchId, "batchId"));
        if (batch == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Report batch does not exist for batchId=" + batchId
            );
        }
        verifyTenantAccess(batch.getTenantId());
        return batch;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context");
        }
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context");
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this report batch");
        }
    }

    private void putIfPresent(Map<String, Object> context, String key, String value) {
        if (StringUtils.hasText(value)) {
            context.put(key, value);
        }
    }

    private Charset resolveCharset(String charsetName) {
        if (!StringUtils.hasText(charsetName)) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(charsetName.trim());
    }

    private byte[] decodeBase64(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalidArgument("contentBase64", "contentBase64 is required");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw invalidArgument("contentBase64", "contentBase64 must be valid Base64");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw invalidArgument(fieldName, fieldName + " is required");
        }
        return normalized;
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, message + " [" + fieldName + "]");
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, prefix + "-" + THREAD_SEQUENCE.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    private String extractReportImportSql(String value) {
        String sqlText = trimToNull(value);
        if (!StringUtils.hasText(sqlText) || !sqlText.startsWith("--")) {
            return sqlText;
        }
        int firstLineEnd = firstLineEnd(sqlText);
        int searchEnd = firstLineEnd < 0 ? sqlText.length() : firstLineEnd;
        int sqlStart = findReportSqlStart(sqlText, searchEnd);
        return sqlStart > 0 ? trimToNull(sqlText.substring(sqlStart)) : sqlText;
    }

    private int firstLineEnd(String value) {
        int newline = value.indexOf('\n');
        int carriageReturn = value.indexOf('\r');
        if (newline < 0) {
            return carriageReturn;
        }
        if (carriageReturn < 0) {
            return newline;
        }
        return Math.min(newline, carriageReturn);
    }

    private int findReportSqlStart(String sqlText, int searchEnd) {
        Matcher matcher = REPORT_SQL_START_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            if (matcher.start() >= searchEnd) {
                return -1;
            }
            return matcher.start();
        }
        return -1;
    }

    private List<ReportSourceRow> expandReportRows(List<String> headers, List<String> values, String rawLine) {
        ReportSourceRow base = new ReportSourceRow();
        base.rawLine = rawLine;
        base.reportCode = trimToNull(valueAt(values, 0));

        List<ReportSourceRow> rows = new ArrayList<ReportSourceRow>();
        int sqlOrdinal = 0;
        for (int index = 1; index < values.size(); index++) {
            String sqlText = trimToNull(valueAt(values, index));
            if (!StringUtils.hasText(sqlText)) {
                continue;
            }
            sqlOrdinal++;
            ReportSourceRow sqlRow = base.copy();
            String header = headerAt(headers, index);
            sqlRow.rawLine = rawLine + " column=" + header;
            sqlRow.sqlColumnName = header;
            sqlRow.sqlOrdinalInReport = Integer.valueOf(sqlOrdinal);
            sqlRow.sqlText = extractReportImportSql(sqlText);
            rows.add(sqlRow);
        }
        if (rows.isEmpty() && StringUtils.hasText(base.reportCode)) {
            rows.add(base);
        }
        return rows;
    }

    private static final class PageSelection {
        private static final int MAX_PAGE_SIZE = 500;

        private final int pageNumber;
        private final int pageSize;
        private final String reportCode;

        private PageSelection(int pageNumber, int pageSize, String reportCode) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.reportCode = reportCode;
        }

        private static PageSelection from(Integer pageNumber, Integer pageSize, String reportCode, int defaultPageSize) {
            int normalizedPageNumber = pageNumber == null ? 1 : Math.max(1, pageNumber.intValue());
            int normalizedPageSize = pageSize == null ? defaultPageSize : pageSize.intValue();
            normalizedPageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, normalizedPageSize));
            String normalizedReportCode = StringUtils.hasText(reportCode) ? reportCode.trim() : null;
            return new PageSelection(normalizedPageNumber, normalizedPageSize, normalizedReportCode);
        }
    }

    private static final class ReportSourceRow {
        String reportCode;
        String reportName;
        String datasourceCode;
        String stage;
        String priority;
        String rawLine;
        String sqlColumnName;
        Integer sqlOrdinalInReport;
        String sqlText;

        ReportSourceRow() {
        }

        ReportSourceRow copy() {
            ReportSourceRow row = new ReportSourceRow();
            row.reportCode = reportCode;
            row.reportName = reportName;
            row.datasourceCode = datasourceCode;
            row.stage = stage;
            row.priority = priority;
            row.rawLine = rawLine;
            return row;
        }
    }
}
