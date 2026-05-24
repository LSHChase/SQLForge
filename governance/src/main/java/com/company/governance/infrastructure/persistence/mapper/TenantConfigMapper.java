package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.tenant.entity.TenantConfig;
import java.util.List;

public interface TenantConfigMapper {

    TenantConfig selectByTenantId(String tenantId);

    List<TenantConfig> selectAll();

    int insert(TenantConfig tenantConfig);

    int updateByTenantId(TenantConfig tenantConfig);

    int deleteByTenantId(String tenantId);
}
