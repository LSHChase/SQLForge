package com.company.governance.domain.datasource.repository;

import com.company.governance.domain.datasource.DatasourceConfig;
import java.util.List;
import java.util.Optional;

public interface DatasourceConfigRepository {

    DatasourceConfig save(DatasourceConfig config);

    Optional<DatasourceConfig> findByTenantIdAndDatasourceId(String tenantId, String datasourceId);

    List<DatasourceConfig> findByTenantId(String tenantId);
}
