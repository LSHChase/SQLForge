package com.company.benchmarkengine.application.service;

public class BenchmarkArtifactReadResult {

    private final BenchmarkRenderedReport renderedReport;
    private final boolean fallbackRecoveryUsed;
    private final String recoverySource;
    private final String readStatus;

    public BenchmarkArtifactReadResult(BenchmarkRenderedReport renderedReport,
                                       boolean fallbackRecoveryUsed,
                                       String recoverySource,
                                       String readStatus) {
        this.renderedReport = renderedReport;
        this.fallbackRecoveryUsed = fallbackRecoveryUsed;
        this.recoverySource = recoverySource;
        this.readStatus = readStatus;
    }

    public BenchmarkRenderedReport getRenderedReport() {
        return renderedReport;
    }

    public boolean isFallbackRecoveryUsed() {
        return fallbackRecoveryUsed;
    }

    public String getRecoverySource() {
        return recoverySource;
    }

    public String getReadStatus() {
        return readStatus;
    }
}
