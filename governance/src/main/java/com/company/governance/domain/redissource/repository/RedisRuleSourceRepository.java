package com.company.governance.domain.redissource.repository;

import com.company.governance.domain.redissource.RedisRuleSourceConfig;
import java.util.List;
import java.util.Optional;

public interface RedisRuleSourceRepository {

    RedisRuleSourceConfig save(RedisRuleSourceConfig config);

    List<RedisRuleSourceConfig> findByTenantId(String tenantId);

    Optional<RedisRuleSourceConfig> findByTenantIdAndSourceId(String tenantId, String sourceId);
}
