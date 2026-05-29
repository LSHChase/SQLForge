package com.company.governance.application.controller.vo;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class GovernanceTraceQueryHistoryVO {

    private String historyId;
    private String resultId;
    private String historyType;
    private String datasourceCode;
    private String datasourceType;
    private String reportCode;
    private String stageCode;
    private LocalDate bizDate;
    private LocalDate queryDateStart;
    private LocalDate queryDateEnd;
    private String queryDateStatus;
    private String accessChannel;
    private Boolean parameterizedSqlFlag;
    private String bindingMode;
    private String bindingRenderStatus;
    private String sqlTemplateFingerprint;
    private String boundSqlFingerprint;
    private String sqlFingerprint;
    private String requestId;
    private String sagaId;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime createTime;
    private Map<String, Object> commentContext;
    private Map<String, Object> bindingSummary;
    private List<LogicalObjectSurface> logicalObjectHits;
    private Map<String, Object> routeSummary;
    private Map<String, Object> cacheSummary;
    private Map<String, Object> queryContext;

    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    public String getHistoryType() { return historyType; }
    public void setHistoryType(String historyType) { this.historyType = historyType; }
    public String getDatasourceType() { return datasourceType; }
    public void setDatasourceType(String datasourceType) { this.datasourceType = datasourceType; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public LocalDate getBizDate() { return bizDate; }
    public void setBizDate(LocalDate bizDate) { this.bizDate = bizDate; }
    public LocalDate getQueryDateStart() { return queryDateStart; }
    public void setQueryDateStart(LocalDate queryDateStart) { this.queryDateStart = queryDateStart; }
    public LocalDate getQueryDateEnd() { return queryDateEnd; }
    public void setQueryDateEnd(LocalDate queryDateEnd) { this.queryDateEnd = queryDateEnd; }
    public String getQueryDateStatus() { return queryDateStatus; }
    public void setQueryDateStatus(String queryDateStatus) { this.queryDateStatus = queryDateStatus; }
    public String getAccessChannel() { return accessChannel; }
    public void setAccessChannel(String accessChannel) { this.accessChannel = accessChannel; }
    public Boolean getParameterizedSqlFlag() { return parameterizedSqlFlag; }
    public void setParameterizedSqlFlag(Boolean parameterizedSqlFlag) { this.parameterizedSqlFlag = parameterizedSqlFlag; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public String getBindingRenderStatus() { return bindingRenderStatus; }
    public void setBindingRenderStatus(String bindingRenderStatus) { this.bindingRenderStatus = bindingRenderStatus; }
    public String getSqlTemplateFingerprint() { return sqlTemplateFingerprint; }
    public void setSqlTemplateFingerprint(String sqlTemplateFingerprint) { this.sqlTemplateFingerprint = sqlTemplateFingerprint; }
    public String getBoundSqlFingerprint() { return boundSqlFingerprint; }
    public void setBoundSqlFingerprint(String boundSqlFingerprint) { this.boundSqlFingerprint = boundSqlFingerprint; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Map<String, Object> getCommentContext() { return commentContext; }
    public void setCommentContext(Map<String, Object> commentContext) { this.commentContext = commentContext; }
    public Map<String, Object> getBindingSummary() { return bindingSummary; }
    public void setBindingSummary(Map<String, Object> bindingSummary) { this.bindingSummary = bindingSummary; }
    public List<LogicalObjectSurface> getLogicalObjectHits() { return logicalObjectHits; }
    public void setLogicalObjectHits(List<LogicalObjectSurface> logicalObjectHits) { this.logicalObjectHits = logicalObjectHits; }
    public Map<String, Object> getRouteSummary() { return routeSummary; }
    public void setRouteSummary(Map<String, Object> routeSummary) { this.routeSummary = routeSummary; }
    public Map<String, Object> getCacheSummary() { return cacheSummary; }
    public void setCacheSummary(Map<String, Object> cacheSummary) { this.cacheSummary = cacheSummary; }
    public Map<String, Object> getQueryContext() { return queryContext; }
    public void setQueryContext(Map<String, Object> queryContext) { this.queryContext = queryContext; }
}
