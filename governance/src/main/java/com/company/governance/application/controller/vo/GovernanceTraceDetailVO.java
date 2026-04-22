package com.company.governance.application.controller.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class GovernanceTraceDetailVO extends GovernanceTraceSummaryVO {

    private List<AuditEventVO> auditEvents;
    private List<QueryHistoryVO> queryHistories;
    private List<ExportRecordVO> exportRecords;

    public GovernanceTraceDetailVO() {
    }

    public List<AuditEventVO> getAuditEvents() {
        return auditEvents;
    }

    public void setAuditEvents(List<AuditEventVO> auditEvents) {
        this.auditEvents = auditEvents;
    }

    public List<QueryHistoryVO> getQueryHistories() {
        return queryHistories;
    }

    public void setQueryHistories(List<QueryHistoryVO> queryHistories) {
        this.queryHistories = queryHistories;
    }

    public List<ExportRecordVO> getExportRecords() {
        return exportRecords;
    }

    public void setExportRecords(List<ExportRecordVO> exportRecords) {
        this.exportRecords = exportRecords;
    }

    public static class AuditEventVO {

        private Long id;
        private String serviceCode;
        private String operationType;
        private String targetType;
        private String targetId;
        private String status;
        private String requestId;
        private String traceId;
        private String historyId;
        private String exportId;
        private String resultId;
        private Long costMs;
        private LocalDateTime createTime;
        private Map<String, Object> requestParams;
        private Map<String, Object> responseSummary;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getServiceCode() {
            return serviceCode;
        }

        public void setServiceCode(String serviceCode) {
            this.serviceCode = serviceCode;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getTargetType() {
            return targetType;
        }

        public void setTargetType(String targetType) {
            this.targetType = targetType;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getHistoryId() {
            return historyId;
        }

        public void setHistoryId(String historyId) {
            this.historyId = historyId;
        }

        public String getExportId() {
            return exportId;
        }

        public void setExportId(String exportId) {
            this.exportId = exportId;
        }

        public String getResultId() {
            return resultId;
        }

        public void setResultId(String resultId) {
            this.resultId = resultId;
        }

        public Long getCostMs() {
            return costMs;
        }

        public void setCostMs(Long costMs) {
            this.costMs = costMs;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public Map<String, Object> getRequestParams() {
            return requestParams;
        }

        public void setRequestParams(Map<String, Object> requestParams) {
            this.requestParams = requestParams;
        }

        public Map<String, Object> getResponseSummary() {
            return responseSummary;
        }

        public void setResponseSummary(Map<String, Object> responseSummary) {
            this.responseSummary = responseSummary;
        }
    }

    public static class QueryHistoryVO {

        private String historyId;
        private String resultId;
        private String historyType;
        private String datasourceType;
        private String sqlFingerprint;
        private String requestId;
        private String sagaId;
        private String submittedBy;
        private LocalDateTime submittedAt;
        private LocalDateTime createTime;

        public String getHistoryId() {
            return historyId;
        }

        public void setHistoryId(String historyId) {
            this.historyId = historyId;
        }

        public String getResultId() {
            return resultId;
        }

        public void setResultId(String resultId) {
            this.resultId = resultId;
        }

        public String getHistoryType() {
            return historyType;
        }

        public void setHistoryType(String historyType) {
            this.historyType = historyType;
        }

        public String getDatasourceType() {
            return datasourceType;
        }

        public void setDatasourceType(String datasourceType) {
            this.datasourceType = datasourceType;
        }

        public String getSqlFingerprint() {
            return sqlFingerprint;
        }

        public void setSqlFingerprint(String sqlFingerprint) {
            this.sqlFingerprint = sqlFingerprint;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getSagaId() {
            return sagaId;
        }

        public void setSagaId(String sagaId) {
            this.sagaId = sagaId;
        }

        public String getSubmittedBy() {
            return submittedBy;
        }

        public void setSubmittedBy(String submittedBy) {
            this.submittedBy = submittedBy;
        }

        public LocalDateTime getSubmittedAt() {
            return submittedAt;
        }

        public void setSubmittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    public static class ExportRecordVO {

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

        public String getExportId() {
            return exportId;
        }

        public void setExportId(String exportId) {
            this.exportId = exportId;
        }

        public String getHistoryId() {
            return historyId;
        }

        public void setHistoryId(String historyId) {
            this.historyId = historyId;
        }

        public String getResultId() {
            return resultId;
        }

        public void setResultId(String resultId) {
            this.resultId = resultId;
        }

        public String getExportFormat() {
            return exportFormat;
        }

        public void setExportFormat(String exportFormat) {
            this.exportFormat = exportFormat;
        }

        public String getExportStatus() {
            return exportStatus;
        }

        public void setExportStatus(String exportStatus) {
            this.exportStatus = exportStatus;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getStorageType() {
            return storageType;
        }

        public void setStorageType(String storageType) {
            this.storageType = storageType;
        }

        public String getStorageUri() {
            return storageUri;
        }

        public void setStorageUri(String storageUri) {
            this.storageUri = storageUri;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(LocalDateTime finishedAt) {
            this.finishedAt = finishedAt;
        }
    }
}
