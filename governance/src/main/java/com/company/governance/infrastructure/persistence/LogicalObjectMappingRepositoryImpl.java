package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.logicalview.entity.LogicalObjectMapping;
import com.company.governance.domain.logicalview.repository.LogicalObjectMappingRepository;
import com.company.governance.infrastructure.persistence.mapper.LogicalObjectMappingMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class LogicalObjectMappingRepositoryImpl implements LogicalObjectMappingRepository {

    private final LogicalObjectMappingMapper logicalObjectMappingMapper;

    public LogicalObjectMappingRepositoryImpl(LogicalObjectMappingMapper logicalObjectMappingMapper) {
        this.logicalObjectMappingMapper = logicalObjectMappingMapper;
    }

    @Override
    public List<LogicalObjectMapping> findByTenantAndLogicalViewId(String tenantId, String logicalViewId) {
        return logicalObjectMappingMapper.selectByTenantAndLogicalViewId(tenantId, logicalViewId);
    }
}
