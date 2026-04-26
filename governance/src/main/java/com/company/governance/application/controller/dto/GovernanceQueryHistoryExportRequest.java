package com.company.governance.application.controller.dto;

public class GovernanceQueryHistoryExportRequest {

    private String historyId;
    private String exportFormat;
    private Boolean includeTraceDetail;
    private String exportReason;

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
