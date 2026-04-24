package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkArtifactStorageService {

    private final BenchmarkArtifactStorageProperties storageProperties;
    private final BenchmarkArtifactTenantPolicyResolver tenantPolicyResolver;
    private final Map<String, BenchmarkArtifactStorageAdapter> adaptersByType;

    public BenchmarkArtifactStorageService(BenchmarkArtifactStorageProperties storageProperties,
                                           GovernanceCapabilityClient governanceCapabilityClient) {
        this.storageProperties = storageProperties;
        this.tenantPolicyResolver = new BenchmarkArtifactTenantPolicyResolver(storageProperties, governanceCapabilityClient);
        this.adaptersByType = new LinkedHashMap<String, BenchmarkArtifactStorageAdapter>();
        registerAdapter(new LocalFileBenchmarkArtifactStorageAdapter(storageProperties));
        registerAdapter(new EnvironmentBackedObjectStorageArtifactAdapter(storageProperties));
    }

    public List<BenchmarkReportArtifact> externalize(String reportId,
                                                     String tenantId,
                                                     Instant generatedAt,
                                                     List<BenchmarkReportArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Collections.emptyList();
        }
        BenchmarkArtifactTenantPolicy tenantPolicy = tenantPolicyResolver.resolve(tenantId, generatedAt);
        BenchmarkArtifactStorageContext context = new BenchmarkArtifactStorageContext(reportId, tenantId, generatedAt, tenantPolicy);
        List<BenchmarkReportArtifact> externalized = new ArrayList<BenchmarkReportArtifact>(artifacts.size());
        Set<String> retainedFileNames = new LinkedHashSet<String>(artifacts.size());
        BenchmarkArtifactStorageAdapter activeAdapter = requireAdapter(storageProperties.getStorageType());
        for (BenchmarkReportArtifact artifact : artifacts) {
            BenchmarkReportArtifact externalizedArtifact = activeAdapter.externalize(context, artifact);
            externalized.add(externalizedArtifact);
            if (externalizedArtifact != null && StringUtils.hasText(externalizedArtifact.getFileName())) {
                retainedFileNames.add(externalizedArtifact.getFileName());
            }
        }
        if (storageProperties.isCleanupStaleFiles()) {
            cleanupStaleFiles(resolveCleanupDir(reportId, activeAdapter.storageType()), retainedFileNames);
        }
        return externalized;
    }

    public BenchmarkReportArtifact externalize(String reportId,
                                               String tenantId,
                                               Instant generatedAt,
                                               BenchmarkReportArtifact artifact) {
        List<BenchmarkReportArtifact> artifacts = externalize(
            reportId,
            tenantId,
            generatedAt,
            Collections.singletonList(artifact)
        );
        return artifacts.isEmpty() ? null : artifacts.get(0);
    }

    public BenchmarkArtifactLoadResult loadOrRecover(String reportId,
                                                     String tenantId,
                                                     Instant generatedAt,
                                                     BenchmarkReportArtifact artifact,
                                                     Supplier<BenchmarkReportArtifact> recoverySupplier) {
        BenchmarkReportArtifact resolvedArtifact = maybeBackfillPolicy(tenantId, generatedAt, artifact);
        boolean policyBackfilled = resolvedArtifact != artifact;
        try {
            return new BenchmarkArtifactLoadResult(
                load(resolvedArtifact),
                resolvedArtifact,
                false,
                policyBackfilled ? "STORED_POLICY_BACKFILLED" : "STORED"
            );
        } catch (RuntimeException ex) {
            if (!storageProperties.isRecoveryEnabled() || recoverySupplier == null) {
                throw ex;
            }
            BenchmarkReportArtifact recoveredArtifact = recoverySupplier.get();
            if (recoveredArtifact == null || !StringUtils.hasText(recoveredArtifact.getContent())) {
                throw ex;
            }
            BenchmarkReportArtifact reExternalized = externalize(reportId, tenantId, generatedAt, recoveredArtifact);
            if (StringUtils.hasText(artifact.getExportId())) {
                reExternalized = reExternalized.withExportId(artifact.getExportId());
            }
            return new BenchmarkArtifactLoadResult(
                load(reExternalized),
                reExternalized,
                true,
                policyBackfilled ? "RECOVERED_FROM_REPORT_SNAPSHOT_POLICY_BACKFILLED" : "RECOVERED_FROM_REPORT_SNAPSHOT"
            );
        }
    }

    public BenchmarkRenderedReport load(BenchmarkReportArtifact artifact) {
        return requireAdapter(resolveStoredType(artifact)).load(artifact);
    }

    private BenchmarkReportArtifact maybeBackfillPolicy(String tenantId,
                                                        Instant generatedAt,
                                                        BenchmarkReportArtifact artifact) {
        if (!storageProperties.isTenantPolicyBackfillEnabled() || artifact == null) {
            return artifact;
        }
        if (StringUtils.hasText(artifact.getRetentionPolicySource())
            && (!StringUtils.hasText(artifact.getStorageEvidence())
                || !"ENVIRONMENT_OBJECT_STORAGE".equals(resolveStoredType(artifact)))) {
            return artifact;
        }
        BenchmarkArtifactTenantPolicy tenantPolicy = tenantPolicyResolver.resolve(tenantId, generatedAt);
        BenchmarkReportArtifact updatedArtifact = artifact.withRetentionPolicy(
            tenantPolicy.getRetentionDays(),
            tenantPolicy.getRetentionPolicySource(),
            tenantPolicy.getRetentionDeleteAfter()
        );
        if ("ENVIRONMENT_OBJECT_STORAGE".equals(resolveStoredType(artifact))
            && !StringUtils.hasText(updatedArtifact.getStorageEvidence())) {
            updatedArtifact = updatedArtifact.withStorageEvidence(
                "policyBackfill=true;mode=repo-local-mirror;source=" + tenantPolicy.getRetentionPolicySource()
            );
        }
        return updatedArtifact;
    }

    private void registerAdapter(BenchmarkArtifactStorageAdapter adapter) {
        adaptersByType.put(adapter.storageType(), adapter);
    }

    private BenchmarkArtifactStorageAdapter requireAdapter(String storageType) {
        String effectiveType = StringUtils.hasText(storageType) ? storageType : "LOCAL_FILE";
        BenchmarkArtifactStorageAdapter adapter = adaptersByType.get(effectiveType);
        if (adapter == null) {
            throw new IllegalStateException("Unsupported benchmark artifact storage type: " + effectiveType);
        }
        return adapter;
    }

    private String resolveStoredType(BenchmarkReportArtifact artifact) {
        if (artifact == null || !StringUtils.hasText(artifact.getStorageType())) {
            return "LOCAL_FILE";
        }
        return artifact.getStorageType();
    }

    private Path resolveCleanupDir(String reportId, String storageType) {
        if ("ENVIRONMENT_OBJECT_STORAGE".equals(storageType)) {
            return Paths.get(storageProperties.getEnvironmentObjectStorage().getMirrorDir())
                .toAbsolutePath()
                .normalize()
                .resolve(reportId);
        }
        return Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize().resolve(reportId);
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
