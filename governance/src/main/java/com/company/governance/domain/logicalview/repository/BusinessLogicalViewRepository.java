package com.company.governance.domain.logicalview.repository;

import com.company.governance.domain.logicalview.entity.BusinessLogicalView;
import java.util.List;
import java.util.Optional;

public interface BusinessLogicalViewRepository {

    List<BusinessLogicalView> findByTenantAndDatasource(String tenantId, String datasourceCode);

    Optional<BusinessLogicalView> findByTenantAndViewCode(String tenantId, String viewCode);
}
