package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
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
            traceArtifacts.add(traceArtifact);
        }
        return traceArtifacts;
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

    private String sanitizeKey(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "unknown";
        }
        return rawValue.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
