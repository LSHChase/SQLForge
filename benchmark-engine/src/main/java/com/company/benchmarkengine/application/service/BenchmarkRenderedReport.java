package com.company.benchmarkengine.application.service;

import org.springframework.http.MediaType;

public class BenchmarkRenderedReport {

    private final MediaType mediaType;
    private final String fileName;
    private final byte[] content;

    public BenchmarkRenderedReport(MediaType mediaType, String fileName, byte[] content) {
        this.mediaType = mediaType;
        this.fileName = fileName;
        this.content = content;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return content;
    }
}
