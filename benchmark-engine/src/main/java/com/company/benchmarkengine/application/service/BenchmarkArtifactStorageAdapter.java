package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;

public interface BenchmarkArtifactStorageAdapter {

    String storageType();

    BenchmarkReportArtifact externalize(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact);

    BenchmarkArtifactReadResult loadArtifact(BenchmarkReportArtifact artifact);

    void cleanupStaleArtifacts(BenchmarkArtifactStorageContext context, BenchmarkArtifactCleanupPlan cleanupPlan);

    BenchmarkArtifactCleanupResult cleanupArtifact(BenchmarkArtifactStorageContext context,
                                                   BenchmarkReportArtifact artifact,
                                                   String cleanupScope);
}
