package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkArtifactStorageServiceTest {

    @Test
    void shouldCleanupStaleFilesWhenExternalizingLatestArtifactSet(@TempDir Path tempDir) throws Exception {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

        BenchmarkReportArtifact pdfArtifact = artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1");
        BenchmarkReportArtifact htmlArtifact = artifact("html-export", "benchmark-report-report-001.html", BenchmarkReportFormat.HTML, "html-v1");
        service.externalize("report-001", "tenant-a", Instant.parse("2026-04-24T00:00:00Z"), Arrays.asList(pdfArtifact, htmlArtifact));

        BenchmarkReportArtifact updatedPdfArtifact = artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v2");
        service.externalize("report-001", "tenant-a", Instant.parse("2026-04-24T00:00:00Z"), Collections.singletonList(updatedPdfArtifact));

        Path pdfPath = tempDir.resolve("artifacts/report-001/benchmark-report-report-001.pdf");
        Path htmlPath = tempDir.resolve("artifacts/report-001/benchmark-report-report-001.html");
        assertTrue(Files.exists(pdfPath));
        assertEquals("pdf-v2", new String(Files.readAllBytes(pdfPath)));
        assertTrue(Files.notExists(htmlPath));
    }

    @Test
    void shouldRecoverMissingFileFromSuppliedArtifact(@TempDir Path tempDir) throws Exception {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

        BenchmarkReportArtifact persistedArtifact = service.externalize(
            "report-001",
            "tenant-a",
            Instant.parse("2026-04-24T00:00:00Z"),
            artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
        ).withExportId("export-benchmark-report-001-pdf-export");
        Files.delete(Paths.get(URI.create(persistedArtifact.getStorageUri())));

        BenchmarkArtifactLoadResult loadResult = service.loadOrRecover(
            "report-001",
            "tenant-a",
            Instant.parse("2026-04-24T00:00:00Z"),
            persistedArtifact,
            () -> artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v2")
        );

        assertTrue(loadResult.isRecovered());
        assertEquals("RECOVERED_FROM_REPORT_SNAPSHOT", loadResult.getRecoveryStatus());
        assertEquals("export-benchmark-report-001-pdf-export", loadResult.getResolvedArtifact().getExportId());
        assertEquals("pdf-v2", new String(loadResult.getRenderedReport().getContent()));
    }

    @Test
    void shouldBackfillTenantRetentionPolicyFromGovernance(@TempDir Path tempDir) {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        GovernanceTenantArtifactPolicyResponse response = new GovernanceTenantArtifactPolicyResponse();
        response.setTenantId("tenant-a");
        response.setRetentionDays(Integer.valueOf(180));
        response.setRetentionPolicySource("GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS");
        response.setRetentionPolicyStatus("TENANT_RETENTION_ACTIVE");
        when(governanceCapabilityClient.resolveTenantArtifactPolicy(eq("tenant-a"), eq("BENCHMARK_ARTIFACT"))).thenReturn(response);
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, governanceCapabilityClient);

        BenchmarkReportArtifact artifact = service.externalize(
            "report-001",
            "tenant-a",
            Instant.parse("2026-04-24T00:00:00Z"),
            artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
        );

        assertEquals(Integer.valueOf(180), artifact.getRetentionDays());
        assertEquals("GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS", artifact.getRetentionPolicySource());
        assertEquals("2026-10-21T00:00:00Z", artifact.getRetentionDeleteAfter());
    }

    @Test
    void shouldExternalizeToEnvironmentBackedMirrorWithObjectStorageEvidence(@TempDir Path tempDir) {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
        properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
        properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
        properties.getEnvironmentObjectStorage().setExternalWriteDir(tempDir.resolve("external").toString());
        properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
        properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

        BenchmarkReportArtifact artifact = service.externalize(
            "report-001",
            "tenant-a",
            Instant.parse("2026-04-24T00:00:00Z"),
            artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
        );

        assertEquals("ENVIRONMENT_OBJECT_STORAGE", artifact.getStorageType());
        assertEquals("env-obj://benchmark-bucket/tenant-artifacts/tenant-a/report-001/benchmark-report-report-001.pdf", artifact.getStorageUri());
        assertTrue(artifact.getStorageEvidence().contains("mirrorPath="));
        assertTrue(artifact.getStorageEvidence().contains("mode=repo-local-mirror+external-write-verified"));
        assertTrue(artifact.getStorageEvidence().contains("externalWriteStatus=VERIFIED"));
        assertTrue(artifact.getStorageEvidence().contains("recoveryVerificationStatus=VERIFIED"));
        assertTrue(artifact.getStorageEvidence().contains("externalWritePath="));
        assertTrue(artifact.getStorageEvidence().contains("liveEvidencePath="));
        assertTrue(artifact.getStorageEvidence().contains("liveEvidenceStatus=EXTERNAL_WRITE_RECOVERY_VERIFIED"));
        assertTrue(
            Files.exists(
                tempDir.resolve("external/benchmark-bucket/tenant-artifacts/tenant-a/report-001/benchmark-report-report-001.pdf")
            )
        );
    }

    @Test
    void shouldRecoverEnvironmentBackedArtifactFromExternalWritePath(@TempDir Path tempDir) throws Exception {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
        properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
        properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
        properties.getEnvironmentObjectStorage().setExternalWriteDir(tempDir.resolve("external").toString());
        properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
        properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

        BenchmarkReportArtifact artifact = service.externalize(
            "report-001",
            "tenant-a",
            Instant.parse("2026-04-24T00:00:00Z"),
            artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
        );
        Files.delete(Paths.get(resolveEvidenceValue(artifact.getStorageEvidence(), "mirrorPath")));

        BenchmarkRenderedReport renderedReport = service.load(artifact);

        assertEquals("pdf-v1", new String(renderedReport.getContent()));
        assertTrue(Files.exists(Paths.get(resolveEvidenceValue(artifact.getStorageEvidence(), "mirrorPath"))));
    }

    @Test
    void shouldCaptureProviderBackedLiveEvidenceAndRecoverFromProvider(@TempDir Path tempDir) throws Exception {
        Map<String, byte[]> objectStore = new ConcurrentHashMap<String, byte[]>();
        HttpServer server = startObjectStoreServer(objectStore);
        try {
            BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
            properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
            properties.setTenantPolicyBackfillEnabled(false);
            properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
            properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
            properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
            properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
            properties.getEnvironmentObjectStorage().setEndpoint(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setCredentials("Bearer benchmark-test-token");
            BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

            BenchmarkReportArtifact artifact = service.externalize(
                "report-001",
                "tenant-a",
                Instant.parse("2026-04-24T00:00:00Z"),
                artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
            );
            Files.delete(Paths.get(resolveEvidenceValue(artifact.getStorageEvidence(), "mirrorPath")));

            BenchmarkRenderedReport renderedReport = service.load(artifact);

            assertEquals("pdf-v1", new String(renderedReport.getContent()));
            assertTrue(artifact.getStorageEvidence().contains("providerWriteStatus=VERIFIED"));
            assertTrue(artifact.getStorageEvidence().contains("providerRecoveryStatus=VERIFIED"));
            assertTrue(artifact.getStorageEvidence().contains("providerHeadStatus=VERIFIED"));
            assertTrue(artifact.getStorageEvidence().contains("providerDialect=GENERIC_HTTP"));
            assertTrue(artifact.getStorageEvidence().contains("providerRequestId=request-primary"));
            assertTrue(artifact.getStorageEvidence().contains("mode=repo-local-mirror+provider-live-evidence-verified"));
            assertTrue(artifact.getStorageEvidence().contains("providerObjectUrl="));
            assertTrue(artifact.getStorageEvidence().contains("liveEvidenceStatus=PROVIDER_LIVE_EVIDENCE_VERIFIED"));
            assertTrue(Files.exists(Paths.get(resolveEvidenceValue(artifact.getStorageEvidence(), "mirrorPath"))));
            assertEquals(1, objectStore.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRecoverFromRecoveryProviderWhenPrimaryProviderIsUnavailable(@TempDir Path tempDir) throws Exception {
        Map<String, byte[]> primaryStore = new ConcurrentHashMap<String, byte[]>();
        Map<String, byte[]> recoveryStore = new ConcurrentHashMap<String, byte[]>();
        HttpServer primaryServer = startObjectStoreServer(primaryStore);
        HttpServer recoveryServer = startObjectStoreServer(recoveryStore);
        try {
            BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
            properties.setStorageType("ENVIRONMENT_OBJECT_STORAGE");
            properties.setTenantPolicyBackfillEnabled(false);
            properties.getEnvironmentObjectStorage().setMirrorDir(tempDir.resolve("mirror").toString());
            properties.getEnvironmentObjectStorage().setLiveEvidenceDir(tempDir.resolve("live-evidence").toString());
            properties.getEnvironmentObjectStorage().setBucket("benchmark-bucket");
            properties.getEnvironmentObjectStorage().setKeyPrefix("tenant-artifacts");
            properties.getEnvironmentObjectStorage().setEndpoint(
                "http://127.0.0.1:" + primaryServer.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setProviderName("PRIMARY_HTTP");
            properties.getEnvironmentObjectStorage().setProviderContract("HTTP_PUT_GET_DELETE");
            properties.getEnvironmentObjectStorage().setRecoveryProviderEndpoint(
                "http://127.0.0.1:" + recoveryServer.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setRecoveryProviderBucket("benchmark-recovery-bucket");
            properties.getEnvironmentObjectStorage().setRecoveryProviderName("RECOVERY_HTTP");
            properties.getEnvironmentObjectStorage().setRecoveryProviderContract("HTTP_PUT_GET_DELETE");
            BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

            BenchmarkReportArtifact artifact = service.externalize(
                "report-001",
                "tenant-a",
                Instant.parse("2026-04-24T00:00:00Z"),
                artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
            );
            Files.delete(Paths.get(resolveEvidenceValue(artifact.getStorageEvidence(), "mirrorPath")));
            primaryServer.stop(0);

            BenchmarkArtifactLoadResult loadResult = service.loadOrRecover(
                "report-001",
                "tenant-a",
                Instant.parse("2026-04-24T00:00:00Z"),
                artifact,
                null
            );

            assertFalse(loadResult.isRecovered());
            assertEquals("STORED", loadResult.getRecoveryStatus());
            assertEquals("RECOVERY_PROVIDER", loadResult.getStorageRecoverySource());
            assertEquals("RECOVERED_FROM_RECOVERY_PROVIDER", loadResult.getStorageReadStatus());
            assertEquals("pdf-v1", new String(loadResult.getRenderedReport().getContent()));
            assertTrue(artifact.getStorageEvidence().contains("providerMode=PRIMARY_PLUS_RECOVERY_PROVIDER"));
            assertTrue(artifact.getStorageEvidence().contains("recoveryProvider=RECOVERY_HTTP"));
            assertTrue(artifact.getStorageEvidence().contains("recoveryProviderWriteStatus=VERIFIED"));
            assertTrue(artifact.getStorageEvidence().contains("recoveryProviderRecoveryStatus=VERIFIED"));
            assertEquals(1, recoveryStore.size());
        } finally {
            primaryServer.stop(0);
            recoveryServer.stop(0);
        }
    }

    @Test
    void shouldCleanupStaleEnvironmentBackedArtifactsAcrossMirrorExternalEvidenceAndProviders(@TempDir Path tempDir) throws Exception {
        Map<String, byte[]> primaryStore = new ConcurrentHashMap<String, byte[]>();
        Map<String, byte[]> recoveryStore = new ConcurrentHashMap<String, byte[]>();
        HttpServer primaryServer = startObjectStoreServer(primaryStore);
        HttpServer recoveryServer = startObjectStoreServer(recoveryStore);
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
            properties.getEnvironmentObjectStorage().setProviderName("PRIMARY_HTTP");
            properties.getEnvironmentObjectStorage().setProviderContract("HTTP_PUT_GET_DELETE");
            properties.getEnvironmentObjectStorage().setRecoveryProviderEndpoint(
                "http://127.0.0.1:" + recoveryServer.getAddress().getPort() + "/provider"
            );
            properties.getEnvironmentObjectStorage().setRecoveryProviderBucket("benchmark-recovery-bucket");
            properties.getEnvironmentObjectStorage().setRecoveryProviderName("RECOVERY_HTTP");
            properties.getEnvironmentObjectStorage().setRecoveryProviderContract("HTTP_PUT_GET_DELETE");
            BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties, null);

            BenchmarkReportArtifact pdfArtifact = artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1");
            BenchmarkReportArtifact htmlArtifact = artifact("html-export", "benchmark-report-report-001.html", BenchmarkReportFormat.HTML, "html-v1");
            service.externalize(
                "report-001",
                "tenant-a",
                Instant.parse("2026-04-24T00:00:00Z"),
                Arrays.asList(pdfArtifact, htmlArtifact)
            );

            BenchmarkReportArtifact updatedPdfArtifact = artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v2");
            service.externalize(
                "report-001",
                "tenant-a",
                Instant.parse("2026-04-24T00:00:00Z"),
                Collections.singletonList(updatedPdfArtifact)
            );

            assertTrue(Files.notExists(tempDir.resolve("mirror/report-001/benchmark-report-report-001.html")));
            assertTrue(Files.notExists(tempDir.resolve("external/benchmark-bucket/tenant-artifacts/tenant-a/report-001/benchmark-report-report-001.html")));
            assertTrue(Files.notExists(tempDir.resolve("live-evidence/report-001/html-export.json")));
            assertEquals(1, primaryStore.size());
            assertEquals(1, recoveryStore.size());
            assertFalse(primaryStore.keySet().iterator().next().contains("benchmark-report-report-001.html"));
            assertFalse(recoveryStore.keySet().iterator().next().contains("benchmark-report-report-001.html"));
        } finally {
            primaryServer.stop(0);
            recoveryServer.stop(0);
        }
    }

    private BenchmarkReportArtifact artifact(String artifactKey,
                                             String fileName,
                                             BenchmarkReportFormat format,
                                             String content) {
        return new BenchmarkReportArtifact(
            artifactKey,
            BenchmarkReportArtifactKind.REPORT_EXPORT,
            format,
            fileName,
            format.getContentType(),
            Integer.valueOf(content.length()),
            "checksum",
            null,
            null,
            null,
            content
        );
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

    private HttpServer startObjectStoreServer(Map<String, byte[]> objectStore) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/provider", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                String path = exchange.getRequestURI().getPath();
                if (!path.startsWith("/provider/")) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                String key = path.substring("/provider/".length());
                if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    objectStore.put(key, readBody(exchange));
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && objectStore.containsKey(key)) {
                    byte[] bytes = objectStore.get(key);
                    exchange.getResponseHeaders().add("ETag", "etag-" + key.hashCode());
                    exchange.getResponseHeaders().add("X-Request-Id", "request-primary");
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
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }
                if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    objectStore.remove(key);
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
