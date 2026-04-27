package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.dbview.entity.DatabaseViewRef;
import com.company.governance.domain.dbview.repository.DatabaseViewRepository;
import com.company.governance.infrastructure.persistence.mapper.DatabaseViewMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseViewRepositoryImpl implements DatabaseViewRepository {

    private final DatabaseViewMapper databaseViewMapper;

    public DatabaseViewRepositoryImpl(DatabaseViewMapper databaseViewMapper) {
        this.databaseViewMapper = databaseViewMapper;
    }

    @Override
    public Optional<DatabaseViewRef> findByTenantDatasourceAndViewName(String tenantId, String datasourceCode, String viewName) {
        return Optional.ofNullable(databaseViewMapper.selectByTenantDatasourceAndViewName(tenantId, datasourceCode, viewName));
    }

    @Override
    public List<DatabaseViewRef> findByTenantAndDatasource(String tenantId, String datasourceCode) {
        return databaseViewMapper.selectByTenantAndDatasource(tenantId, datasourceCode);
    }
}
