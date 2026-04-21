package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.tenant.entity.TenantConfig;

public interface TenantConfigMapper {

    TenantConfig selectByTenantId(String tenantId);

    int insert(TenantConfig tenantConfig);

    int updateByTenantId(TenantConfig tenantConfig);

    int deleteByTenantId(String tenantId);
}
