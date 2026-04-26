package com.company.governance.domain.logicalview.repository;

import com.company.governance.domain.logicalview.entity.LogicalObjectMapping;
import java.util.List;

public interface LogicalObjectMappingRepository {

    List<LogicalObjectMapping> findByTenantAndLogicalViewId(String tenantId, String logicalViewId);
}
