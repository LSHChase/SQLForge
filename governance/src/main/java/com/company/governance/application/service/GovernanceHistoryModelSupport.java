package com.company.governance.application.service;

import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

abstract class GovernanceHistoryModelSupport {

    protected static final Logger LOGGER = LoggerFactory.getLogger(GovernanceHistoryApplicationService.class);
    protected static final String SYSTEM_TENANT_ID = "system";
    protected static final String BATCH_OPERATION_RETENTION = "EXECUTE_RETENTION_BATCH";
    protected static final String BATCH_OPERATION_RECOVERY = "RECOVER_ARTIFACT_BATCH";
    protected static final String ITEM_OPERATION_CLEANUP = "CLEANUP_ARTIFACT";
    protected static final String ITEM_OPERATION_RECOVER = "RECOVER_ARTIFACT";
    protected static final String BATCH_DEFAULT_RETENTION_SCOPE = "MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER";
    protected static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";
    protected static final String DEFAULT_EXPORT_STATUS = "GENERATED";
    protected static final String DEFAULT_EXPORT_STORAGE_TYPE = "INLINE_RESPONSE";
    protected static final String OPERATION_QUERY_HISTORY_EXPORT = "QUERY_HISTORY_EXPORT";
    protected static final String TARGET_QUERY_HISTORY = "QUERY_HISTORY";
    protected static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    protected static final String REWRITE_RECORD_AGGREGATION_STAGE = "QUERY_HISTORY_REWRITE_RECORD_AGGREGATION";
    protected static final int DEFAULT_LIMIT = 12;
    protected static final int MAX_LIMIT = 50;
    protected static final int DEFAULT_PAGE_NO = 1;
    protected static final int RECENT_SOURCE_SCAN_MULTIPLIER = 5;
    protected static final int MAX_RECENT_SOURCE_SCAN_LIMIT = 200;
    protected static final int LOOKUP_SOURCE_SCAN_MULTIPLIER = 8;
    protected static final int MIN_LOOKUP_SOURCE_SCAN_LIMIT = 120;
    protected static final int MAX_LOOKUP_SOURCE_SCAN_LIMIT = 400;
    protected static final int LOOKUP_PAGE_FETCH_OVERFLOW = 1;
    protected static final int TRACE_LOOKUP_SOURCE_LIMIT = 100;
    protected static final String LOOKUP_TYPE_TASK = "TASK";
    protected static final String LOOKUP_TYPE_REPORT = "REPORT";
    protected static final List<String> TASK_TARGET_TYPES = Collections.unmodifiableList(
        java.util.Arrays.asList("TASK", "SQL_OPTIMIZATION_TASK", "BENCHMARK_ENGINE_TASK")
    );
    protected static final List<String> REPORT_TARGET_TYPES = Collections.unmodifiableList(
        java.util.Arrays.asList("REPORT", "BENCHMARK_ENGINE_REPORT")
    );
    protected static final List<String> ALLOWED_HISTORY_TYPES = Collections.unmodifiableList(
        java.util.Arrays.asList(
            "QUERY_EXECUTION",
            "SQL_PARSE",
            "ACCELERATION_PLAN",
            "BENCHMARK_REPORT_EXPORT",
            "SQL_OPTIMIZATION",
            "BENCHMARK"
        )
    );
    protected static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() {
        };

    protected static Map<String, Object> parseJsonObject(String content) {
        return GovernanceHistoryApplicationServiceSupport.parseJsonObject(content);
    }

    protected static List<LogicalObjectSurface> normalizeLogicalObjectHits(String logicalObjectHitsJson) {
        return GovernanceHistoryApplicationServiceSupport.normalizeLogicalObjectHits(logicalObjectHitsJson);
    }

