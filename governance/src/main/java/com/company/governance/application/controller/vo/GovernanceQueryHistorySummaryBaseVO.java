package com.company.governance.application.controller.vo;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GovernanceQueryHistorySummaryBaseVO {

    private String historyId;
    private String resultId;
    private String traceId;
    private String tenantId;
    private String historyType;
    private String reportCode;
    private String datasourceCode;
    private String datasourceType;
    private String stageCode;
    private LocalDate bizDate;
    private LocalDate queryDateStart;
    private LocalDate queryDateEnd;
    private String queryDateStatus;
    private String accessChannel;
    private Boolean parameterizedSqlFlag;
    private String bindingMode;
    private String bindingRenderStatus;
    private String sqlFingerprint;
    private String sqlTemplateFingerprint;
    private String boundSqlFingerprint;
    private String resultStatus;
    private String targetEngine;
    private Long returnedRowCount;
    private Boolean cacheHit;
    private Boolean rewriteApplied;
    private Boolean accelerationApplied;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String errorCode;
    private List<String> logicalObjectTypes;
    private List<LogicalObjectSurface> logicalObjectHits;

    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getHistoryType() { return historyType; }
    public void setHistoryType(String historyType) { this.historyType = historyType; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getDatasourceType() { return datasourceType; }
    public void setDatasourceType(String datasourceType) { this.datasourceType = datasourceType; }
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
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getSqlTemplateFingerprint() { return sqlTemplateFingerprint; }
    public void setSqlTemplateFingerprint(String sqlTemplateFingerprint) { this.sqlTemplateFingerprint = sqlTemplateFingerprint; }
    public String getBoundSqlFingerprint() { return boundSqlFingerprint; }
    public void setBoundSqlFingerprint(String boundSqlFingerprint) { this.boundSqlFingerprint = boundSqlFingerprint; }
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
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public List<String> getLogicalObjectTypes() { return logicalObjectTypes; }
    public void setLogicalObjectTypes(List<String> logicalObjectTypes) { this.logicalObjectTypes = logicalObjectTypes; }
    public List<LogicalObjectSurface> getLogicalObjectHits() { return logicalObjectHits; }
    public void setLogicalObjectHits(List<LogicalObjectSurface> logicalObjectHits) { this.logicalObjectHits = logicalObjectHits; }
}
