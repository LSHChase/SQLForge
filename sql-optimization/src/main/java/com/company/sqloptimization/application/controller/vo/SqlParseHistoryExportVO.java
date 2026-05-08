package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.Map;

public class SqlParseHistoryExportVO {

    private String exportId;
    private String parseHistoryId;
    private String historyId;
    private String exportFormat;
    private String exportStatus;
    private String fileName;
    private String contentType;
    private String storageType;
    private String storageUri;
    private Instant exportedAt;
    private Map<String, Object> auditReference;
    private String payload;

    public String getExportId() { return exportId; }
    public void setExportId(String exportId) { this.exportId = exportId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    public String getExportStatus() { return exportStatus; }
    public void setExportStatus(String exportStatus) { this.exportStatus = exportStatus; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getStorageUri() { return storageUri; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public Instant getExportedAt() { return exportedAt; }
    public void setExportedAt(Instant exportedAt) { this.exportedAt = exportedAt; }
    public Map<String, Object> getAuditReference() { return auditReference; }
    public void setAuditReference(Map<String, Object> auditReference) { this.auditReference = auditReference; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
