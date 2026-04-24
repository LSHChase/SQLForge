package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.util.StringUtils;

public class BenchmarkArtifactTenantPolicyResolver {

    private static final String POLICY_SCOPE = "BENCHMARK_ARTIFACT";

    private final BenchmarkArtifactStorageProperties storageProperties;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public BenchmarkArtifactTenantPolicyResolver(BenchmarkArtifactStorageProperties storageProperties,
                                                 GovernanceCapabilityClient governanceCapabilityClient) {
        this.storageProperties = storageProperties;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public BenchmarkArtifactTenantPolicy resolve(String tenantId, Instant generatedAt) {
        GovernanceTenantArtifactPolicyResponse response = null;
        if (governanceCapabilityClient != null && StringUtils.hasText(tenantId)) {
            response = governanceCapabilityClient.resolveTenantArtifactPolicy(tenantId, POLICY_SCOPE);
        }
        Integer retentionDays = response == null ? null : response.getRetentionDays();
        String retentionPolicySource = response == null ? null : response.getRetentionPolicySource();
        String retentionPolicyStatus = response == null ? null : response.getRetentionPolicyStatus();
        if (retentionDays == null && storageProperties.getDefaultRetentionDays() != null) {
            retentionDays = storageProperties.getDefaultRetentionDays();
            retentionPolicySource = "BENCHMARK_ENGINE_DEFAULT_RETENTION_DAYS";
            retentionPolicyStatus = "DEFAULT_RETENTION_ACTIVE";
        }
        if (!StringUtils.hasText(retentionPolicySource)) {
            retentionPolicySource = "LONG_TERM_DEFAULT";
        }
        if (!StringUtils.hasText(retentionPolicyStatus)) {
            retentionPolicyStatus = retentionDays == null ? "LONG_TERM_DEFAULT" : "DEFAULT_RETENTION_ACTIVE";
        }
        String retentionDeleteAfter = null;
        if (generatedAt != null && retentionDays != null && retentionDays.intValue() > 0) {
            retentionDeleteAfter = generatedAt.plus(retentionDays.longValue(), ChronoUnit.DAYS).toString();
        }
        return new BenchmarkArtifactTenantPolicy(
            tenantId,
            retentionDays,
            retentionPolicySource,
            retentionPolicyStatus,
            retentionDeleteAfter
        );
    }
}
