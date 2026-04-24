package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;

public class BenchmarkArtifactLoadResult {

    private final BenchmarkRenderedReport renderedReport;
    private final BenchmarkReportArtifact resolvedArtifact;
    private final boolean recovered;
    private final String recoveryStatus;
    private final String storageRecoverySource;
    private final String storageReadStatus;

    public BenchmarkArtifactLoadResult(BenchmarkRenderedReport renderedReport,
                                       BenchmarkReportArtifact resolvedArtifact,
                                       boolean recovered,
                                       String recoveryStatus,
                                       String storageRecoverySource,
                                       String storageReadStatus) {
        this.renderedReport = renderedReport;
        this.resolvedArtifact = resolvedArtifact;
        this.recovered = recovered;
        this.recoveryStatus = recoveryStatus;
        this.storageRecoverySource = storageRecoverySource;
        this.storageReadStatus = storageReadStatus;
    }

    public BenchmarkRenderedReport getRenderedReport() {
        return renderedReport;
    }

    public BenchmarkReportArtifact getResolvedArtifact() {
        return resolvedArtifact;
    }

    public boolean isRecovered() {
        return recovered;
    }

    public String getRecoveryStatus() {
        return recoveryStatus;
    }

    public String getStorageRecoverySource() {
        return storageRecoverySource;
    }

    public String getStorageReadStatus() {
        return storageReadStatus;
    }
}
