package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.governance.infrastructure.persistence.mapper.TenantConfigMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantConfigRepositoryImpl implements TenantConfigRepository {

    private final TenantConfigMapper tenantConfigMapper;

    public TenantConfigRepositoryImpl(TenantConfigMapper tenantConfigMapper) {
        this.tenantConfigMapper = tenantConfigMapper;
    }

    @Override
    public Optional<TenantConfig> findByTenantId(String tenantId) {
        return Optional.ofNullable(tenantConfigMapper.selectByTenantId(tenantId));
    }

    @Override
    public List<TenantConfig> findAll() {
        return tenantConfigMapper.selectAll();
    }

    @Override
    public int save(TenantConfig tenantConfig) {
        return tenantConfigMapper.insert(tenantConfig);
    }

    @Override
    public int update(TenantConfig tenantConfig) {
        return tenantConfigMapper.updateByTenantId(tenantConfig);
    }

    @Override
    public int deleteByTenantId(String tenantId) {
        return tenantConfigMapper.deleteByTenantId(tenantId);
    }
}
