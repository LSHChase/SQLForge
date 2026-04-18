package com.company.governance.domain.tenant.repository;

import com.company.governance.domain.tenant.entity.TenantConfig;
import java.util.Optional;

public interface TenantConfigRepository {

    Optional<TenantConfig> findByTenantId(String tenantId);

    int save(TenantConfig tenantConfig);

    int update(TenantConfig tenantConfig);

    int deleteByTenantId(String tenantId);
}
