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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

public class EnvironmentBackedObjectStorageArtifactAdapter implements BenchmarkArtifactStorageAdapter {

    private static final String VERIFIED = "VERIFIED";

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
            LoadedArtifact loadedArtifact = loadBytesWithRecovery(artifact);
            return new BenchmarkArtifactReadResult(
                new BenchmarkRenderedReport(
                    MediaType.parseMediaType(artifact.getMediaType()),
                    artifact.getFileName(),
                    loadedArtifact.bytes
                ),
                loadedArtifact.fallbackRecoveryUsed,
                loadedArtifact.recoverySource,
                loadedArtifact.readStatus
            );
        } catch (NoSuchFileException ex) {
            throw new IllegalStateException("Benchmark artifact is missing from environment-backed mirror", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load benchmark artifact from environment-backed mirror", ex);
        }
    }

    @Override
    public void cleanupStaleArtifacts(BenchmarkArtifactStorageContext context, BenchmarkArtifactCleanupPlan cleanupPlan) {
        Set<String> retainedFileNames = cleanupPlan == null
            ? Collections.<String>emptySet()
            : cleanupPlan.getRetainedFileNames();
        Set<String> retainedArtifactKeys = cleanupPlan == null
            ? Collections.<String>emptySet()
            : cleanupPlan.getRetainedArtifactKeys();
        Path mirrorDir = resolveMirrorReportDir(context.getReportId());
        Set<String> staleFileNames = listStaleFileNames(mirrorDir, retainedFileNames);
        deleteNamedFiles(mirrorDir, staleFileNames);
        deleteStaleEvidenceManifests(resolveLiveEvidenceReportDir(context.getReportId()), retainedArtifactKeys);
        Path externalRoot = resolveExternalWriteRoot();
        if (externalRoot != null) {
            deleteNamedFiles(resolveExternalWriteReportDir(externalRoot, context), staleFileNames);
        }
        cleanupStaleProviderObjects(resolvePrimaryProviderTarget(), context, staleFileNames);
        cleanupStaleProviderObjects(resolveRecoveryProviderTarget(), context, staleFileNames);
    }

    private Path resolveMirrorReportDir(String reportId) {
        String mirrorDir = storageProperties.getEnvironmentObjectStorage().getMirrorDir();
        return Paths.get(mirrorDir).toAbsolutePath().normalize().resolve(reportId);
    }

    private Path resolveLiveEvidenceReportDir(String reportId) {
        return Paths.get(storageProperties.getEnvironmentObjectStorage().getLiveEvidenceDir())
            .toAbsolutePath()
            .normalize()
            .resolve(reportId);
    }

    private String buildObjectUri(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact) {
        ProviderTarget primaryProvider = resolvePrimaryProviderTarget();
        String effectiveBucket = primaryProvider == null
            ? resolveObjectUriBucket()
            : trimSlashes(primaryProvider.bucket);
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

    private String buildObjectKey(BenchmarkArtifactStorageContext context, String fileName) {
        String keyPrefix = storageProperties.getEnvironmentObjectStorage().getKeyPrefix();
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(keyPrefix)) {
            key.append(trimSlashes(keyPrefix)).append('/');
        }
        if (StringUtils.hasText(context.getTenantId())) {
            key.append(trimSlashes(context.getTenantId())).append('/');
        }
        key.append(trimSlashes(context.getReportId())).append('/').append(fileName);
        return key.toString();
    }

    private String buildEvidence(String objectUri,
                                 Path mirrorPath,
                                 StorageVerification verification,
                                 LiveEvidenceManifest liveEvidenceManifest) {
        return "objectUri=" + objectUri
            + ";mirrorPath=" + mirrorPath.toAbsolutePath().normalize()
            + ";endpointEnv=" + storageProperties.getEnvironmentObjectStorage().getEndpointEnvName()
            + ";bucketEnv=" + storageProperties.getEnvironmentObjectStorage().getBucketEnvName()
            + ";credentialsEnv=" + storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName()
            + ";externalWriteDirEnv=" + storageProperties.getEnvironmentObjectStorage().getExternalWriteDirEnvName()
            + ";mode=" + verification.describeMode()
            + ";providerMode=" + verification.getProviderMode()
            + ";recoveryOrder=" + verification.getRecoveryOrder()
            + ";cleanupScope=" + verification.getCleanupScope()
            + ";primaryProvider=" + verification.getPrimaryProviderName()
            + ";primaryProviderContract=" + verification.getPrimaryProviderContract()
            + ";providerEndpoint=" + verification.getPrimaryProviderEndpoint()
            + ";providerObjectUrl=" + verification.getPrimaryProviderObjectUrl()
            + ";providerDialect=" + verification.getPrimaryProviderDialect()
            + ";providerWriteStatus=" + verification.getPrimaryProviderWriteStatus()
            + ";providerRecoveryStatus=" + verification.getPrimaryProviderRecoveryStatus()
            + ";providerHeadStatus=" + verification.getPrimaryProviderHeadStatus()
            + ";providerContentLength=" + verification.getPrimaryProviderContentLength()
            + ";providerContentType=" + verification.getPrimaryProviderContentType()
            + ";providerEtag=" + verification.getPrimaryProviderEtag()
            + ";providerRequestId=" + verification.getPrimaryProviderRequestId()
            + ";recoveryProvider=" + verification.getRecoveryProviderName()
            + ";recoveryProviderContract=" + verification.getRecoveryProviderContract()
            + ";recoveryProviderEndpoint=" + verification.getRecoveryProviderEndpoint()
            + ";recoveryProviderObjectUrl=" + verification.getRecoveryProviderObjectUrl()
            + ";recoveryProviderDialect=" + verification.getRecoveryProviderDialect()
            + ";recoveryProviderWriteStatus=" + verification.getRecoveryProviderWriteStatus()
            + ";recoveryProviderRecoveryStatus=" + verification.getRecoveryProviderRecoveryStatus()
            + ";recoveryProviderHeadStatus=" + verification.getRecoveryProviderHeadStatus()
            + ";recoveryProviderContentLength=" + verification.getRecoveryProviderContentLength()
            + ";recoveryProviderContentType=" + verification.getRecoveryProviderContentType()
            + ";recoveryProviderEtag=" + verification.getRecoveryProviderEtag()
            + ";recoveryProviderRequestId=" + verification.getRecoveryProviderRequestId()
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
        Path reportDir = resolveLiveEvidenceReportDir(context.getReportId());
        Files.createDirectories(reportDir);
        Path manifestPath = reportDir.resolve(artifact.getArtifactKey() + ".json");
        String effectiveBucket = resolveBucketValue();
        String endpoint = resolveEndpointValue();
        String credentials = resolveCredentialsValue();
        String evidenceStatus = verification.isPrimaryProviderVerified()
            || verification.isRecoveryProviderVerified()
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
        manifest.put("providerMode", verification.getProviderMode());
        manifest.put("recoveryOrder", verification.getRecoveryOrder());
        manifest.put("cleanupScope", verification.getCleanupScope());
        manifest.put("resolvedEndpoint", endpoint);
        manifest.put("resolvedBucket", effectiveBucket);
        manifest.put("credentialsPresent", Boolean.valueOf(StringUtils.hasText(credentials)));
        manifest.put("endpointEnvName", storageProperties.getEnvironmentObjectStorage().getEndpointEnvName());
        manifest.put("bucketEnvName", storageProperties.getEnvironmentObjectStorage().getBucketEnvName());
        manifest.put("credentialsEnvName", storageProperties.getEnvironmentObjectStorage().getCredentialsEnvName());
        manifest.put("primaryProvider", verification.getPrimaryProviderName());
        manifest.put("primaryProviderContract", verification.getPrimaryProviderContract());
        manifest.put("providerEndpoint", verification.getPrimaryProviderEndpoint());
        manifest.put("providerObjectUrl", verification.getPrimaryProviderObjectUrl());
        manifest.put("providerDialect", verification.getPrimaryProviderDialect());
        manifest.put("providerWriteStatus", verification.getPrimaryProviderWriteStatus());
        manifest.put("providerRecoveryStatus", verification.getPrimaryProviderRecoveryStatus());
        manifest.put("providerHeadStatus", verification.getPrimaryProviderHeadStatus());
        manifest.put("providerContentLength", verification.getPrimaryProviderContentLength());
        manifest.put("providerContentType", verification.getPrimaryProviderContentType());
        manifest.put("providerEtag", verification.getPrimaryProviderEtag());
        manifest.put("providerRequestId", verification.getPrimaryProviderRequestId());
        manifest.put("recoveryProvider", verification.getRecoveryProviderName());
        manifest.put("recoveryProviderContract", verification.getRecoveryProviderContract());
        manifest.put("recoveryProviderEndpoint", verification.getRecoveryProviderEndpoint());
        manifest.put("recoveryProviderObjectUrl", verification.getRecoveryProviderObjectUrl());
        manifest.put("recoveryProviderDialect", verification.getRecoveryProviderDialect());
        manifest.put("recoveryProviderWriteStatus", verification.getRecoveryProviderWriteStatus());
        manifest.put("recoveryProviderRecoveryStatus", verification.getRecoveryProviderRecoveryStatus());
        manifest.put("recoveryProviderHeadStatus", verification.getRecoveryProviderHeadStatus());
        manifest.put("recoveryProviderContentLength", verification.getRecoveryProviderContentLength());
        manifest.put("recoveryProviderContentType", verification.getRecoveryProviderContentType());
        manifest.put("recoveryProviderEtag", verification.getRecoveryProviderEtag());
        manifest.put("recoveryProviderRequestId", verification.getRecoveryProviderRequestId());
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
        StorageVerification verification = StorageVerification.pending(
            resolveProviderMode(),
            resolveRecoveryOrder(),
            resolveCleanupScope()
        );
        verification = verifyProviderBackedObject(resolvePrimaryProviderTarget(), context, artifact, contentBytes, verification, true);
        verification = verifyProviderBackedObject(resolveRecoveryProviderTarget(), context, artifact, contentBytes, verification, false);
        verification = verifyExternalWrite(context, artifact, contentBytes, verification);
        return verification;
    }

    private StorageVerification verifyProviderBackedObject(ProviderTarget providerTarget,
                                                           BenchmarkArtifactStorageContext context,
                                                           BenchmarkReportArtifact artifact,
                                                           byte[] contentBytes,
                                                           StorageVerification verification,
                                                           boolean primaryProvider) throws IOException {
        if (providerTarget == null) {
            return verification;
        }
        String providerObjectUrl = buildProviderObjectUrl(providerTarget, context, artifact.getFileName());
        uploadProviderObject(providerObjectUrl, contentBytes, providerTarget.credentials);
        byte[] reloaded = downloadProviderObject(providerObjectUrl, providerTarget.credentials);
        if (!Arrays.equals(contentBytes, reloaded)) {
            throw new IllegalStateException("Provider-backed object-storage verification readback mismatch");
        }
        ProviderObjectMetadata metadata = headProviderObject(providerObjectUrl, providerTarget.credentials);
        if (primaryProvider) {
            return verification.withPrimaryProviderVerification(
                providerTarget.providerName,
                providerTarget.providerContract,
                providerTarget.endpoint,
                providerObjectUrl,
                resolveProviderDialect(providerTarget),
                VERIFIED,
                VERIFIED,
                metadata
            );
        }
        return verification.withRecoveryProviderVerification(
            providerTarget.providerName,
            providerTarget.providerContract,
            providerTarget.endpoint,
            providerObjectUrl,
            resolveProviderDialect(providerTarget),
            VERIFIED,
            VERIFIED,
            metadata
        );
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
        return verification.withExternalVerification(externalRoot, externalPath, VERIFIED, VERIFIED);
    }

    private LoadedArtifact loadBytesWithRecovery(BenchmarkReportArtifact artifact) throws IOException {
        Path mirrorPath = resolveMirrorPath(artifact);
        try {
            return new LoadedArtifact(Files.readAllBytes(mirrorPath), false, "REPO_LOCAL_MIRROR", "STORED_MIRROR_READ");
        } catch (NoSuchFileException mirrorMissing) {
            IOException lastFailure = mirrorMissing;
            String primaryProviderObjectUrl = resolveEvidenceValue(artifact.getStorageEvidence(), "providerObjectUrl");
            if (StringUtils.hasText(primaryProviderObjectUrl)
                && VERIFIED.equals(resolveEvidenceValue(artifact.getStorageEvidence(), "providerRecoveryStatus"))) {
                try {
                    byte[] bytes = downloadProviderObject(
                        primaryProviderObjectUrl,
                        resolveProviderCredentials(resolvePrimaryProviderTarget())
                    );
                    writeMirrorBytes(mirrorPath, bytes);
                    return new LoadedArtifact(bytes, true, "PRIMARY_PROVIDER", "RECOVERED_FROM_PRIMARY_PROVIDER");
                } catch (IOException ex) {
                    lastFailure = ex;
                }
            }
            String recoveryProviderObjectUrl = resolveEvidenceValue(artifact.getStorageEvidence(), "recoveryProviderObjectUrl");
            if (StringUtils.hasText(recoveryProviderObjectUrl)
                && VERIFIED.equals(resolveEvidenceValue(artifact.getStorageEvidence(), "recoveryProviderRecoveryStatus"))) {
                try {
                    byte[] bytes = downloadProviderObject(
                        recoveryProviderObjectUrl,
                        resolveProviderCredentials(resolveRecoveryProviderTarget())
                    );
                    writeMirrorBytes(mirrorPath, bytes);
                    return new LoadedArtifact(bytes, true, "RECOVERY_PROVIDER", "RECOVERED_FROM_RECOVERY_PROVIDER");
                } catch (IOException ex) {
                    lastFailure = ex;
                }
            }
            Path externalPath = resolveExternalWritePath(artifact);
            if (externalPath != null && Files.exists(externalPath)) {
                byte[] bytes = Files.readAllBytes(externalPath);
                writeMirrorBytes(mirrorPath, bytes);
                return new LoadedArtifact(bytes, true, "EXTERNAL_WRITE", "RECOVERED_FROM_EXTERNAL_WRITE");
            }
            throw lastFailure;
        }
    }

    private void writeMirrorBytes(Path mirrorPath, byte[] bytes) throws IOException {
        Files.createDirectories(mirrorPath.getParent());
        Files.write(
            mirrorPath,
            bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }

    private Path resolveExternalWriteRoot() {
        String externalWriteDir = storageProperties.getEnvironmentObjectStorage().getExternalWriteDir();
        if (!StringUtils.hasText(externalWriteDir)) {
            return null;
        }
        return Paths.get(externalWriteDir).toAbsolutePath().normalize();
    }

    private Path resolveExternalWriteReportDir(Path externalRoot, BenchmarkArtifactStorageContext context) {
        String effectiveBucket = resolveExternalBucket();
        String keyPrefix = storageProperties.getEnvironmentObjectStorage().getKeyPrefix();
        Path reportDir = externalRoot.resolve(effectiveBucket);
        if (StringUtils.hasText(keyPrefix)) {
            reportDir = reportDir.resolve(trimSlashes(keyPrefix));
        }
        if (StringUtils.hasText(context.getTenantId())) {
            reportDir = reportDir.resolve(trimSlashes(context.getTenantId()));
        }
        return reportDir.resolve(trimSlashes(context.getReportId()));
    }

    private String buildProviderObjectUrl(ProviderTarget providerTarget,
                                          BenchmarkArtifactStorageContext context,
                                          String fileName) {
        return trimTrailingSlash(providerTarget.endpoint)
            + "/"
            + trimSlashes(providerTarget.bucket)
            + "/"
            + buildObjectKey(context, fileName);
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

    private ProviderTarget resolvePrimaryProviderTarget() {
        String endpoint = resolveEndpointValue();
        String bucket = resolveBucketValue();
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)) {
            return null;
        }
        return new ProviderTarget(
            "PRIMARY_PROVIDER",
            firstNonBlank(storageProperties.getEnvironmentObjectStorage().getProviderName(), "GENERIC_HTTP"),
            firstNonBlank(storageProperties.getEnvironmentObjectStorage().getProviderContract(), "HTTP_PUT_GET"),
            endpoint.trim(),
            bucket.trim(),
            resolveCredentialsValue()
        );
    }

    private ProviderTarget resolveRecoveryProviderTarget() {
        String endpoint = storageProperties.getEnvironmentObjectStorage().getRecoveryProviderEndpoint();
        String bucket = storageProperties.getEnvironmentObjectStorage().getRecoveryProviderBucket();
        if (!StringUtils.hasText(endpoint)) {
            return null;
        }
        String resolvedBucket = StringUtils.hasText(bucket) ? bucket.trim() : resolveBucketValue();
        if (!StringUtils.hasText(resolvedBucket)) {
            return null;
        }
        return new ProviderTarget(
            "RECOVERY_PROVIDER",
            firstNonBlank(storageProperties.getEnvironmentObjectStorage().getRecoveryProviderName(), "GENERIC_HTTP_RECOVERY"),
            firstNonBlank(
                storageProperties.getEnvironmentObjectStorage().getRecoveryProviderContract(),
                storageProperties.getEnvironmentObjectStorage().getProviderContract(),
                "HTTP_PUT_GET"
            ),
            endpoint.trim(),
            resolvedBucket.trim(),
            storageProperties.getEnvironmentObjectStorage().getRecoveryProviderCredentials()
        );
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

    private Set<String> listStaleFileNames(Path reportDir, Set<String> retainedFileNames) {
        LinkedHashMap<String, Boolean> staleFileNames = new LinkedHashMap<String, Boolean>();
        if (!Files.isDirectory(reportDir)) {
            return staleFileNames.keySet();
        }
        try (java.util.stream.Stream<Path> paths = Files.list(reportDir)) {
            paths
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(fileName -> !retainedFileNames.contains(fileName))
                .forEach(fileName -> staleFileNames.put(fileName, Boolean.TRUE));
            return staleFileNames.keySet();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to inspect stale environment-backed benchmark artifacts", ex);
        }
    }

    private void deleteNamedFiles(Path reportDir, Set<String> fileNames) {
        if (!Files.isDirectory(reportDir)) {
            return;
        }
        for (String fileName : fileNames) {
            deleteLocalFile(reportDir.resolve(fileName));
        }
    }

    private void deleteStaleEvidenceManifests(Path reportDir, Set<String> retainedArtifactKeys) {
        if (!Files.isDirectory(reportDir)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.list(reportDir)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .filter(path -> !retainedArtifactKeys.contains(stripJsonSuffix(path.getFileName().toString())))
                .forEach(this::deleteLocalFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to cleanup stale environment-backed evidence manifests", ex);
        }
    }

    private void cleanupStaleProviderObjects(ProviderTarget providerTarget,
                                             BenchmarkArtifactStorageContext context,
                                             Set<String> staleFileNames) {
        if (providerTarget == null || staleFileNames == null || staleFileNames.isEmpty()) {
            return;
        }
        for (String fileName : staleFileNames) {
            deleteProviderObjectQuietly(
                buildProviderObjectUrl(providerTarget, context, fileName),
                providerTarget.credentials
            );
        }
    }

    private void deleteProviderObjectQuietly(String providerObjectUrl, String credentials) {
        try {
            deleteProviderObject(providerObjectUrl, credentials);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to cleanup stale provider-backed artifact " + providerObjectUrl, ex);
        }
    }

    private void deleteProviderObject(String providerObjectUrl, String credentials) throws IOException {
        HttpURLConnection connection = openProviderConnection(providerObjectUrl, "DELETE", credentials);
        try {
            connection.connect();
            int status = connection.getResponseCode();
            if (status == 404) {
                return;
            }
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Provider-backed object-storage delete failed with status " + status);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void deleteLocalFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete stale benchmark artifact " + path, ex);
        }
    }

    private String resolveProviderMode() {
        ProviderTarget primary = resolvePrimaryProviderTarget();
        ProviderTarget recovery = resolveRecoveryProviderTarget();
        if (primary != null && recovery != null) {
            return "PRIMARY_PLUS_RECOVERY_PROVIDER";
        }
        if (primary != null) {
            return "PRIMARY_PROVIDER_ONLY";
        }
        if (recovery != null) {
            return "RECOVERY_PROVIDER_ONLY";
        }
        if (resolveExternalWriteRoot() != null) {
            return "EXTERNAL_WRITE_ONLY";
        }
        return "REPO_LOCAL_MIRROR_ONLY";
    }

    private String resolveRecoveryOrder() {
        StringBuilder recoveryOrder = new StringBuilder("REPO_LOCAL_MIRROR");
        if (resolvePrimaryProviderTarget() != null) {
            recoveryOrder.append(",PRIMARY_PROVIDER");
        }
        if (resolveRecoveryProviderTarget() != null) {
            recoveryOrder.append(",RECOVERY_PROVIDER");
        }
        if (resolveExternalWriteRoot() != null) {
            recoveryOrder.append(",EXTERNAL_WRITE");
        }
        recoveryOrder.append(",REPORT_SNAPSHOT");
        return recoveryOrder.toString();
    }

    private String resolveCleanupScope() {
        return firstNonBlank(
            storageProperties.getEnvironmentObjectStorage().getCleanupScope(),
            "MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER"
        );
    }

    private String resolveProviderCredentials(ProviderTarget providerTarget) {
        return providerTarget == null ? null : providerTarget.credentials;
    }

    private String resolveProviderDialect(ProviderTarget providerTarget) {
        if (providerTarget == null || !StringUtils.hasText(providerTarget.endpoint)) {
            return "GENERIC_HTTP";
        }
        String endpoint = providerTarget.endpoint.toLowerCase();
        if (endpoint.contains("amazonaws.com")) {
            return "S3_COMPATIBLE";
        }
        if (endpoint.contains("blob.core.windows.net")) {
            return "AZURE_BLOB";
        }
        if (endpoint.contains("storage.googleapis.com")) {
            return "GCS";
        }
        return "GENERIC_HTTP";
    }

    private String stripJsonSuffix(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
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

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

    private String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(first, firstNonBlank(second, third));
    }

    private String firstNonBlank(String first, String second, String third, String fourth) {
        return firstNonBlank(first, firstNonBlank(second, third, fourth));
    }

    private void uploadProviderObject(String providerObjectUrl, byte[] contentBytes, String credentials) throws IOException {
        HttpURLConnection connection = openProviderConnection(providerObjectUrl, "PUT", credentials);
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

    private byte[] downloadProviderObject(String providerObjectUrl, String credentials) throws IOException {
        HttpURLConnection connection = openProviderConnection(providerObjectUrl, "GET", credentials);
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

    private ProviderObjectMetadata headProviderObject(String providerObjectUrl, String credentials) throws IOException {
        HttpURLConnection connection = openProviderConnection(providerObjectUrl, "HEAD", credentials);
        try {
            connection.connect();
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return ProviderObjectMetadata.pending();
            }
            return new ProviderObjectMetadata(
                VERIFIED,
                connection.getHeaderField("Content-Length"),
                firstNonBlank(connection.getContentType(), connection.getHeaderField("Content-Type")),
                firstNonBlank(connection.getHeaderField("ETag"), connection.getHeaderField("Etag")),
                firstNonBlank(
                    connection.getHeaderField("x-amz-request-id"),
                    connection.getHeaderField("x-ms-request-id"),
                    connection.getHeaderField("x-goog-request-id"),
                    connection.getHeaderField("X-Request-Id")
                )
            );
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openProviderConnection(String providerObjectUrl,
                                                     String method,
                                                     String credentials) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(providerObjectUrl).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(storageProperties.getEnvironmentObjectStorage().getProviderConnectTimeoutMs());
        connection.setReadTimeout(storageProperties.getEnvironmentObjectStorage().getProviderReadTimeoutMs());
        if (StringUtils.hasText(credentials)) {
            connection.setRequestProperty("Authorization", credentials.trim());
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

    private static final class LoadedArtifact {

        private final byte[] bytes;
        private final boolean fallbackRecoveryUsed;
        private final String recoverySource;
        private final String readStatus;

        private LoadedArtifact(byte[] bytes, boolean fallbackRecoveryUsed, String recoverySource, String readStatus) {
            this.bytes = bytes;
            this.fallbackRecoveryUsed = fallbackRecoveryUsed;
            this.recoverySource = recoverySource;
            this.readStatus = readStatus;
        }
    }

    private static final class ProviderTarget {

        private final String role;
        private final String providerName;
        private final String providerContract;
        private final String endpoint;
        private final String bucket;
        private final String credentials;

        private ProviderTarget(String role,
                               String providerName,
                               String providerContract,
                               String endpoint,
                               String bucket,
                               String credentials) {
            this.role = role;
            this.providerName = providerName;
            this.providerContract = providerContract;
            this.endpoint = endpoint;
            this.bucket = bucket;
            this.credentials = credentials;
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

        private final String providerMode;
        private final String recoveryOrder;
        private final String cleanupScope;
        private final String primaryProviderName;
        private final String primaryProviderContract;
        private final String primaryProviderEndpoint;
        private final String primaryProviderObjectUrl;
        private final String primaryProviderDialect;
        private final String primaryProviderWriteStatus;
        private final String primaryProviderRecoveryStatus;
        private final String primaryProviderHeadStatus;
        private final String primaryProviderContentLength;
        private final String primaryProviderContentType;
        private final String primaryProviderEtag;
        private final String primaryProviderRequestId;
        private final String recoveryProviderName;
        private final String recoveryProviderContract;
        private final String recoveryProviderEndpoint;
        private final String recoveryProviderObjectUrl;
        private final String recoveryProviderDialect;
        private final String recoveryProviderWriteStatus;
        private final String recoveryProviderRecoveryStatus;
        private final String recoveryProviderHeadStatus;
        private final String recoveryProviderContentLength;
        private final String recoveryProviderContentType;
        private final String recoveryProviderEtag;
        private final String recoveryProviderRequestId;
        private final String externalWritePath;
        private final String externalWriteDir;
        private final String externalWriteStatus;
        private final String recoveryVerificationStatus;

        private StorageVerification(String providerMode,
                                    String recoveryOrder,
                                    String cleanupScope,
                                    String primaryProviderName,
                                    String primaryProviderContract,
                                    String primaryProviderEndpoint,
                                    String primaryProviderObjectUrl,
                                    String primaryProviderDialect,
                                    String primaryProviderWriteStatus,
                                    String primaryProviderRecoveryStatus,
                                    String primaryProviderHeadStatus,
                                    String primaryProviderContentLength,
                                    String primaryProviderContentType,
                                    String primaryProviderEtag,
                                    String primaryProviderRequestId,
                                    String recoveryProviderName,
                                    String recoveryProviderContract,
                                    String recoveryProviderEndpoint,
                                    String recoveryProviderObjectUrl,
                                    String recoveryProviderDialect,
                                    String recoveryProviderWriteStatus,
                                    String recoveryProviderRecoveryStatus,
                                    String recoveryProviderHeadStatus,
                                    String recoveryProviderContentLength,
                                    String recoveryProviderContentType,
                                    String recoveryProviderEtag,
                                    String recoveryProviderRequestId,
                                    String externalWritePath,
                                    String externalWriteDir,
                                    String externalWriteStatus,
                                    String recoveryVerificationStatus) {
            this.providerMode = providerMode;
            this.recoveryOrder = recoveryOrder;
            this.cleanupScope = cleanupScope;
            this.primaryProviderName = primaryProviderName;
            this.primaryProviderContract = primaryProviderContract;
            this.primaryProviderEndpoint = primaryProviderEndpoint;
            this.primaryProviderObjectUrl = primaryProviderObjectUrl;
            this.primaryProviderDialect = primaryProviderDialect;
            this.primaryProviderWriteStatus = primaryProviderWriteStatus;
            this.primaryProviderRecoveryStatus = primaryProviderRecoveryStatus;
            this.primaryProviderHeadStatus = primaryProviderHeadStatus;
            this.primaryProviderContentLength = primaryProviderContentLength;
            this.primaryProviderContentType = primaryProviderContentType;
            this.primaryProviderEtag = primaryProviderEtag;
            this.primaryProviderRequestId = primaryProviderRequestId;
            this.recoveryProviderName = recoveryProviderName;
            this.recoveryProviderContract = recoveryProviderContract;
            this.recoveryProviderEndpoint = recoveryProviderEndpoint;
            this.recoveryProviderObjectUrl = recoveryProviderObjectUrl;
            this.recoveryProviderDialect = recoveryProviderDialect;
            this.recoveryProviderWriteStatus = recoveryProviderWriteStatus;
            this.recoveryProviderRecoveryStatus = recoveryProviderRecoveryStatus;
            this.recoveryProviderHeadStatus = recoveryProviderHeadStatus;
            this.recoveryProviderContentLength = recoveryProviderContentLength;
            this.recoveryProviderContentType = recoveryProviderContentType;
            this.recoveryProviderEtag = recoveryProviderEtag;
            this.recoveryProviderRequestId = recoveryProviderRequestId;
            this.externalWritePath = externalWritePath;
            this.externalWriteDir = externalWriteDir;
            this.externalWriteStatus = externalWriteStatus;
            this.recoveryVerificationStatus = recoveryVerificationStatus;
        }

        private static StorageVerification pending(String providerMode, String recoveryOrder, String cleanupScope) {
            return new StorageVerification(
                providerMode,
                recoveryOrder,
                cleanupScope,
                null,
                null,
                null,
                null,
                null,
                "PENDING",
                "PENDING",
                "PENDING",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "PENDING",
                "PENDING",
                "PENDING",
                null,
                null,
                null,
                null,
                null,
                null,
                "PENDING",
                "PENDING"
            );
        }

        private StorageVerification withPrimaryProviderVerification(String providerName,
                                                                    String providerContract,
                                                                    String providerEndpoint,
                                                                    String providerObjectUrl,
                                                                    String providerDialect,
                                                                    String providerWriteStatus,
                                                                    String providerRecoveryStatus,
                                                                    ProviderObjectMetadata metadata) {
            return new StorageVerification(
                providerMode,
                recoveryOrder,
                cleanupScope,
                providerName,
                providerContract,
                providerEndpoint,
                providerObjectUrl,
                providerDialect,
                providerWriteStatus,
                providerRecoveryStatus,
                metadata.getHeadStatus(),
                metadata.getContentLength(),
                metadata.getContentType(),
                metadata.getEtag(),
                metadata.getRequestId(),
                recoveryProviderName,
                recoveryProviderContract,
                recoveryProviderEndpoint,
                recoveryProviderObjectUrl,
                recoveryProviderDialect,
                recoveryProviderWriteStatus,
                recoveryProviderRecoveryStatus,
                recoveryProviderHeadStatus,
                recoveryProviderContentLength,
                recoveryProviderContentType,
                recoveryProviderEtag,
                recoveryProviderRequestId,
                externalWritePath,
                externalWriteDir,
                externalWriteStatus,
                recoveryVerificationStatus
            );
        }

        private StorageVerification withRecoveryProviderVerification(String providerName,
                                                                     String providerContract,
                                                                     String providerEndpoint,
                                                                     String providerObjectUrl,
                                                                     String providerDialect,
                                                                     String providerWriteStatus,
                                                                     String providerRecoveryStatus,
                                                                     ProviderObjectMetadata metadata) {
            return new StorageVerification(
                providerMode,
                recoveryOrder,
                cleanupScope,
                primaryProviderName,
                primaryProviderContract,
                primaryProviderEndpoint,
                primaryProviderObjectUrl,
                primaryProviderDialect,
                primaryProviderWriteStatus,
                primaryProviderRecoveryStatus,
                primaryProviderHeadStatus,
                primaryProviderContentLength,
                primaryProviderContentType,
                primaryProviderEtag,
                primaryProviderRequestId,
                providerName,
                providerContract,
                providerEndpoint,
                providerObjectUrl,
                providerDialect,
                providerWriteStatus,
                providerRecoveryStatus,
                metadata.getHeadStatus(),
                metadata.getContentLength(),
                metadata.getContentType(),
                metadata.getEtag(),
                metadata.getRequestId(),
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
                providerMode,
                recoveryOrder,
                cleanupScope,
                primaryProviderName,
                primaryProviderContract,
                primaryProviderEndpoint,
                primaryProviderObjectUrl,
                primaryProviderDialect,
                primaryProviderWriteStatus,
                primaryProviderRecoveryStatus,
                primaryProviderHeadStatus,
                primaryProviderContentLength,
                primaryProviderContentType,
                primaryProviderEtag,
                primaryProviderRequestId,
                recoveryProviderName,
                recoveryProviderContract,
                recoveryProviderEndpoint,
                recoveryProviderObjectUrl,
                recoveryProviderDialect,
                recoveryProviderWriteStatus,
                recoveryProviderRecoveryStatus,
                recoveryProviderHeadStatus,
                recoveryProviderContentLength,
                recoveryProviderContentType,
                recoveryProviderEtag,
                recoveryProviderRequestId,
                externalWritePath.toAbsolutePath().normalize().toString(),
                externalWriteDir.toAbsolutePath().normalize().toString(),
                externalWriteStatus,
                recoveryVerificationStatus
            );
        }

        private boolean isPrimaryProviderVerified() {
            return VERIFIED.equals(primaryProviderWriteStatus) && VERIFIED.equals(primaryProviderRecoveryStatus);
        }

        private boolean isRecoveryProviderVerified() {
            return VERIFIED.equals(recoveryProviderWriteStatus) && VERIFIED.equals(recoveryProviderRecoveryStatus);
        }

        private boolean isExternalWriteVerified() {
            return VERIFIED.equals(externalWriteStatus) && VERIFIED.equals(recoveryVerificationStatus);
        }

        private String describeMode() {
            String mode = "repo-local-mirror";
            if (isPrimaryProviderVerified()) {
                mode = mode + "+provider-live-evidence-verified";
            }
            if (isRecoveryProviderVerified()) {
                mode = mode + "+recovery-provider-live-evidence-verified";
            }
            if (isExternalWriteVerified()) {
                mode = mode + "+external-write-verified";
            }
            return mode;
        }

        private String getProviderMode() {
            return providerMode;
        }

        private String getRecoveryOrder() {
            return recoveryOrder;
        }

        private String getCleanupScope() {
            return cleanupScope;
        }

        private String getPrimaryProviderName() {
            return primaryProviderName;
        }

        private String getPrimaryProviderContract() {
            return primaryProviderContract;
        }

        private String getPrimaryProviderEndpoint() {
            return primaryProviderEndpoint;
        }

        private String getPrimaryProviderObjectUrl() {
            return primaryProviderObjectUrl;
        }

        private String getPrimaryProviderDialect() {
            return primaryProviderDialect;
        }

        private String getPrimaryProviderWriteStatus() {
            return primaryProviderWriteStatus;
        }

        private String getPrimaryProviderRecoveryStatus() {
            return primaryProviderRecoveryStatus;
        }

        private String getPrimaryProviderHeadStatus() {
            return primaryProviderHeadStatus;
        }

        private String getPrimaryProviderContentLength() {
            return primaryProviderContentLength;
        }

        private String getPrimaryProviderContentType() {
            return primaryProviderContentType;
        }

        private String getPrimaryProviderEtag() {
            return primaryProviderEtag;
        }

        private String getPrimaryProviderRequestId() {
            return primaryProviderRequestId;
        }

        private String getRecoveryProviderName() {
            return recoveryProviderName;
        }

        private String getRecoveryProviderContract() {
            return recoveryProviderContract;
        }

        private String getRecoveryProviderEndpoint() {
            return recoveryProviderEndpoint;
        }

        private String getRecoveryProviderObjectUrl() {
            return recoveryProviderObjectUrl;
        }

        private String getRecoveryProviderDialect() {
            return recoveryProviderDialect;
        }

        private String getRecoveryProviderWriteStatus() {
            return recoveryProviderWriteStatus;
        }

        private String getRecoveryProviderRecoveryStatus() {
            return recoveryProviderRecoveryStatus;
        }

        private String getRecoveryProviderHeadStatus() {
            return recoveryProviderHeadStatus;
        }

        private String getRecoveryProviderContentLength() {
            return recoveryProviderContentLength;
        }

        private String getRecoveryProviderContentType() {
            return recoveryProviderContentType;
        }

        private String getRecoveryProviderEtag() {
            return recoveryProviderEtag;
        }

        private String getRecoveryProviderRequestId() {
            return recoveryProviderRequestId;
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

    private static final class ProviderObjectMetadata {

        private final String headStatus;
        private final String contentLength;
        private final String contentType;
        private final String etag;
        private final String requestId;

        private ProviderObjectMetadata(String headStatus,
                                       String contentLength,
                                       String contentType,
                                       String etag,
                                       String requestId) {
            this.headStatus = headStatus;
            this.contentLength = contentLength;
            this.contentType = contentType;
            this.etag = etag;
            this.requestId = requestId;
        }

        private static ProviderObjectMetadata pending() {
            return new ProviderObjectMetadata("PENDING", null, null, null, null);
        }

        private String getHeadStatus() {
            return headStatus;
        }

        private String getContentLength() {
            return contentLength;
        }

        private String getContentType() {
            return contentType;
        }

        private String getEtag() {
            return etag;
        }

        private String getRequestId() {
            return requestId;
        }
    }
}
