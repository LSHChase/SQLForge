package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.domain.datasource.repository.DatasourceConfigRepository;
import com.company.governance.infrastructure.persistence.entity.DatasourceConfigRecord;
import com.company.governance.infrastructure.persistence.mapper.DatasourceConfigMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DatasourceConfigRepositoryImpl implements DatasourceConfigRepository {

    private final DatasourceConfigMapper datasourceConfigMapper;

    public DatasourceConfigRepositoryImpl(DatasourceConfigMapper datasourceConfigMapper) {
        this.datasourceConfigMapper = datasourceConfigMapper;
    }

    @Override
    public DatasourceConfig save(DatasourceConfig config) {
        datasourceConfigMapper.upsert(toRecord(config));
        return config;
    }

    @Override
    public Optional<DatasourceConfig> findByTenantIdAndDatasourceId(String tenantId, String datasourceId) {
        return Optional.ofNullable(toDomain(datasourceConfigMapper.selectByTenantIdAndDatasourceId(tenantId, datasourceId)));
    }

    @Override
    public Optional<DatasourceConfig> findByTenantIdAndDatasourceCodeAndEngineType(String tenantId,
                                                                                   String datasourceCode,
                                                                                   String engineType) {
        return Optional.ofNullable(toDomain(
            datasourceConfigMapper.selectByTenantIdAndDatasourceCodeAndEngineType(tenantId, datasourceCode, engineType)
        ));
    }

    @Override
    public List<DatasourceConfig> findByTenantId(String tenantId) {
        List<DatasourceConfigRecord> records = datasourceConfigMapper.selectByTenantId(tenantId);
        List<DatasourceConfig> result = new ArrayList<DatasourceConfig>(records == null ? 0 : records.size());
        if (records != null) {
            for (DatasourceConfigRecord record : records) {
                result.add(toDomain(record));
            }
        }
        return result;
    }

    private DatasourceConfigRecord toRecord(DatasourceConfig config) {
        DatasourceConfigRecord record = new DatasourceConfigRecord();
        record.setDatasourceId(config.getDatasourceId());
        record.setTenantId(config.getTenantId());
        record.setDatasourceCode(config.getDatasourceCode());
        record.setDatasourceName(config.getDatasourceName());
        record.setEngineType(config.getEngineType());
        record.setConnectionMode(config.getConnectionMode());
        record.setStage(config.getStage());
        record.setJdbcUrl(config.getJdbcUrl());
        record.setJdbcDriverClassName(config.getJdbcDriverClassName());
        record.setDriverSourceType(config.getDriverSourceType());
        record.setDriverArtifactId(config.getDriverArtifactId());
        record.setDriverVersionLabel(config.getDriverVersionLabel());
        record.setDriverSha256(config.getDriverSha256());
        record.setDriverLoadStatus(config.getDriverLoadStatus());
        record.setUsername(config.getUsername());
        record.setApiBaseUrl(config.getApiBaseUrl());
        record.setClientEndpoint(config.getClientEndpoint());
        record.setGatewayEndpoint(config.getGatewayEndpoint());
        record.setProxyEndpoint(config.getProxyEndpoint());
        record.setAuthMode(config.getAuthMode());
        record.setCredentialMode(config.getCredentialMode());
        record.setCredentialRef(config.getCredentialRef());
        record.setCredentialMask(config.getCredentialMask());
        record.setCredentialCiphertext(config.getCredentialCiphertext());
        record.setEncryptionAlgorithm(config.getEncryptionAlgorithm());
        record.setEncryptionKeyId(config.getEncryptionKeyId());
        record.setTlsEnabled(Boolean.valueOf(config.isTlsEnabled()));
        record.setVerifyPeer(Boolean.valueOf(config.isVerifyPeer()));
        record.setReadonly(Boolean.valueOf(config.isReadonly()));
        record.setEnabled(Boolean.valueOf(config.isEnabled()));
        record.setTimeoutMs(Integer.valueOf(config.getTimeoutMs()));
        record.setHealthStatus(config.getHealthStatus());
        record.setLastFailureReason(config.getLastFailureReason());
        record.setLastCheckedAt(config.getLastCheckedAt());
        record.setLastCheckElapsedMs(config.getLastCheckElapsedMs());
        Instant now = config.getUpdatedAt() == null ? Instant.now() : config.getUpdatedAt();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private DatasourceConfig toDomain(DatasourceConfigRecord record) {
        if (record == null) {
            return null;
        }
        return new DatasourceConfig(
            record.getDatasourceId(),
            record.getTenantId(),
            record.getDatasourceCode(),
            record.getDatasourceName(),
            record.getEngineType(),
            record.getConnectionMode(),
            record.getStage(),
            record.getJdbcUrl(),
            record.getJdbcDriverClassName(),
            record.getDriverSourceType(),
            record.getDriverArtifactId(),
            record.getDriverVersionLabel(),
            record.getDriverSha256(),
            record.getDriverLoadStatus(),
            record.getUsername(),
            record.getApiBaseUrl(),
            record.getClientEndpoint(),
            record.getGatewayEndpoint(),
            record.getProxyEndpoint(),
            record.getAuthMode(),
            record.getCredentialMode(),
            record.getCredentialRef(),
            record.getCredentialMask(),
            record.getCredentialCiphertext(),
            record.getEncryptionAlgorithm(),
            record.getEncryptionKeyId(),
            Boolean.TRUE.equals(record.getTlsEnabled()),
            record.getVerifyPeer() == null || Boolean.TRUE.equals(record.getVerifyPeer()),
            record.getReadonly() == null || Boolean.TRUE.equals(record.getReadonly()),
            record.getEnabled() == null || Boolean.TRUE.equals(record.getEnabled()),
            record.getTimeoutMs() == null ? 3000 : record.getTimeoutMs().intValue(),
            record.getHealthStatus(),
            record.getLastFailureReason(),
            record.getLastCheckedAt(),
            record.getLastCheckElapsedMs(),
            record.getUpdateTime()
        );
    }
}
