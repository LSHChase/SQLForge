package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkArtifactStorageService {

    private final BenchmarkArtifactStorageProperties storageProperties;

    public BenchmarkArtifactStorageService(BenchmarkArtifactStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public List<BenchmarkReportArtifact> externalize(String reportId, List<BenchmarkReportArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkReportArtifact> externalized = new ArrayList<BenchmarkReportArtifact>(artifacts.size());
        for (BenchmarkReportArtifact artifact : artifacts) {
            externalized.add(externalize(reportId, artifact));
        }
        return externalized;
    }

    public BenchmarkReportArtifact externalize(String reportId, BenchmarkReportArtifact artifact) {
        if (artifact == null) {
            return null;
        }
        if (!StringUtils.hasText(artifact.getContent())) {
            return artifact;
        }
        try {
            Path baseDir = Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
            Path reportDir = baseDir.resolve(reportId);
            Files.createDirectories(reportDir);
            Path artifactPath = reportDir.resolve(artifact.getFileName());
            Files.write(
                artifactPath,
                artifact.getContent().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            return artifact.externalized(storageProperties.getStorageType(), artifactPath.toUri().toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to externalize benchmark artifact", ex);
        }
    }

    public BenchmarkRenderedReport load(BenchmarkReportArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("Benchmark artifact must not be null");
        }
        try {
            if (StringUtils.hasText(artifact.getContent())) {
                return new BenchmarkRenderedReport(
                    MediaType.parseMediaType(artifact.getMediaType()),
                    artifact.getFileName(),
                    artifact.getContent().getBytes(StandardCharsets.UTF_8)
                );
            }
            if (!StringUtils.hasText(artifact.getStorageUri())) {
                throw new IllegalStateException("Benchmark artifact content and storageUri are both missing");
            }
            byte[] bytes = Files.readAllBytes(Paths.get(URI.create(artifact.getStorageUri())));
            return new BenchmarkRenderedReport(
                MediaType.parseMediaType(artifact.getMediaType()),
                artifact.getFileName(),
                bytes
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load benchmark artifact from storage", ex);
        }
    }
}
