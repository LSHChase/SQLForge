package com.company.benchmarkengine.domain.benchmark;

public enum BenchmarkReportFormat {

    JSON("application/json", "json"),
    PDF("application/pdf", "pdf"),
    HTML("text/html", "html");

    private final String contentType;
    private final String fileExtension;

    BenchmarkReportFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
