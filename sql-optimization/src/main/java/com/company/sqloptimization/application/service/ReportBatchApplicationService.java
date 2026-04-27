package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchItemVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusResponse;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.service.report.ReportSqlResolveRequest;
import com.company.sqloptimization.application.service.report.ReportSqlResolveResult;
import com.company.sqloptimization.application.service.report.ReportSqlResolver;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportBatchApplicationService {

    private final ReportBatchRepository reportBatchRepository;
    private final ReportBatchItemRepository reportBatchItemRepository;
    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;
    private final ReportSqlResolver reportSqlResolver;

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
        String fileType = requireText(request == null ? null : request.getFileType(), "fileType").toUpperCase(Locale.ROOT);
        if (!"TXT".equals(fileType)) {
            throw invalidArgument("fileType", "Report catalog import currently supports TXT mock source only");
        }
        String reportCodeField = requireText(request == null ? null : request.getReportCodeField(), "reportCodeField");
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
            "TXT_MOCK_SOURCE",
            RequestContext.getUserId(),
            now
        );
        List<ReportBatchItem> items = buildItems(batch, request);
        if (items.isEmpty()) {
            throw invalidArgument("contentBase64", "No report codes were found in the mock source payload");
        }
        batch.recordImportedItems(items.size(), now);
        reportBatchRepository.save(batch);
        for (ReportBatchItem item : items) {
            reportBatchItemRepository.save(item);
        }
        return toResponse(batch, items);
    }

    public ReportBatchStatusResponse resolveSqls(String batchId) {
        ReportBatch batch = requireBatch(batchId);
        List<ReportBatchItem> items = reportBatchItemRepository.findByBatchId(batch.getBatchId());
        if (items.isEmpty()) {
            return toResponse(batch, items);
        }

        Instant now = Instant.now();
        batch.transitionTo(ReportBatch.ParseStatus.RESOLVING_SQLS, now, "REPORT_SQL_RESOLUTION_STARTED");
        List<ReportBatchItem> resolvedItems = new ArrayList<ReportBatchItem>(items.size());
        for (ReportBatchItem item : items) {
            ReportSqlResolveResult resolvedSql = reportSqlResolver.resolve(buildReportSqlResolveRequest(batch, item));
            Map<String, Object> commentContext = buildCommentContext(batch, item);
            StructureParseResponseVO structureParse = parseStructure(resolvedSql.getSqlText(), batch, commentContext);
            List<String> issueScenes = extractIssueScenes(structureParse.getIssues());
            List<String> logicalObjectKeys = extractLogicalObjectKeys(structureParse.getLogicalObjectHits());
            AccessParseResponseVO accessParse = parseAccessIfPossible(resolvedSql.getSqlText(), batch, commentContext, structureParse.getParseTaskId());
            ReportBatchItem.Status status = resolveStatus(structureParse, accessParse);
            item.complete(
                resolvedSql.getSqlText(),
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
            reportBatchItemRepository.save(item);
            resolvedItems.add(item);
        }

        recalculate(batch, resolvedItems, now);
        reportBatchRepository.save(batch);
        return toResponse(batch, resolvedItems);
    }

    public ReportBatchStatusResponse getBatch(String batchId) {
        ReportBatch batch = requireBatch(batchId);
        return toResponse(batch, reportBatchItemRepository.findByBatchId(batch.getBatchId()));
    }

    private ReportBatchStatusResponse toResponse(ReportBatch batch, List<ReportBatchItem> items) {
        ReportBatchStatusResponse response = new ReportBatchStatusResponse();
        response.setBatchId(batch.getBatchId());
        response.setTenantId(batch.getTenantId());
        response.setBatchName(batch.getBatchName());
        response.setFileType(batch.getFileType());
        response.setReportCodeField(batch.getReportCodeField());
        response.setDatasourceCode(batch.getDatasourceCode());
        response.setStage(batch.getStage());
        response.setPriority(batch.getPriority());
        response.setSourceType(batch.getSourceType());
        response.setStatus(batch.getStatus().name());
        response.setTotalReports(Integer.valueOf(batch.getTotalReports()));
        response.setResolvedReports(Integer.valueOf(batch.getResolvedReports()));
        response.setFailedReports(Integer.valueOf(batch.getFailedReports()));
        response.setReportItems(toItemVos(items));
        response.setStatusHistory(toStatusHistory(batch.getStatusHistory()));
        response.setCreatedAt(batch.getCreatedAt());
        response.setUpdatedAt(batch.getUpdatedAt());
        return response;
    }

    private void recalculate(ReportBatch batch, List<ReportBatchItem> items, Instant now) {
        int total = items.size();
        int resolved = 0;
        int failed = 0;
        for (ReportBatchItem item : items) {
            if (item.getStatus() == ReportBatchItem.Status.RESOLVED) {
                resolved++;
            } else if (item.getStatus() == ReportBatchItem.Status.FAILED) {
                failed++;
            } else {
                failed++;
            }
        }
        ReportBatch.ParseStatus terminalStatus = failed > 0
            ? ReportBatch.ParseStatus.PARTIAL_COMPLETED
            : ReportBatch.ParseStatus.COMPLETED;
        batch.applySummary(total, resolved, failed, terminalStatus, now, "REPORT_SQL_RESOLUTION_COMPLETED");
    }

    private ReportBatchItem.Status resolveStatus(StructureParseResponseVO structureParse, AccessParseResponseVO accessParse) {
        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            return ReportBatchItem.Status.FAILED;
        }
        if (accessParse == null) {
            return ReportBatchItem.Status.RESOLVED;
        }
        if ("AVAILABLE".equals(accessParse.getServiceStatus()) && "CONNECTED".equals(accessParse.getConnectionStatus())) {
            return ReportBatchItem.Status.RESOLVED;
        }
        return ReportBatchItem.Status.PARTIAL_RESOLVED;
    }

    private String resolveFailureReason(StructureParseResponseVO structureParse, AccessParseResponseVO accessParse) {
        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            return "STRUCTURE_PARSE_INVALID";
        }
        if (accessParse == null) {
            return null;
        }
        return "AVAILABLE".equals(accessParse.getServiceStatus()) && "CONNECTED".equals(accessParse.getConnectionStatus())
            ? null
            : accessParse.getDegradeReason();
    }

    private StructureParseResponseVO parseStructure(String sqlText, ReportBatch batch, Map<String, Object> commentContext) {
        com.company.sqloptimization.application.controller.dto.StructureParseRequest request = new com.company.sqloptimization.application.controller.dto.StructureParseRequest();
        request.setSqlText(sqlText);
        request.setDatasourceCode(batch.getDatasourceCode());
        request.setCommentContext(commentContext);
        return structureParseApplicationService.parse(request);
    }

    private AccessParseResponseVO parseAccessIfPossible(String sqlText,
                                                        ReportBatch batch,
                                                        Map<String, Object> commentContext,
                                                        String parseTaskId) {
        if (!StringUtils.hasText(batch.getDatasourceCode())) {
            return null;
        }
        AccessParseRequest request = new AccessParseRequest();
        request.setSqlText(sqlText);
        request.setDatasourceCode(batch.getDatasourceCode());
        request.setCommentContext(commentContext);
        request.setConnectionRequired(Boolean.TRUE);
        return accessParseApplicationService.parseAccess(request, parseTaskId);
    }

    private List<ReportBatchItem> buildItems(ReportBatch batch, ReportBatchImportRequest request) {
        byte[] content = decodeBase64(trimToNull(request == null ? null : request.getContentBase64()));
        String text = new String(content, resolveCharset(request == null ? null : request.getCharset()));
        List<ReportSourceRow> rows = parseSourceRows(text);
        List<ReportBatchItem> items = new ArrayList<ReportBatchItem>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            ReportSourceRow row = rows.get(i);
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
                batch.getCreatedAt()
            );
            items.add(item);
        }
        return items;
    }

    private List<ReportSourceRow> parseSourceRows(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        String normalized = text.replace("\r\n", "\n").trim();
        if (normalized.contains(",") && normalized.split("\n", 2)[0].toLowerCase(Locale.ROOT).contains("report_code")) {
            return parseCsvSource(normalized);
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

    private List<ReportSourceRow> parseCsvSource(String text) {
        try {
            CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .build()
                .parse(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            List<ReportSourceRow> rows = new ArrayList<ReportSourceRow>();
            for (CSVRecord record : parser) {
                Map<String, String> values = record.toMap();
                ReportSourceRow row = new ReportSourceRow();
                row.rawLine = values.toString();
                row.reportCode = trimToNull(firstNonBlank(values.get("report_code"), values.get("reportCode")));
                row.reportName = trimToNull(firstNonBlank(values.get("report_name"), values.get("reportName")));
                row.datasourceCode = trimToNull(firstNonBlank(values.get("datasource"), values.get("datasource_code")));
                row.stage = trimToNull(values.get("stage"));
                row.priority = trimToNull(values.get("priority"));
                rows.add(row);
            }
            return rows;
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse report catalog CSV payload: " + ex.getMessage());
        }
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
            if (issue != null && StringUtils.hasText(issue.getIssueScene())) {
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
            vo.setSqlText(item.getSqlText());
            vo.setParseTaskId(item.getParseTaskId());
            vo.setStructureSyntaxStatus(item.getStructureSyntaxStatus());
            vo.setAccessServiceStatus(item.getAccessServiceStatus());
            vo.setAccessConnectionStatus(item.getAccessConnectionStatus());
            vo.setFailureReason(item.getFailureReason());
            vo.setStatus(item.getStatus() == null ? null : item.getStatus().name());
            vo.setIssueScenes(new ArrayList<String>(item.getIssueScenes()));
            vo.setLogicalObjectKeys(new ArrayList<String>(item.getLogicalObjectKeys()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setUpdatedAt(item.getUpdatedAt());
            vos.add(vo);
        }
        return vos;
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

    private static final class ReportSourceRow {
        private String reportCode;
        private String reportName;
        private String datasourceCode;
        private String stage;
        private String priority;
        private String rawLine;
    }
}
