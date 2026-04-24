package com.company.benchmarkengine.application.service;

import java.time.Instant;

public class BenchmarkArtifactStorageContext {

    private final String reportId;
    private final String tenantId;
    private final Instant generatedAt;
    private final BenchmarkArtifactTenantPolicy tenantPolicy;

    public BenchmarkArtifactStorageContext(String reportId,
                                           String tenantId,
                                           Instant generatedAt,
                                           BenchmarkArtifactTenantPolicy tenantPolicy) {
        this.reportId = reportId;
        this.tenantId = tenantId;
        this.generatedAt = generatedAt;
        this.tenantPolicy = tenantPolicy;
    }

    public String getReportId() {
        return reportId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public BenchmarkArtifactTenantPolicy getTenantPolicy() {
        return tenantPolicy;
    }
}
