package com.company.sqloptimization.application.controller.dto;

public class SqlParseHistoryExportRequest {

    private String parseHistoryId;
    private String historyId;
    private String exportFormat;
    private Boolean includeTraceDetail;
    private String exportReason;

    public String getParseHistoryId() {
        return parseHistoryId;
    }

    public void setParseHistoryId(String parseHistoryId) {
        this.parseHistoryId = parseHistoryId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public Boolean getIncludeTraceDetail() {
        return includeTraceDetail;
    }

    public void setIncludeTraceDetail(Boolean includeTraceDetail) {
        this.includeTraceDetail = includeTraceDetail;
    }

    public String getExportReason() {
        return exportReason;
    }

    public void setExportReason(String exportReason) {
        this.exportReason = exportReason;
    }
}
