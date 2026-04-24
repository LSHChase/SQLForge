package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactTraceResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkGovernanceTraceService {

    private static final String CONFIG_SNAPSHOT_PREFIX = "cfg-benchmark-";
    private static final String RESULT_PREFIX = "result-benchmark-";
    private static final String HISTORY_PREFIX = "history-benchmark-";
    private static final String EXPORT_PREFIX = "export-benchmark-";
    private static final String SAGA_PREFIX = "benchmark-report-";

    private final GovernanceCapabilityClient governanceCapabilityClient;

    public BenchmarkGovernanceTraceService(GovernanceCapabilityClient governanceCapabilityClient) {
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public List<BenchmarkReportArtifact> registerTrace(BenchmarkTask task,
                                                       BenchmarkReport report,
                                                       BenchmarkReportResponse reportResponse,
                                                       BenchmarkReportRawDataResponse rawDataResponse,
                                                       List<BenchmarkReportArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Collections.emptyList();
        }
        GovernanceBenchmarkReportTraceRequest request = new GovernanceBenchmarkReportTraceRequest();
        request.setReportId(report.getReportId());
        request.setTaskId(task.getTaskId());
        request.setTaskType(task.getTaskType().name());
        request.setSqlFingerprint(report.getSqlFingerprint());
        request.setSqlText(task.getSqlText());
        request.setResultStatus(task.getStatus().name());
        request.setReadonlyRequired(String.valueOf(task.getReadonlyRequired()));
        request.setShadowEnvironmentMode(task.getShadowEnvironmentMode() == null ? null : task.getShadowEnvironmentMode().name());
        request.setDesensitizationRequirement(
            task.getDesensitizationRequirement() == null ? null : task.getDesensitizationRequirement().name()
        );
        request.setGeneratedAt(String.valueOf(report.getGeneratedAt()));
        request.setStartedAt(String.valueOf(task.getStartedAt()));
        request.setFinishedAt(String.valueOf(task.getFinishedAt()));
        request.setReportQueryPath(reportResponse.getReportQueryPath());
        request.setRawDataDownloadPath(reportResponse.getRawDataDownloadPath());
        request.setWorkloadDigest(report.getExecutionSummary() == null ? null : report.getExecutionSummary().getWorkloadDigest());
        request.setWorkloadSource(resolvePhaseNoteValue(report.getExecutionSummary(), "workloadSource="));
        request.setBackfillApplied(parseBoolean(resolvePhaseNoteValue(report.getExecutionSummary(), "backfillApplied=")));
        request.setWorkloadEvidenceJson(buildWorkloadEvidenceJson(report.getExecutionSummary()));
        request.setExecutionSummaryJson(JsonUtils.toJson(report.getExecutionSummary()));
        request.setTargetEngines(stringifyEngines(reportResponse.getTargetEngines()));
        request.setArtifacts(toTraceArtifacts(artifacts));

        GovernanceBenchmarkReportTraceResponse response = governanceCapabilityClient.writeBenchmarkReportTrace(request);
        if (response == null || response.getArtifacts() == null || response.getArtifacts().isEmpty()) {
            return artifacts;
        }
        Map<String, String> exportIdByArtifactKey = new LinkedHashMap<String, String>();
        for (GovernanceBenchmarkArtifactTraceResponse artifact : response.getArtifacts()) {
            exportIdByArtifactKey.put(artifact.getArtifactKey(), artifact.getExportId());
        }
        List<BenchmarkReportArtifact> enriched = new ArrayList<BenchmarkReportArtifact>(artifacts.size());
        for (BenchmarkReportArtifact artifact : artifacts) {
            String exportId = exportIdByArtifactKey.get(artifact.getArtifactKey());
            enriched.add(exportId == null ? artifact : artifact.withExportId(exportId));
        }
        return enriched;
    }

    public String resolveConfigSnapshotId(String reportId, boolean governanceTraceAvailable) {
        return governanceTraceAvailable ? CONFIG_SNAPSHOT_PREFIX + sanitizeKey(reportId) : null;
    }

    public String resolveResultId(String reportId, boolean governanceTraceAvailable) {
        return governanceTraceAvailable ? RESULT_PREFIX + sanitizeKey(reportId) : null;
    }

    public String resolveHistoryId(String reportId, boolean governanceTraceAvailable) {
        return governanceTraceAvailable ? HISTORY_PREFIX + sanitizeKey(reportId) : null;
    }

    public String resolveExportId(String reportId, String artifactKey, boolean governanceTraceAvailable) {
        if (!governanceTraceAvailable) {
            return null;
        }
        return EXPORT_PREFIX + sanitizeKey(reportId) + "-" + sanitizeKey(artifactKey);
    }

    public String resolveSagaId(String taskId, boolean governanceTraceAvailable) {
        if (!governanceTraceAvailable || !StringUtils.hasText(taskId)) {
            return null;
        }
        return SAGA_PREFIX + taskId;
    }

    private List<GovernanceBenchmarkArtifactTraceRequest> toTraceArtifacts(List<BenchmarkReportArtifact> artifacts) {
        List<GovernanceBenchmarkArtifactTraceRequest> traceArtifacts =
            new ArrayList<GovernanceBenchmarkArtifactTraceRequest>(artifacts.size());
        for (BenchmarkReportArtifact artifact : artifacts) {
            GovernanceBenchmarkArtifactTraceRequest traceArtifact = new GovernanceBenchmarkArtifactTraceRequest();
            traceArtifact.setArtifactKey(artifact.getArtifactKey());
            traceArtifact.setArtifactKind(artifact.getArtifactKind().name());
            traceArtifact.setExportFormat(artifact.getFormat().name());
            traceArtifact.setMediaType(artifact.getMediaType());
            traceArtifact.setFileName(artifact.getFileName());
            traceArtifact.setContentLength(artifact.getContentLength());
            traceArtifact.setChecksumSha256(artifact.getChecksumSha256());
            traceArtifact.setStorageType(artifact.getStorageType());
            traceArtifact.setStorageUri(artifact.getStorageUri());
            traceArtifact.setStorageEvidence(artifact.getStorageEvidence());
            traceArtifact.setRetentionDays(artifact.getRetentionDays());
            traceArtifact.setRetentionPolicySource(artifact.getRetentionPolicySource());
            traceArtifact.setRetentionDeleteAfter(artifact.getRetentionDeleteAfter());
            traceArtifacts.add(traceArtifact);
        }
        return traceArtifacts;
    }

    private String buildWorkloadEvidenceJson(BenchmarkExecutionSummary executionSummary) {
        if (executionSummary == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("executionMode", executionSummary.getExecutionMode());
        payload.put("isolationSummary", executionSummary.getIsolationSummary());
        payload.put("sampleCount", executionSummary.getSampleCount());
        payload.put("executionDurationMs", executionSummary.getExecutionDurationMs());
        payload.put("workloadDigest", executionSummary.getWorkloadDigest());
        payload.put("phaseNotes", executionSummary.getPhaseNotes());
        Map<String, Object> queryExecution = new LinkedHashMap<String, Object>();
        Map<String, Object> engineEvidence = new LinkedHashMap<String, Object>();
        if (executionSummary.getPhaseNotes() != null) {
            for (String note : executionSummary.getPhaseNotes()) {
                if (!StringUtils.hasText(note)) {
                    continue;
                }
                if (note.startsWith("queryExecutionWorkloadSource=")) {
                    queryExecution.put("workloadSource", valueAfterEquals(note));
                    continue;
                }
                if (note.startsWith("queryExecutionImplementationStage=")) {
                    queryExecution.put("implementationStage", valueAfterEquals(note));
                    continue;
                }
                if (note.startsWith("queryExecutionCompensationApplied=")) {
                    queryExecution.put("compensationApplied", coerceScalar(valueAfterEquals(note)));
                    continue;
                }
                if (note.startsWith("queryExecutionCompensationStrategy=")) {
                    queryExecution.put("compensationStrategy", valueAfterEquals(note));
                    continue;
                }
                if (note.startsWith("queryExecution[") && note.contains("]=")) {
                    int engineStart = "queryExecution[".length();
                    int engineEnd = note.indexOf("]=");
                    if (engineEnd > engineStart) {
                        String engine = note.substring(engineStart, engineEnd);
                        String detail = note.substring(engineEnd + 2);
                        engineEvidence.put(engine, parseCommaSeparatedValues(detail));
                    }
                    continue;
                }
                if (note.startsWith("workloadOrchestration=")) {
                    payload.put("orchestration", parseCommaSeparatedValues(note));
                }
            }
        }
        if (!engineEvidence.isEmpty()) {
            queryExecution.put("engines", engineEvidence);
        }
        if (!queryExecution.isEmpty()) {
            payload.put("queryExecution", queryExecution);
        }
        return JsonUtils.toJson(payload);
    }

    private List<String> stringifyEngines(List<?> engines) {
        if (engines == null || engines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<String>(engines.size());
        for (Object engine : engines) {
            values.add(String.valueOf(engine));
        }
        return values;
    }

    private String resolvePhaseNoteValue(BenchmarkExecutionSummary executionSummary, String prefix) {
        if (executionSummary == null || executionSummary.getPhaseNotes() == null || !StringUtils.hasText(prefix)) {
            return null;
        }
        for (String note : executionSummary.getPhaseNotes()) {
            if (StringUtils.hasText(note) && note.startsWith(prefix)) {
                return note.substring(prefix.length());
            }
        }
        return null;
    }

    private Boolean parseBoolean(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return Boolean.valueOf(Boolean.parseBoolean(rawValue.trim()));
    }

    private Map<String, Object> parseCommaSeparatedValues(String rawValue) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        if (!StringUtils.hasText(rawValue)) {
            return values;
        }
        String[] parts = rawValue.split(",");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator <= 0) {
                values.put(part.trim(), Boolean.TRUE);
                continue;
            }
            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();
            values.put(key, coerceScalar(value));
        }
        return values;
    }

    private Object coerceScalar(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.valueOf(Boolean.parseBoolean(value));
        }
        if (value.matches("-?\\d+")) {
            try {
                return Long.valueOf(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                return value;
            }
        }
        return value;
    }

    private String valueAfterEquals(String rawValue) {
        int separator = rawValue == null ? -1 : rawValue.indexOf('=');
        return separator < 0 ? rawValue : rawValue.substring(separator + 1);
    }

    private String sanitizeKey(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "unknown";
        }
        return rawValue.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
