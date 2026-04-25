package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import org.springframework.http.MediaType;

public class LocalFileBenchmarkArtifactStorageAdapter implements BenchmarkArtifactStorageAdapter {

    private final BenchmarkArtifactStorageProperties storageProperties;

    public LocalFileBenchmarkArtifactStorageAdapter(BenchmarkArtifactStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String storageType() {
        return "LOCAL_FILE";
    }

    @Override
    public BenchmarkReportArtifact externalize(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact) {
        if (artifact == null || artifact.getContent() == null) {
            return artifact;
        }
        try {
            Path reportDir = resolveReportDir(context.getReportId());
            Files.createDirectories(reportDir);
            Path artifactPath = reportDir.resolve(artifact.getFileName());
            Files.write(
                artifactPath,
                artifact.getContent().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            BenchmarkArtifactTenantPolicy tenantPolicy = context.getTenantPolicy();
            return artifact.externalized(
                storageType(),
                artifactPath.toUri().toString(),
                "repo-local-path=" + artifactPath.toAbsolutePath().normalize(),
                tenantPolicy == null ? null : tenantPolicy.getRetentionDays(),
                tenantPolicy == null ? null : tenantPolicy.getRetentionPolicySource(),
                tenantPolicy == null ? null : tenantPolicy.getRetentionDeleteAfter()
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to externalize benchmark artifact", ex);
        }
    }

    @Override
    public BenchmarkArtifactReadResult loadArtifact(BenchmarkReportArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("Benchmark artifact must not be null");
        }
        try {
            if (artifact.getContent() != null) {
                return new BenchmarkArtifactReadResult(
                    new BenchmarkRenderedReport(
                        MediaType.parseMediaType(artifact.getMediaType()),
                        artifact.getFileName(),
                        artifact.getContent().getBytes(StandardCharsets.UTF_8)
                    ),
                    false,
                    "INLINE_CONTENT",
                    "INLINE_CONTENT"
                );
            }
            if (artifact.getStorageUri() == null) {
                throw new IllegalStateException("Benchmark artifact content and storageUri are both missing");
            }
            byte[] bytes = Files.readAllBytes(Paths.get(URI.create(artifact.getStorageUri())));
            return new BenchmarkArtifactReadResult(
                new BenchmarkRenderedReport(
                    MediaType.parseMediaType(artifact.getMediaType()),
                    artifact.getFileName(),
                    bytes
                ),
                false,
                "LOCAL_FILE",
                "LOCAL_FILE_READ"
            );
        } catch (NoSuchFileException ex) {
            throw new IllegalStateException("Benchmark artifact is missing from storage", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load benchmark artifact from storage", ex);
        }
    }

    @Override
    public void cleanupStaleArtifacts(BenchmarkArtifactStorageContext context, BenchmarkArtifactCleanupPlan cleanupPlan) {
        Path reportDir = resolveReportDir(context.getReportId());
        deleteStaleFiles(reportDir, cleanupPlan == null ? java.util.Collections.<String>emptySet() : cleanupPlan.getRetainedFileNames());
    }

    @Override
    public BenchmarkArtifactCleanupResult cleanupArtifact(BenchmarkArtifactStorageContext context,
                                                          BenchmarkReportArtifact artifact,
                                                          String cleanupScope) {
        if (artifact == null || artifact.getStorageUri() == null) {
            throw new IllegalStateException("Local benchmark artifact is missing storageUri");
        }
        Path artifactPath = Paths.get(URI.create(artifact.getStorageUri()));
        boolean deleted = deleteIfExists(artifactPath);
        LinkedHashMap<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("cleanupTargets", Collections.singletonList("localFile"));
        details.put("localFileDeleteStatus", deleted ? "DELETED" : "MISSING");
        details.put("providerAuthUsed", Boolean.FALSE);
        return new BenchmarkArtifactCleanupResult(
            "CLEANUP_COMPLETED",
            cleanupScope,
            "LOCAL_FILE",
            deleted ? "LOCAL_FILE_DELETED" : "LOCAL_FILE_MISSING",
            "localFile",
            details
        );
    }

    private Path resolveReportDir(String reportId) {
        Path baseDir = Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
        return baseDir.resolve(reportId);
    }

    private void deleteStaleFiles(Path reportDir, Set<String> retainedFileNames) {
        if (!Files.isDirectory(reportDir)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.list(reportDir)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> !retainedFileNames.contains(path.getFileName().toString()))
                .forEach(this::deleteQuietly);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to cleanup stale benchmark artifacts", ex);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete stale benchmark artifact " + path, ex);
        }
    }

    private boolean deleteIfExists(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to cleanup benchmark artifact " + path, ex);
        }
    }
}
