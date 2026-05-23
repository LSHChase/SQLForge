package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.redissource.RedisRuleSourceConfig;
import com.company.governance.domain.redissource.repository.RedisRuleSourceRepository;
import com.company.governance.infrastructure.persistence.entity.RedisRuleSourceRecord;
import com.company.governance.infrastructure.persistence.mapper.RedisRuleSourceMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRedisRuleSourceRepository implements RedisRuleSourceRepository {

    private final RedisRuleSourceMapper mapper;

    public MybatisRedisRuleSourceRepository(RedisRuleSourceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RedisRuleSourceConfig save(RedisRuleSourceConfig config) {
        mapper.upsert(toRecord(config));
        return config;
    }

    @Override
    public List<RedisRuleSourceConfig> findByTenantId(String tenantId) {
        List<RedisRuleSourceRecord> records = mapper.selectByTenantId(tenantId);
        List<RedisRuleSourceConfig> result = new ArrayList<RedisRuleSourceConfig>(records == null ? 0 : records.size());
        if (records != null) {
            for (RedisRuleSourceRecord record : records) {
                result.add(toDomain(record));
            }
        }
        return result;
    }

    @Override
    public Optional<RedisRuleSourceConfig> findByTenantIdAndSourceId(String tenantId, String sourceId) {
        return Optional.ofNullable(toDomain(mapper.selectByTenantIdAndSourceId(tenantId, sourceId)));
    }

    private RedisRuleSourceRecord toRecord(RedisRuleSourceConfig config) {
        RedisRuleSourceRecord record = new RedisRuleSourceRecord();
        record.setSourceId(config.getSourceId());
        record.setTenantId(config.getTenantId());
        record.setSourceName(config.getSourceName());
        record.setRedisEndpoints(config.getRedisEndpoints());
        record.setRedisNamespace(config.getRedisNamespace());
        record.setKeyPattern(config.getKeyPattern());
        record.setAuthMode(config.getAuthMode());
        record.setCredentialRef(config.getCredentialRef());
        record.setBypassOnUnavailable(Boolean.valueOf(config.isBypassOnUnavailable()));
        record.setEnabled(Boolean.valueOf(config.isEnabled()));
        Instant now = config.getUpdatedAt() == null ? Instant.now() : config.getUpdatedAt();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private RedisRuleSourceConfig toDomain(RedisRuleSourceRecord record) {
        if (record == null) {
            return null;
        }
        return new RedisRuleSourceConfig(
            record.getSourceId(),
            record.getTenantId(),
            record.getSourceName(),
            record.getRedisEndpoints(),
            record.getRedisNamespace(),
            record.getKeyPattern(),
            record.getAuthMode(),
            record.getCredentialRef(),
            record.getBypassOnUnavailable() == null || Boolean.TRUE.equals(record.getBypassOnUnavailable()),
            record.getEnabled() == null || Boolean.TRUE.equals(record.getEnabled()),
            record.getUpdateTime()
        );
    }
}
