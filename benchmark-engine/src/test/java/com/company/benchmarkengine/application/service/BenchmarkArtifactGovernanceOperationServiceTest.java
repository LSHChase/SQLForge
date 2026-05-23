package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    @Test
    void shouldCleanupProviderBackedTargetsWithAuthenticatedDelete(@TempDir Path tempDir) throws Exception {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        Map<String, byte[]> primaryStore = new ConcurrentHashMap<String, byte[]>();
        Map<String, byte[]> recoveryStore = new ConcurrentHashMap<String, byte[]>();
        HttpServer primaryServer = startAuthenticatedObjectStoreServer(primaryStore, "Bearer cleanup-token");
        HttpServer recoveryServer = startAuthenticatedObjectStoreServer(recoveryStore, "Bearer cleanup-token");
        try {
            BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
            properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
            properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
            properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
            properties.getEnvironmentObjectStorage().setExternalWriteDir(tempDir.resolve("external").toString());
            properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
            properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
            properties.getEnvironmentObjectStorage().setEndpoint(
                "http://127.0.0.1:" + primaryServer.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setCredentials("Bearer cleanup-token");
            properties.getEnvironmentObjectStorage().setProviderName("PRIMARY_HTTP");
            properties.getEnvironmentObjectStorage().setProviderContract("HTTP_PUT_GET_DELETE");
            properties.getEnvironmentObjectStorage().setRecoveryProviderEndpoint(
                "http://127.0.0.1:" + recoveryServer.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setRecoveryProviderBucket("benchmark-recovery-bucket");
            properties.getEnvironmentObjectStorage().setRecoveryProviderCredentials("Bearer cleanup-token");
            properties.getEnvironmentObjectStorage().setRecoveryProviderName("RECOVERY_HTTP");
            properties.getEnvironmentObjectStorage().setRecoveryProviderContract("HTTP_PUT_GET_DELETE");
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
            cleanupRequest.setCleanupScope("MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER");
            cleanupRequest.setOrchestrationType("EXECUTE_RETENTION_BATCH");
            cleanupRequest.setBatchId("batch-001");
            cleanupRequest.setBatchIndex(Integer.valueOf(1));
            cleanupRequest.setBatchSize(Integer.valueOf(1));
            GovernanceBenchmarkArtifactOperationResponse cleanupResponse = service.operate(cleanupRequest);

            Path mirrorPath = Paths.get(resolveEvidenceValue(storedArtifact.getStorageEvidence(), "mirrorPath"));
            Path liveEvidencePath = Paths.get(resolveEvidenceValue(storedArtifact.getStorageEvidence(), "liveEvidencePath"));
            Path externalWritePath = Paths.get(resolveEvidenceValue(storedArtifact.getStorageEvidence(), "externalWritePath"));
            assertTrue(Files.notExists(mirrorPath));
            assertTrue(Files.notExists(liveEvidencePath));
            assertTrue(Files.notExists(externalWritePath));
            assertTrue(primaryStore.isEmpty());
            assertTrue(recoveryStore.isEmpty());
            assertEquals("batch-001", cleanupResponse.getBatchId());
            assertEquals("EXECUTE_RETENTION_BATCH", cleanupResponse.getOrchestrationType());
            assertEquals("CLEANUP_COMPLETED", cleanupResponse.getOperationStatus());
            assertEquals(Boolean.TRUE, cleanupResponse.getArtifactOperationSurface().get("providerAuthUsed"));
            assertEquals("VERIFIED", cleanupResponse.getArtifactOperationSurface().get("primaryProviderDeleteStatus"));
            assertEquals("VERIFIED", cleanupResponse.getArtifactOperationSurface().get("recoveryProviderDeleteStatus"));
            assertFalse(((java.util.List<?>) cleanupResponse.getArtifactOperationSurface().get("cleanupTargets")).isEmpty());
            verify(governanceCapabilityClient).writeAudit(any());
        } finally {
            primaryServer.stop(0);
            recoveryServer.stop(0);
        }
    }

    @Test
    void shouldFailProviderCleanupWhenAuthenticatedDeleteCredentialsAreMissing(@TempDir Path tempDir) throws Exception {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        Map<String, byte[]> primaryStore = new ConcurrentHashMap<String, byte[]>();
        HttpServer primaryServer = startAuthenticatedObjectStoreServer(primaryStore, "Bearer cleanup-token");
        try {
            BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
            properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
            properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
            properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
            properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
            properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
            properties.getEnvironmentObjectStorage().setEndpoint(
                "http://127.0.0.1:" + primaryServer.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setCredentials("Bearer cleanup-token");
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
            properties.getEnvironmentObjectStorage().setCredentials("");

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
            cleanupRequest.setCleanupScope("MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER");

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.operate(cleanupRequest));

            assertTrue(exception.getMessage().contains("配置凭据"));
            verify(governanceCapabilityClient).writeAudit(any());
        } finally {
            primaryServer.stop(0);
        }
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

    private HttpServer startAuthenticatedObjectStoreServer(Map<String, byte[]> objectStore, String expectedAuthorization)
        throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/provider", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                if (!expectedAuthorization.equals(authorization)) {
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                if (!path.startsWith("/provider/")) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                String key = path.substring("/provider/".length());
                if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    objectStore.put(key, readBody(exchange));
                    exchange.getResponseHeaders().add("Connection", "close");
                    exchange.sendResponseHeaders(200, 0);
                    exchange.close();
                    return;
                }
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && objectStore.containsKey(key)) {
                    byte[] bytes = objectStore.get(key);
                    exchange.getResponseHeaders().add("ETag", "etag-" + key.hashCode());
                    exchange.getResponseHeaders().add("X-Request-Id", "request-primary");
                    exchange.getResponseHeaders().add("Connection", "close");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                    return;
                }
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod()) && objectStore.containsKey(key)) {
                    byte[] bytes = objectStore.get(key);
                    exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(bytes.length));
                    exchange.getResponseHeaders().add("ETag", "etag-" + key.hashCode());
                    exchange.getResponseHeaders().add("X-Request-Id", "request-primary");
                    exchange.getResponseHeaders().add("Connection", "close");
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }
                if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    objectStore.remove(key);
                    exchange.getResponseHeaders().add("Connection", "close");
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private byte[] readBody(HttpExchange exchange) throws java.io.IOException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (true) {
            int read = exchange.getRequestBody().read(buffer);
            if (read < 0) {
                return outputStream.toByteArray();
            }
            outputStream.write(buffer, 0, read);
        }
    }
}
