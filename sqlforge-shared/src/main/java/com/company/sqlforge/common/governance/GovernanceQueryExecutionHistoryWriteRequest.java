package com.company.sqlforge.common.governance;

public class GovernanceQueryExecutionHistoryWriteRequest {

    private String configSnapshotId;
    private String resultId;
    private String historyId;
    private String tenantId;
    private String sqlText;
    private String sqlTemplate;
    private String boundSql;
    private String sqlFingerprint;
    private String datasourceCode;
    private String datasourceType;
    private String historyType = "QUERY_EXECUTION";
    private String resultStatus;
    private String targetEngine;
    private Long returnedRowCount;
    private Boolean cacheHit;
    private Boolean rewriteApplied;
    private Boolean accelerationApplied;
    private String accessChannel;
    private String commentContext;
    private String queryDateSummary;
    private String bindingSummary;
    private String logicalObjectHits;
    private String routeSummary;
    private String cacheSummary;
    private String queryContext;
    private String traceId;
    private String requestId;
    private String sagaId;
    private String submittedBy;
    private String startedAt;
    private String finishedAt;
    private Long elapsedMs;
    private Integer errorCode;
    private String errorMessage;

    public String getConfigSnapshotId() { return configSnapshotId; }
    public void setConfigSnapshotId(String configSnapshotId) { this.configSnapshotId = configSnapshotId; }
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlTemplate() { return sqlTemplate; }
    public void setSqlTemplate(String sqlTemplate) { this.sqlTemplate = sqlTemplate; }
    public String getBoundSql() { return boundSql; }
    public void setBoundSql(String boundSql) { this.boundSql = boundSql; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getDatasourceType() { return datasourceType; }
    public void setDatasourceType(String datasourceType) { this.datasourceType = datasourceType; }
    public String getHistoryType() { return historyType; }
    public void setHistoryType(String historyType) { this.historyType = historyType; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public Long getReturnedRowCount() { return returnedRowCount; }
    public void setReturnedRowCount(Long returnedRowCount) { this.returnedRowCount = returnedRowCount; }
    public Boolean getCacheHit() { return cacheHit; }
    public void setCacheHit(Boolean cacheHit) { this.cacheHit = cacheHit; }
    public Boolean getRewriteApplied() { return rewriteApplied; }
    public void setRewriteApplied(Boolean rewriteApplied) { this.rewriteApplied = rewriteApplied; }
    public Boolean getAccelerationApplied() { return accelerationApplied; }
    public void setAccelerationApplied(Boolean accelerationApplied) { this.accelerationApplied = accelerationApplied; }
    public String getAccessChannel() { return accessChannel; }
    public void setAccessChannel(String accessChannel) { this.accessChannel = accessChannel; }
    public String getCommentContext() { return commentContext; }
    public void setCommentContext(String commentContext) { this.commentContext = commentContext; }
    public String getQueryDateSummary() { return queryDateSummary; }
    public void setQueryDateSummary(String queryDateSummary) { this.queryDateSummary = queryDateSummary; }
    public String getBindingSummary() { return bindingSummary; }
    public void setBindingSummary(String bindingSummary) { this.bindingSummary = bindingSummary; }
    public String getLogicalObjectHits() { return logicalObjectHits; }
    public void setLogicalObjectHits(String logicalObjectHits) { this.logicalObjectHits = logicalObjectHits; }
    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }
    public String getCacheSummary() { return cacheSummary; }
    public void setCacheSummary(String cacheSummary) { this.cacheSummary = cacheSummary; }
    public String getQueryContext() { return queryContext; }
    public void setQueryContext(String queryContext) { this.queryContext = queryContext; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public Integer getErrorCode() { return errorCode; }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
