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

    public static class QueryHistoryVO extends GovernanceTraceQueryHistoryVO {
    }

    public static class ExportRecordVO extends GovernanceTraceExportRecordVO {
    }
}
