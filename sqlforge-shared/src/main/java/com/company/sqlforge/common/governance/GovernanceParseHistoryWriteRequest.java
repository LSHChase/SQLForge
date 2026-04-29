package com.company.sqlforge.common.governance;

public class GovernanceParseHistoryWriteRequest {

    private String parseTaskId;
    private String sqlFingerprint;
    private String datasourceCode;
    private String datasourceType;
    private String sqlText;
    private String sqlTemplateText;
    private String bindingMode;
    private String resultStatus;
    private String resultSummaryJson;
    private String resultPayloadJson;
    private String queryContextJson;
    private String logicalObjectHitsJson;
    private String submittedAt;

    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getDatasourceType() { return datasourceType; }
    public void setDatasourceType(String datasourceType) { this.datasourceType = datasourceType; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlTemplateText() { return sqlTemplateText; }
    public void setSqlTemplateText(String sqlTemplateText) { this.sqlTemplateText = sqlTemplateText; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getResultSummaryJson() { return resultSummaryJson; }
    public void setResultSummaryJson(String resultSummaryJson) { this.resultSummaryJson = resultSummaryJson; }
    public String getResultPayloadJson() { return resultPayloadJson; }
    public void setResultPayloadJson(String resultPayloadJson) { this.resultPayloadJson = resultPayloadJson; }
    public String getQueryContextJson() { return queryContextJson; }
    public void setQueryContextJson(String queryContextJson) { this.queryContextJson = queryContextJson; }
    public String getLogicalObjectHitsJson() { return logicalObjectHitsJson; }
    public void setLogicalObjectHitsJson(String logicalObjectHitsJson) { this.logicalObjectHitsJson = logicalObjectHitsJson; }
    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
}
