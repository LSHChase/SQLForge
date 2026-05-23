package com.company.governance.infrastructure.repository;

import com.company.governance.domain.redissource.RedisRuleSourceConfig;
import com.company.governance.domain.redissource.repository.RedisRuleSourceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRedisRuleSourceRepository implements RedisRuleSourceRepository {

    private final Map<String, RedisRuleSourceConfig> configs = new ConcurrentHashMap<String, RedisRuleSourceConfig>();

    @Override
    public RedisRuleSourceConfig save(RedisRuleSourceConfig config) {
        configs.put(config.getSourceId(), config);
        return config;
    }

    @Override
    public List<RedisRuleSourceConfig> findByTenantId(String tenantId) {
        List<RedisRuleSourceConfig> result = new ArrayList<RedisRuleSourceConfig>();
        for (RedisRuleSourceConfig config : configs.values()) {
            if (tenantId.equals(config.getTenantId())) {
                result.add(config);
            }
        }
        result.sort(Comparator.comparing(RedisRuleSourceConfig::getSourceName));
        return result;
    }

    @Override
    public Optional<RedisRuleSourceConfig> findByTenantIdAndSourceId(String tenantId, String sourceId) {
        RedisRuleSourceConfig config = configs.get(sourceId);
        if (config == null || !tenantId.equals(config.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(config);
    }
}
