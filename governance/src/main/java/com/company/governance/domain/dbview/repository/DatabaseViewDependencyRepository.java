package com.company.governance.domain.dbview.repository;

import com.company.governance.domain.dbview.entity.DatabaseViewDependency;
import java.util.List;

public interface DatabaseViewDependencyRepository {

    List<DatabaseViewDependency> findByTenantAndDbViewId(String tenantId, String dbViewId);
}