    protected static String readText(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    protected static Boolean readBoolean(Map<String, Object> payload, String key) {
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

    protected static Map<String, Object> readNestedMap(Map<String, Object> payload, String key) {
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

    protected static Map<String, Object> readEvidenceMap(Object value) {
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

    protected static Map<String, Object> buildCompensationReplayEvidence(Map<String, Object> queryContext) {
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

    protected static Map<String, Object> buildCacheGovernanceSurface(Map<String, Object> responseSummary,
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

    protected static Object normalizeCacheGovernanceEvidence(Object value) {
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

    protected static Map<String, Object> buildArtifactStorageContract(Map<String, Object> responseSummary,
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

    protected static Map<String, Object> buildArtifactRecoverySurface(Map<String, Object> responseSummary,
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

    protected static Map<String, Object> buildArtifactOperationSurface(Map<String, Object> responseSummary) {
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

    protected static Map<String, Object> mergeEvidenceMaps(Map<String, Object> first, Map<String, Object> second) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<String, Object>();
        if (first != null && !first.isEmpty()) {
            merged.putAll(first);
        }
        if (second != null && !second.isEmpty()) {
            merged.putAll(second);
        }
        return merged;
    }

    protected static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    protected static Boolean normalizeRewriteApplied(Boolean rewriteApplied) {
        return Boolean.valueOf(Boolean.TRUE.equals(rewriteApplied));
    }

    protected static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

    protected static String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(first, firstNonBlank(second, third));
    }

    protected static String firstNonBlank(String first, String second, String third, String fourth) {
        return firstNonBlank(first, firstNonBlank(second, third, fourth));
    }

    protected static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || !StringUtils.hasText(key) || value == null) {
            return;
        }
        target.put(key, value);
    }

    protected static class TraceAggregate {

        protected final String traceId;
        protected String requestId;
        protected String serviceCode;
        protected String operationType;
        protected String resourceType;
        protected String resourceId;
        protected String latestStatus;
        protected LocalDateTime lastSeenAt;
        protected int auditEventCount;
        protected int nonSuccessEventCount;
        protected int queryHistoryCount;
        protected int exportRecordCount;
        protected String taskId;
        protected String reportId;
        protected String sqlFingerprint;
        protected String errorCode;
        protected String targetEngine;
        protected Boolean degraded;
        protected Map<String, Object> compensationReplayEvidence;
        protected Map<String, Object> cacheGovernanceSurface;
        protected Map<String, Object> artifactStorageContract;
        protected Map<String, Object> artifactRecoverySurface;
        protected Map<String, Object> artifactOperationSurface;
        protected boolean summaryUsesBusinessAudit;
        protected boolean hasBusinessAudit;
        protected final List<GovernanceTraceDetailVO.AuditEventVO> auditEvents;
        protected final List<GovernanceTraceDetailVO.QueryHistoryVO> queryHistories;
        protected final List<GovernanceTraceDetailVO.ExportRecordVO> exportRecords;

        protected TraceAggregate(String traceId) {
            this.traceId = traceId;
            this.auditEvents = new ArrayList<GovernanceTraceDetailVO.AuditEventVO>();
            this.queryHistories = new ArrayList<GovernanceTraceDetailVO.QueryHistoryVO>();
            this.exportRecords = new ArrayList<GovernanceTraceDetailVO.ExportRecordVO>();
        }

        protected void applyAudit(AuditLogRecord record, Map<String, Object> requestParams, Map<String, Object> responseSummary) {
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

        protected void applyQueryHistory(QueryHistoryRecord record) {
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

        protected void applyExportRecord(ExportRecord record) {
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

        protected boolean shouldRefreshAuditSummary(AuditLogRecord record, LocalDateTime candidateTime) {
            boolean candidateBusinessAudit = isBusinessAudit(record);
            if (candidateBusinessAudit && !this.summaryUsesBusinessAudit) {
                return true;
            }
            if (!candidateBusinessAudit && this.summaryUsesBusinessAudit) {
                return false;
            }
            return shouldRefreshSummary(candidateTime);
        }

        protected boolean shouldRefreshSummary(LocalDateTime candidateTime) {
            if (candidateTime == null) {
                return !StringUtils.hasText(this.latestStatus);
            }
            return this.lastSeenAt == null || candidateTime.compareTo(this.lastSeenAt) >= 0;
        }

        protected String inferTaskId(AuditLogRecord record) {
            if (record == null) {
                return null;
            }
            if ("TASK".equalsIgnoreCase(record.getTargetType())) {
                return record.getTargetId();
            }
            return null;
        }

        protected boolean shouldDisplayInRecentList() {
            return this.hasBusinessAudit || this.queryHistoryCount > 0 || this.exportRecordCount > 0;
        }

        protected boolean isBusinessAudit(AuditLogRecord record) {
            return record != null
                && StringUtils.hasText(record.getServiceCode())
                && !"GOVERNANCE".equalsIgnoreCase(record.getServiceCode());
        }

        protected String toDisplayGroupKey() {
            String businessKey = firstNonBlank(this.taskId, this.reportId, this.resourceId, this.sqlFingerprint, this.traceId);
            return firstNonBlank(this.serviceCode, "TRACE") + "::" + businessKey;
        }

        protected boolean matchesLookupCriteria(String candidateTraceId, String candidateTaskId, String candidateReportId) {
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

        protected String inferReportId(AuditLogRecord record) {
            if (record == null) {
                return null;
            }
            if ("REPORT".equalsIgnoreCase(record.getTargetType())) {
                return record.getTargetId();
            }
            return null;
        }

        protected GovernanceTraceSummaryVO toSummaryVO() {
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

        protected GovernanceTraceDetailVO toDetailVO() {
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

        protected LocalDateTime getLastSeenAt() {
            return lastSeenAt;
        }

        protected int getAuditEventCount() {
            return auditEventCount;
        }

        protected int getQueryHistoryCount() {
            return queryHistoryCount;
        }

        protected int getExportRecordCount() {
            return exportRecordCount;
        }

        protected static <T> T firstNonNull(T first, T second) {
            return first != null ? first : second;
        }

        protected static Map<String, Object> selectEvidenceMap(Map<String, Object> current,
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

        protected static String firstNonBlank(String first, String second) {
            if (StringUtils.hasText(first)) {
                return first.trim();
            }
            if (StringUtils.hasText(second)) {
                return second.trim();
            }
            return null;
        }

        protected static String firstNonBlank(String first, String second, String third) {
            return firstNonBlank(first, firstNonBlank(second, third));
        }

        protected static String firstNonBlank(String first, String second, String third, String fourth) {
            return firstNonBlank(first, firstNonBlank(second, third, fourth));
        }

        protected static String firstNonBlank(String first, String second, String third, String fourth, String fifth) {
            return firstNonBlank(first, firstNonBlank(second, third, fourth, fifth));
        }
    }

    protected static class RewriteHistoryScope {

        protected final List<String> includeHistoryIds;
        protected final List<String> excludeHistoryIds;

        protected RewriteHistoryScope(List<String> includeHistoryIds, List<String> excludeHistoryIds) {
            this.includeHistoryIds = includeHistoryIds;
            this.excludeHistoryIds = excludeHistoryIds;
        }

        protected static RewriteHistoryScope unfiltered() {
            return new RewriteHistoryScope(null, null);
        }

        protected static RewriteHistoryScope include(List<String> historyIds) {
            return new RewriteHistoryScope(historyIds == null ? Collections.<String>emptyList() : historyIds, null);
        }

        protected static RewriteHistoryScope exclude(List<String> historyIds) {
            return new RewriteHistoryScope(null, historyIds == null ? Collections.<String>emptyList() : historyIds);
        }

        protected boolean isEmptyInclude() {
            return includeHistoryIds != null && includeHistoryIds.isEmpty();
        }
    }

    protected static class LookupCursor {

        protected final LocalDateTime createdAt;
        protected final Long auditId;

        protected LookupCursor(LocalDateTime createdAt, Long auditId) {
            this.createdAt = createdAt;
            this.auditId = auditId;
        }

        protected LocalDateTime getCreatedAt() {
            return createdAt;
        }

        protected Long getAuditId() {
            return auditId;
        }
    }
}
