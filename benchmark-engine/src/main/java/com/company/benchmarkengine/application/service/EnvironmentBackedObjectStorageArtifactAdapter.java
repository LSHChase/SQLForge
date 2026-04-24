package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.sqlforge.common.utils.JsonUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

public class EnvironmentBackedObjectStorageArtifactAdapter implements BenchmarkArtifactStorageAdapter {

    private final BenchmarkArtifactStorageProperties storageProperties;

    public EnvironmentBackedObjectStorageArtifactAdapter(BenchmarkArtifactStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String storageType() {
        return "ENVIRONMENT_OBJECT_STORAGE";
    }

    @Override
    public BenchmarkReportArtifact externalize(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact) {
        if (artifact == null || artifact.getContent() == null) {
            return artifact;
        }
        try {
            Path reportDir = resolveMirrorReportDir(context.getReportId());
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
            String objectUri = buildObjectUri(context, artifact);
            LiveEvidenceManifest liveEvidenceManifest = writeLiveEvidence(context, artifact, objectUri, artifactPath);
            return artifact.externalized(
                storageType(),
                objectUri,
                buildEvidence(objectUri, artifactPath, liveEvidenceManifest),
                tenantPolicy == null ? null : tenantPolicy.getRetentionDays(),
                tenantPolicy == null ? null : tenantPolicy.getRetentionPolicySource(),
                tenantPolicy == null ? null : tenantPolicy.getRetentionDeleteAfter()
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to externalize benchmark artifact to environment-backed mirror", ex);
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
            byte[] bytes = Files.readAllBytes(resolveMirrorPath(artifact));
            return new BenchmarkRenderedReport(
                MediaType.parseMediaType(artifact.getMediaType()),
                artifact.getFileName(),
                bytes
            );
        } catch (NoSuchFileException ex) {
            throw new IllegalStateException("Benchmark artifact is missing from environment-backed mirror", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load benchmark artifact from environment-backed mirror", ex);
        }
    }

    private Path resolveMirrorReportDir(String reportId) {
        String mirrorDir = storageProperties.getEnvironmentObjectStorage().getMirrorDir();
        return Paths.get(mirrorDir).toAbsolutePath().normalize().resolve(reportId);
    }

    private String buildObjectUri(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact) {
        String bucket = storageProperties.getEnvironmentObjectStorage().getBucket();
        String effectiveBucket = StringUtils.hasText(bucket) ? bucket : "{env:"
            + storageProperties.getEnvironmentObjectStorage().getBucketEnvName() + "}";
        String keyPrefix = storageProperties.getEnvironmentObjectStorage().getKeyPrefix();
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(keyPrefix)) {
            key.append(trimSlashes(keyPrefix)).append('/');
        }
        if (StringUtils.hasText(context.getTenantId())) {
            key.append(trimSlashes(context.getTenantId())).append('/');
        }
        key.append(trimSlashes(context.getReportId())).append('/').append(artifact.getFileName());
        return "env-obj://" + effectiveBucket + "/" + key.toString();
    }

    private String buildEvidence(String objectUri, Path mirrorPath, LiveEvidenceManifest liveEvidenceManifest) {
        return "objectUri=" + objectUri
            + ";mirrorPath=" + mirrorPath.toAbsolutePath().normalize()
            + ";endpointEnv=" + storageProperties.getEnvironmentObjectStorage().getEndpointEnvName()
            + ";bucketEnv=" + storageProperties.getEnvironmentObjectStorage().getBucketEnvName()
            + ";credentialsEnv=" + storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName()
            + ";mode=repo-local-mirror"
            + ";liveEvidencePath=" + liveEvidenceManifest.getManifestPath()
            + ";liveEvidenceStatus=" + liveEvidenceManifest.getEvidenceStatus();
    }

    private LiveEvidenceManifest writeLiveEvidence(BenchmarkArtifactStorageContext context,
                                                   BenchmarkReportArtifact artifact,
                                                   String objectUri,
                                                   Path mirrorPath) throws IOException {
        Path reportDir = Paths.get(storageProperties.getEnvironmentObjectStorage().getLiveEvidenceDir())
            .toAbsolutePath()
            .normalize()
            .resolve(context.getReportId());
        Files.createDirectories(reportDir);
        Path manifestPath = reportDir.resolve(artifact.getArtifactKey() + ".json");
        String configuredBucket = storageProperties.getEnvironmentObjectStorage().getBucket();
        String envBucket = System.getenv(storageProperties.getEnvironmentObjectStorage().getBucketEnvName());
        String endpoint = System.getenv(storageProperties.getEnvironmentObjectStorage().getEndpointEnvName());
        String credentials = System.getenv(storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName());
        String effectiveBucket = StringUtils.hasText(configuredBucket) ? configuredBucket : envBucket;
        String evidenceStatus = StringUtils.hasText(endpoint) && StringUtils.hasText(effectiveBucket)
            ? "LIVE_ENVIRONMENT_CONFIG_CAPTURED"
            : "ENVIRONMENT_CONFIG_PENDING";
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        manifest.put("storageType", storageType());
        manifest.put("reportId", context.getReportId());
        manifest.put("tenantId", context.getTenantId());
        manifest.put("artifactKey", artifact.getArtifactKey());
        manifest.put("fileName", artifact.getFileName());
        manifest.put("objectUri", objectUri);
        manifest.put("mirrorPath", mirrorPath.toAbsolutePath().normalize().toString());
        manifest.put("evidenceStatus", evidenceStatus);
        manifest.put("resolvedEndpoint", endpoint);
        manifest.put("resolvedBucket", effectiveBucket);
        manifest.put("credentialsPresent", Boolean.valueOf(StringUtils.hasText(credentials)));
        manifest.put("endpointEnvName", storageProperties.getEnvironmentObjectStorage().getEndpointEnvName());
        manifest.put("bucketEnvName", storageProperties.getEnvironmentObjectStorage().getBucketEnvName());
        manifest.put("credentialsEnvName", storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName());
        manifest.put("generatedAt", context.getGeneratedAt() == null ? null : context.getGeneratedAt().toString());
        Files.write(
            manifestPath,
            JsonUtils.toJson(manifest).getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
        return new LiveEvidenceManifest(manifestPath.toAbsolutePath().normalize(), evidenceStatus);
    }

    private Path resolveMirrorPath(BenchmarkReportArtifact artifact) {
        String storageEvidence = artifact.getStorageEvidence();
        if (StringUtils.hasText(storageEvidence)) {
            String[] parts = storageEvidence.split(";");
            for (String part : parts) {
                if (part.startsWith("mirrorPath=")) {
                    return Paths.get(part.substring("mirrorPath=".length()));
                }
            }
        }
        return Paths.get(URI.create(artifact.getStorageUri()));
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static final class LiveEvidenceManifest {

        private final Path manifestPath;
        private final String evidenceStatus;

        private LiveEvidenceManifest(Path manifestPath, String evidenceStatus) {
            this.manifestPath = manifestPath;
            this.evidenceStatus = evidenceStatus;
        }

        private String getManifestPath() {
            return manifestPath.toString();
        }

        private String getEvidenceStatus() {
            return evidenceStatus;
        }
    }
}
