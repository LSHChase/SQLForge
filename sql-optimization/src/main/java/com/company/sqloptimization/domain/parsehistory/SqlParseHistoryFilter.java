package com.company.sqloptimization.domain.parsehistory;

import java.time.LocalDateTime;

public class SqlParseHistoryFilter {

    private String tenantId;
    private String sourceType;
    private String reportCode;
    private String datasourceCode;
    private String stageCode;
    private String bizDate;
    private String queryDateStart;
    private String queryDateEnd;
    private String status;
    private String logicalObjectType;
    private String accessChannel;
    private String engine;
    private String submittedBy;
    private String traceId;
    private String parseTaskId;
    private LocalDateTime submittedStart;
    private LocalDateTime submittedEnd;
    private String orderByClause;
    private int offset;
    private int limit;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public String getBizDate() { return bizDate; }
    public void setBizDate(String bizDate) { this.bizDate = bizDate; }
    public String getQueryDateStart() { return queryDateStart; }
    public void setQueryDateStart(String queryDateStart) { this.queryDateStart = queryDateStart; }
    public String getQueryDateEnd() { return queryDateEnd; }
    public void setQueryDateEnd(String queryDateEnd) { this.queryDateEnd = queryDateEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLogicalObjectType() { return logicalObjectType; }
    public void setLogicalObjectType(String logicalObjectType) { this.logicalObjectType = logicalObjectType; }
    public String getAccessChannel() { return accessChannel; }
    public void setAccessChannel(String accessChannel) { this.accessChannel = accessChannel; }
    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public LocalDateTime getSubmittedStart() { return submittedStart; }
    public void setSubmittedStart(LocalDateTime submittedStart) { this.submittedStart = submittedStart; }
    public LocalDateTime getSubmittedEnd() { return submittedEnd; }
    public void setSubmittedEnd(LocalDateTime submittedEnd) { this.submittedEnd = submittedEnd; }
    public String getOrderByClause() { return orderByClause; }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
