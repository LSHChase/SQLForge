package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkArtifactGovernanceOperationService {

    private static final String RESOURCE_TYPE_REPORT = "BENCHMARK_ENGINE_REPORT";
    private static final String OPERATION_RECOVER = "RECOVER_ARTIFACT";
    private static final String OPERATION_CLEANUP = "CLEANUP_ARTIFACT";
    private static final String CLEANUP_SCOPE_MIRROR_ONLY = "MIRROR_ONLY";

    private final BenchmarkTaskRepository benchmarkTaskRepository;
    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkReportExportService benchmarkReportExportService;
    private final BenchmarkArtifactStorageService benchmarkArtifactStorageService;
    private final BenchmarkGovernanceTraceService benchmarkGovernanceTraceService;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public BenchmarkArtifactGovernanceOperationService(
        BenchmarkTaskRepository benchmarkTaskRepository,
        BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
        BenchmarkReportExportService benchmarkReportExportService,
        BenchmarkArtifactStorageService benchmarkArtifactStorageService,
        BenchmarkGovernanceTraceService benchmarkGovernanceTraceService,
        GovernanceCapabilityClient governanceCapabilityClient
    ) {
        this.benchmarkTaskRepository = benchmarkTaskRepository;
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkReportExportService = benchmarkReportExportService;
        this.benchmarkArtifactStorageService = benchmarkArtifactStorageService;
        this.benchmarkGovernanceTraceService = benchmarkGovernanceTraceService;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public GovernanceBenchmarkArtifactOperationResponse operate(GovernanceBenchmarkArtifactOperationRequest request) {
        long start = System.currentTimeMillis();
        String reportId = requireText(request == null ? null : request.getReportId(), "reportId");
        String artifactKey = requireText(request == null ? null : request.getArtifactKey(), "artifactKey");
        String operationType = normalizeOperationType(request == null ? null : request.getOperationType());
        String cleanupScope = normalizeCleanupScope(request == null ? null : request.getCleanupScope());
        BenchmarkReport report = loadReport(reportId);
        BenchmarkReportArtifact artifact = report.findArtifactByArtifactKey(artifactKey);
        if (artifact == null) {
            throw new BizException(
                ErrorCodeConstants.BENCHMARK_REPORT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Benchmark report artifact does not exist for reportId=" + reportId + ", artifactKey=" + artifactKey
            );
        }
        try {
            GovernanceBenchmarkArtifactOperationResponse response = OPERATION_RECOVER.equals(operationType)
                ? recoverArtifact(report, artifact, request, cleanupScope)
                : cleanupArtifact(report, artifact, request, cleanupScope);
            writeAudit(report, artifact, request, response, "SUCCESS", System.currentTimeMillis() - start);
            return response;
        } catch (RuntimeException ex) {
            writeFailureAudit(report, artifact, request, operationType, cleanupScope, ex, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private GovernanceBenchmarkArtifactOperationResponse recoverArtifact(BenchmarkReport report,
                                                                         BenchmarkReportArtifact artifact,
                                                                         GovernanceBenchmarkArtifactOperationRequest request,
                                                                         String cleanupScope) {
        BenchmarkArtifactLoadResult loadResult = benchmarkArtifactStorageService.loadOrRecover(
            report.getReportId(),
            report.getTenantId(),
            report.getGeneratedAt(),
            artifact,
            () -> rebuildArtifact(report, artifact)
        );
        BenchmarkReport persistedReport = persistRecoveredArtifact(report, loadResult.getResolvedArtifact());
        Map<String, Object> operationSurface = buildOperationSurface(
            OPERATION_RECOVER,
            "RECOVERY_COMPLETED",
            cleanupScope,
            loadResult.getResolvedArtifact(),
            loadResult.getRecoveryStatus(),
            loadResult.getStorageRecoverySource(),
            loadResult.getStorageReadStatus(),
            request == null ? null : request.getOperationReason()
        );
        GovernanceBenchmarkArtifactOperationResponse response = new GovernanceBenchmarkArtifactOperationResponse();
        response.setTenantId(persistedReport.getTenantId());
        response.setReportId(persistedReport.getReportId());
        response.setArtifactKey(loadResult.getResolvedArtifact().getArtifactKey());
        response.setOperationType(OPERATION_RECOVER);
        response.setOperationStatus("RECOVERY_COMPLETED");
        response.setStorageType(loadResult.getResolvedArtifact().getStorageType());
        response.setStorageUri(loadResult.getResolvedArtifact().getStorageUri());
        response.setStorageEvidence(loadResult.getResolvedArtifact().getStorageEvidence());
        response.setArtifactRecoveryStatus(loadResult.getRecoveryStatus());
        response.setStorageRecoverySource(loadResult.getStorageRecoverySource());
        response.setStorageReadStatus(loadResult.getStorageReadStatus());
        response.setArtifactOperationSurface(operationSurface);
        return response;
    }

    private GovernanceBenchmarkArtifactOperationResponse cleanupArtifact(BenchmarkReport report,
                                                                         BenchmarkReportArtifact artifact,
                                                                         GovernanceBenchmarkArtifactOperationRequest request,
                                                                         String cleanupScope) {
        CleanupResult cleanupResult = cleanupLocalReplica(artifact, cleanupScope);
        Map<String, Object> operationSurface = buildOperationSurface(
            OPERATION_CLEANUP,
            cleanupResult.getOperationStatus(),
            cleanupResult.getCleanupScope(),
            artifact,
            "PENDING_RECOVERY_AFTER_CLEANUP",
            cleanupResult.getStorageRecoverySource(),
            cleanupResult.getStorageReadStatus(),
            request == null ? null : request.getOperationReason()
        );
        operationSurface.put("cleanupTarget", cleanupResult.getCleanupTarget());
        GovernanceBenchmarkArtifactOperationResponse response = new GovernanceBenchmarkArtifactOperationResponse();
        response.setTenantId(report.getTenantId());
        response.setReportId(report.getReportId());
        response.setArtifactKey(artifact.getArtifactKey());
        response.setOperationType(OPERATION_CLEANUP);
        response.setOperationStatus(cleanupResult.getOperationStatus());
        response.setStorageType(artifact.getStorageType());
        response.setStorageUri(artifact.getStorageUri());
        response.setStorageEvidence(artifact.getStorageEvidence());
        response.setArtifactRecoveryStatus("PENDING_RECOVERY_AFTER_CLEANUP");
        response.setStorageRecoverySource(cleanupResult.getStorageRecoverySource());
        response.setStorageReadStatus(cleanupResult.getStorageReadStatus());
        response.setArtifactOperationSurface(operationSurface);
        return response;
    }

    private CleanupResult cleanupLocalReplica(BenchmarkReportArtifact artifact, String cleanupScope) {
        if ("ENVIRONMENT_OBJECT_STORAGE".equals(artifact.getStorageType())) {
            Path mirrorPath = resolveEnvironmentMirrorPath(artifact);
            deleteIfExists(mirrorPath);
            return new CleanupResult("CLEANUP_COMPLETED", cleanupScope, "REPO_LOCAL_MIRROR", "MIRROR_DELETED", "repoLocalMirror");
        }
        Path localPath = resolveLocalPath(artifact);
        deleteIfExists(localPath);
        return new CleanupResult("CLEANUP_COMPLETED", cleanupScope, "LOCAL_FILE", "LOCAL_FILE_DELETED", "localFile");
    }

    private BenchmarkReportArtifact rebuildArtifact(BenchmarkReport report, BenchmarkReportArtifact artifact) {
        if (artifact.getArtifactKind() == BenchmarkReportArtifactKind.RAW_DATA_SNAPSHOT) {
            BenchmarkReportRawDataResponse rawDataResponse = benchmarkTaskModelApplicationService.buildRawDataResponse(report);
            return benchmarkReportExportService.buildRawDataArtifact(rawDataResponse);
        }
        BenchmarkReportResponse reportResponse = benchmarkTaskModelApplicationService.buildReportResponse(report);
        BenchmarkReportFormat format = artifact.getFormat();
        return benchmarkReportExportService.buildReportArtifact(reportResponse, format);
    }

    private BenchmarkReport persistRecoveredArtifact(BenchmarkReport report, BenchmarkReportArtifact resolvedArtifact) {
        if (report == null || resolvedArtifact == null) {
            return report;
        }
        BenchmarkReport updatedReport = report.withReplacedArtifact(resolvedArtifact);
        if (updatedReport != report) {
            return benchmarkTaskRepository.saveReport(updatedReport);
        }
        return report;
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
        return report;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
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

    private void writeAudit(BenchmarkReport report,
                            BenchmarkReportArtifact artifact,
                            GovernanceBenchmarkArtifactOperationRequest request,
                            GovernanceBenchmarkArtifactOperationResponse response,
                            String resultStatus,
                            long elapsedMs) {
        Map<String, Object> requestParams = new LinkedHashMap<String, Object>();
        requestParams.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        requestParams.put("tenantId", report == null ? null : report.getTenantId());
        requestParams.put("reportId", report == null ? null : report.getReportId());
        requestParams.put("taskId", report == null ? null : report.getTaskId());
        requestParams.put("artifactKey", artifact == null ? null : artifact.getArtifactKey());
        requestParams.put("artifactKind", artifact == null || artifact.getArtifactKind() == null ? null : artifact.getArtifactKind().name());
        requestParams.put("operationType", response == null ? null : response.getOperationType());
        requestParams.put("operationReason", request == null ? null : request.getOperationReason());
        requestParams.put("cleanupScope", request == null ? null : normalizeCleanupScope(request.getCleanupScope()));

        Map<String, Object> responseSummary = new LinkedHashMap<String, Object>();
        responseSummary.put("resultStatus", resultStatus);
        responseSummary.put("reportId", report == null ? null : report.getReportId());
        responseSummary.put("artifactKey", artifact == null ? null : artifact.getArtifactKey());
        responseSummary.put("artifactStorageType", response == null ? null : response.getStorageType());
        responseSummary.put("artifactStorageEvidence", response == null ? null : response.getStorageEvidence());
        responseSummary.put("artifactRecoveryStatus", response == null ? null : response.getArtifactRecoveryStatus());
        responseSummary.put("artifactStorageRecoverySource", response == null ? null : response.getStorageRecoverySource());
        responseSummary.put("artifactStorageReadStatus", response == null ? null : response.getStorageReadStatus());
        responseSummary.put("artifactOperationSurface", response == null ? null : response.getArtifactOperationSurface());
        boolean governanceTraceAvailable = artifact != null && StringUtils.hasText(artifact.getExportId());

        governanceCapabilityClient.writeAudit(
            new BenchmarkAuditRecord(
                resolveAuditOperationCode(response == null ? null : response.getOperationType()),
                RESOURCE_TYPE_REPORT,
                report == null ? null : report.getReportId(),
                resultStatus,
                elapsedMs,
                report == null ? null : benchmarkGovernanceTraceService.resolveSagaId(report.getTaskId(), governanceTraceAvailable),
                report == null ? null : benchmarkGovernanceTraceService.resolveConfigSnapshotId(report.getReportId(), governanceTraceAvailable),
                report == null ? null : benchmarkGovernanceTraceService.resolveResultId(report.getReportId(), governanceTraceAvailable),
                report == null ? null : benchmarkGovernanceTraceService.resolveHistoryId(report.getReportId(), governanceTraceAvailable),
                artifact == null ? null : artifact.getExportId(),
                JsonUtils.toJson(requestParams),
                JsonUtils.toJson(responseSummary)
            )
        );
    }

    private void writeFailureAudit(BenchmarkReport report,
                                   BenchmarkReportArtifact artifact,
                                   GovernanceBenchmarkArtifactOperationRequest request,
                                   String operationType,
                                   String cleanupScope,
                                   RuntimeException ex,
                                   long elapsedMs) {
        GovernanceBenchmarkArtifactOperationResponse response = new GovernanceBenchmarkArtifactOperationResponse();
        response.setOperationType(operationType);
        response.setOperationStatus("FAILED");
        response.setStorageType(artifact == null ? null : artifact.getStorageType());
        response.setStorageEvidence(artifact == null ? null : artifact.getStorageEvidence());
        response.setArtifactRecoveryStatus("FAILED");
        response.setStorageRecoverySource("FAILED");
        response.setStorageReadStatus("FAILED");
        Map<String, Object> operationSurface = buildOperationSurface(
            operationType,
            "FAILED",
            cleanupScope,
            artifact,
            "FAILED",
            "FAILED",
            "FAILED",
            request == null ? null : request.getOperationReason()
        );
        operationSurface.put("failureReason", ex.getMessage());
        response.setArtifactOperationSurface(operationSurface);
        writeAudit(report, artifact, request, response, "FAILED", elapsedMs);
    }

    private String resolveAuditOperationCode(String operationType) {
        return OPERATION_CLEANUP.equals(operationType) ? "BENCHMARK_ARTIFACT_CLEANUP" : "BENCHMARK_ARTIFACT_RECOVER";
    }

    private Map<String, Object> buildOperationSurface(String operationType,
                                                      String operationStatus,
                                                      String cleanupScope,
                                                      BenchmarkReportArtifact artifact,
                                                      String artifactRecoveryStatus,
                                                      String storageRecoverySource,
                                                      String storageReadStatus,
                                                      String operationReason) {
        LinkedHashMap<String, Object> operationSurface = new LinkedHashMap<String, Object>();
        operationSurface.put("operationType", operationType);
        operationSurface.put("operationStatus", operationStatus);
        operationSurface.put("cleanupScope", cleanupScope);
        operationSurface.put("artifactRecoveryStatus", artifactRecoveryStatus);
        operationSurface.put("storageRecoverySource", storageRecoverySource);
        operationSurface.put("storageReadStatus", storageReadStatus);
        operationSurface.put("storageType", artifact == null ? null : artifact.getStorageType());
        operationSurface.put("storageUri", artifact == null ? null : artifact.getStorageUri());
        operationSurface.put("liveEvidenceStatus", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "liveEvidenceStatus"));
        operationSurface.put("providerMode", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "providerMode"));
        operationSurface.put("primaryProvider", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "primaryProvider"));
        operationSurface.put("providerHeadStatus", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "providerHeadStatus"));
        operationSurface.put("providerRequestId", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "providerRequestId"));
        operationSurface.put("recoveryProvider", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "recoveryProvider"));
        operationSurface.put("recoveryProviderHeadStatus", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "recoveryProviderHeadStatus"));
        operationSurface.put("recoveryProviderRequestId", resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "recoveryProviderRequestId"));
        operationSurface.put("operationReason", operationReason);
        return operationSurface;
    }

    private Path resolveEnvironmentMirrorPath(BenchmarkReportArtifact artifact) {
        String mirrorPath = resolveEvidenceValue(artifact == null ? null : artifact.getStorageEvidence(), "mirrorPath");
        if (!StringUtils.hasText(mirrorPath)) {
            throw new IllegalStateException("Environment-backed artifact is missing mirrorPath evidence");
        }
        return Paths.get(mirrorPath);
    }

    private Path resolveLocalPath(BenchmarkReportArtifact artifact) {
        if (artifact == null || !StringUtils.hasText(artifact.getStorageUri())) {
            throw new IllegalStateException("Local benchmark artifact is missing storageUri");
        }
        return Paths.get(URI.create(artifact.getStorageUri()));
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to cleanup benchmark artifact replica " + path, ex);
        }
    }

    private String resolveEvidenceValue(String storageEvidence, String key) {
        if (!StringUtils.hasText(storageEvidence) || !StringUtils.hasText(key)) {
            return null;
        }
        String[] parts = storageEvidence.split(";");
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }

    private String normalizeOperationType(String operationType) {
        String normalized = requireText(operationType, "operationType").trim().toUpperCase();
        if (!OPERATION_RECOVER.equals(normalized) && !OPERATION_CLEANUP.equals(normalized)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "Unsupported benchmark artifact operationType: " + operationType
            );
        }
        return normalized;
    }

    private String normalizeCleanupScope(String cleanupScope) {
        if (!StringUtils.hasText(cleanupScope)) {
            return CLEANUP_SCOPE_MIRROR_ONLY;
        }
        String normalized = cleanupScope.trim().toUpperCase();
        if (!CLEANUP_SCOPE_MIRROR_ONLY.equals(normalized)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "Unsupported benchmark artifact cleanupScope: " + cleanupScope
            );
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " must not be empty"
            );
        }
        return value;
    }

    private static final class CleanupResult {

        private final String operationStatus;
        private final String cleanupScope;
        private final String storageRecoverySource;
        private final String storageReadStatus;
        private final String cleanupTarget;

        private CleanupResult(String operationStatus,
                              String cleanupScope,
                              String storageRecoverySource,
                              String storageReadStatus,
                              String cleanupTarget) {
            this.operationStatus = operationStatus;
            this.cleanupScope = cleanupScope;
            this.storageRecoverySource = storageRecoverySource;
            this.storageReadStatus = storageReadStatus;
            this.cleanupTarget = cleanupTarget;
        }

        private String getOperationStatus() {
            return operationStatus;
        }

        private String getCleanupScope() {
            return cleanupScope;
        }

        private String getStorageRecoverySource() {
            return storageRecoverySource;
        }

        private String getStorageReadStatus() {
            return storageReadStatus;
        }

        private String getCleanupTarget() {
            return cleanupTarget;
        }
    }
}
