package com.company.sqloptimization.application.controller.dto;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;

public class RewriteTrialRequest {

    private String tenantId;

    @NotBlank(message = "sqlText 为必填项")
    private String sqlText;

    private String datasourceCode;
    private String sourceKind;
    private String sourceId;
    private String parseTaskId;
    private String parseHistoryId;
    private String historyId;
    private List<Map<String, Object>> sourceProblems;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public List<Map<String, Object>> getSourceProblems() { return sourceProblems; }
    public void setSourceProblems(List<Map<String, Object>> sourceProblems) { this.sourceProblems = sourceProblems; }
}
