package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.dbview.entity.DatabaseViewDependency;
import com.company.governance.domain.dbview.repository.DatabaseViewDependencyRepository;
import com.company.governance.infrastructure.persistence.mapper.DatabaseViewDependencyMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseViewDependencyRepositoryImpl implements DatabaseViewDependencyRepository {

    private final DatabaseViewDependencyMapper databaseViewDependencyMapper;

    public DatabaseViewDependencyRepositoryImpl(DatabaseViewDependencyMapper databaseViewDependencyMapper) {
        this.databaseViewDependencyMapper = databaseViewDependencyMapper;
    }

    @Override
    public List<DatabaseViewDependency> findByTenantAndDbViewId(String tenantId, String dbViewId) {
        return databaseViewDependencyMapper.selectByTenantAndDbViewId(tenantId, dbViewId);
    }
}
