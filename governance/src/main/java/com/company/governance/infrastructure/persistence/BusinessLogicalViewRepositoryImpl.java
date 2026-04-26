package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.logicalview.entity.BusinessLogicalView;
import com.company.governance.domain.logicalview.repository.BusinessLogicalViewRepository;
import com.company.governance.infrastructure.persistence.mapper.BusinessLogicalViewMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessLogicalViewRepositoryImpl implements BusinessLogicalViewRepository {

    private final BusinessLogicalViewMapper businessLogicalViewMapper;

    public BusinessLogicalViewRepositoryImpl(BusinessLogicalViewMapper businessLogicalViewMapper) {
        this.businessLogicalViewMapper = businessLogicalViewMapper;
    }

    @Override
    public List<BusinessLogicalView> findByTenantAndDatasource(String tenantId, String datasourceCode) {
        return businessLogicalViewMapper.selectByTenantAndDatasource(tenantId, datasourceCode);
    }

    @Override
    public Optional<BusinessLogicalView> findByTenantAndViewCode(String tenantId, String viewCode) {
        return Optional.ofNullable(businessLogicalViewMapper.selectByTenantAndViewCode(tenantId, viewCode));
    }
}
