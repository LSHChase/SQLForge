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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
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
        Path reportDir = resolveReportDir(reportId);
        List<BenchmarkReportArtifact> externalized = new ArrayList<BenchmarkReportArtifact>(artifacts.size());
        Set<String> retainedFileNames = new LinkedHashSet<String>(artifacts.size());
        for (BenchmarkReportArtifact artifact : artifacts) {
            BenchmarkReportArtifact externalizedArtifact = externalize(reportDir, artifact);
            externalized.add(externalizedArtifact);
            retainedFileNames.add(externalizedArtifact.getFileName());
        }
        if (storageProperties.isCleanupStaleFiles()) {
            cleanupStaleFiles(reportDir, retainedFileNames);
        }
        return externalized;
    }

    public BenchmarkReportArtifact externalize(String reportId, BenchmarkReportArtifact artifact) {
        return externalize(resolveReportDir(reportId), artifact);
    }

    public BenchmarkArtifactLoadResult loadOrRecover(String reportId,
                                                     BenchmarkReportArtifact artifact,
                                                     Supplier<BenchmarkReportArtifact> recoverySupplier) {
        try {
            return new BenchmarkArtifactLoadResult(load(artifact), artifact, false, "STORED");
        } catch (RuntimeException ex) {
            if (!storageProperties.isRecoveryEnabled() || recoverySupplier == null) {
                throw ex;
            }
            BenchmarkReportArtifact recoveredArtifact = recoverySupplier.get();
            if (recoveredArtifact == null || !StringUtils.hasText(recoveredArtifact.getContent())) {
                throw ex;
            }
            BenchmarkReportArtifact reExternalized = externalize(reportId, recoveredArtifact);
            if (StringUtils.hasText(artifact.getExportId())) {
                reExternalized = reExternalized.withExportId(artifact.getExportId());
            }
            return new BenchmarkArtifactLoadResult(load(reExternalized), reExternalized, true, "RECOVERED_FROM_REPORT_SNAPSHOT");
        }
    }

    private BenchmarkReportArtifact externalize(Path reportDir, BenchmarkReportArtifact artifact) {
        if (artifact == null) {
            return null;
        }
        if (!StringUtils.hasText(artifact.getContent())) {
            return artifact;
        }
        try {
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

    private void cleanupStaleFiles(Path reportDir, Set<String> retainedFileNames) {
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
}
