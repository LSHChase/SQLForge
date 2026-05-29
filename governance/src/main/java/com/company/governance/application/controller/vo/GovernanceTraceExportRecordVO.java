package com.company.governance.application.controller.vo;

import java.time.LocalDateTime;
import java.util.Map;

public class GovernanceTraceExportRecordVO {

    private String exportId;
    private String historyId;
    private String resultId;
    private String exportFormat;
    private String exportStatus;
    private String requestId;
    private String storageType;
    private String storageUri;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishedAt;
    private Map<String, Object> exportOptions;

    public String getExportId() { return exportId; }
    public void setExportId(String exportId) { this.exportId = exportId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    public String getExportStatus() { return exportStatus; }
    public void setExportStatus(String exportStatus) { this.exportStatus = exportStatus; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getStorageUri() { return storageUri; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Map<String, Object> getExportOptions() { return exportOptions; }
    public void setExportOptions(Map<String, Object> exportOptions) { this.exportOptions = exportOptions; }
}
