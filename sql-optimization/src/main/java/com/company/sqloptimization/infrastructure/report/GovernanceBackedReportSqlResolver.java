package com.company.sqloptimization.infrastructure.report;

import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqloptimization.application.service.report.MockReportSqlFactory;
import com.company.sqloptimization.application.service.report.ReportSqlResolveRequest;
import com.company.sqloptimization.application.service.report.ReportSqlResolveResult;
import com.company.sqloptimization.application.service.report.ReportSqlResolver;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GovernanceBackedReportSqlResolver implements ReportSqlResolver {

    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final ReportSqlHttpClient reportSqlHttpClient;

    public GovernanceBackedReportSqlResolver(GovernanceCapabilityClient governanceCapabilityClient,
                                             ReportSqlHttpClient reportSqlHttpClient) {
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.reportSqlHttpClient = reportSqlHttpClient;
    }

    @Override
    public ReportSqlResolveResult resolve(ReportSqlResolveRequest request) {
        GovernanceReportInterfaceConfigResponse config;
        try {
            config = governanceCapabilityClient.resolveReportInterfaceConfig(toGovernanceRequest(request));
        } catch (RuntimeException ex) {
            return MockReportSqlFactory.resolve(request, "GOVERNANCE_REPORT_INTERFACE_CONFIG_UNAVAILABLE");
        }
        if (config == null || !Boolean.TRUE.equals(config.getEnabled()) || !"ACTIVE".equals(config.getResolverStatus())) {
            return MockReportSqlFactory.resolve(request, config == null ? "REPORT_INTERFACE_CONFIG_EMPTY" : config.getUnavailableReason());
        }
        if (!"HTTP_API".equals(config.getSourceType())) {
            return MockReportSqlFactory.resolve(request, "REPORT_INTERFACE_SOURCE_TYPE_NOT_HTTP_API");
        }
        try {
            String sql = reportSqlHttpClient.fetchSql(config, request);
            if (StringUtils.hasText(sql)) {
                return new ReportSqlResolveResult(sql, "HTTP_API", "RESOLVED", null);
            }
            return MockReportSqlFactory.resolve(request, "REPORT_SQL_EMPTY");
        } catch (RuntimeException ex) {
            return MockReportSqlFactory.resolve(request, "REPORT_SQL_FETCH_FAILED");
        }
    }

    private GovernanceReportInterfaceConfigRequest toGovernanceRequest(ReportSqlResolveRequest request) {
        GovernanceReportInterfaceConfigRequest governanceRequest = new GovernanceReportInterfaceConfigRequest();
        governanceRequest.setTenantId(request == null ? null : request.getTenantId());
        governanceRequest.setDatasourceCode(request == null ? null : request.getDatasourceCode());
        governanceRequest.setStage(request == null ? null : request.getStage());
        governanceRequest.setReportCode(request == null ? null : request.getReportCode());
        return governanceRequest;
    }
}
