package com.company.sqloptimization.infrastructure.report;

import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqloptimization.application.service.report.ReportSqlResolveRequest;

public interface ReportSqlHttpClient {

    String fetchSql(GovernanceReportInterfaceConfigResponse config, ReportSqlResolveRequest request);
}
