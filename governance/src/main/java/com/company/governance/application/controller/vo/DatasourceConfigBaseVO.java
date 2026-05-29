package com.company.governance.application.controller.vo;

import java.time.Instant;

public class DatasourceConfigBaseVO {

    private String datasourceId;
    private String tenantId;
    private String datasourceCode;
    private String datasourceName;
    private String engineType;
    private String connectionMode;
    private String stage;
    private String jdbcUrl;
    private String jdbcDriverClassName;
    private String driverSourceType;
    private String driverArtifactId;
    private String driverVersionLabel;
    private String driverSha256;
    private String driverLoadStatus;
    private String username;
    private String apiBaseUrl;
    private String clientEndpoint;
    private String gatewayEndpoint;
    private String proxyEndpoint;
    private String authMode;
    private String credentialMode;
    private String credentialRef;
    private String credentialMask;
    private boolean tlsEnabled;
    private boolean verifyPeer;
    private boolean readonly;
    private boolean enabled;
    private Integer timeoutMs;
    private String healthStatus;
    private String lastFailureReason;
    private Instant lastCheckedAt;
    private String contractStage;
    private String implementationStage;

    public String getDatasourceId() { return datasourceId; }

    public void setDatasourceId(String datasourceId) { this.datasourceId = datasourceId; }

    public String getTenantId() { return tenantId; }

    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDatasourceCode() { return datasourceCode; }

    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }

    public String getDatasourceName() { return datasourceName; }

    public void setDatasourceName(String datasourceName) { this.datasourceName = datasourceName; }

    public String getEngineType() { return engineType; }

    public void setEngineType(String engineType) { this.engineType = engineType; }

    public String getConnectionMode() { return connectionMode; }

    public void setConnectionMode(String connectionMode) { this.connectionMode = connectionMode; }

    public String getStage() { return stage; }

    public void setStage(String stage) { this.stage = stage; }

    public String getJdbcUrl() { return jdbcUrl; }

    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }

    public String getJdbcDriverClassName() { return jdbcDriverClassName; }

    public void setJdbcDriverClassName(String jdbcDriverClassName) { this.jdbcDriverClassName = jdbcDriverClassName; }

    public String getDriverSourceType() { return driverSourceType; }

    public void setDriverSourceType(String driverSourceType) { this.driverSourceType = driverSourceType; }

    public String getDriverArtifactId() { return driverArtifactId; }

    public void setDriverArtifactId(String driverArtifactId) { this.driverArtifactId = driverArtifactId; }

    public String getDriverVersionLabel() { return driverVersionLabel; }

    public void setDriverVersionLabel(String driverVersionLabel) { this.driverVersionLabel = driverVersionLabel; }

    public String getDriverSha256() { return driverSha256; }

    public void setDriverSha256(String driverSha256) { this.driverSha256 = driverSha256; }

    public String getDriverLoadStatus() { return driverLoadStatus; }

    public void setDriverLoadStatus(String driverLoadStatus) { this.driverLoadStatus = driverLoadStatus; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getApiBaseUrl() { return apiBaseUrl; }

    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getClientEndpoint() { return clientEndpoint; }

    public void setClientEndpoint(String clientEndpoint) { this.clientEndpoint = clientEndpoint; }

    public String getGatewayEndpoint() { return gatewayEndpoint; }

    public void setGatewayEndpoint(String gatewayEndpoint) { this.gatewayEndpoint = gatewayEndpoint; }

    public String getProxyEndpoint() { return proxyEndpoint; }

    public void setProxyEndpoint(String proxyEndpoint) { this.proxyEndpoint = proxyEndpoint; }

    public String getAuthMode() { return authMode; }

    public void setAuthMode(String authMode) { this.authMode = authMode; }

    public String getCredentialMode() { return credentialMode; }

    public void setCredentialMode(String credentialMode) { this.credentialMode = credentialMode; }

    public String getCredentialRef() { return credentialRef; }

    public void setCredentialRef(String credentialRef) { this.credentialRef = credentialRef; }

    public String getCredentialMask() { return credentialMask; }

    public void setCredentialMask(String credentialMask) { this.credentialMask = credentialMask; }

    public boolean isTlsEnabled() { return tlsEnabled; }

    public void setTlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }

    public boolean isVerifyPeer() { return verifyPeer; }

    public void setVerifyPeer(boolean verifyPeer) { this.verifyPeer = verifyPeer; }

    public boolean isReadonly() { return readonly; }

    public void setReadonly(boolean readonly) { this.readonly = readonly; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getTimeoutMs() { return timeoutMs; }

    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public String getHealthStatus() { return healthStatus; }

    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public String getLastFailureReason() { return lastFailureReason; }

    public void setLastFailureReason(String lastFailureReason) { this.lastFailureReason = lastFailureReason; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }

    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public String getContractStage() { return contractStage; }

    public void setContractStage(String contractStage) { this.contractStage = contractStage; }

    public String getImplementationStage() { return implementationStage; }

    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
