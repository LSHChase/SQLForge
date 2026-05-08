package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;

public class SqlParseHistorySummaryVO {

    private String parseHistoryId;
    private String historyId;
    private String tenantId;
    private String historyType;
    private String sourceType;
    private String sourceId;
    private String batchKey;
    private String parseTaskId;
    private String sqlFingerprint;
    private String datasourceCode;
    private String datasourceType;
    private String reportCode;
    private String stageCode;
    private String bizDate;
    private String queryDateStart;
    private String queryDateEnd;
    private String queryDateStatus;
    private String accessChannel;
    private String parserMode;
    private String bindingMode;
    private Boolean parameterizedSqlFlag;
    private String resultStatus;
    private String targetEngine;
    private String traceId;
    private String requestId;
    private String sagaId;
    private String submittedBy;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getHistoryType() { return historyType; }
    public void setHistoryType(String historyType) { this.historyType = historyType; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getBatchKey() { return batchKey; }
    public void setBatchKey(String batchKey) { this.batchKey = batchKey; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getDatasourceType() { return datasourceType; }
    public void setDatasourceType(String datasourceType) { this.datasourceType = datasourceType; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public String getBizDate() { return bizDate; }
    public void setBizDate(String bizDate) { this.bizDate = bizDate; }
    public String getQueryDateStart() { return queryDateStart; }
    public void setQueryDateStart(String queryDateStart) { this.queryDateStart = queryDateStart; }
    public String getQueryDateEnd() { return queryDateEnd; }
    public void setQueryDateEnd(String queryDateEnd) { this.queryDateEnd = queryDateEnd; }
    public String getQueryDateStatus() { return queryDateStatus; }
    public void setQueryDateStatus(String queryDateStatus) { this.queryDateStatus = queryDateStatus; }
    public String getAccessChannel() { return accessChannel; }
    public void setAccessChannel(String accessChannel) { this.accessChannel = accessChannel; }
    public String getParserMode() { return parserMode; }
    public void setParserMode(String parserMode) { this.parserMode = parserMode; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public Boolean getParameterizedSqlFlag() { return parameterizedSqlFlag; }
    public void setParameterizedSqlFlag(Boolean parameterizedSqlFlag) { this.parameterizedSqlFlag = parameterizedSqlFlag; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
