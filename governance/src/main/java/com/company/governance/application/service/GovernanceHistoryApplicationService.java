package com.company.governance.application.service;

import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceLookupPageVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.domain.trace.entity.TraceLookupHitRecord;
import com.company.governance.infrastructure.benchmarkengine.GovernanceBenchmarkEngineClient;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.GovernanceHistoryLookupIndexMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationTarget;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceHistoryApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceHistoryApplicationService.class);
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String OPERATOR = "OPERATOR";
    private static final String BATCH_OPERATION_RETENTION = "EXECUTE_RETENTION_BATCH";
    private static final String BATCH_OPERATION_RECOVERY = "RECOVER_ARTIFACT_BATCH";
    private static final String ITEM_OPERATION_CLEANUP = "CLEANUP_ARTIFACT";
    private static final String ITEM_OPERATION_RECOVER = "RECOVER_ARTIFACT";
    private static final String BATCH_DEFAULT_RETENTION_SCOPE = "MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER";
    private static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";
    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 50;
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
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() {
        };

    private final AuditLogMapper auditLogMapper;
    private final GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final TenantAccessLogic tenantAccessLogic;
    private final GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient;

    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic) {
        this(auditLogMapper, null, queryHistoryMapper, exportRecordMapper, tenantAccessLogic, null);
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
            null
        );
    }

    @Autowired
    public GovernanceHistoryApplicationService(AuditLogMapper auditLogMapper,
                                               GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper,
                                               QueryHistoryMapper queryHistoryMapper,
                                               ExportRecordMapper exportRecordMapper,
                                               TenantAccessLogic tenantAccessLogic,
                                               GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient) {
        this.auditLogMapper = auditLogMapper;
        this.governanceHistoryLookupIndexMapper = governanceHistoryLookupIndexMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.tenantAccessLogic = tenantAccessLogic;
        this.governanceBenchmarkEngineClient = governanceBenchmarkEngineClient;
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
                "one of traceId, taskId, or reportId must be provided"
            );
        }
        if (windowStartAt != null && windowEndAt != null && windowStartAt.isAfter(windowEndAt)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "windowStart must not be later than windowEnd"
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
            LOGGER.info("Loaded governance trace lookup, tenantId={}, traceId={}, taskId={}, reportId={}, count={}, mode=TRACE",
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
            LOGGER.info("Loaded governance trace lookup, tenantId={}, traceId=-, taskId={}, reportId={}, count=0, mode=INDEXED",
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
        LOGGER.info("Loaded governance trace lookup, tenantId={}, traceId=-, taskId={}, reportId={}, count={}, hasMore={}, mode=INDEXED",
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

    public GovernanceBenchmarkArtifactOperationResponse operateArtifact(
        GovernanceBenchmarkArtifactOperationRequest request
    ) {
        if (governanceBenchmarkEngineClient == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Governance benchmark-engine client is unavailable"
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
                "Governance benchmark-engine client is unavailable"
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
                "artifact batch operation targets must not be empty"
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
                    "reportId and artifactKey must not be empty"
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
                    "Duplicate target suppressed by governance batch orchestration"
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
                        "Benchmark-engine artifact operation returned no response"
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

    private void assertArtifactOperationRole() {
        if (RequestContext.hasRole(PLATFORM_ADMIN) || RequestContext.hasRole(TENANT_ADMIN)) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
            HttpStatus.FORBIDDEN,
            "current role cannot trigger artifact cleanup or recovery"
        );
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
                fieldName + " is invalid"
            );
        }
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
                "lookup cursor is invalid"
            );
        }
        try {
            return new LookupCursor(LocalDateTime.parse(parts[0]), Long.valueOf(parts[1]));
        } catch (Exception ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "lookup cursor is invalid"
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
            "Unsupported governance artifact batch operationType: " + operationType
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
            historyVO.setDatasourceType(record.getDatasourceType());
            historyVO.setSqlFingerprint(record.getSqlFingerprint());
            historyVO.setRequestId(record.getRequestId());
            historyVO.setSagaId(record.getSagaId());
            historyVO.setSubmittedBy(record.getSubmittedBy());
            historyVO.setSubmittedAt(record.getSubmittedAt());
            historyVO.setCreateTime(record.getCreateTime());
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
