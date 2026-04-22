package com.company.benchmarkengine.infrastructure.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;

public interface GovernanceCapabilityClient {

    void assertTenantScope(String tenantId);

    void assertDatasourceAccess(String tenantId, DataSourceTypeEnum datasourceType);

    void writeAudit(BenchmarkAuditRecord auditRecord);
}
