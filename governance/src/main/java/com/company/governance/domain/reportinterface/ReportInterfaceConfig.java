package com.company.governance.domain.reportinterface;

import java.time.Instant;

public class ReportInterfaceConfig {

    private final String configId;
    private final String tenantId;
    private final String datasourceCode;
    private final String stage;
    private final String sourceType;
    private final String endpointCode;
    private final String endpointName;
    private final String baseUrl;
    private final String pathTemplate;
    private final String httpMethod;
    private final String reportCodeParamName;
    private final String sqlJsonPath;
    private final String authMode;
    private final int timeoutMs;
    private final boolean enabled;
    private final Instant updatedAt;

    public ReportInterfaceConfig(String configId,
                                 String tenantId,
                                 String datasourceCode,
                                 String stage,
                                 String sourceType,
                                 String endpointCode,
                                 String endpointName,
                                 String baseUrl,
                                 String pathTemplate,
                                 String httpMethod,
                                 String reportCodeParamName,
                                 String sqlJsonPath,
                                 String authMode,
                                 int timeoutMs,
                                 boolean enabled,
                                 Instant updatedAt) {
        this.configId = configId;
        this.tenantId = tenantId;
        this.datasourceCode = datasourceCode;
        this.stage = stage;
        this.sourceType = sourceType;
        this.endpointCode = endpointCode;
        this.endpointName = endpointName;
        this.baseUrl = baseUrl;
        this.pathTemplate = pathTemplate;
        this.httpMethod = httpMethod;
        this.reportCodeParamName = reportCodeParamName;
        this.sqlJsonPath = sqlJsonPath;
        this.authMode = authMode;
        this.timeoutMs = timeoutMs;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public String getConfigId() { return configId; }
    public String getTenantId() { return tenantId; }
    public String getDatasourceCode() { return datasourceCode; }
    public String getStage() { return stage; }
    public String getSourceType() { return sourceType; }
    public String getEndpointCode() { return endpointCode; }
    public String getEndpointName() { return endpointName; }
    public String getBaseUrl() { return baseUrl; }
    public String getPathTemplate() { return pathTemplate; }
    public String getHttpMethod() { return httpMethod; }
    public String getReportCodeParamName() { return reportCodeParamName; }
    public String getSqlJsonPath() { return sqlJsonPath; }
    public String getAuthMode() { return authMode; }
    public int getTimeoutMs() { return timeoutMs; }
    public boolean isEnabled() { return enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
}
