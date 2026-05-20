package com.company.governance.domain.trace.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueryHistoryRecord {

    private String historyId;
    private String resultId;
    private String tenantId;
    private String historyType;
    private String sqlFingerprint;
    private byte[] sqlTextCipher;
    private byte[] sqlTemplateCipher;
    private byte[] boundSqlTextCipher;
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
    private String traceId;
    private String requestId;
    private String sagaId;
    private String commentContext;
    private String bindingSummary;
    private String rewriteRecordId;
    private String runtimeBindingId;
    private Long rewriteRuleVersion;
    private String runtimeRuleVersion;
    private String runtimeRewriteStatus;
    private String rewriteActivationStatusSnapshot;
    private String rewriteFallbackReason;
    private String logicalObjectHits;
    private String routeSummary;
    private String cacheSummary;
    private String queryContext;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHistoryType() {
        return historyType;
    }

    public void setHistoryType(String historyType) {
        this.historyType = historyType;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public byte[] getSqlTextCipher() {
        return sqlTextCipher;
    }

    public void setSqlTextCipher(byte[] sqlTextCipher) {
        this.sqlTextCipher = sqlTextCipher;
    }

    public byte[] getSqlTemplateCipher() {
        return sqlTemplateCipher;
    }

    public void setSqlTemplateCipher(byte[] sqlTemplateCipher) {
        this.sqlTemplateCipher = sqlTemplateCipher;
    }

    public byte[] getBoundSqlTextCipher() {
        return boundSqlTextCipher;
    }

    public void setBoundSqlTextCipher(byte[] boundSqlTextCipher) {
        this.boundSqlTextCipher = boundSqlTextCipher;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public LocalDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(LocalDate bizDate) {
        this.bizDate = bizDate;
    }

    public LocalDate getQueryDateStart() {
        return queryDateStart;
    }

    public void setQueryDateStart(LocalDate queryDateStart) {
        this.queryDateStart = queryDateStart;
    }

    public LocalDate getQueryDateEnd() {
        return queryDateEnd;
    }

    public void setQueryDateEnd(LocalDate queryDateEnd) {
        this.queryDateEnd = queryDateEnd;
    }

    public String getQueryDateStatus() {
        return queryDateStatus;
    }

    public void setQueryDateStatus(String queryDateStatus) {
        this.queryDateStatus = queryDateStatus;
    }

    public String getAccessChannel() {
        return accessChannel;
    }

    public void setAccessChannel(String accessChannel) {
        this.accessChannel = accessChannel;
    }

    public Boolean getParameterizedSqlFlag() {
        return parameterizedSqlFlag;
    }

    public void setParameterizedSqlFlag(Boolean parameterizedSqlFlag) {
        this.parameterizedSqlFlag = parameterizedSqlFlag;
    }

    public String getBindingMode() {
        return bindingMode;
    }

    public void setBindingMode(String bindingMode) {
        this.bindingMode = bindingMode;
    }

    public String getBindingRenderStatus() {
        return bindingRenderStatus;
    }

    public void setBindingRenderStatus(String bindingRenderStatus) {
        this.bindingRenderStatus = bindingRenderStatus;
    }

    public String getSqlTemplateFingerprint() {
        return sqlTemplateFingerprint;
    }

    public void setSqlTemplateFingerprint(String sqlTemplateFingerprint) {
        this.sqlTemplateFingerprint = sqlTemplateFingerprint;
    }

    public String getBoundSqlFingerprint() {
        return boundSqlFingerprint;
    }

    public void setBoundSqlFingerprint(String boundSqlFingerprint) {
        this.boundSqlFingerprint = boundSqlFingerprint;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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

    public String getCommentContext() {
        return commentContext;
    }

    public void setCommentContext(String commentContext) {
        this.commentContext = commentContext;
    }

    public String getBindingSummary() {
        return bindingSummary;
    }

    public void setBindingSummary(String bindingSummary) {
        this.bindingSummary = bindingSummary;
    }

    public String getRewriteRecordId() {
        return rewriteRecordId;
    }

    public void setRewriteRecordId(String rewriteRecordId) {
        this.rewriteRecordId = rewriteRecordId;
    }

    public String getRuntimeBindingId() {
        return runtimeBindingId;
    }

    public void setRuntimeBindingId(String runtimeBindingId) {
        this.runtimeBindingId = runtimeBindingId;
    }

    public Long getRewriteRuleVersion() {
        return rewriteRuleVersion;
    }

    public void setRewriteRuleVersion(Long rewriteRuleVersion) {
        this.rewriteRuleVersion = rewriteRuleVersion;
    }

    public String getRuntimeRuleVersion() {
        return runtimeRuleVersion;
    }

    public void setRuntimeRuleVersion(String runtimeRuleVersion) {
        this.runtimeRuleVersion = runtimeRuleVersion;
    }

    public String getRuntimeRewriteStatus() {
        return runtimeRewriteStatus;
    }

    public void setRuntimeRewriteStatus(String runtimeRewriteStatus) {
        this.runtimeRewriteStatus = runtimeRewriteStatus;
    }

    public String getRewriteActivationStatusSnapshot() {
        return rewriteActivationStatusSnapshot;
    }

    public void setRewriteActivationStatusSnapshot(String rewriteActivationStatusSnapshot) {
        this.rewriteActivationStatusSnapshot = rewriteActivationStatusSnapshot;
    }

    public String getRewriteFallbackReason() {
        return rewriteFallbackReason;
    }

    public void setRewriteFallbackReason(String rewriteFallbackReason) {
        this.rewriteFallbackReason = rewriteFallbackReason;
    }

    public String getLogicalObjectHits() {
        return logicalObjectHits;
    }

    public void setLogicalObjectHits(String logicalObjectHits) {
        this.logicalObjectHits = logicalObjectHits;
    }

    public String getRouteSummary() {
        return routeSummary;
    }

    public void setRouteSummary(String routeSummary) {
        this.routeSummary = routeSummary;
    }

    public String getCacheSummary() {
        return cacheSummary;
    }

    public void setCacheSummary(String cacheSummary) {
        this.cacheSummary = cacheSummary;
    }

    public String getQueryContext() {
        return queryContext;
    }

    public void setQueryContext(String queryContext) {
        this.queryContext = queryContext;
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
