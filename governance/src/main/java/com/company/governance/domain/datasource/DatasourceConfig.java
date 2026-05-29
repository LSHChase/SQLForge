package com.company.governance.domain.datasource;

import java.time.Instant;

public class DatasourceConfig extends DatasourceConfigBase {

    public DatasourceConfig(String datasourceId,
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
        super(datasourceId, tenantId, datasourceCode, datasourceName, engineType, connectionMode, stage, jdbcUrl,
            jdbcDriverClassName, driverSourceType, driverArtifactId, driverVersionLabel, driverSha256,
            driverLoadStatus, username, apiBaseUrl, clientEndpoint, gatewayEndpoint, proxyEndpoint, authMode,
            credentialMode, credentialRef, credentialMask, credentialCiphertext, encryptionAlgorithm,
            encryptionKeyId, tlsEnabled, verifyPeer, readonly, enabled, timeoutMs, healthStatus, lastFailureReason,
            lastCheckedAt, lastCheckElapsedMs, updatedAt);
    }
}
