package com.company.sqloptimization.application.controller;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqloptimization.application.controller.dto.SqlParseHistoryExportRequest;
import com.company.sqloptimization.application.controller.vo.SqlParseHistoryDetailVO;
import com.company.sqloptimization.application.controller.vo.SqlParseHistoryExportVO;
import com.company.sqloptimization.application.controller.vo.SqlParseHistoryPageVO;
import com.company.sqloptimization.application.service.SqlParseHistoryApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse-history")
public class SqlParseHistoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlParseHistoryController.class);

    private final SqlParseHistoryApplicationService sqlParseHistoryApplicationService;

    public SqlParseHistoryController(SqlParseHistoryApplicationService sqlParseHistoryApplicationService) {
        this.sqlParseHistoryApplicationService = sqlParseHistoryApplicationService;
    }

    @GetMapping
    public SqlParseHistoryPageVO getParseHistoryPage(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "sourceType", required = false) String sourceType,
        @RequestParam(value = "reportCode", required = false) String reportCode,
        @RequestParam(value = "datasourceCode", required = false) String datasourceCode,
        @RequestParam(value = "stage", required = false) String stage,
        @RequestParam(value = "bizDate", required = false) String bizDate,
        @RequestParam(value = "queryDateStart", required = false) String queryDateStart,
        @RequestParam(value = "queryDateEnd", required = false) String queryDateEnd,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "logicalObjectType", required = false) String logicalObjectType,
        @RequestParam(value = "accessChannel", required = false) String accessChannel,
        @RequestParam(value = "engine", required = false) String engine,
        @RequestParam(value = "submittedBy", required = false) String submittedBy,
        @RequestParam(value = "traceId", required = false) String traceId,
        @RequestParam(value = "parseTaskId", required = false) String parseTaskId,
        @RequestParam(value = "submittedStart", required = false) String submittedStart,
        @RequestParam(value = "submittedEnd", required = false) String submittedEnd,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortOrder", required = false) String sortOrder,
        @RequestParam(value = "pageNo", required = false) Integer pageNo,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        String effectiveTenantId = effectiveTenantId(tenantId);
        LOGGER.info(
            "Handling sql parse-history page, tenantId={}, sourceType={}, datasourceCode={}, status={}, requestTraceId={}",
            effectiveTenantId,
            sourceType,
            datasourceCode,
            status,
            RequestContext.getTraceId()
        );
        return sqlParseHistoryApplicationService.findPage(
            effectiveTenantId,
            sourceType,
            reportCode,
            datasourceCode,
            stage,
            bizDate,
            queryDateStart,
            queryDateEnd,
            status,
            logicalObjectType,
            accessChannel,
            engine,
            submittedBy,
            traceId,
            parseTaskId,
            submittedStart,
            submittedEnd,
            sortBy,
            sortOrder,
            pageNo,
            pageSize
        );
    }

    @GetMapping("/{parseHistoryId}")
    public SqlParseHistoryDetailVO getParseHistoryDetail(
        @PathVariable("parseHistoryId") String parseHistoryId,
        @RequestParam(value = "tenantId", required = false) String tenantId) {
        String effectiveTenantId = effectiveTenantId(tenantId);
        LOGGER.info(
            "Handling sql parse-history detail, tenantId={}, parseHistoryId={}, requestTraceId={}",
            effectiveTenantId,
            parseHistoryId,
            RequestContext.getTraceId()
        );
        return sqlParseHistoryApplicationService.findDetail(effectiveTenantId, parseHistoryId);
    }

    @PostMapping("/export")
    public SqlParseHistoryExportVO exportParseHistory(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestBody SqlParseHistoryExportRequest request) {
        String effectiveTenantId = effectiveTenantId(tenantId);
        LOGGER.info(
            "Handling sql parse-history export, tenantId={}, parseHistoryId={}, exportFormat={}, requestTraceId={}",
            effectiveTenantId,
            request == null ? null : request.getParseHistoryId(),
            request == null ? null : request.getExportFormat(),
            RequestContext.getTraceId()
        );
        return sqlParseHistoryApplicationService.exportHistory(effectiveTenantId, request);
    }

    private String effectiveTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
    }
}
