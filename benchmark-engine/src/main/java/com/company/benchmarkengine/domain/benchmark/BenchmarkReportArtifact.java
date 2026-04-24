package com.company.benchmarkengine.domain.benchmark;

public class BenchmarkReportArtifact {

    private final BenchmarkReportFormat format;
    private final String fileName;
    private final String mediaType;
    private final Integer contentLength;
    private final String checksumSha256;
    private final String content;

    public BenchmarkReportArtifact(BenchmarkReportFormat format,
                                   String fileName,
                                   String mediaType,
                                   Integer contentLength,
                                   String checksumSha256,
                                   String content) {
        this.format = format;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.contentLength = contentLength;
        this.checksumSha256 = checksumSha256;
        this.content = content;
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

    public String getContent() {
        return content;
    }
}
