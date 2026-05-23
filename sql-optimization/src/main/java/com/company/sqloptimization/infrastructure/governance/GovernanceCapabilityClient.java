package com.company.sqloptimization.infrastructure.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertRequest;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertResponse;

public interface GovernanceCapabilityClient {

    void assertDatasourceAccess(String tenantId,
                             DataSourceTypeEnum datasourceType,
                             String resourceType,
                             String resourceId,
                             String operationCode);

    GovernanceAccelerationPlanTraceResponse writeAccelerationPlanTrace(GovernanceAccelerationPlanTraceRequest request);

    GovernanceDbViewResolveResponse resolveDbView(GovernanceDbViewResolveRequest request);

    GovernanceJdbcDatasourceResolveResponse resolveJdbcDatasource(GovernanceJdbcDatasourceResolveRequest request);

    GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request);

    GovernanceReportInterfaceConfigResponse resolveReportInterfaceConfig(GovernanceReportInterfaceConfigRequest request);

    GovernanceSqlRewriteDivergenceAlertResponse emitSqlRewriteDivergenceAlert(
        GovernanceSqlRewriteDivergenceAlertRequest request
    );

    void writeAudit(OptimizationAuditRecord auditRecord);
}
