package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.application.controller.vo.ParseBatchTemplateColumnVO;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchFileType;
import com.company.sqloptimization.domain.batch.ParseBatchImportMode;
import com.company.sqloptimization.domain.batch.ParseBatchSourceType;
import com.company.sqloptimization.domain.batch.ParseBatchStatus;
import com.company.sqloptimization.domain.batch.ParseBatchStatusTransition;
import com.company.sqloptimization.domain.batch.repository.ParseBatchRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ParseBatchApplicationService {

    private final ParseBatchRepository parseBatchRepository;

    public ParseBatchApplicationService(ParseBatchRepository parseBatchRepository) {
        this.parseBatchRepository = parseBatchRepository;
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
        return toResponse(batch);
    }

    public ParseBatchStatusResponse getBatch(String batchId) {
        ParseBatch batch = parseBatchRepository.findByBatchId(requireText(batchId, "batchId"));
        if (batch == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Parse batch does not exist for batchId=" + batchId
            );
        }
        verifyTenantAccess(batch.getTenantId());
        return toResponse(batch);
    }

    private ParseBatchStatusResponse toResponse(ParseBatch batch) {
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
        response.setSupportedFileTypes(resolveSupportedFileTypes(batch.getImportMode()));
        response.setTemplateColumns(resolveTemplateColumns(batch.getImportMode()));
        response.setStatusHistory(toStatusHistory(batch.getStatusHistory()));
        response.setCreatedAt(batch.getCreatedAt());
        response.setUpdatedAt(batch.getUpdatedAt());
        return response;
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
}
