package com.company.governance.domain.dbview.repository;

import com.company.governance.domain.dbview.entity.DatabaseViewRef;
import java.util.List;
import java.util.Optional;

public interface DatabaseViewRepository {

    Optional<DatabaseViewRef> findByTenantDatasourceAndViewName(String tenantId, String datasourceCode, String viewName);

    List<DatabaseViewRef> findByTenantAndDatasource(String tenantId, String datasourceCode);
}
