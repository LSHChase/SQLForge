package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.sqlforge.common.utils.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
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
            byte[] contentBytes = artifact.getContent().getBytes(StandardCharsets.UTF_8);
            Path reportDir = resolveMirrorReportDir(context.getReportId());
            Files.createDirectories(reportDir);
            Path artifactPath = reportDir.resolve(artifact.getFileName());
            Files.write(
                artifactPath,
                contentBytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            BenchmarkArtifactTenantPolicy tenantPolicy = context.getTenantPolicy();
            String objectUri = buildObjectUri(context, artifact);
            StorageVerification verification = verifyStorage(context, artifact, contentBytes);
            LiveEvidenceManifest liveEvidenceManifest = writeLiveEvidence(context, artifact, objectUri, artifactPath, verification);
            return artifact.externalized(
                storageType(),
                objectUri,
                buildEvidence(objectUri, artifactPath, verification, liveEvidenceManifest),
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
            byte[] bytes = loadBytesWithRecovery(artifact);
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
        String effectiveBucket = resolveObjectUriBucket();
        String key = buildObjectKey(context, artifact);
        return "env-obj://" + effectiveBucket + "/" + key;
    }

    private String buildObjectKey(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact) {
        String keyPrefix = storageProperties.getEnvironmentObjectStorage().getKeyPrefix();
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(keyPrefix)) {
            key.append(trimSlashes(keyPrefix)).append('/');
        }
        if (StringUtils.hasText(context.getTenantId())) {
            key.append(trimSlashes(context.getTenantId())).append('/');
        }
        key.append(trimSlashes(context.getReportId())).append('/').append(artifact.getFileName());
        return key.toString();
    }

    private String buildEvidence(String objectUri,
                                 Path mirrorPath,
                                 StorageVerification verification,
                                 LiveEvidenceManifest liveEvidenceManifest) {
        String mode = verification.describeMode();
        return "objectUri=" + objectUri
            + ";mirrorPath=" + mirrorPath.toAbsolutePath().normalize()
            + ";endpointEnv=" + storageProperties.getEnvironmentObjectStorage().getEndpointEnvName()
            + ";bucketEnv=" + storageProperties.getEnvironmentObjectStorage().getBucketEnvName()
            + ";credentialsEnv=" + storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName()
            + ";externalWriteDirEnv=" + storageProperties.getEnvironmentObjectStorage().getExternalWriteDirEnvName()
            + ";mode=" + mode
            + ";providerEndpoint=" + verification.getProviderEndpoint()
            + ";providerObjectUrl=" + verification.getProviderObjectUrl()
            + ";providerWriteStatus=" + verification.getProviderWriteStatus()
            + ";providerRecoveryStatus=" + verification.getProviderRecoveryStatus()
            + ";externalWritePath=" + verification.getExternalWritePath()
            + ";externalWriteStatus=" + verification.getExternalWriteStatus()
            + ";recoveryVerificationStatus=" + verification.getRecoveryVerificationStatus()
            + ";liveEvidencePath=" + liveEvidenceManifest.getManifestPath()
            + ";liveEvidenceStatus=" + liveEvidenceManifest.getEvidenceStatus();
    }

    private LiveEvidenceManifest writeLiveEvidence(BenchmarkArtifactStorageContext context,
                                                   BenchmarkReportArtifact artifact,
                                                   String objectUri,
                                                   Path mirrorPath,
                                                   StorageVerification verification) throws IOException {
        Path reportDir = Paths.get(storageProperties.getEnvironmentObjectStorage().getLiveEvidenceDir())
            .toAbsolutePath()
            .normalize()
            .resolve(context.getReportId());
        Files.createDirectories(reportDir);
        Path manifestPath = reportDir.resolve(artifact.getArtifactKey() + ".json");
        String effectiveBucket = resolveBucketValue();
        String endpoint = resolveEndpointValue();
        String credentials = resolveCredentialsValue();
        String evidenceStatus = verification.isProviderVerified()
            ? "PROVIDER_LIVE_EVIDENCE_VERIFIED"
            : verification.isExternalWriteVerified()
            ? "EXTERNAL_WRITE_RECOVERY_VERIFIED"
            : StringUtils.hasText(endpoint) && StringUtils.hasText(effectiveBucket)
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
        manifest.put("providerEndpoint", verification.getProviderEndpoint());
        manifest.put("providerObjectUrl", verification.getProviderObjectUrl());
        manifest.put("providerWriteStatus", verification.getProviderWriteStatus());
        manifest.put("providerRecoveryStatus", verification.getProviderRecoveryStatus());
        manifest.put("externalWritePath", verification.getExternalWritePath());
        manifest.put("externalWriteStatus", verification.getExternalWriteStatus());
        manifest.put("recoveryVerificationStatus", verification.getRecoveryVerificationStatus());
        manifest.put("externalWriteDir", verification.getExternalWriteDir());
        manifest.put("externalWriteDirEnvName", storageProperties.getEnvironmentObjectStorage().getExternalWriteDirEnvName());
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

    private StorageVerification verifyStorage(BenchmarkArtifactStorageContext context,
                                              BenchmarkReportArtifact artifact,
                                              byte[] contentBytes) throws IOException {
        StorageVerification verification = StorageVerification.pending();
        verification = verifyProviderBackedObject(context, artifact, contentBytes, verification);
        verification = verifyExternalWrite(context, artifact, contentBytes, verification);
        return verification;
    }

    private StorageVerification verifyProviderBackedObject(BenchmarkArtifactStorageContext context,
                                                           BenchmarkReportArtifact artifact,
                                                           byte[] contentBytes,
                                                           StorageVerification verification) throws IOException {
        String endpoint = resolveEndpointValue();
        if (!StringUtils.hasText(endpoint)) {
            return verification;
        }
        String providerObjectUrl = buildProviderObjectUrl(context, artifact);
        uploadProviderObject(providerObjectUrl, contentBytes);
        byte[] reloaded = downloadProviderObject(providerObjectUrl);
        if (!Arrays.equals(contentBytes, reloaded)) {
            throw new IllegalStateException("Provider-backed object-storage verification readback mismatch");
        }
        return verification.withProviderVerification(endpoint, providerObjectUrl, "VERIFIED", "VERIFIED");
    }

    private StorageVerification verifyExternalWrite(BenchmarkArtifactStorageContext context,
                                                    BenchmarkReportArtifact artifact,
                                                    byte[] contentBytes,
                                                    StorageVerification verification) throws IOException {
        Path externalRoot = resolveExternalWriteRoot();
        if (externalRoot == null) {
            return verification;
        }
        Path externalPath = resolveExternalWritePath(externalRoot, context, artifact);
        Files.createDirectories(externalPath.getParent());
        Files.write(
            externalPath,
            contentBytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
        byte[] reloaded = Files.readAllBytes(externalPath);
        if (!Arrays.equals(contentBytes, reloaded)) {
            throw new IllegalStateException("External object-storage verification readback mismatch");
        }
        return verification.withExternalVerification(externalRoot, externalPath, "VERIFIED", "VERIFIED");
    }

    private byte[] loadBytesWithRecovery(BenchmarkReportArtifact artifact) throws IOException {
        Path mirrorPath = resolveMirrorPath(artifact);
        try {
            return Files.readAllBytes(mirrorPath);
        } catch (NoSuchFileException mirrorMissing) {
            String providerObjectUrl = resolveEvidenceValue(artifact.getStorageEvidence(), "providerObjectUrl");
            String providerRecoveryStatus = resolveEvidenceValue(artifact.getStorageEvidence(), "providerRecoveryStatus");
            if (StringUtils.hasText(providerObjectUrl) && "VERIFIED".equals(providerRecoveryStatus)) {
                byte[] bytes = downloadProviderObject(providerObjectUrl);
                Files.createDirectories(mirrorPath.getParent());
                Files.write(
                    mirrorPath,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                );
                return bytes;
            }
            Path externalPath = resolveExternalWritePath(artifact);
            if (externalPath == null) {
                throw mirrorMissing;
            }
            byte[] bytes = Files.readAllBytes(externalPath);
            Files.createDirectories(mirrorPath.getParent());
            Files.write(
                mirrorPath,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            return bytes;
        }
    }

    private Path resolveExternalWriteRoot() {
        String externalWriteDir = storageProperties.getEnvironmentObjectStorage().getExternalWriteDir();
        if (!StringUtils.hasText(externalWriteDir)) {
            return null;
        }
        return Paths.get(externalWriteDir).toAbsolutePath().normalize();
    }

    private String buildProviderObjectUrl(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact) {
        String endpoint = trimTrailingSlash(resolveEndpointValue());
        return endpoint + "/" + resolveExternalBucket() + "/" + buildObjectKey(context, artifact);
    }

    private Path resolveExternalWritePath(Path externalRoot,
                                          BenchmarkArtifactStorageContext context,
                                          BenchmarkReportArtifact artifact) {
        String effectiveBucket = resolveExternalBucket();
        return externalRoot.resolve(effectiveBucket).resolve(buildObjectKey(context, artifact));
    }

    private Path resolveExternalWritePath(BenchmarkReportArtifact artifact) {
        String externalPath = resolveEvidenceValue(artifact.getStorageEvidence(), "externalWritePath");
        if (!StringUtils.hasText(externalPath)) {
            return null;
        }
        return Paths.get(externalPath);
    }

    private String resolveEndpointValue() {
        String endpoint = storageProperties.getEnvironmentObjectStorage().getEndpoint();
        if (StringUtils.hasText(endpoint)) {
            return endpoint.trim();
        }
        String envEndpoint = System.getenv(storageProperties.getEnvironmentObjectStorage().getEndpointEnvName());
        return StringUtils.hasText(envEndpoint) ? envEndpoint.trim() : null;
    }

    private String resolveCredentialsValue() {
        String credentials = storageProperties.getEnvironmentObjectStorage().getCredentials();
        if (StringUtils.hasText(credentials)) {
            return credentials.trim();
        }
        String envCredentials = System.getenv(storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName());
        return StringUtils.hasText(envCredentials) ? envCredentials.trim() : null;
    }

    private String resolveObjectUriBucket() {
        String bucket = resolveBucketValue();
        return StringUtils.hasText(bucket)
            ? bucket
            : "{env:" + storageProperties.getEnvironmentObjectStorage().getBucketEnvName() + "}";
    }

    private String resolveExternalBucket() {
        String bucket = resolveBucketValue();
        if (StringUtils.hasText(bucket)) {
            return trimSlashes(bucket);
        }
        return "unresolved-bucket";
    }

    private String resolveBucketValue() {
        String configuredBucket = storageProperties.getEnvironmentObjectStorage().getBucket();
        if (StringUtils.hasText(configuredBucket)) {
            return configuredBucket.trim();
        }
        String envBucket = System.getenv(storageProperties.getEnvironmentObjectStorage().getBucketEnvName());
        return StringUtils.hasText(envBucket) ? envBucket.trim() : null;
    }

    private Path resolveMirrorPath(BenchmarkReportArtifact artifact) {
        String mirrorPath = resolveEvidenceValue(artifact.getStorageEvidence(), "mirrorPath");
        if (StringUtils.hasText(mirrorPath)) {
            return Paths.get(mirrorPath);
        }
        throw new IllegalStateException("Environment-backed artifact is missing mirrorPath evidence");
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

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void uploadProviderObject(String providerObjectUrl, byte[] contentBytes) throws IOException {
        HttpURLConnection connection = openProviderConnection(providerObjectUrl, "PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setFixedLengthStreamingMode(contentBytes.length);
        try {
            connection.connect();
            connection.getOutputStream().write(contentBytes);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Provider-backed object-storage write failed with status " + status);
            }
        } finally {
            connection.disconnect();
        }
    }

    private byte[] downloadProviderObject(String providerObjectUrl) throws IOException {
        HttpURLConnection connection = openProviderConnection(providerObjectUrl, "GET");
        try {
            connection.connect();
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Provider-backed object-storage read failed with status " + status);
            }
            return readAllBytes(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openProviderConnection(String providerObjectUrl, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(providerObjectUrl).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(storageProperties.getEnvironmentObjectStorage().getProviderConnectTimeoutMs());
        connection.setReadTimeout(storageProperties.getEnvironmentObjectStorage().getProviderReadTimeoutMs());
        String credentials = resolveCredentialsValue();
        if (StringUtils.hasText(credentials)) {
            connection.setRequestProperty("Authorization", credentials);
        }
        return connection;
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (true) {
            int read = inputStream.read(buffer);
            if (read < 0) {
                return outputStream.toByteArray();
            }
            outputStream.write(buffer, 0, read);
        }
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

    private static final class StorageVerification {

        private final String providerEndpoint;
        private final String providerObjectUrl;
        private final String providerWriteStatus;
        private final String providerRecoveryStatus;
        private final String externalWritePath;
        private final String externalWriteDir;
        private final String externalWriteStatus;
        private final String recoveryVerificationStatus;

        private StorageVerification(String providerEndpoint,
                                    String providerObjectUrl,
                                    String providerWriteStatus,
                                    String providerRecoveryStatus,
                                    String externalWritePath,
                                    String externalWriteDir,
                                    String externalWriteStatus,
                                    String recoveryVerificationStatus) {
            this.providerEndpoint = providerEndpoint;
            this.providerObjectUrl = providerObjectUrl;
            this.providerWriteStatus = providerWriteStatus;
            this.providerRecoveryStatus = providerRecoveryStatus;
            this.externalWritePath = externalWritePath;
            this.externalWriteDir = externalWriteDir;
            this.externalWriteStatus = externalWriteStatus;
            this.recoveryVerificationStatus = recoveryVerificationStatus;
        }

        private static StorageVerification pending() {
            return new StorageVerification(null, null, "PENDING", "PENDING", null, null, "PENDING", "PENDING");
        }

        private StorageVerification withProviderVerification(String providerEndpoint,
                                                             String providerObjectUrl,
                                                             String providerWriteStatus,
                                                             String providerRecoveryStatus) {
            return new StorageVerification(
                providerEndpoint,
                providerObjectUrl,
                providerWriteStatus,
                providerRecoveryStatus,
                externalWritePath,
                externalWriteDir,
                externalWriteStatus,
                recoveryVerificationStatus
            );
        }

        private StorageVerification withExternalVerification(Path externalWriteDir,
                                                             Path externalWritePath,
                                                             String externalWriteStatus,
                                                             String recoveryVerificationStatus) {
            return new StorageVerification(
                providerEndpoint,
                providerObjectUrl,
                providerWriteStatus,
                providerRecoveryStatus,
                externalWritePath.toAbsolutePath().normalize().toString(),
                externalWriteDir.toAbsolutePath().normalize().toString(),
                externalWriteStatus,
                recoveryVerificationStatus
            );
        }

        private boolean isProviderVerified() {
            return "VERIFIED".equals(providerWriteStatus) && "VERIFIED".equals(providerRecoveryStatus);
        }

        private boolean isExternalWriteVerified() {
            return "VERIFIED".equals(externalWriteStatus) && "VERIFIED".equals(recoveryVerificationStatus);
        }

        private String describeMode() {
            if (isProviderVerified() && isExternalWriteVerified()) {
                return "repo-local-mirror+provider-live-evidence-verified+external-write-verified";
            }
            if (isProviderVerified()) {
                return "repo-local-mirror+provider-live-evidence-verified";
            }
            if (isExternalWriteVerified()) {
                return "repo-local-mirror+external-write-verified";
            }
            return "repo-local-mirror";
        }

        private String getProviderEndpoint() {
            return providerEndpoint;
        }

        private String getProviderObjectUrl() {
            return providerObjectUrl;
        }

        private String getProviderWriteStatus() {
            return providerWriteStatus;
        }

        private String getProviderRecoveryStatus() {
            return providerRecoveryStatus;
        }

        private String getExternalWritePath() {
            return externalWritePath;
        }

        private String getExternalWriteDir() {
            return externalWriteDir;
        }

        private String getExternalWriteStatus() {
            return externalWriteStatus;
        }

        private String getRecoveryVerificationStatus() {
            return recoveryVerificationStatus;
        }
    }
}
