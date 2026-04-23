package com.company.benchmarkengine.infrastructure.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;

public interface GovernanceCapabilityClient {

    void assertAuthorization(String tenantId,
                             DataSourceTypeEnum datasourceType,
                             String resourceType,
                             String resourceId,
                             String operationCode);

    void writeAudit(BenchmarkAuditRecord auditRecord);
}
