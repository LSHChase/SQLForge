package com.company.sqloptimization.infrastructure.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqloptimization.application.service.report.ReportSqlResolveRequest;
import com.company.sqloptimization.application.service.report.ReportSqlResolveResult;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import org.junit.jupiter.api.Test;

class GovernanceBackedReportSqlResolverTest {

    @Test
    void shouldFetchSqlThroughConfiguredHttpInterface() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        ReportSqlHttpClient reportSqlHttpClient = mock(ReportSqlHttpClient.class);
        GovernanceBackedReportSqlResolver resolver =
            new GovernanceBackedReportSqlResolver(governanceCapabilityClient, reportSqlHttpClient);
        GovernanceReportInterfaceConfigResponse config = activeConfig();
        when(governanceCapabilityClient.resolveReportInterfaceConfig(org.mockito.ArgumentMatchers.any(GovernanceReportInterfaceConfigRequest.class)))
            .thenReturn(config);
        ReportSqlResolveRequest request = new ReportSqlResolveRequest("tenant-a", "hetu_main", "PROD", "high", "RPT_A");
        when(reportSqlHttpClient.fetchSql(config, request)).thenReturn("SELECT * FROM remote_report_sql WHERE report_code = 'RPT_A'");

        ReportSqlResolveResult result = resolver.resolve(request);

        assertEquals("RESOLVED", result.getResolverStatus());
        assertEquals("HTTP_API", result.getSourceType());
        assertEquals("SELECT * FROM remote_report_sql WHERE report_code = 'RPT_A'", result.getSqlText());
    }

    @Test
    void shouldFallbackToMockWhenGovernanceConfigIsUnavailable() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        ReportSqlHttpClient reportSqlHttpClient = mock(ReportSqlHttpClient.class);
        GovernanceBackedReportSqlResolver resolver =
            new GovernanceBackedReportSqlResolver(governanceCapabilityClient, reportSqlHttpClient);
        when(governanceCapabilityClient.resolveReportInterfaceConfig(org.mockito.ArgumentMatchers.any(GovernanceReportInterfaceConfigRequest.class)))
            .thenThrow(new IllegalStateException("governance 不可用"));

        ReportSqlResolveResult result = resolver.resolve(
            new ReportSqlResolveRequest("tenant-a", "hetu_main", "PROD", "high", "RPT_A")
        );

        assertEquals("MOCK_FALLBACK", result.getResolverStatus());
        assertEquals("TXT_MOCK_SOURCE", result.getSourceType());
        assertTrue(result.getSqlText().contains("mock_report_source"));
    }

    private GovernanceReportInterfaceConfigResponse activeConfig() {
        GovernanceReportInterfaceConfigResponse response = new GovernanceReportInterfaceConfigResponse();
        response.setTenantId("tenant-a");
        response.setDatasourceCode("hetu_main");
        response.setStage("PROD");
        response.setSourceType("HTTP_API");
        response.setEndpointCode("report-api-main");
        response.setBaseUrl("http://report-api.local");
        response.setPathTemplate("/api/reports/sql");
        response.setHttpMethod("GET");
        response.setReportCodeParamName("report_code");
        response.setSqlJsonPath("$.sql");
        response.setEnabled(Boolean.TRUE);
        response.setResolverStatus("ACTIVE");
        return response;
    }
}
