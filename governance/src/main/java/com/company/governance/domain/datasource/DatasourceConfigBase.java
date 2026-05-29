package com.company.governance.domain.datasource;

import java.time.Instant;

public class DatasourceConfigBase {

    private final String datasourceId;
    private final String tenantId;
    private final String datasourceCode;
    private final String datasourceName;
    private final String engineType;
    private final String connectionMode;
    private final String stage;
    private final String jdbcUrl;
    private final String jdbcDriverClassName;
    private final String driverSourceType;
    private final String driverArtifactId;
    private final String driverVersionLabel;
    private final String driverSha256;
    private final String driverLoadStatus;
    private final String username;
    private final String apiBaseUrl;
    private final String clientEndpoint;
    private final String gatewayEndpoint;
    private final String proxyEndpoint;
    private final String authMode;
    private final String credentialMode;
    private final String credentialRef;
    private final String credentialMask;
    private final String credentialCiphertext;
    private final String encryptionAlgorithm;
    private final String encryptionKeyId;
    private final boolean tlsEnabled;
    private final boolean verifyPeer;
    private final boolean readonly;
    private final boolean enabled;
    private final int timeoutMs;
    private final String healthStatus;
    private final String lastFailureReason;
    private final Instant lastCheckedAt;
    private final Long lastCheckElapsedMs;
    private final Instant updatedAt;

    public DatasourceConfigBase(String datasourceId,
                            String tenantId,
                            String datasourceCode,
                            String datasourceName,
                            String engineType,
                            String connectionMode,
                            String stage,
                            String jdbcUrl,
                            String jdbcDriverClassName,
                            String driverSourceType,
                            String driverArtifactId,
                            String driverVersionLabel,
                            String driverSha256,
                            String driverLoadStatus,
                            String username,
                            String apiBaseUrl,
                            String clientEndpoint,
                            String gatewayEndpoint,
                            String proxyEndpoint,
                            String authMode,
                            String credentialMode,
                            String credentialRef,
                            String credentialMask,
                            String credentialCiphertext,
                            String encryptionAlgorithm,
                            String encryptionKeyId,
                            boolean tlsEnabled,
                            boolean verifyPeer,
                            boolean readonly,
                            boolean enabled,
                            int timeoutMs,
                            String healthStatus,
                            String lastFailureReason,
                            Instant lastCheckedAt,
                            Long lastCheckElapsedMs,
                            Instant updatedAt) {
        this.datasourceId = datasourceId;
        this.tenantId = tenantId;
        this.datasourceCode = datasourceCode;
        this.datasourceName = datasourceName;
        this.engineType = engineType;
        this.connectionMode = connectionMode;
        this.stage = stage;
        this.jdbcUrl = jdbcUrl;
        this.jdbcDriverClassName = jdbcDriverClassName;
        this.driverSourceType = driverSourceType;
        this.driverArtifactId = driverArtifactId;
        this.driverVersionLabel = driverVersionLabel;
        this.driverSha256 = driverSha256;
        this.driverLoadStatus = driverLoadStatus;
        this.username = username;
        this.apiBaseUrl = apiBaseUrl;
        this.clientEndpoint = clientEndpoint;
        this.gatewayEndpoint = gatewayEndpoint;
        this.proxyEndpoint = proxyEndpoint;
        this.authMode = authMode;
        this.credentialMode = credentialMode;
        this.credentialRef = credentialRef;
        this.credentialMask = credentialMask;
        this.credentialCiphertext = credentialCiphertext;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.encryptionKeyId = encryptionKeyId;
        this.tlsEnabled = tlsEnabled;
        this.verifyPeer = verifyPeer;
        this.readonly = readonly;
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
        this.healthStatus = healthStatus;
        this.lastFailureReason = lastFailureReason;
        this.lastCheckedAt = lastCheckedAt;
        this.lastCheckElapsedMs = lastCheckElapsedMs;
        this.updatedAt = updatedAt;
    }

    public DatasourceConfig withHealthStatus(String nextHealthStatus,
                                             String nextFailureReason,
                                             Instant nextCheckedAt,
                                             Long nextElapsedMs) {
        return new DatasourceConfig(
            datasourceId,
            tenantId,
            datasourceCode,
            datasourceName,
            engineType,
            connectionMode,
            stage,
            jdbcUrl,
            jdbcDriverClassName,
            driverSourceType,
            driverArtifactId,
            driverVersionLabel,
            driverSha256,
            driverLoadStatus,
            username,
            apiBaseUrl,
            clientEndpoint,
            gatewayEndpoint,
            proxyEndpoint,
            authMode,
            credentialMode,
            credentialRef,
            credentialMask,
            credentialCiphertext,
            encryptionAlgorithm,
            encryptionKeyId,
            tlsEnabled,
            verifyPeer,
            readonly,
            enabled,
            timeoutMs,
            nextHealthStatus,
            nextFailureReason,
            nextCheckedAt,
            nextElapsedMs,
            Instant.now()
        );
    }

    public String getDatasourceId() { return datasourceId; }

    public String getTenantId() { return tenantId; }

    public String getDatasourceCode() { return datasourceCode; }

    public String getDatasourceName() { return datasourceName; }

    public String getEngineType() { return engineType; }

    public String getConnectionMode() { return connectionMode; }

    public String getStage() { return stage; }

    public String getJdbcUrl() { return jdbcUrl; }

    public String getJdbcDriverClassName() { return jdbcDriverClassName; }

    public String getDriverSourceType() { return driverSourceType; }

    public String getDriverArtifactId() { return driverArtifactId; }

    public String getDriverVersionLabel() { return driverVersionLabel; }

    public String getDriverSha256() { return driverSha256; }

    public String getDriverLoadStatus() { return driverLoadStatus; }

    public String getUsername() { return username; }

    public String getApiBaseUrl() { return apiBaseUrl; }

    public String getClientEndpoint() { return clientEndpoint; }

    public String getGatewayEndpoint() { return gatewayEndpoint; }

    public String getProxyEndpoint() { return proxyEndpoint; }

    public String getAuthMode() { return authMode; }

    public String getCredentialMode() { return credentialMode; }

    public String getCredentialRef() { return credentialRef; }

    public String getCredentialMask() { return credentialMask; }

    public String getCredentialCiphertext() { return credentialCiphertext; }

    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }

    public String getEncryptionKeyId() { return encryptionKeyId; }

    public boolean isTlsEnabled() { return tlsEnabled; }

    public boolean isVerifyPeer() { return verifyPeer; }

    public boolean isReadonly() { return readonly; }

    public boolean isEnabled() { return enabled; }

    public int getTimeoutMs() { return timeoutMs; }

    public String getHealthStatus() { return healthStatus; }

    public String getLastFailureReason() { return lastFailureReason; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }

    public Long getLastCheckElapsedMs() { return lastCheckElapsedMs; }

    public Instant getUpdatedAt() { return updatedAt; }
}
