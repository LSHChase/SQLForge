package com.company.benchmarkengine.application.service;

public class BenchmarkArtifactTenantPolicy {

    private final String tenantId;
    private final Integer retentionDays;
    private final String retentionPolicySource;
    private final String retentionPolicyStatus;
    private final String retentionDeleteAfter;

    public BenchmarkArtifactTenantPolicy(String tenantId,
                                         Integer retentionDays,
                                         String retentionPolicySource,
                                         String retentionPolicyStatus,
                                         String retentionDeleteAfter) {
        this.tenantId = tenantId;
        this.retentionDays = retentionDays;
        this.retentionPolicySource = retentionPolicySource;
        this.retentionPolicyStatus = retentionPolicyStatus;
        this.retentionDeleteAfter = retentionDeleteAfter;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public String getRetentionPolicySource() {
        return retentionPolicySource;
    }

    public String getRetentionPolicyStatus() {
        return retentionPolicyStatus;
    }

    public String getRetentionDeleteAfter() {
        return retentionDeleteAfter;
    }
}
