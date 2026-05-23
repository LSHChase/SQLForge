package com.company.governance.infrastructure.persistence.entity;

import java.time.Instant;

public class ReportInterfaceConfigRecord {

    private String configId;
    private String tenantId;
    private String datasourceCode;
    private String stage;
    private String sourceType;
    private String endpointCode;
    private String endpointName;
    private String baseUrl;
    private String pathTemplate;
    private String httpMethod;
    private String reportCodeParamName;
    private String sqlJsonPath;
    private String authMode;
    private Integer timeoutMs;
    private Boolean enabled;
    private Instant createTime;
    private Instant updateTime;

    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getEndpointCode() { return endpointCode; }
    public void setEndpointCode(String endpointCode) { this.endpointCode = endpointCode; }
    public String getEndpointName() { return endpointName; }
    public void setEndpointName(String endpointName) { this.endpointName = endpointName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getPathTemplate() { return pathTemplate; }
    public void setPathTemplate(String pathTemplate) { this.pathTemplate = pathTemplate; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getReportCodeParamName() { return reportCodeParamName; }
    public void setReportCodeParamName(String reportCodeParamName) { this.reportCodeParamName = reportCodeParamName; }
    public String getSqlJsonPath() { return sqlJsonPath; }
    public void setSqlJsonPath(String sqlJsonPath) { this.sqlJsonPath = sqlJsonPath; }
    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreateTime() { return createTime; }
    public void setCreateTime(Instant createTime) { this.createTime = createTime; }
    public Instant getUpdateTime() { return updateTime; }
    public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
}
