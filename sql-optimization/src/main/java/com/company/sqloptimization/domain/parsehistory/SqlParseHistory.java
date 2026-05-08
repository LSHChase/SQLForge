package com.company.sqloptimization.domain.parsehistory;

import java.time.Instant;

public class SqlParseHistory {

    private String parseHistoryId;
    private String tenantId;
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
    private String sqlText;
    private String sqlTemplateText;
    private String bindingMode;
    private Boolean parameterizedSqlFlag;
    private String resultStatus;
    private String targetEngine;
    private String structureParseSummaryJson;
    private String accessParseSummaryJson;
    private String resultSummaryJson;
    private String resultPayloadJson;
    private String queryContextJson;
    private String commentContextJson;
    private String bindingSummaryJson;
    private String logicalObjectHitsJson;
    private String issueScenesJson;
    private String logicalObjectKeysJson;
    private String traceId;
    private String requestId;
    private String sagaId;
    private String submittedBy;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlTemplateText() { return sqlTemplateText; }
    public void setSqlTemplateText(String sqlTemplateText) { this.sqlTemplateText = sqlTemplateText; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public Boolean getParameterizedSqlFlag() { return parameterizedSqlFlag; }
    public void setParameterizedSqlFlag(Boolean parameterizedSqlFlag) { this.parameterizedSqlFlag = parameterizedSqlFlag; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public String getStructureParseSummaryJson() { return structureParseSummaryJson; }
    public void setStructureParseSummaryJson(String structureParseSummaryJson) { this.structureParseSummaryJson = structureParseSummaryJson; }
    public String getAccessParseSummaryJson() { return accessParseSummaryJson; }
    public void setAccessParseSummaryJson(String accessParseSummaryJson) { this.accessParseSummaryJson = accessParseSummaryJson; }
    public String getResultSummaryJson() { return resultSummaryJson; }
    public void setResultSummaryJson(String resultSummaryJson) { this.resultSummaryJson = resultSummaryJson; }
    public String getResultPayloadJson() { return resultPayloadJson; }
    public void setResultPayloadJson(String resultPayloadJson) { this.resultPayloadJson = resultPayloadJson; }
    public String getQueryContextJson() { return queryContextJson; }
    public void setQueryContextJson(String queryContextJson) { this.queryContextJson = queryContextJson; }
    public String getCommentContextJson() { return commentContextJson; }
    public void setCommentContextJson(String commentContextJson) { this.commentContextJson = commentContextJson; }
    public String getBindingSummaryJson() { return bindingSummaryJson; }
    public void setBindingSummaryJson(String bindingSummaryJson) { this.bindingSummaryJson = bindingSummaryJson; }
    public String getLogicalObjectHitsJson() { return logicalObjectHitsJson; }
    public void setLogicalObjectHitsJson(String logicalObjectHitsJson) { this.logicalObjectHitsJson = logicalObjectHitsJson; }
    public String getIssueScenesJson() { return issueScenesJson; }
    public void setIssueScenesJson(String issueScenesJson) { this.issueScenesJson = issueScenesJson; }
    public String getLogicalObjectKeysJson() { return logicalObjectKeysJson; }
    public void setLogicalObjectKeysJson(String logicalObjectKeysJson) { this.logicalObjectKeysJson = logicalObjectKeysJson; }
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
