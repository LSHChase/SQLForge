package com.company.governance.application.service;

import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactTraceResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceBenchmarkTraceabilityApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "DATABASE_TRACE_EXPORT_ORCHESTRATION_BASELINE";
    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;

    public GovernanceBenchmarkTraceabilityApplicationService(
        GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
        ConfigSnapshotMapper configSnapshotMapper,
        ExecutionResultMapper executionResultMapper,
        QueryHistoryMapper queryHistoryMapper,
        ExportRecordMapper exportRecordMapper
    ) {
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
        this.configSnapshotMapper = configSnapshotMapper;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
    }

    public GovernanceBenchmarkReportTraceResponse writeBenchmarkReportTrace(GovernanceBenchmarkReportTraceRequest request) {
        if (request == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "request 不能为 null"
            );
        }
        String tenantId = requireContext("tenantId", RequestContext.getTenantId());
        String userId = requireContext("userId", RequestContext.getUserId());
        String requestId = requireContext("requestId", RequestContext.getRequestId());
        String traceId = requireContext("traceId", RequestContext.getTraceId());
        requireText(request == null ? null : request.getReportId(), "reportId");
        requireText(request == null ? null : request.getTaskId(), "taskId");
        requireText(request == null ? null : request.getTaskType(), "taskType");
        requireText(request == null ? null : request.getSqlFingerprint(), "sqlFingerprint");
        requireText(request == null ? null : request.getResultStatus(), "resultStatus");

        String sagaId = hasText(request.getTaskId()) ? "benchmark-report-" + request.getTaskId() : traceId;
        String configSnapshotId = "cfg-benchmark-" + sanitizeKey(request.getReportId());
        String resultId = "result-benchmark-" + sanitizeKey(request.getReportId());
        String historyId = "history-benchmark-" + sanitizeKey(request.getReportId());

        persistConfigSnapshotIfMissing(request, tenantId, userId, requestId, traceId, sagaId, configSnapshotId);
        persistExecutionResultIfMissing(request, tenantId, requestId, traceId, sagaId, configSnapshotId, resultId);
        persistQueryHistoryIfMissing(request, tenantId, userId, requestId, traceId, sagaId, historyId, resultId);

        List<GovernanceBenchmarkArtifactTraceResponse> artifactResponses =
            persistExportRecordsIfMissing(request, tenantId, userId, requestId, traceId, sagaId, historyId, resultId);

        GovernanceBenchmarkReportTraceResponse response = new GovernanceBenchmarkReportTraceResponse();
        response.setConfigSnapshotId(configSnapshotId);
        response.setResultId(resultId);
        response.setHistoryId(historyId);
        response.setTraceId(traceId);
        response.setRequestId(requestId);
        response.setSagaId(sagaId);
        response.setArtifacts(artifactResponses);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private void persistConfigSnapshotIfMissing(GovernanceBenchmarkReportTraceRequest request,
                                                String tenantId,
                                                String userId,
                                                String requestId,
                                                String traceId,
                                                String sagaId,
                                                String configSnapshotId) {
        if (configSnapshotMapper.selectById(configSnapshotId) != null) {
            return;
        }
        ConfigSnapshotRecord record = new ConfigSnapshotRecord();
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.BENCHMARK_ENGINE);
        record.setSourceConfigType("BENCHMARK_REPORT_EXPORT");
        record.setSourceConfigId(request.getReportId());
        record.setSourceVersion(request.getGeneratedAt());
        record.setSnapshotStatus("CAPTURED");
        record.setSnapshotReason("压测报告产物编排基线");
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setSnapshotPayload(buildSnapshotPayload(request));
        record.setCreatedBy(userId);
        record.setCreateTime(parseDateTime(request.getGeneratedAt(), Instant.now()));
        governanceProtectedPersistenceService.saveConfigSnapshot(record);
    }

    private void persistExecutionResultIfMissing(GovernanceBenchmarkReportTraceRequest request,
                                                 String tenantId,
                                                 String requestId,
                                                 String traceId,
                                                 String sagaId,
                                                 String configSnapshotId,
                                                 String resultId) {
        if (executionResultMapper.selectById(resultId) != null) {
            return;
        }
        ExecutionResultRecord record = new ExecutionResultRecord();
        record.setResultId(resultId);
        record.setConfigSnapshotId(configSnapshotId);
        record.setTenantId(tenantId);
        record.setServiceCode(ServiceCodeConstants.BENCHMARK_ENGINE);
        record.setTaskId(request.getTaskId());
        record.setTaskType(request.getTaskType());
        record.setResultStatus(request.getResultStatus());
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setResultSummary(buildResultSummary(request));
        record.setResultPayload(buildResultPayload(request));
        record.setStartedAt(parseDateTime(request.getStartedAt(), Instant.now()));
        record.setFinishedAt(parseDateTime(request.getFinishedAt(), Instant.now()));
        record.setCreateTime(parseDateTime(request.getGeneratedAt(), Instant.now()));
        governanceProtectedPersistenceService.saveExecutionResult(record);
    }

    private void persistQueryHistoryIfMissing(GovernanceBenchmarkReportTraceRequest request,
                                              String tenantId,
                                              String userId,
                                              String requestId,
                                              String traceId,
                                              String sagaId,
                                              String historyId,
                                              String resultId) {
        if (queryHistoryMapper.selectById(historyId) != null) {
            return;
        }
        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setHistoryId(historyId);
        record.setResultId(resultId);
        record.setTenantId(tenantId);
        record.setHistoryType("BENCHMARK_REPORT_EXPORT");
        record.setSqlFingerprint(request.getSqlFingerprint());
        record.setDatasourceType(firstTargetEngine(request.getTargetEngines()));
        record.setTraceId(traceId);
        record.setRequestId(requestId);
        record.setSagaId(sagaId);
        record.setQueryContext(buildQueryContext(request));
        record.setSubmittedBy(userId);
        record.setSubmittedAt(parseDateTime(request.getGeneratedAt(), Instant.now()));
        record.setCreateTime(parseDateTime(request.getGeneratedAt(), Instant.now()));
        governanceProtectedPersistenceService.saveQueryHistoryWithSqlText(record, request.getSqlText());
    }

    private List<GovernanceBenchmarkArtifactTraceResponse> persistExportRecordsIfMissing(
        GovernanceBenchmarkReportTraceRequest request,
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String sagaId,
        String historyId,
        String resultId
    ) {
        List<GovernanceBenchmarkArtifactTraceRequest> artifacts = request.getArtifacts();
        if (artifacts == null || artifacts.isEmpty()) {
            return new ArrayList<GovernanceBenchmarkArtifactTraceResponse>();
        }
        List<GovernanceBenchmarkArtifactTraceResponse> responses =
            new ArrayList<GovernanceBenchmarkArtifactTraceResponse>(artifacts.size());
        for (GovernanceBenchmarkArtifactTraceRequest artifact : artifacts) {
            String artifactKey = requireText(artifact.getArtifactKey(), "artifactKey");
            String exportId = "export-benchmark-" + sanitizeKey(request.getReportId()) + "-" + sanitizeKey(artifactKey);
            if (exportRecordMapper.selectById(exportId) == null) {
                ExportRecord record = new ExportRecord();
                record.setExportId(exportId);
                record.setHistoryId(historyId);
                record.setResultId(resultId);
                record.setTenantId(tenantId);
                record.setExportFormat(requireText(artifact.getExportFormat(), "exportFormat"));
                record.setExportStatus("AVAILABLE");
                record.setTraceId(traceId);
                record.setRequestId(requestId);
                record.setSagaId(sagaId);
                record.setStorageType(requireText(artifact.getStorageType(), "storageType"));
                record.setStorageUri(requireText(artifact.getStorageUri(), "storageUri"));
                record.setChecksum(artifact.getChecksumSha256());
                record.setExportOptions(buildExportOptions(request.getReportId(), artifact));
                record.setCreatedBy(userId);
                record.setCreateTime(parseDateTime(request.getGeneratedAt(), Instant.now()));
                record.setFinishedAt(parseDateTime(request.getFinishedAt(), Instant.now()));
                governanceProtectedPersistenceService.saveExportRecord(record);
            }
            GovernanceBenchmarkArtifactTraceResponse response = new GovernanceBenchmarkArtifactTraceResponse();
            response.setArtifactKey(artifactKey);
            response.setExportId(exportId);
            response.setExportStatus("AVAILABLE");
            responses.add(response);
        }
        return responses;
    }

    private String buildSnapshotPayload(GovernanceBenchmarkReportTraceRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("reportId", request.getReportId());
        payload.put("taskId", request.getTaskId());
        payload.put("taskType", request.getTaskType());
        payload.put("readonlyRequired", request.getReadonlyRequired());
        payload.put("shadowEnvironmentMode", request.getShadowEnvironmentMode());
        payload.put("desensitizationRequirement", request.getDesensitizationRequirement());
        payload.put("targetEngines", request.getTargetEngines());
        payload.put("workloadDigest", request.getWorkloadDigest());
        payload.put("workloadSource", request.getWorkloadSource());
        payload.put("backfillApplied", request.getBackfillApplied());
        payload.put("workloadEvidence", parseJsonValue(request.getWorkloadEvidenceJson()));
        payload.put("executionSummary", parseJsonValue(request.getExecutionSummaryJson()));
        payload.put("artifactStorage", buildArtifactStorageSummaries(request.getArtifacts()));
        return JsonUtils.toJson(payload);
    }

    private String buildResultSummary(GovernanceBenchmarkReportTraceRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("reportId", request.getReportId());
        payload.put("resultStatus", request.getResultStatus());
        payload.put("artifactCount", Integer.valueOf(request.getArtifacts() == null ? 0 : request.getArtifacts().size()));
        payload.put("generatedAt", request.getGeneratedAt());
        payload.put("workloadDigest", request.getWorkloadDigest());
        payload.put("workloadSource", request.getWorkloadSource());
        payload.put("backfillApplied", request.getBackfillApplied());
        payload.put("objectStorageVerification", buildObjectStorageVerificationSummary(request.getArtifacts()));
        return JsonUtils.toJson(payload);
    }

    private String buildResultPayload(GovernanceBenchmarkReportTraceRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("reportQueryPath", request.getReportQueryPath());
        payload.put("rawDataDownloadPath", request.getRawDataDownloadPath());
        payload.put("targetEngines", request.getTargetEngines());
        payload.put("artifacts", buildArtifactStorageSummaries(request.getArtifacts()));
        payload.put("workloadEvidence", parseJsonValue(request.getWorkloadEvidenceJson()));
        payload.put("executionSummary", parseJsonValue(request.getExecutionSummaryJson()));
        return JsonUtils.toJson(payload);
    }

    private String buildQueryContext(GovernanceBenchmarkReportTraceRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("reportId", request.getReportId());
        payload.put("reportQueryPath", request.getReportQueryPath());
        payload.put("rawDataDownloadPath", request.getRawDataDownloadPath());
        payload.put("targetEngines", request.getTargetEngines());
        payload.put("workloadSource", request.getWorkloadSource());
        payload.put("backfillApplied", request.getBackfillApplied());
        payload.put("workloadEvidence", parseJsonValue(request.getWorkloadEvidenceJson()));
        return JsonUtils.toJson(payload);
    }

    private String buildExportOptions(String reportId, GovernanceBenchmarkArtifactTraceRequest artifact) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("reportId", reportId);
        payload.put("artifactKey", artifact.getArtifactKey());
        payload.put("artifactKind", artifact.getArtifactKind());
        payload.put("mediaType", artifact.getMediaType());
        payload.put("fileName", artifact.getFileName());
        payload.put("contentLength", artifact.getContentLength());
        payload.put("storageType", artifact.getStorageType());
        payload.put("storageUri", artifact.getStorageUri());
        payload.put("storageEvidence", parseEvidenceString(artifact.getStorageEvidence()));
        payload.put("retentionDays", artifact.getRetentionDays());
        payload.put("retentionPolicySource", artifact.getRetentionPolicySource());
        payload.put("retentionDeleteAfter", artifact.getRetentionDeleteAfter());
        return JsonUtils.toJson(payload);
    }

    private List<Map<String, Object>> buildArtifactStorageSummaries(List<GovernanceBenchmarkArtifactTraceRequest> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> summaries = new ArrayList<Map<String, Object>>(artifacts.size());
        for (GovernanceBenchmarkArtifactTraceRequest artifact : artifacts) {
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("artifactKey", artifact.getArtifactKey());
            summary.put("artifactKind", artifact.getArtifactKind());
            summary.put("exportFormat", artifact.getExportFormat());
            summary.put("storageType", artifact.getStorageType());
            summary.put("storageUri", artifact.getStorageUri());
            summary.put("storageEvidence", parseEvidenceString(artifact.getStorageEvidence()));
            summary.put("retentionDays", artifact.getRetentionDays());
            summary.put("retentionPolicySource", artifact.getRetentionPolicySource());
            summary.put("retentionDeleteAfter", artifact.getRetentionDeleteAfter());
            summaries.add(summary);
        }
        return summaries;
    }

    private Map<String, Object> buildObjectStorageVerificationSummary(List<GovernanceBenchmarkArtifactTraceRequest> artifacts) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        if (artifacts == null || artifacts.isEmpty()) {
            summary.put("environmentBackedArtifactCount", Integer.valueOf(0));
            summary.put("providerLiveEvidenceVerifiedCount", Integer.valueOf(0));
            summary.put("externalWriteVerifiedCount", Integer.valueOf(0));
            summary.put("recoveryVerificationCount", Integer.valueOf(0));
            return summary;
        }
        int environmentBackedArtifactCount = 0;
        int providerLiveEvidenceVerifiedCount = 0;
        int externalWriteVerifiedCount = 0;
        int recoveryVerificationCount = 0;
        List<Map<String, Object>> artifactStatuses = new ArrayList<Map<String, Object>>();
        for (GovernanceBenchmarkArtifactTraceRequest artifact : artifacts) {
            if (!"ENVIRONMENT_OBJECT_STORAGE".equals(artifact.getStorageType())) {
                continue;
            }
            environmentBackedArtifactCount++;
            Map<String, String> evidence = parseEvidenceString(artifact.getStorageEvidence());
            if ("VERIFIED".equals(evidence.get("providerWriteStatus"))
                && "VERIFIED".equals(evidence.get("providerRecoveryStatus"))) {
                providerLiveEvidenceVerifiedCount++;
            }
            if ("VERIFIED".equals(evidence.get("externalWriteStatus"))) {
                externalWriteVerifiedCount++;
            }
            if ("VERIFIED".equals(evidence.get("recoveryVerificationStatus"))) {
                recoveryVerificationCount++;
            }
            Map<String, Object> artifactStatus = new LinkedHashMap<String, Object>();
            artifactStatus.put("artifactKey", artifact.getArtifactKey());
            artifactStatus.put("providerWriteStatus", evidence.get("providerWriteStatus"));
            artifactStatus.put("providerRecoveryStatus", evidence.get("providerRecoveryStatus"));
            artifactStatus.put("externalWriteStatus", evidence.get("externalWriteStatus"));
            artifactStatus.put("recoveryVerificationStatus", evidence.get("recoveryVerificationStatus"));
            artifactStatus.put("mode", evidence.get("mode"));
            artifactStatuses.add(artifactStatus);
        }
        summary.put("environmentBackedArtifactCount", Integer.valueOf(environmentBackedArtifactCount));
        summary.put("providerLiveEvidenceVerifiedCount", Integer.valueOf(providerLiveEvidenceVerifiedCount));
        summary.put("externalWriteVerifiedCount", Integer.valueOf(externalWriteVerifiedCount));
        summary.put("recoveryVerificationCount", Integer.valueOf(recoveryVerificationCount));
        if (!artifactStatuses.isEmpty()) {
            summary.put("artifacts", artifactStatuses);
        }
        return summary;
    }

    private Object parseJsonValue(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }
        try {
            return JsonUtils.objectMapper().readValue(rawJson, Object.class);
        } catch (IOException ex) {
            return rawJson;
        }
    }

    private Map<String, String> parseEvidenceString(String storageEvidence) {
        if (!StringUtils.hasText(storageEvidence)) {
            return Collections.emptyMap();
        }
        Map<String, String> evidence = new LinkedHashMap<String, String>();
        String[] parts = storageEvidence.split(";");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator <= 0) {
                evidence.put(part.trim(), "true");
                continue;
            }
            evidence.put(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
        }
        return evidence;
    }

    private LocalDateTime parseDateTime(String rawValue, Instant fallback) {
        Instant instant = fallback;
        if (StringUtils.hasText(rawValue) && !"null".equalsIgnoreCase(rawValue.trim())) {
            instant = Instant.parse(rawValue.trim());
        }
        return LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private String firstTargetEngine(List<String> targetEngines) {
        if (targetEngines == null || targetEngines.isEmpty()) {
            return "HETU";
        }
        return targetEngines.get(0);
    }

    private String requireContext(String fieldName, String value) {
        return requireText(value, fieldName);
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private String sanitizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9]+", "-");
    }
}
