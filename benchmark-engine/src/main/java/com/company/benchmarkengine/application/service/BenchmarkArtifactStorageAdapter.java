package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;

public interface BenchmarkArtifactStorageAdapter {

    String storageType();

    BenchmarkReportArtifact externalize(BenchmarkArtifactStorageContext context, BenchmarkReportArtifact artifact);

    BenchmarkRenderedReport load(BenchmarkReportArtifact artifact);
}
