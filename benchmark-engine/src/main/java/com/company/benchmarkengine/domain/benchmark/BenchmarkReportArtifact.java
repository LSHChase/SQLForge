package com.company.benchmarkengine.domain.benchmark;

public class BenchmarkReportArtifact {

    private final String artifactKey;
    private final BenchmarkReportArtifactKind artifactKind;
    private final BenchmarkReportFormat format;
    private final String fileName;
    private final String mediaType;
    private final Integer contentLength;
    private final String checksumSha256;
    private final String storageType;
    private final String storageUri;
    private final String storageEvidence;
    private final String exportId;
    private final Integer retentionDays;
    private final String retentionPolicySource;
    private final String retentionDeleteAfter;
    private final String content;

    public BenchmarkReportArtifact(BenchmarkReportFormat format,
                                   String fileName,
                                   String mediaType,
                                   Integer contentLength,
                                   String checksumSha256,
                                   String content) {
        this(
            defaultArtifactKey(BenchmarkReportArtifactKind.REPORT_EXPORT, format),
            BenchmarkReportArtifactKind.REPORT_EXPORT,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            null,
            null,
            null,
            null,
            null,
            null,
            content
        );
    }

    public BenchmarkReportArtifact(String artifactKey,
                                   BenchmarkReportArtifactKind artifactKind,
                                   BenchmarkReportFormat format,
                                   String fileName,
                                   String mediaType,
                                   Integer contentLength,
                                   String checksumSha256,
                                   String storageType,
                                   String storageUri,
                                   String exportId,
                                   String content) {
        this(
            artifactKey,
            artifactKind,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            storageType,
            storageUri,
            null,
            exportId,
            null,
            null,
            null,
            content
        );
    }

    public BenchmarkReportArtifact(String artifactKey,
                                   BenchmarkReportArtifactKind artifactKind,
                                   BenchmarkReportFormat format,
                                   String fileName,
                                   String mediaType,
                                   Integer contentLength,
                                   String checksumSha256,
                                   String storageType,
                                   String storageUri,
                                   String exportId,
                                   Integer retentionDays,
                                   String retentionPolicySource,
                                   String retentionDeleteAfter,
                                   String content) {
        this(
            artifactKey,
            artifactKind,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            storageType,
            storageUri,
            null,
            exportId,
            retentionDays,
            retentionPolicySource,
            retentionDeleteAfter,
            content
        );
    }

    public BenchmarkReportArtifact(String artifactKey,
                                   BenchmarkReportArtifactKind artifactKind,
                                   BenchmarkReportFormat format,
                                   String fileName,
                                   String mediaType,
                                   Integer contentLength,
                                   String checksumSha256,
                                   String storageType,
                                   String storageUri,
                                   String storageEvidence,
                                   String exportId,
                                   Integer retentionDays,
                                   String retentionPolicySource,
                                   String retentionDeleteAfter,
                                   String content) {
        this.artifactKey = artifactKey;
        this.artifactKind = artifactKind;
        this.format = format;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.contentLength = contentLength;
        this.checksumSha256 = checksumSha256;
        this.storageType = storageType;
        this.storageUri = storageUri;
        this.storageEvidence = storageEvidence;
        this.exportId = exportId;
        this.retentionDays = retentionDays;
        this.retentionPolicySource = retentionPolicySource;
        this.retentionDeleteAfter = retentionDeleteAfter;
        this.content = content;
    }

    private static String defaultArtifactKey(BenchmarkReportArtifactKind artifactKind, BenchmarkReportFormat format) {
        if (artifactKind == BenchmarkReportArtifactKind.RAW_DATA_SNAPSHOT) {
            return "raw-data";
        }
        return format == null ? "artifact" : format.name().toLowerCase() + "-export";
    }

    public String getArtifactKey() {
        return artifactKey;
    }

    public BenchmarkReportArtifactKind getArtifactKind() {
        return artifactKind;
    }

    public BenchmarkReportFormat getFormat() {
        return format;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public String getStorageEvidence() {
        return storageEvidence;
    }

    public String getExportId() {
        return exportId;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public String getRetentionPolicySource() {
        return retentionPolicySource;
    }

    public String getRetentionDeleteAfter() {
        return retentionDeleteAfter;
    }

    public String getContent() {
        return content;
    }

    public BenchmarkReportArtifact externalized(String storageType,
                                               String storageUri,
                                               String storageEvidence,
                                               Integer retentionDays,
                                               String retentionPolicySource,
                                               String retentionDeleteAfter) {
        return new BenchmarkReportArtifact(
            artifactKey,
            artifactKind,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            storageType,
            storageUri,
            storageEvidence,
            exportId,
            retentionDays,
            retentionPolicySource,
            retentionDeleteAfter,
            null
        );
    }

    public BenchmarkReportArtifact withExportId(String exportId) {
        return new BenchmarkReportArtifact(
            artifactKey,
            artifactKind,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            storageType,
            storageUri,
            storageEvidence,
            exportId,
            retentionDays,
            retentionPolicySource,
            retentionDeleteAfter,
            content
        );
    }

    public BenchmarkReportArtifact withRetentionPolicy(Integer retentionDays,
                                                       String retentionPolicySource,
                                                       String retentionDeleteAfter) {
        return new BenchmarkReportArtifact(
            artifactKey,
            artifactKind,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            storageType,
            storageUri,
            storageEvidence,
            exportId,
            retentionDays,
            retentionPolicySource,
            retentionDeleteAfter,
            content
        );
    }

    public BenchmarkReportArtifact withStorageEvidence(String storageEvidence) {
        return new BenchmarkReportArtifact(
            artifactKey,
            artifactKind,
            format,
            fileName,
            mediaType,
            contentLength,
            checksumSha256,
            storageType,
            storageUri,
            storageEvidence,
            exportId,
            retentionDays,
            retentionPolicySource,
            retentionDeleteAfter,
            content
        );
    }

}
