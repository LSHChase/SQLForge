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
    public BenchmarkRenderedReport load(BenchmarkReportArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("Benchmark artifact must not be null");
        }
        try {
            if (artifact.getContent() != null) {
                return new BenchmarkRenderedReport(
                    MediaType.parseMediaType(artifact.getMediaType()),
                    artifact.getFileName(),
                    artifact.getContent().getBytes(StandardCharsets.UTF_8)
                );
            }
            if (artifact.getStorageUri() == null) {
                throw new IllegalStateException("Benchmark artifact content and storageUri are both missing");
            }
            byte[] bytes = Files.readAllBytes(Paths.get(URI.create(artifact.getStorageUri())));
            return new BenchmarkRenderedReport(
                MediaType.parseMediaType(artifact.getMediaType()),
                artifact.getFileName(),
                bytes
            );
        } catch (NoSuchFileException ex) {
            throw new IllegalStateException("Benchmark artifact is missing from storage", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load benchmark artifact from storage", ex);
        }
    }

    private Path resolveReportDir(String reportId) {
        Path baseDir = Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
        return baseDir.resolve(reportId);
    }
}
