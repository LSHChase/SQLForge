package com.company.governance.infrastructure.persistence.entity;

import java.time.Instant;

public class DatasourceConfigRecord {

    private String datasourceId;
    private String tenantId;
    private String datasourceCode;
    private String datasourceName;
    private String engineType;
    private String connectionMode;
    private String stage;
    private String jdbcUrl;
    private String jdbcDriverClassName;
    private String username;
    private String apiBaseUrl;
    private String clientEndpoint;
    private String gatewayEndpoint;
    private String proxyEndpoint;
    private String authMode;
    private String credentialMode;
    private String credentialRef;
    private String credentialMask;
    private String credentialCiphertext;
    private String encryptionAlgorithm;
    private String encryptionKeyId;
    private Boolean tlsEnabled;
    private Boolean verifyPeer;
    private Boolean readonly;
    private Boolean enabled;
    private Integer timeoutMs;
    private String healthStatus;
    private String lastFailureReason;
    private Instant lastCheckedAt;
    private Long lastCheckElapsedMs;
    private Instant createTime;
    private Instant updateTime;

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
    public String getCredentialCiphertext() { return credentialCiphertext; }
    public void setCredentialCiphertext(String credentialCiphertext) { this.credentialCiphertext = credentialCiphertext; }
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }
    public String getEncryptionKeyId() { return encryptionKeyId; }
    public void setEncryptionKeyId(String encryptionKeyId) { this.encryptionKeyId = encryptionKeyId; }
    public Boolean getTlsEnabled() { return tlsEnabled; }
    public void setTlsEnabled(Boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }
    public Boolean getVerifyPeer() { return verifyPeer; }
    public void setVerifyPeer(Boolean verifyPeer) { this.verifyPeer = verifyPeer; }
    public Boolean getReadonly() { return readonly; }
    public void setReadonly(Boolean readonly) { this.readonly = readonly; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public String getLastFailureReason() { return lastFailureReason; }
    public void setLastFailureReason(String lastFailureReason) { this.lastFailureReason = lastFailureReason; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    public Long getLastCheckElapsedMs() { return lastCheckElapsedMs; }
    public void setLastCheckElapsedMs(Long lastCheckElapsedMs) { this.lastCheckElapsedMs = lastCheckElapsedMs; }
    public Instant getCreateTime() { return createTime; }
    public void setCreateTime(Instant createTime) { this.createTime = createTime; }
    public Instant getUpdateTime() { return updateTime; }
    public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
}
