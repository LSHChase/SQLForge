package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.GovernanceQueryHistoryExportRequest;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryExportVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryPageVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryRewriteRecordsVO;
import com.company.governance.application.service.GovernanceHistoryApplicationService;
import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/query-history")
public class GovernanceQueryHistoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceQueryHistoryController.class);

    private final GovernanceHistoryApplicationService governanceHistoryApplicationService;

    public GovernanceQueryHistoryController(GovernanceHistoryApplicationService governanceHistoryApplicationService) {
        this.governanceHistoryApplicationService = governanceHistoryApplicationService;
    }

    @GetMapping
    public GovernanceQueryHistoryPageVO getQueryHistoryPage(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "historyType", required = false) String historyType,
        @RequestParam(value = "reportCode", required = false) String reportCode,
        @RequestParam(value = "datasourceCode", required = false) String datasourceCode,
        @RequestParam(value = "stage", required = false) String stage,
        @RequestParam(value = "bizDate", required = false) String bizDate,
        @RequestParam(value = "queryDateStart", required = false) String queryDateStart,
        @RequestParam(value = "queryDateEnd", required = false) String queryDateEnd,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "cacheHit", required = false) Boolean cacheHit,
        @RequestParam(value = "rewriteApplied", required = false) Boolean rewriteApplied,
        @RequestParam(value = "accelerationApplied", required = false) Boolean accelerationApplied,
        @RequestParam(value = "parameterizedSql", required = false) Boolean parameterizedSql,
        @RequestParam(value = "logicalObjectType", required = false) String logicalObjectType,
        @RequestParam(value = "accessChannel", required = false) String accessChannel,
        @RequestParam(value = "engine", required = false) String engine,
        @RequestParam(value = "submittedBy", required = false) String submittedBy,
        @RequestParam(value = "submittedStart", required = false) String submittedStart,
        @RequestParam(value = "submittedEnd", required = false) String submittedEnd,
        @RequestParam(value = "hasRewriteRecord", required = false) Boolean hasRewriteRecord,
        @RequestParam(value = "rewriteValidationStatus", required = false) String rewriteValidationStatus,
        @RequestParam(value = "rewriteSourceType", required = false) String rewriteSourceType,
        @RequestParam(value = "recommendationId", required = false) String recommendationId,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortOrder", required = false) String sortOrder,
        @RequestParam(value = "pageNo", required = false) Integer pageNo,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        String normalizedHistoryType = GovernanceHistoryApplicationService.normalizeHistoryTypeFilter(historyType);
        LOGGER.info(
            "Handling governance query-history page, tenantId={}, historyType={}, reportCode={}, datasourceCode={}, stage={}, status={}, requestTraceId={}",
            effectiveTenantId,
            normalizedHistoryType,
            reportCode,
            datasourceCode,
            stage,
            status,
            RequestContext.getTraceId()
        );
        return governanceHistoryApplicationService.findQueryHistoryPage(
            effectiveTenantId,
            normalizedHistoryType,
            reportCode,
            datasourceCode,
            stage,
            bizDate,
            queryDateStart,
            queryDateEnd,
            status,
            cacheHit,
            rewriteApplied,
            accelerationApplied,
            parameterizedSql,
            logicalObjectType,
            normalizeAccessChannelFilter(accessChannel),
            engine,
            submittedBy,
            submittedStart,
            submittedEnd,
            hasRewriteRecord,
            rewriteValidationStatus,
            rewriteSourceType,
            recommendationId,
            sortBy,
            sortOrder,
            pageNo,
            pageSize
        );
    }

    @GetMapping("/{historyId}")
    public GovernanceQueryHistoryDetailVO getQueryHistoryDetail(
        @PathVariable("historyId") String historyId,
        @RequestParam(value = "tenantId", required = false) String tenantId) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info(
            "Handling governance query-history detail, tenantId={}, historyId={}, requestTraceId={}",
            effectiveTenantId,
            historyId,
            RequestContext.getTraceId()
        );
        return governanceHistoryApplicationService.findQueryHistoryDetail(effectiveTenantId, historyId);
    }

    @GetMapping("/{historyId}/rewrite-records")
    public GovernanceQueryHistoryRewriteRecordsVO getQueryHistoryRewriteRecords(
        @PathVariable("historyId") String historyId,
        @RequestParam(value = "tenantId", required = false) String tenantId) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info(
            "Handling governance query-history rewrite records, tenantId={}, historyId={}, requestTraceId={}",
            effectiveTenantId,
            historyId,
            RequestContext.getTraceId()
        );
        return governanceHistoryApplicationService.findQueryHistoryRewriteRecords(effectiveTenantId, historyId);
    }

    @PostMapping("/export")
    public GovernanceQueryHistoryExportVO exportQueryHistory(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestBody GovernanceQueryHistoryExportRequest request) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info(
            "Handling governance query-history export, tenantId={}, historyId={}, exportFormat={}, requestTraceId={}",
            effectiveTenantId,
            request == null ? null : request.getHistoryId(),
            request == null ? null : request.getExportFormat(),
            RequestContext.getTraceId()
        );
        return governanceHistoryApplicationService.exportQueryHistory(effectiveTenantId, request);
    }

    private String normalizeAccessChannelFilter(String accessChannel) {
        if (!StringUtils.hasText(accessChannel)) {
            return null;
        }
        AccessChannel normalized = AccessChannel.fromWireValue(accessChannel);
        if (normalized == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "accessChannel must be one of PAGE/API/JDBC_AGENT/SDK/CLIENT"
            );
        }
        return normalized.name();
    }
}
