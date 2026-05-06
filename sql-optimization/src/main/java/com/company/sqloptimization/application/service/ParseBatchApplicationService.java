package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchIngestRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchRetryAccessRequest;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchIssueStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchItemVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchStageStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.application.controller.vo.ParseBatchTemplateColumnVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchFileType;
import com.company.sqloptimization.domain.batch.ParseBatchImportMode;
import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.ParseBatchItemStatus;
import com.company.sqloptimization.domain.batch.ParseBatchSourceType;
import com.company.sqloptimization.domain.batch.ParseBatchStatus;
import com.company.sqloptimization.domain.batch.ParseBatchStatusTransition;
import com.company.sqloptimization.domain.batch.repository.ParseBatchItemRepository;
import com.company.sqloptimization.domain.batch.repository.ParseBatchRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ParseBatchApplicationService {

    private static final int ITEM_PREVIEW_LIMIT = 500;
    private static final int FAILURE_PREVIEW_LIMIT = 200;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final String FAILURE_FILTER_ALL = "ALL";
    private static final String FAILURE_FILTER_UNAVAILABLE = "UNAVAILABLE";
    private static final String FAILURE_FILTER_FAILED = "FAILED";

    private final ParseBatchRepository parseBatchRepository;
    private final ParseBatchItemRepository parseBatchItemRepository;
    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;

    public ParseBatchApplicationService(ParseBatchRepository parseBatchRepository,
                                        ParseBatchItemRepository parseBatchItemRepository,
                                        StructureParseApplicationService structureParseApplicationService,
                                        AccessParseApplicationService accessParseApplicationService) {
        this.parseBatchRepository = parseBatchRepository;
        this.parseBatchItemRepository = parseBatchItemRepository;
        this.structureParseApplicationService = structureParseApplicationService;
        this.accessParseApplicationService = accessParseApplicationService;
    }

    public ParseBatchStatusResponse createBatch(ParseBatchCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        ParseBatchImportMode importMode = requireImportMode(request == null ? null : request.getImportMode());
        ParseBatchFileType fileType = requireFileType(request == null ? null : request.getFileType());
        validateFileTypeCompatibility(importMode, fileType);
        String batchName = requireText(request == null ? null : request.getBatchName(), "batchName");
        Instant now = Instant.now();
        ParseBatch batch = ParseBatch.initialize(
            UUID.randomUUID().toString(),
            tenantId,
            batchName,
            importMode,
            resolveSourceType(importMode),
            fileType,
            trimToNull(request == null ? null : request.getTemplateVersion()),
            trimToNull(request == null ? null : request.getDatasourceCode()),
            Boolean.TRUE.equals(request == null ? null : request.getStructureParseOnly()),
            RequestContext.getUserId(),
            now
        );
        parseBatchRepository.save(batch);
        return toResponse(batch, Collections.<ParseBatchItem>emptyList());
    }

    public ParseBatchStatusResponse ingestBatch(String batchId, ParseBatchIngestRequest request) {
        ParseBatch batch = requireBatch(batchId);
        if (!parseBatchItemRepository.findByBatchId(batch.getBatchId()).isEmpty()) {
            throw invalidArgument("batchId", "Parse batch has already been ingested");
        }
        List<ImportedBatchRow> rows = parseRows(batch, request);
        if (rows.isEmpty()) {
            throw invalidArgument("contentBase64", "No parseable records were found in the uploaded payload");
        }

        Instant now = Instant.now();
        batch.transitionTo(ParseBatchStatus.RUNNING_STRUCTURE, now, "BATCH_INGESTION_STARTED");
        if (!batch.isStructureParseOnly()) {
            batch.transitionTo(ParseBatchStatus.RUNNING_ACCESS, now, "ACCESS_PARSE_ORCHESTRATION_STARTED");
        }

        List<ParseBatchItem> persistedItems = new ArrayList<ParseBatchItem>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            ImportedBatchRow row = rows.get(index);
            ParseBatchItem item = ParseBatchItem.create(
                UUID.randomUUID().toString(),
                batch.getBatchId(),
                index + 1,
                row.reportCode,
                row.reportName,
                firstNonBlank(row.datasourceCode, batch.getDatasourceCode()),
                row.stage,
                row.bizDate,
                row.priority,
                row.owner,
                row.tags,
                row.sqlText,
                row.sqlTemplateText,
                row.bindParametersJson,
                row.bindingMode,
                now
            );
            parseItem(batch, item, row, now);
            parseBatchItemRepository.save(item);
            persistedItems.add(item);
        }

        recalculateBatch(batch, persistedItems, now);
        parseBatchRepository.save(batch);
        return toResponse(batch, persistedItems);
    }

    public ParseBatchStatusResponse retryAccess(String batchId, ParseBatchRetryAccessRequest request) {
        ParseBatch batch = requireBatch(batchId);
        List<ParseBatchItem> items = parseBatchItemRepository.findByBatchId(batch.getBatchId());
        if (items.isEmpty()) {
            return toResponse(batch, items);
        }
        String normalizedFailureFilter = normalizeFailureFilter(request == null ? null : request.getFailureFilter());
        String datasourceOverride = trimToNull(request == null ? null : request.getDatasourceCode());
        Instant now = Instant.now();
        batch.transitionTo(ParseBatchStatus.RUNNING_ACCESS, now, "ACCESS_PARSE_RETRY_STARTED");

        for (ParseBatchItem item : items) {
            if (!shouldRetry(item, normalizedFailureFilter)) {
                continue;
            }
            if (!"VALID".equals(item.getStructureSyntaxStatus())) {
                continue;
            }
            AccessParseRequest accessRequest = new AccessParseRequest();
            accessRequest.setSqlText(item.getSqlText());
            accessRequest.setSqlTemplateText(item.getSqlTemplateText());
            accessRequest.setBindParameters(parseBindParameters(item.getBindParametersJson()));
            accessRequest.setBindingMode(item.getBindingMode());
            accessRequest.setDatasourceCode(firstNonBlank(datasourceOverride, item.getDatasourceCode(), batch.getDatasourceCode()));
            accessRequest.setCommentContext(buildCommentContext(batch, item));
            accessRequest.setConnectionRequired(Boolean.TRUE);
            AccessParseResponseVO accessParse = accessParseApplicationService.parseAccess(accessRequest, item.getParseTaskId());
            updateItemFromAccess(item, accessParse, now);
            parseBatchItemRepository.save(item);
        }

        List<ParseBatchItem> refreshedItems = parseBatchItemRepository.findByBatchId(batch.getBatchId());
        recalculateBatch(batch, refreshedItems, now);
        parseBatchRepository.save(batch);
        return toResponse(batch, refreshedItems);
    }

    public ParseBatchStatusResponse getBatch(String batchId) {
        ParseBatch batch = requireBatch(batchId);
        return toResponse(batch, parseBatchItemRepository.findByBatchId(batch.getBatchId()));
    }

    public List<ParseBatchStatusResponse> listBatches() {
        String tenantId = requireAuthorizedTenant(null);
        List<ParseBatchStatusResponse> result = new ArrayList<ParseBatchStatusResponse>();
        for (ParseBatch batch : parseBatchRepository.findAll()) {
            if (!tenantId.equals(batch.getTenantId())) {
                continue;
            }
            result.add(toResponse(batch, Collections.<ParseBatchItem>emptyList()));
        }
        return result;
    }

    private ParseBatch requireBatch(String batchId) {
        ParseBatch batch = parseBatchRepository.findByBatchId(requireText(batchId, "batchId"));
        if (batch == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Parse batch does not exist for batchId=" + batchId
            );
        }
        verifyTenantAccess(batch.getTenantId());
        return batch;
    }

    private void parseItem(ParseBatch batch, ParseBatchItem item, ImportedBatchRow row, Instant now) {
        StructureParseRequest structureRequest = new StructureParseRequest();
        structureRequest.setSqlText(row.sqlText);
        structureRequest.setSqlTemplateText(row.sqlTemplateText);
        structureRequest.setBindParameters(parseBindParameters(row.bindParametersJson));
        structureRequest.setBindingMode(row.bindingMode);
        structureRequest.setDatasourceCode(firstNonBlank(row.datasourceCode, batch.getDatasourceCode()));
        structureRequest.setCommentContext(buildCommentContext(batch, row));
        StructureParseResponseVO structureParse = structureParseApplicationService.parse(structureRequest);

        List<String> issueScenes = extractIssueScenes(structureParse.getIssues());
        List<String> logicalObjectKeys = extractLogicalObjectKeys(structureParse.getLogicalObjectHits());
        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            item.complete(
                structureParse.getParseTaskId(),
                structureParse.getSyntaxStatus(),
                "SKIPPED",
                "SKIPPED",
                ParseBatchItemStatus.FAILED,
                "STRUCTURE_PARSE_INVALID",
                issueScenes,
                logicalObjectKeys,
                now
            );
            return;
        }

        if (batch.isStructureParseOnly()) {
            item.complete(
                structureParse.getParseTaskId(),
                structureParse.getSyntaxStatus(),
                "SKIPPED",
                "SKIPPED",
                ParseBatchItemStatus.SUCCESS,
                null,
                issueScenes,
                logicalObjectKeys,
                now
            );
            return;
        }

        AccessParseRequest accessRequest = new AccessParseRequest();
        accessRequest.setSqlText(row.sqlText);
        accessRequest.setSqlTemplateText(row.sqlTemplateText);
        accessRequest.setBindParameters(parseBindParameters(row.bindParametersJson));
        accessRequest.setBindingMode(row.bindingMode);
        accessRequest.setDatasourceCode(firstNonBlank(row.datasourceCode, batch.getDatasourceCode()));
        accessRequest.setCommentContext(buildCommentContext(batch, row));
        accessRequest.setConnectionRequired(Boolean.TRUE);

        AccessParseResponseVO accessParse = accessParseApplicationService.parseAccess(accessRequest, structureParse.getParseTaskId());
        ParseBatchItemStatus terminalStatus = resolveTerminalStatus(accessParse);
        structureParseApplicationService.writeParseHistoryWithAccess(
            structureParse,
            accessParse,
            structureRequest,
            terminalStatus == ParseBatchItemStatus.SUCCESS ? "SUCCESS" : "PARTIAL"
        );
        item.complete(
            structureParse.getParseTaskId(),
            structureParse.getSyntaxStatus(),
            accessParse.getServiceStatus(),
            accessParse.getConnectionStatus(),
            terminalStatus,
            resolveFailureReason(accessParse),
            issueScenes,
            logicalObjectKeys,
            now
        );
    }

    private ParseBatchItemStatus resolveTerminalStatus(AccessParseResponseVO accessParse) {
        if ("AVAILABLE".equals(accessParse.getServiceStatus()) && "CONNECTED".equals(accessParse.getConnectionStatus())) {
            return ParseBatchItemStatus.SUCCESS;
        }
        return ParseBatchItemStatus.PARTIAL_SUCCESS;
    }

    private String resolveFailureReason(AccessParseResponseVO accessParse) {
        return resolveTerminalStatus(accessParse) == ParseBatchItemStatus.SUCCESS ? null : accessParse.getDegradeReason();
    }

    private void updateItemFromAccess(ParseBatchItem item, AccessParseResponseVO accessParse, Instant now) {
        item.complete(
            item.getParseTaskId(),
            item.getStructureSyntaxStatus(),
            accessParse.getServiceStatus(),
            accessParse.getConnectionStatus(),
            resolveTerminalStatus(accessParse),
            resolveFailureReason(accessParse),
            item.getIssueScenes(),
            item.getLogicalObjectKeys(),
            now
        );
    }

    private void recalculateBatch(ParseBatch batch, List<ParseBatchItem> items, Instant now) {
        int total = items.size();
        int structureSuccess = 0;
        int accessSuccess = 0;
        int partial = 0;
        int failed = 0;
        for (ParseBatchItem item : items) {
            if ("VALID".equals(item.getStructureSyntaxStatus())) {
                structureSuccess++;
            }
            if (item.getStatus() == ParseBatchItemStatus.SUCCESS) {
                accessSuccess++;
            } else if (item.getStatus() == ParseBatchItemStatus.PARTIAL_SUCCESS) {
                partial++;
            } else {
                failed++;
            }
        }
        ParseBatchStatus terminalStatus;
        if (total == 0 || failed == total) {
            terminalStatus = ParseBatchStatus.FAILED;
        } else if (failed > 0 || partial > 0) {
            terminalStatus = ParseBatchStatus.PARTIAL_COMPLETED;
        } else {
            terminalStatus = ParseBatchStatus.COMPLETED;
        }
        batch.applyRunSummary(
            total,
            accessSuccess,
            partial,
            failed,
            successRate(structureSuccess, total),
            batch.isStructureParseOnly() ? Double.valueOf(0.0D) : successRate(accessSuccess, total),
            terminalStatus,
            now,
            "BATCH_INGESTION_COMPLETED"
        );
    }

    private Double successRate(int success, int total) {
        if (total <= 0) {
            return Double.valueOf(0.0D);
        }
        return Double.valueOf(((double) success) / ((double) total));
    }

    private ParseBatchStatusResponse toResponse(ParseBatch batch, List<ParseBatchItem> items) {
        ParseBatchStatusResponse response = new ParseBatchStatusResponse();
        response.setBatchId(batch.getBatchId());
        response.setTenantId(batch.getTenantId());
        response.setBatchName(batch.getBatchName());
        response.setImportMode(batch.getImportMode().name());
        response.setSourceType(batch.getSourceType().name());
        response.setFileType(batch.getFileType().name());
        response.setTemplateVersion(batch.getTemplateVersion());
        response.setDatasourceCode(batch.getDatasourceCode());
        response.setStructureParseOnly(Boolean.valueOf(batch.isStructureParseOnly()));
        response.setStatus(batch.getStatus().name());
        response.setTotalRecords(Integer.valueOf(batch.getTotalRecords()));
        response.setSuccessRecords(Integer.valueOf(batch.getSuccessRecords()));
        response.setPartialSuccessRecords(Integer.valueOf(batch.getPartialSuccessRecords()));
        response.setFailedRecords(Integer.valueOf(batch.getFailedRecords()));
        response.setStructureParseSuccessRate(batch.getStructureParseSuccessRate());
        response.setAccessParseSuccessRate(batch.getAccessParseSuccessRate());
        response.setItemPreviewLimit(Integer.valueOf(ITEM_PREVIEW_LIMIT));
        response.setItemPreviewTruncated(Boolean.valueOf(items.size() > ITEM_PREVIEW_LIMIT));
        response.setOmittedItemCount(Integer.valueOf(Math.max(0, items.size() - ITEM_PREVIEW_LIMIT)));
        List<ParseBatchItem> failureItems = failureItems(items);
        response.setFailurePreviewLimit(Integer.valueOf(FAILURE_PREVIEW_LIMIT));
        response.setFailurePreviewTruncated(Boolean.valueOf(failureItems.size() > FAILURE_PREVIEW_LIMIT));
        response.setOmittedFailureCount(Integer.valueOf(Math.max(0, failureItems.size() - FAILURE_PREVIEW_LIMIT)));
        response.setSupportedFileTypes(resolveSupportedFileTypes(batch.getImportMode()));
        response.setTemplateColumns(resolveTemplateColumns(batch.getImportMode()));
        response.setStatusHistory(toStatusHistory(batch.getStatusHistory()));
        response.setImportedRecords(toItemVOs(previewItems(items, ITEM_PREVIEW_LIMIT)));
        response.setFailureRecords(toItemVOs(previewItems(failureItems, FAILURE_PREVIEW_LIMIT)));
        response.setStructureParseStatistics(buildStructureStatistics(items));
        response.setAccessParseStatistics(buildAccessStatistics(items, batch.isStructureParseOnly()));
        response.setIssueStatistics(buildIssueStatistics(items));
        response.setReportStatistics(buildReportStatistics(items));
        response.setCreatedAt(batch.getCreatedAt());
        response.setUpdatedAt(batch.getUpdatedAt());
        return response;
    }

    private ParseBatchStageStatisticsVO buildStructureStatistics(List<ParseBatchItem> items) {
        int success = 0;
        int failed = 0;
        for (ParseBatchItem item : items) {
            if ("VALID".equals(item.getStructureSyntaxStatus())) {
                success++;
            } else {
                failed++;
            }
        }
        ParseBatchStageStatisticsVO vo = new ParseBatchStageStatisticsVO();
        vo.setSuccessRecords(Integer.valueOf(success));
        vo.setPartialSuccessRecords(Integer.valueOf(0));
        vo.setFailedRecords(Integer.valueOf(failed));
        vo.setSuccessRate(successRate(success, items.size()));
        return vo;
    }

    private ParseBatchStageStatisticsVO buildAccessStatistics(List<ParseBatchItem> items, boolean structureParseOnly) {
        ParseBatchStageStatisticsVO vo = new ParseBatchStageStatisticsVO();
        if (structureParseOnly) {
            vo.setSuccessRecords(Integer.valueOf(0));
            vo.setPartialSuccessRecords(Integer.valueOf(0));
            vo.setFailedRecords(Integer.valueOf(0));
            vo.setSuccessRate(Double.valueOf(0.0D));
            return vo;
        }
        int success = 0;
        int partial = 0;
        int failed = 0;
        for (ParseBatchItem item : items) {
            if (item.getStatus() == ParseBatchItemStatus.SUCCESS) {
                success++;
            } else if (item.getStatus() == ParseBatchItemStatus.PARTIAL_SUCCESS) {
                partial++;
            } else {
                failed++;
            }
        }
        vo.setSuccessRecords(Integer.valueOf(success));
        vo.setPartialSuccessRecords(Integer.valueOf(partial));
        vo.setFailedRecords(Integer.valueOf(failed));
        vo.setSuccessRate(successRate(success, items.size()));
        return vo;
    }

    private List<ParseBatchIssueStatisticVO> buildIssueStatistics(List<ParseBatchItem> items) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (ParseBatchItem item : items) {
            for (String issueScene : item.getIssueScenes()) {
                counts.put(issueScene, Integer.valueOf(counts.getOrDefault(issueScene, Integer.valueOf(0)).intValue() + 1));
            }
        }
        List<ParseBatchIssueStatisticVO> statistics = new ArrayList<ParseBatchIssueStatisticVO>(counts.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            ParseBatchIssueStatisticVO vo = new ParseBatchIssueStatisticVO();
            vo.setIssueScene(entry.getKey());
            vo.setAffectedRecords(entry.getValue());
            vo.setRatio(successRate(entry.getValue().intValue(), items.size()));
            statistics.add(vo);
        }
        statistics.sort(new Comparator<ParseBatchIssueStatisticVO>() {
            @Override
            public int compare(ParseBatchIssueStatisticVO left, ParseBatchIssueStatisticVO right) {
                return Integer.compare(right.getAffectedRecords().intValue(), left.getAffectedRecords().intValue());
            }
        });
        return statistics;
    }

    private List<ParseBatchReportStatisticVO> buildReportStatistics(List<ParseBatchItem> items) {
        Map<String, int[]> counts = new LinkedHashMap<String, int[]>();
        for (ParseBatchItem item : items) {
            String reportCode = firstNonBlank(item.getReportCode(), "UNSPECIFIED");
            int[] stat = counts.computeIfAbsent(reportCode, key -> new int[2]);
            stat[0]++;
            if (!item.getIssueScenes().isEmpty()) {
                stat[1]++;
            }
        }
        List<ParseBatchReportStatisticVO> statistics = new ArrayList<ParseBatchReportStatisticVO>(counts.size());
        for (Map.Entry<String, int[]> entry : counts.entrySet()) {
            ParseBatchReportStatisticVO vo = new ParseBatchReportStatisticVO();
            vo.setReportCode(entry.getKey());
            vo.setSqlCount(Integer.valueOf(entry.getValue()[0]));
            vo.setIssueCount(Integer.valueOf(entry.getValue()[1]));
            vo.setRatio(successRate(entry.getValue()[0], items.size()));
            statistics.add(vo);
        }
        statistics.sort(new Comparator<ParseBatchReportStatisticVO>() {
            @Override
            public int compare(ParseBatchReportStatisticVO left, ParseBatchReportStatisticVO right) {
                return Integer.compare(right.getSqlCount().intValue(), left.getSqlCount().intValue());
            }
        });
        return statistics;
    }

    private List<ParseBatchItemVO> toItemVOs(List<ParseBatchItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<ParseBatchItemVO> results = new ArrayList<ParseBatchItemVO>(items.size());
        for (ParseBatchItem item : items) {
            ParseBatchItemVO vo = new ParseBatchItemVO();
            vo.setItemId(item.getItemId());
            vo.setSequenceNumber(Integer.valueOf(item.getSequenceNumber()));
            vo.setReportCode(item.getReportCode());
            vo.setReportName(item.getReportName());
            vo.setDatasourceCode(item.getDatasourceCode());
            vo.setStage(item.getStage());
            vo.setBizDate(item.getBizDate());
            vo.setPriority(item.getPriority());
            vo.setOwner(item.getOwner());
            vo.setTags(item.getTags());
            vo.setSqlText(item.getSqlText());
            vo.setSqlTemplateText(item.getSqlTemplateText());
            vo.setBindingMode(item.getBindingMode());
            vo.setStatus(item.getStatus() == null ? null : item.getStatus().name());
            vo.setParseTaskId(item.getParseTaskId());
            vo.setStructureSyntaxStatus(item.getStructureSyntaxStatus());
            vo.setAccessServiceStatus(item.getAccessServiceStatus());
            vo.setAccessConnectionStatus(item.getAccessConnectionStatus());
            vo.setFailureReason(item.getFailureReason());
            vo.setIssueScenes(new ArrayList<String>(item.getIssueScenes()));
            vo.setLogicalObjectKeys(new ArrayList<String>(item.getLogicalObjectKeys()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setUpdatedAt(item.getUpdatedAt());
            results.add(vo);
        }
        return results;
    }

    private List<ParseBatchItem> failureItems(List<ParseBatchItem> items) {
        List<ParseBatchItem> failures = new ArrayList<ParseBatchItem>();
        if (items == null) {
            return failures;
        }
        for (ParseBatchItem item : items) {
            if (item.getStatus() == ParseBatchItemStatus.FAILED) {
                failures.add(item);
            }
        }
        return failures;
    }

    private List<ParseBatchItem> previewItems(List<ParseBatchItem> items, int limit) {
        if (items == null || items.size() <= limit) {
            return items;
        }
        return new ArrayList<ParseBatchItem>(items.subList(0, limit));
    }

    private List<ParseBatchStatusHistoryVO> toStatusHistory(List<ParseBatchStatusTransition> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<ParseBatchStatusHistoryVO> items = new ArrayList<ParseBatchStatusHistoryVO>(history.size());
        for (ParseBatchStatusTransition transition : history) {
            ParseBatchStatusHistoryVO vo = new ParseBatchStatusHistoryVO();
            vo.setPreviousStatus(transition.getPreviousStatus() == null ? null : transition.getPreviousStatus().name());
            vo.setCurrentStatus(transition.getCurrentStatus() == null ? null : transition.getCurrentStatus().name());
            vo.setOccurredAt(transition.getOccurredAt());
            vo.setNote(transition.getNote());
            items.add(vo);
        }
        return items;
    }

    private List<String> resolveSupportedFileTypes(ParseBatchImportMode importMode) {
        if (importMode == ParseBatchImportMode.SQL_FILE) {
            return Arrays.asList(ParseBatchFileType.TXT.name(), ParseBatchFileType.SQL.name());
        }
        if (importMode == ParseBatchImportMode.REPORT_CATALOG) {
            return Arrays.asList(
                ParseBatchFileType.XLSX.name(),
                ParseBatchFileType.CSV.name(),
                ParseBatchFileType.TXT.name(),
                ParseBatchFileType.XLS.name(),
                ParseBatchFileType.ET.name()
            );
        }
        return Arrays.asList(
            ParseBatchFileType.XLSX.name(),
            ParseBatchFileType.CSV.name(),
            ParseBatchFileType.TXT.name(),
            ParseBatchFileType.SQL.name(),
            ParseBatchFileType.XLS.name(),
            ParseBatchFileType.ET.name()
        );
    }

    private List<ParseBatchTemplateColumnVO> resolveTemplateColumns(ParseBatchImportMode importMode) {
        List<ParseBatchTemplateColumnVO> columns = new ArrayList<ParseBatchTemplateColumnVO>();
        if (importMode == ParseBatchImportMode.SQL_FILE) {
            columns.add(templateColumn("sql_text", "SQL Text", Boolean.TRUE, "Single SQL or multi-SQL file payload."));
            columns.add(templateColumn("datasource", "Datasource", Boolean.FALSE, "Optional datasource override for SQL file entries."));
            return columns;
        }
        if (importMode == ParseBatchImportMode.REPORT_CATALOG) {
            columns.add(templateColumn("report_code", "Report Code", Boolean.TRUE, "Unique report key used to resolve SQL text."));
            columns.add(templateColumn("report_name", "Report Name", Boolean.FALSE, "Human-readable report title."));
            columns.add(templateColumn("tenant_id", "Tenant ID", Boolean.FALSE, "Optional tenant override when allowed by governance."));
            columns.add(templateColumn("datasource", "Datasource", Boolean.FALSE, "Optional datasource override for report resolution."));
            columns.add(templateColumn("stage", "Stage", Boolean.FALSE, "Stage such as DEV/UAT/PROD."));
            columns.add(templateColumn("priority", "Priority", Boolean.FALSE, "Governance priority hint."));
            return columns;
        }
        columns.add(templateColumn("report_code", "Report Code", Boolean.FALSE, "Optional report key carried into parse governance context."));
        columns.add(templateColumn("report_name", "Report Name", Boolean.FALSE, "Optional report title."));
        columns.add(templateColumn("tenant_id", "Tenant ID", Boolean.FALSE, "Optional tenant override when allowed by governance."));
        columns.add(templateColumn("datasource", "Datasource", Boolean.FALSE, "Optional datasource override."));
        columns.add(templateColumn("stage", "Stage", Boolean.FALSE, "Stage such as DEV/UAT/PROD."));
        columns.add(templateColumn("biz_date", "Biz Date", Boolean.FALSE, "Execution date / batch date metadata."));
        columns.add(templateColumn("priority", "Priority", Boolean.FALSE, "Governance priority hint."));
        columns.add(templateColumn("owner", "Owner", Boolean.FALSE, "Responsible analyst or team."));
        columns.add(templateColumn("sql_text", "SQL Text", Boolean.TRUE, "SQL text to parse."));
        columns.add(templateColumn("sql_template_text", "SQL Template Text", Boolean.FALSE, "Prepared SQL template when available."));
        columns.add(templateColumn("bind_parameters", "Bind Parameters", Boolean.FALSE, "Masked bind parameter payload."));
        columns.add(templateColumn("tags", "Tags", Boolean.FALSE, "Optional free-form tags."));
        return columns;
    }

    private ParseBatchTemplateColumnVO templateColumn(String key, String displayName, Boolean required, String description) {
        ParseBatchTemplateColumnVO column = new ParseBatchTemplateColumnVO();
        column.setColumnKey(key);
        column.setDisplayName(displayName);
        column.setRequired(required);
        column.setDescription(description);
        return column;
    }

    private void validateFileTypeCompatibility(ParseBatchImportMode importMode, ParseBatchFileType fileType) {
        if (importMode == ParseBatchImportMode.SQL_FILE
            && fileType != ParseBatchFileType.TXT
            && fileType != ParseBatchFileType.SQL) {
            throw invalidArgument("fileType", "SQL_FILE importMode only supports TXT or SQL");
        }
        if (importMode == ParseBatchImportMode.REPORT_CATALOG && fileType == ParseBatchFileType.SQL) {
            throw invalidArgument("fileType", "REPORT_CATALOG importMode does not support raw SQL fileType");
        }
    }

    private ParseBatchSourceType resolveSourceType(ParseBatchImportMode importMode) {
        return importMode == ParseBatchImportMode.REPORT_CATALOG
            ? ParseBatchSourceType.REPORT_CATALOG_IMPORT
            : ParseBatchSourceType.FILE_UPLOAD;
    }

    private ParseBatchImportMode requireImportMode(String importMode) {
        try {
            return ParseBatchImportMode.valueOf(requireText(importMode, "importMode"));
        } catch (IllegalArgumentException ex) {
            throw invalidArgument("importMode", "Unsupported importMode: " + importMode);
        }
    }

    private ParseBatchFileType requireFileType(String fileType) {
        try {
            return ParseBatchFileType.valueOf(requireText(fileType, "fileType"));
        } catch (IllegalArgumentException ex) {
            throw invalidArgument("fileType", "Unsupported fileType: " + fileType);
        }
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
            throw new AccessDeniedException("Authenticated tenant cannot access this parse batch");
        }
    }

    private List<ImportedBatchRow> parseRows(ParseBatch batch, ParseBatchIngestRequest request) {
        byte[] content = decodeBase64(requireText(request == null ? null : request.getContentBase64(), "contentBase64"));
        if (batch.getImportMode() == ParseBatchImportMode.SQL_FILE) {
            return parseSqlFile(batch, content, request == null ? null : request.getCharset());
        }
        if (batch.getFileType() == ParseBatchFileType.XLSX
            || batch.getFileType() == ParseBatchFileType.XLS
            || batch.getFileType() == ParseBatchFileType.ET) {
            return parseWorkbookRows(content, batch.getFileType());
        }
        return parseDelimitedRows(content, request == null ? null : request.getCharset(), batch.getFileType());
    }

    private List<ImportedBatchRow> parseSqlFile(ParseBatch batch, byte[] content, String charsetName) {
        String text = new String(content, resolveCharset(charsetName));
        String normalized = text.replace("\r\n", "\n");
        List<ImportedBatchRow> rows = new ArrayList<ImportedBatchRow>();
        for (String segment : splitSqlStatements(normalized)) {
            String sqlText = trimToNull(segment);
            if (!StringUtils.hasText(sqlText)) {
                continue;
            }
            ImportedBatchRow row = new ImportedBatchRow();
            row.datasourceCode = batch.getDatasourceCode();
            row.sqlText = sqlText;
            rows.add(row);
        }
        if (rows.isEmpty() && StringUtils.hasText(trimToNull(normalized))) {
            ImportedBatchRow row = new ImportedBatchRow();
            row.datasourceCode = batch.getDatasourceCode();
            row.sqlText = normalized.trim();
            rows.add(row);
        }
        return rows;
    }

    private List<String> splitSqlStatements(String sqlText) {
        List<String> statements = new ArrayList<String>();
        if (!StringUtils.hasText(sqlText)) {
            return statements;
        }
        int statementStart = 0;
        int cursor = 0;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            if (current == '\'' || current == '"' || current == '`') {
                cursor = readQuotedSegment(sqlText, cursor, current);
                continue;
            }
            if (current == '-' && cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == '-') {
                cursor = readLineComment(sqlText, cursor);
                continue;
            }
            if (current == '/' && cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == '*') {
                cursor = readBlockComment(sqlText, cursor);
                continue;
            }
            if (current == ';') {
                addStatementIfExecutable(statements, sqlText.substring(statementStart, cursor));
                statementStart = cursor + 1;
            }
            cursor++;
        }
        addStatementIfExecutable(statements, sqlText.substring(statementStart));
        return statements;
    }

    private int readQuotedSegment(String sqlText, int start, char quote) {
        int cursor = start + 1;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            if (current == quote) {
                if (cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == quote) {
                    cursor += 2;
                    continue;
                }
                return cursor + 1;
            }
            if (current == '\\') {
                cursor += 2;
                continue;
            }
            cursor++;
        }
        return cursor;
    }

    private int readLineComment(String sqlText, int start) {
        int newline = sqlText.indexOf('\n', start);
        return newline < 0 ? sqlText.length() : newline;
    }

    private int readBlockComment(String sqlText, int start) {
        int end = sqlText.indexOf("*/", start + 2);
        return end < 0 ? sqlText.length() : end + 2;
    }

    private void addStatementIfExecutable(List<String> statements, String candidate) {
        String normalized = trimToNull(candidate);
        if (StringUtils.hasText(normalized) && hasExecutableSql(normalized)) {
            statements.add(normalized);
        }
    }

    private boolean hasExecutableSql(String sqlText) {
        StringBuilder code = new StringBuilder(sqlText.length());
        int cursor = 0;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            if (current == '\'' || current == '"' || current == '`') {
                int end = readQuotedSegment(sqlText, cursor, current);
                code.append(sqlText, cursor, end);
                cursor = end;
                continue;
            }
            if (current == '-' && cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == '-') {
                cursor = readLineComment(sqlText, cursor);
                continue;
            }
            if (current == '/' && cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == '*') {
                cursor = readBlockComment(sqlText, cursor);
                continue;
            }
            code.append(current);
            cursor++;
        }
        return StringUtils.hasText(code.toString());
    }

    private List<ImportedBatchRow> parseDelimitedRows(byte[] content, String charsetName, ParseBatchFileType fileType) {
        Charset charset = resolveCharset(charsetName);
        char delimiter = inferDelimiter(new String(content, charset), fileType);
        try {
            CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .build()
                .parse(new InputStreamReader(new ByteArrayInputStream(content), charset));
            List<ImportedBatchRow> rows = new ArrayList<ImportedBatchRow>();
            for (CSVRecord record : parser) {
                rows.add(toImportedRow(record.toMap()));
            }
            return rows;
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse delimited batch payload: " + ex.getMessage());
        }
    }

    private List<ImportedBatchRow> parseWorkbookRows(byte[] content, ParseBatchFileType fileType) {
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
            List<String> headers = new ArrayList<String>();
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                headers.add(normalizeHeader(formatter.formatCellValue(headerRow.getCell(cellIndex))));
            }
            List<ImportedBatchRow> rows = new ArrayList<ImportedBatchRow>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<String, String>();
                boolean nonEmpty = false;
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    String header = headers.get(cellIndex);
                    if (!StringUtils.hasText(header)) {
                        continue;
                    }
                    Cell cell = row.getCell(cellIndex);
                    String value = trimToNull(formatter.formatCellValue(cell));
                    if (StringUtils.hasText(value)) {
                        nonEmpty = true;
                    }
                    values.put(header, value);
                }
                if (nonEmpty) {
                    rows.add(toImportedRow(values));
                }
            }
            workbook.close();
            return rows;
        } catch (Exception ex) {
            if (fileType == ParseBatchFileType.ET) {
                throw invalidArgument(
                    "contentBase64",
                    "Failed to parse ET batch payload; please convert the file to XLSX or CSV before retrying."
                );
            }
            throw invalidArgument("contentBase64", "Failed to parse " + fileType.name() + " batch payload: " + ex.getMessage());
        }
    }

    private ImportedBatchRow toImportedRow(Map<String, String> values) {
        ImportedBatchRow row = new ImportedBatchRow();
        row.reportCode = trimToNull(values.get("report_code"));
        row.reportName = trimToNull(values.get("report_name"));
        row.datasourceCode = trimToNull(firstNonBlank(values.get("datasource"), values.get("datasource_code")));
        row.stage = trimToNull(values.get("stage"));
        row.bizDate = trimToNull(values.get("biz_date"));
        row.priority = trimToNull(values.get("priority"));
        row.owner = trimToNull(values.get("owner"));
        row.tags = trimToNull(values.get("tags"));
        row.sqlText = trimToNull(values.get("sql_text"));
        row.sqlTemplateText = trimToNull(values.get("sql_template_text"));
        row.bindParametersJson = trimToNull(values.get("bind_parameters"));
        row.bindingMode = trimToNull(values.get("binding_mode"));
        if (!StringUtils.hasText(row.sqlText)) {
            row.sqlText = trimToNull(values.get("sql"));
        }
        if (!StringUtils.hasText(row.sqlText)) {
            throw invalidArgument("sql_text", "Tabular batch payload must provide sql_text for every record");
        }
        return row;
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

    private Map<String, Object> buildCommentContext(ParseBatch batch, ImportedBatchRow row) {
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        putIfPresent(context, "report_code", row.reportCode);
        putIfPresent(context, "stage", row.stage);
        putIfPresent(context, "biz_date", row.bizDate);
        putIfPresent(context, "tenant_id", batch.getTenantId());
        putIfPresent(context, "datasource", firstNonBlank(row.datasourceCode, batch.getDatasourceCode()));
        putIfPresent(context, "priority", row.priority);
        return context;
    }

    private Map<String, Object> buildCommentContext(ParseBatch batch, ParseBatchItem item) {
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        putIfPresent(context, "report_code", item.getReportCode());
        putIfPresent(context, "stage", item.getStage());
        putIfPresent(context, "biz_date", item.getBizDate());
        putIfPresent(context, "tenant_id", batch.getTenantId());
        putIfPresent(context, "datasource", firstNonBlank(item.getDatasourceCode(), batch.getDatasourceCode()));
        putIfPresent(context, "priority", item.getPriority());
        return context;
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private Map<String, Object> parseBindParameters(String bindParametersJson) {
        if (!StringUtils.hasText(bindParametersJson)) {
            return null;
        }
        try {
            return JsonUtils.objectMapper().readValue(bindParametersJson, MAP_TYPE);
        } catch (Exception ex) {
            throw invalidArgument("bind_parameters", "Failed to deserialize bind parameters JSON");
        }
    }

    private boolean shouldRetry(ParseBatchItem item, String failureFilter) {
        if (item.getStatus() != ParseBatchItemStatus.PARTIAL_SUCCESS) {
            return false;
        }
        if (FAILURE_FILTER_ALL.equals(failureFilter)) {
            return true;
        }
        if (FAILURE_FILTER_UNAVAILABLE.equals(failureFilter)) {
            return "UNAVAILABLE".equals(item.getAccessServiceStatus()) || "UNAVAILABLE".equals(item.getAccessConnectionStatus());
        }
        if (FAILURE_FILTER_FAILED.equals(failureFilter)) {
            return "FAILED".equals(item.getAccessConnectionStatus());
        }
        return FAILURE_FILTER_ALL.equals(failureFilter);
    }

    private String normalizeFailureFilter(String failureFilter) {
        if (!StringUtils.hasText(failureFilter)) {
            return FAILURE_FILTER_ALL;
        }
        String normalized = failureFilter.trim().toUpperCase(Locale.ROOT);
        if (FAILURE_FILTER_UNAVAILABLE.equals(normalized) || FAILURE_FILTER_FAILED.equals(normalized)) {
            return normalized;
        }
        return FAILURE_FILTER_ALL;
    }

    private Charset resolveCharset(String charsetName) {
        if (!StringUtils.hasText(charsetName)) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(charsetName.trim());
    }

    private byte[] decodeBase64(String contentBase64) {
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            throw invalidArgument("contentBase64", "contentBase64 must be valid Base64");
        }
    }

    private char inferDelimiter(String text, ParseBatchFileType fileType) {
        String firstLine = text;
        int newlineIndex = text.indexOf('\n');
        if (newlineIndex >= 0) {
            firstLine = text.substring(0, newlineIndex);
        }
        if (fileType == ParseBatchFileType.TXT && firstLine.indexOf('\t') >= 0) {
            return '\t';
        }
        return ',';
    }

    private String normalizeHeader(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class ImportedBatchRow {
        private String reportCode;
        private String reportName;
        private String datasourceCode;
        private String stage;
        private String bizDate;
        private String priority;
        private String owner;
        private String tags;
        private String sqlText;
        private String sqlTemplateText;
        private String bindParametersJson;
        private String bindingMode;
    }
}
