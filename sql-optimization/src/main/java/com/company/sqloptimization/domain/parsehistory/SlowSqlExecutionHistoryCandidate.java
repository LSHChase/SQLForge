package com.company.sqloptimization.domain.parsehistory;

import java.time.Instant;
import java.util.Map;

public class SlowSqlExecutionHistoryCandidate {

    private String executionHistoryId;
    private String tenantId;
    private String datasourceCode;
    private String sqlText;
    private String sqlTemplateText;
    private Map<String, Object> bindParameters;
    private String bindingMode;
    private String reportCode;
    private String stageCode;
    private String bizDate;
    private String submittedBy;
    private Instant submittedAt;
    private Long elapsedMs;

    public String getExecutionHistoryId() { return executionHistoryId; }
    public void setExecutionHistoryId(String executionHistoryId) { this.executionHistoryId = executionHistoryId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlTemplateText() { return sqlTemplateText; }
    public void setSqlTemplateText(String sqlTemplateText) { this.sqlTemplateText = sqlTemplateText; }
    public Map<String, Object> getBindParameters() { return bindParameters; }
    public void setBindParameters(Map<String, Object> bindParameters) { this.bindParameters = bindParameters; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public String getBizDate() { return bizDate; }
    public void setBizDate(String bizDate) { this.bizDate = bizDate; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
}
