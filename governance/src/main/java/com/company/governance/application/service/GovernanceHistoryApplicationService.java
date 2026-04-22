package com.company.governance.application.service;

import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceHistoryApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceHistoryApplicationService.class);
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String OPERATOR = "OPERATOR";
    private static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";
    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 50;
    private static final int RECENT_SOURCE_SCAN_MULTIPLIER = 5;
    private static final int MAX_RECENT_SOURCE_SCAN_LIMIT = 200;
    private static final int LOOKUP_SOURCE_SCAN_MULTIPLIER = 8;
    private static final int MIN_LOOKUP_SOURCE_SCAN_LIMIT = 120;
    private static final int MAX_LOOKUP_SOURCE_SCAN_LIMIT = 400;
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() {
        };

    private final AuditLogMapper auditLogMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final TenantAccessLogic tenantAccessLogic;

    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic) {
        this.auditLogMapper = auditLogMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.tenantAccessLogic = tenantAccessLogic;
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
        LOGGER.info("Loaded governance trace summaries, tenantId={}, count={}, traceIdSample={}",
            effectiveTenantId,
            Integer.valueOf(summaries.size()),
            summaries.isEmpty() ? "-" : summaries.get(0).getTraceId());
        return summaries;
    }

    public List<GovernanceTraceSummaryVO> lookupTraces(String tenantId,
                                                       String traceId,
                                                       String taskId,
                                                       String reportId,
                                                       Integer limit) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        String normalizedTraceId = trimToNull(traceId);
        String normalizedTaskId = trimToNull(taskId);
        String normalizedReportId = trimToNull(reportId);
        if (!StringUtils.hasText(normalizedTraceId)
            && !StringUtils.hasText(normalizedTaskId)
            && !StringUtils.hasText(normalizedReportId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "one of traceId, taskId, or reportId must be provided"
            );
        }

        int resolvedLimit = normalizeLimit(limit);
        int lookupSourceScanLimit = resolveLookupSourceScanLimit(resolvedLimit);
        List<TraceAggregate> aggregates = loadRecentAggregates(effectiveTenantId, lookupSourceScanLimit);
        List<GovernanceTraceSummaryVO> matches = new ArrayList<GovernanceTraceSummaryVO>();
        for (TraceAggregate aggregate : aggregates) {
            if (!aggregate.shouldDisplayInRecentList()) {
                continue;
            }
            if (!aggregate.matchesLookupCriteria(normalizedTraceId, normalizedTaskId, normalizedReportId)) {
                continue;
            }
            matches.add(aggregate.toSummaryVO());
            if (matches.size() >= resolvedLimit) {
                break;
            }
        }
        LOGGER.info("Loaded governance trace lookup, tenantId={}, traceId={}, taskId={}, reportId={}, count={}",
            effectiveTenantId,
            StringUtils.hasText(normalizedTraceId) ? normalizedTraceId : "-",
            StringUtils.hasText(normalizedTaskId) ? normalizedTaskId : "-",
            StringUtils.hasText(normalizedReportId) ? normalizedReportId : "-",
            Integer.valueOf(matches.size()));
        return matches;
    }

    public GovernanceTraceDetailVO findTraceDetail(String tenantId, String traceId, Integer limit) {
        String effectiveTenantId = resolveAuthorizedTenantId(tenantId);
        if (!StringUtils.hasText(traceId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "traceId must not be empty"
            );
        }

        int resolvedLimit = normalizeLimit(limit);
        TraceAggregate aggregate = new TraceAggregate(traceId.trim());
        mergeAuditLogs(aggregate, auditLogMapper.selectByTraceId(effectiveTenantId, traceId.trim(), resolvedLimit));
        mergeQueryHistories(aggregate, queryHistoryMapper.selectByTraceId(effectiveTenantId, traceId.trim(), resolvedLimit));
        mergeExportRecords(aggregate, exportRecordMapper.selectByTraceId(effectiveTenantId, traceId.trim(), resolvedLimit));

        LOGGER.info("Loaded governance trace detail, tenantId={}, traceId={}, auditEvents={}, histories={}, exports={}",
            effectiveTenantId,
            traceId,
            Integer.valueOf(aggregate.getAuditEventCount()),
            Integer.valueOf(aggregate.getQueryHistoryCount()),
            Integer.valueOf(aggregate.getExportRecordCount()));
        return aggregate.toDetailVO();
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
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN);
        boolean tenantAdmin = RequestContext.hasRole(TENANT_ADMIN);
        boolean operator = RequestContext.hasRole(OPERATOR);
        if (!StringUtils.hasText(currentTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Tenant context is missing"
            );
        }
        if (!platformAdmin && !tenantAdmin && !operator) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                "当前角色无权读取治理历史记录"
            );
        }

        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId.trim() : currentTenantId;
        if (!platformAdmin && !currentTenantId.equals(effectiveTenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
        if (!platformAdmin && !tenantAccessLogic.validateDataSourceAccess(currentTenantId, DEFAULT_DATA_SOURCE_ID)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }
        return effectiveTenantId;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit.intValue() <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(MAX_LIMIT, limit.intValue());
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

    private Map<String, Object> parseJsonObject(String content) {
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

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
                this.summaryUsesBusinessAudit = businessAudit;
            }
        }

        private void applyQueryHistory(QueryHistoryRecord record) {
            if (record == null) {
                return;
            }

            GovernanceTraceDetailVO.QueryHistoryVO historyVO = new GovernanceTraceDetailVO.QueryHistoryVO();
            historyVO.setHistoryId(record.getHistoryId());
            historyVO.setResultId(record.getResultId());
            historyVO.setHistoryType(record.getHistoryType());
            historyVO.setDatasourceType(record.getDatasourceType());
            historyVO.setSqlFingerprint(record.getSqlFingerprint());
            historyVO.setRequestId(record.getRequestId());
            historyVO.setSagaId(record.getSagaId());
            historyVO.setSubmittedBy(record.getSubmittedBy());
            historyVO.setSubmittedAt(record.getSubmittedAt());
            historyVO.setCreateTime(record.getCreateTime());
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
                this.summaryUsesBusinessAudit = false;
            }
        }

        private void applyExportRecord(ExportRecord record) {
            if (record == null) {
                return;
            }

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
                this.summaryUsesBusinessAudit = false;
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
                this.degraded
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
}
