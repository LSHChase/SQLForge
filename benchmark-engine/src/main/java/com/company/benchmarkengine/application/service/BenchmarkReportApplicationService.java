package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkReportApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReportApplicationService.class);

    private static final String QUERY_OPERATION = "BENCHMARK_REPORT_QUERY";
    private static final String RESOURCE_TYPE_REPORT = "BENCHMARK_ENGINE_REPORT";

    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkReportExportService benchmarkReportExportService;
    private final BenchmarkArtifactStorageService benchmarkArtifactStorageService;
    private final BenchmarkGovernanceTraceService benchmarkGovernanceTraceService;
    private final BenchmarkTaskRepository benchmarkTaskRepository;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final BenchmarkMetricsRecorder benchmarkMetricsRecorder;

    public BenchmarkReportApplicationService(BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                                             BenchmarkReportExportService benchmarkReportExportService,
                                             BenchmarkArtifactStorageService benchmarkArtifactStorageService,
                                             BenchmarkGovernanceTraceService benchmarkGovernanceTraceService,
                                             BenchmarkTaskRepository benchmarkTaskRepository,
                                             GovernanceCapabilityClient governanceCapabilityClient,
                                             BenchmarkMetricsRecorder benchmarkMetricsRecorder) {
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkReportExportService = benchmarkReportExportService;
        this.benchmarkArtifactStorageService = benchmarkArtifactStorageService;
        this.benchmarkGovernanceTraceService = benchmarkGovernanceTraceService;
        this.benchmarkTaskRepository = benchmarkTaskRepository;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.benchmarkMetricsRecorder = benchmarkMetricsRecorder;
    }

    public BenchmarkReportResponse getJsonReport(String reportId) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} format={} status=START", QUERY_OPERATION, reportId, BenchmarkReportFormat.JSON);
        try {
            BenchmarkReport report = loadReport(reportId);
            BenchmarkReportResponse response = benchmarkTaskModelApplicationService.buildReportResponse(report);
            BenchmarkReportArtifact artifact = report.findArtifact(BenchmarkReportFormat.JSON);
            benchmarkMetricsRecorder.recordReportResponse(BenchmarkReportFormat.JSON, System.currentTimeMillis() - start);
            logEnd(reportId, BenchmarkReportFormat.JSON, start);
            writeAuditRecord(
                report,
                artifact,
                "SUCCESS",
                System.currentTimeMillis() - start,
                buildReportRequestParams(report, artifact, BenchmarkReportFormat.JSON.name()),
                buildReportResponseSummary("SUCCESS", response.getReportId(), artifact, BenchmarkReportFormat.JSON.name(), response.getVerdict().name(), null, "STRUCTURED_RESPONSE")
            );
            return response;
        } catch (RuntimeException ex) {
            benchmarkMetricsRecorder.recordReportFailure(BenchmarkReportFormat.JSON, System.currentTimeMillis() - start);
            logFailure(reportId, BenchmarkReportFormat.JSON, start, ex);
            writeAuditRecord(
                reportId,
                "FAILED",
                System.currentTimeMillis() - start,
                buildMissingReportRequestParams(reportId, BenchmarkReportFormat.JSON.name()),
                buildReportResponseSummary("FAILED", reportId, null, BenchmarkReportFormat.JSON.name(), null, ex.getMessage(), "NOT_APPLICABLE")
            );
            throw ex;
        }
    }

    public BenchmarkRenderedReport downloadRawDataReport(String reportId) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} format={} status=START", QUERY_OPERATION, reportId, "RAW_DATA");
        try {
            BenchmarkReport report = loadReport(reportId);
            BenchmarkReportArtifact artifact = report.findRawDataArtifact();
            if (artifact == null) {
                throw new BizException(
                    ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_REPORT_MODEL_INVALID,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Benchmark raw-data artifact is missing"
                );
            }
            final BenchmarkReport loadedReport = report;
            BenchmarkArtifactLoadResult loadResult = benchmarkArtifactStorageService.loadOrRecover(
                report.getReportId(),
                artifact,
                () -> benchmarkReportExportService.buildRawDataArtifact(
                    benchmarkTaskModelApplicationService.buildRawDataResponse(loadedReport)
                )
            );
            report = persistRecoveredArtifact(report, loadResult.getResolvedArtifact());
            BenchmarkRenderedReport response = loadResult.getRenderedReport();
            benchmarkMetricsRecorder.recordReportResponse(null, System.currentTimeMillis() - start);
            logEnd(reportId, null, start);
            writeAuditRecord(
                report,
                loadResult.getResolvedArtifact(),
                "SUCCESS",
                System.currentTimeMillis() - start,
                buildReportRequestParams(report, loadResult.getResolvedArtifact(), "RAW_DATA"),
                buildReportResponseSummary(
                    "SUCCESS",
                    reportId,
                    loadResult.getResolvedArtifact(),
                    "RAW_DATA",
                    report.getVerdict().name(),
                    null,
                    loadResult.getRecoveryStatus()
                )
            );
            return response;
        } catch (RuntimeException ex) {
            benchmarkMetricsRecorder.recordReportFailure(null, System.currentTimeMillis() - start);
            logFailure(reportId, null, start, ex);
            writeAuditRecord(
                reportId,
                "FAILED",
                System.currentTimeMillis() - start,
                buildMissingReportRequestParams(reportId, "RAW_DATA"),
                buildReportResponseSummary("FAILED", reportId, null, "RAW_DATA", null, ex.getMessage(), "NOT_APPLICABLE")
            );
            throw ex;
        }
    }

    public BenchmarkRenderedReport renderReport(String reportId, BenchmarkReportFormat format) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} format={} status=START", QUERY_OPERATION, reportId, format);
        try {
            BenchmarkReport report = loadReport(reportId);
            BenchmarkReportResponse response = benchmarkTaskModelApplicationService.buildReportResponse(report);
            BenchmarkReportArtifact artifact = report.findArtifact(format);
            if (artifact == null) {
                throw new BizException(
                    ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_REPORT_MODEL_INVALID,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Benchmark report export artifact is missing for format=" + format
                );
            }
            final BenchmarkReport loadedReport = report;
            final BenchmarkReportResponse loadedResponse = response;
            BenchmarkArtifactLoadResult loadResult = benchmarkArtifactStorageService.loadOrRecover(
                report.getReportId(),
                artifact,
                () -> benchmarkReportExportService.buildReportArtifact(loadedResponse, format)
            );
            report = persistRecoveredArtifact(report, loadResult.getResolvedArtifact());
            BenchmarkRenderedReport renderedReport = loadResult.getRenderedReport();
            benchmarkMetricsRecorder.recordReportResponse(format, System.currentTimeMillis() - start);
            logEnd(reportId, format, start);
            writeAuditRecord(
                report,
                loadResult.getResolvedArtifact(),
                "SUCCESS",
                System.currentTimeMillis() - start,
                buildReportRequestParams(report, loadResult.getResolvedArtifact(), format.name()),
                buildReportResponseSummary(
                    "SUCCESS",
                    response.getReportId(),
                    loadResult.getResolvedArtifact(),
                    format.name(),
                    response.getVerdict().name(),
                    null,
                    loadResult.getRecoveryStatus()
                )
            );
            return renderedReport;
        } catch (RuntimeException ex) {
            benchmarkMetricsRecorder.recordReportFailure(format, System.currentTimeMillis() - start);
            logFailure(reportId, format, start, ex);
            writeAuditRecord(
                reportId,
                "FAILED",
                System.currentTimeMillis() - start,
                buildMissingReportRequestParams(reportId, format == null ? null : format.name()),
                buildReportResponseSummary(
                    "FAILED",
                    reportId,
                    null,
                    format == null ? null : format.name(),
                    null,
                    ex.getMessage(),
                    "NOT_APPLICABLE"
                )
            );
            throw ex;
        }
    }

    public BenchmarkReportFormat parseFormat(String rawFormat) {
        if (rawFormat == null || rawFormat.trim().length() == 0) {
            return BenchmarkReportFormat.JSON;
        }
        try {
            return BenchmarkReportFormat.valueOf(rawFormat.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "Unsupported benchmark report format: " + rawFormat
            );
        }
    }

    private BenchmarkReport loadReport(String reportId) {
        BenchmarkReport report = benchmarkTaskRepository.findReportByReportId(reportId);
        if (report == null) {
            throw new BizException(
                ErrorCodeConstants.BENCHMARK_REPORT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Benchmark report does not exist for reportId=" + reportId
            );
        }
        verifyTenantAccess(report.getTenantId());
        assertAuthorization(report);
        return report;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this benchmark report");
        }
    }

    private void assertAuthorization(BenchmarkReport report) {
        List<BenchmarkEngineProfile> engineProfiles = report.getEngineProfiles();
        if (engineProfiles == null || engineProfiles.isEmpty()) {
            governanceCapabilityClient.assertAuthorization(
                report.getTenantId(),
                DataSourceTypeEnum.HETU,
                RESOURCE_TYPE_REPORT,
                report.getReportId(),
                QUERY_OPERATION
            );
            return;
        }
        for (BenchmarkEngineProfile engineProfile : engineProfiles) {
            governanceCapabilityClient.assertAuthorization(
                report.getTenantId(),
                engineProfile.getEngine(),
                RESOURCE_TYPE_REPORT,
                report.getReportId(),
                QUERY_OPERATION
            );
        }
    }

    private void logEnd(String reportId, BenchmarkReportFormat format, long start) {
        LOGGER.info(
            "operation={} entity={} format={} costMs={} status=END",
            QUERY_OPERATION,
            reportId,
            format,
            System.currentTimeMillis() - start
        );
    }

    private void logFailure(String reportId, BenchmarkReportFormat format, long start, RuntimeException ex) {
        LOGGER.error(
            "operation={} entity={} format={} costMs={} status=FAILED phase=EXCEPTION reason={}",
            QUERY_OPERATION,
            reportId,
            format,
            System.currentTimeMillis() - start,
            ex.getMessage(),
            ex
        );
    }

    private void writeAuditRecord(String reportId,
                                  String resultStatus,
                                  long elapsedMs,
                                  String requestParams,
                                  String responseSummary) {
        writeAuditRecord(null, null, reportId, resultStatus, elapsedMs, requestParams, responseSummary);
    }

    private void writeAuditRecord(BenchmarkReport report,
                                  BenchmarkReportArtifact artifact,
                                  String resultStatus,
                                  long elapsedMs,
                                  String requestParams,
                                  String responseSummary) {
        writeAuditRecord(report, artifact, report == null ? null : report.getReportId(), resultStatus, elapsedMs, requestParams, responseSummary);
    }

    private void writeAuditRecord(BenchmarkReport report,
                                  BenchmarkReportArtifact artifact,
                                  String resourceId,
                                  String resultStatus,
                                  long elapsedMs,
                                  String requestParams,
                                  String responseSummary) {
        boolean governanceTraceAvailable = report != null
            && artifact != null
            && StringUtils.hasText(artifact.getExportId());
        governanceCapabilityClient.writeAudit(
            new BenchmarkAuditRecord(
                QUERY_OPERATION,
                RESOURCE_TYPE_REPORT,
                resourceId,
                resultStatus,
                elapsedMs,
                report == null ? null : benchmarkGovernanceTraceService.resolveSagaId(report.getTaskId(), governanceTraceAvailable),
                report == null ? null : benchmarkGovernanceTraceService.resolveConfigSnapshotId(report.getReportId(), governanceTraceAvailable),
                report == null ? null : benchmarkGovernanceTraceService.resolveResultId(report.getReportId(), governanceTraceAvailable),
                report == null ? null : benchmarkGovernanceTraceService.resolveHistoryId(report.getReportId(), governanceTraceAvailable),
                artifact == null ? null : artifact.getExportId(),
                requestParams,
                responseSummary
            )
        );
    }

    private String buildReportRequestParams(BenchmarkReport report, BenchmarkReportArtifact artifact, String format) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", report.getTenantId());
        payload.put("reportId", report.getReportId());
        payload.put("taskId", report.getTaskId());
        payload.put("format", format);
        payload.put("targetEngines", collectTargetEngines(report));
        payload.put("artifactKey", artifact == null ? null : artifact.getArtifactKey());
        payload.put("artifactKind", artifact == null || artifact.getArtifactKind() == null ? null : artifact.getArtifactKind().name());
        payload.put("exportId", artifact == null ? null : artifact.getExportId());
        payload.put("configSnapshotId", resolveAuditConfigSnapshotId(report, artifact));
        payload.put("resultId", resolveAuditResultId(report, artifact));
        payload.put("historyId", resolveAuditHistoryId(report, artifact));
        return JsonUtils.toJson(payload);
    }

    private String buildMissingReportRequestParams(String reportId, String format) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", RequestContext.getTenantId());
        payload.put("reportId", reportId);
        payload.put("format", format);
        return JsonUtils.toJson(payload);
    }

    private List<String> collectTargetEngines(BenchmarkReport report) {
        List<String> engines = new ArrayList<String>(report.getEngineProfiles().size());
        for (BenchmarkEngineProfile engineProfile : report.getEngineProfiles()) {
            engines.add(engineProfile.getEngine().name());
        }
        return engines;
    }

    private String buildReportResponseSummary(String resultStatus,
                                              String reportId,
                                              BenchmarkReportArtifact artifact,
                                              String format,
                                              String verdict,
                                              String failureReason,
                                              String artifactRecoveryStatus) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", resultStatus);
        payload.put("reportId", reportId);
        payload.put("format", format);
        payload.put("verdict", verdict);
        payload.put("artifactKey", artifact == null ? null : artifact.getArtifactKey());
        payload.put("artifactStorageType", artifact == null ? null : artifact.getStorageType());
        payload.put("exportId", artifact == null ? null : artifact.getExportId());
        payload.put("artifactRecoveryStatus", artifactRecoveryStatus);
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private BenchmarkReport persistRecoveredArtifact(BenchmarkReport report, BenchmarkReportArtifact resolvedArtifact) {
        if (report == null || resolvedArtifact == null) {
            return report;
        }
        BenchmarkReport updatedReport = report.withReplacedArtifact(resolvedArtifact);
        if (updatedReport != report) {
            benchmarkTaskRepository.saveReport(updatedReport);
        }
        return updatedReport;
    }

    private String resolveAuditConfigSnapshotId(BenchmarkReport report, BenchmarkReportArtifact artifact) {
        return benchmarkGovernanceTraceService.resolveConfigSnapshotId(report.getReportId(), hasGovernanceTrace(artifact));
    }

    private String resolveAuditResultId(BenchmarkReport report, BenchmarkReportArtifact artifact) {
        return benchmarkGovernanceTraceService.resolveResultId(report.getReportId(), hasGovernanceTrace(artifact));
    }

    private String resolveAuditHistoryId(BenchmarkReport report, BenchmarkReportArtifact artifact) {
        return benchmarkGovernanceTraceService.resolveHistoryId(report.getReportId(), hasGovernanceTrace(artifact));
    }

    private boolean hasGovernanceTrace(BenchmarkReportArtifact artifact) {
        return artifact != null && StringUtils.hasText(artifact.getExportId());
    }
}
