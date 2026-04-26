package com.company.sqloptimization.infrastructure.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;

public interface GovernanceCapabilityClient {

    void assertAuthorization(String tenantId,
                             DataSourceTypeEnum datasourceType,
                             String resourceType,
                             String resourceId,
                             String operationCode);

    GovernanceAccelerationPlanTraceResponse writeAccelerationPlanTrace(GovernanceAccelerationPlanTraceRequest request);

    void writeAudit(OptimizationAuditRecord auditRecord);
}
