package com.company.sqloptimization.infrastructure.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;

public interface GovernanceCapabilityClient {

    void assertTenantScope(String tenantId);

    void assertDatasourceAccess(String tenantId, DataSourceTypeEnum datasourceType);

    void writeAudit(OptimizationAuditRecord auditRecord);
}
