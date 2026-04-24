package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkArtifactGovernanceOperationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCleanupMirrorAndRecoverEnvironmentBackedArtifact(@TempDir Path tempDir) throws Exception {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
        properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
        properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
        properties.getEnvironmentObjectStorage().setExternalWriteDir(tempDir.resolve("external").toString());
        properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
        properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
        BenchmarkArtifactStorageService storageService = new BenchmarkArtifactStorageService(properties, governanceCapabilityClient);
        BenchmarkReportArtifact storedArtifact = storageService.externalize(
            "report-001",
            "tenant-a",
            Instant.parse("2026-04-24T00:00:00Z"),
            new BenchmarkReportArtifact(
                "json-export",
                BenchmarkReportArtifactKind.REPORT_EXPORT,
                BenchmarkReportFormat.JSON,
                "benchmark-report-report-001.json",
                BenchmarkReportFormat.JSON.getContentType(),
                Integer.valueOf(10),
                "checksum",
                null,
                null,
                null,
                "{\"ok\":1}"
            )
        );
        BenchmarkReport report = new BenchmarkReport(
            "report-001",
            "task-001",
            BenchmarkTaskType.BASELINE,
            "tenant-a",
            "fp-001",
            Instant.parse("2026-04-24T00:00:00Z"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            Collections.singletonList(storedArtifact)
        );
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        repository.saveReport(report);
        BenchmarkArtifactGovernanceOperationService service = new BenchmarkArtifactGovernanceOperationService(
            repository,
            new BenchmarkTaskModelApplicationService(),
            new BenchmarkReportExportService(),
            storageService,
            new BenchmarkGovernanceTraceService(governanceCapabilityClient),
            governanceCapabilityClient
        );
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );

        GovernanceBenchmarkArtifactOperationRequest cleanupRequest = new GovernanceBenchmarkArtifactOperationRequest();
        cleanupRequest.setTenantId("tenant-a");
        cleanupRequest.setReportId("report-001");
        cleanupRequest.setArtifactKey("json-export");
        cleanupRequest.setOperationType("CLEANUP_ARTIFACT");
        cleanupRequest.setOperationReason("prepare-recovery");
        GovernanceBenchmarkArtifactOperationResponse cleanupResponse = service.operate(cleanupRequest);

        Path mirrorPath = Paths.get(resolveEvidenceValue(storedArtifact.getStorageEvidence(), "mirrorPath"));
        assertTrue(Files.notExists(mirrorPath));
        assertEquals("CLEANUP_COMPLETED", cleanupResponse.getOperationStatus());
        assertEquals("repoLocalMirror", cleanupResponse.getArtifactOperationSurface().get("cleanupTarget"));

        GovernanceBenchmarkArtifactOperationRequest recoverRequest = new GovernanceBenchmarkArtifactOperationRequest();
        recoverRequest.setTenantId("tenant-a");
        recoverRequest.setReportId("report-001");
        recoverRequest.setArtifactKey("json-export");
        recoverRequest.setOperationType("RECOVER_ARTIFACT");
        recoverRequest.setOperationReason("verify-recovery");
        GovernanceBenchmarkArtifactOperationResponse recoverResponse = service.operate(recoverRequest);

        assertEquals("RECOVERY_COMPLETED", recoverResponse.getOperationStatus());
        assertEquals("EXTERNAL_WRITE", recoverResponse.getStorageRecoverySource());
        assertEquals("RECOVERED_FROM_EXTERNAL_WRITE", recoverResponse.getStorageReadStatus());
        assertTrue(Files.exists(mirrorPath));
        verify(governanceCapabilityClient, org.mockito.Mockito.times(2)).writeAudit(any());
    }

    private String resolveEvidenceValue(String storageEvidence, String key) {
        String[] parts = storageEvidence.split(";");
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }
}
