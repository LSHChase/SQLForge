package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.governance.application.controller.dto.ReportInterfaceConfigUpsertRequest;
import com.company.governance.infrastructure.repository.InMemoryReportInterfaceConfigRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReportInterfaceConfigApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldUpsertAndResolveBestMatchingReportInterfaceConfig() {
        RequestContext.set("tenant-a", "admin-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        ReportInterfaceConfigApplicationService service =
            new ReportInterfaceConfigApplicationService(new InMemoryReportInterfaceConfigRepository());

        ReportInterfaceConfigUpsertRequest upsertRequest = new ReportInterfaceConfigUpsertRequest();
        upsertRequest.setTenantId("tenant-a");
        upsertRequest.setDatasourceCode("hetu_main");
        upsertRequest.setStage("PROD");
        upsertRequest.setEndpointCode("report-api-main");
        upsertRequest.setEndpointName("Report SQL API");
        upsertRequest.setBaseUrl("http://report-api.local");
        upsertRequest.setPathTemplate("/api/reports/sql");
        upsertRequest.setReportCodeParamName("report_code");
        upsertRequest.setSqlJsonPath("$.sql");

        GovernanceReportInterfaceConfigResponse saved = service.upsert(upsertRequest);

        GovernanceReportInterfaceConfigRequest resolveRequest = new GovernanceReportInterfaceConfigRequest();
        resolveRequest.setTenantId("tenant-a");
        resolveRequest.setDatasourceCode("hetu_main");
        resolveRequest.setStage("PROD");
        resolveRequest.setReportCode("RPT_SALES_DAILY");
        GovernanceReportInterfaceConfigResponse resolved = service.resolve(resolveRequest);

        assertEquals("ACTIVE", saved.getResolverStatus());
        assertEquals("HTTP_API", resolved.getSourceType());
        assertEquals("report-api-main", resolved.getEndpointCode());
        assertEquals("http://report-api.local", resolved.getBaseUrl());
    }

    @Test
    void shouldReturnMockFallbackWhenNoConfigExists() {
        RequestContext.set("tenant-a", "admin-001", Arrays.asList("TENANT_ADMIN"), "request-002", "trace-002", "header", 1L, 2L);
        ReportInterfaceConfigApplicationService service =
            new ReportInterfaceConfigApplicationService(new InMemoryReportInterfaceConfigRepository());

        GovernanceReportInterfaceConfigRequest request = new GovernanceReportInterfaceConfigRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setStage("PROD");

        GovernanceReportInterfaceConfigResponse response = service.resolve(request);

        assertEquals("TXT_MOCK_SOURCE", response.getSourceType());
        assertEquals("MOCK_FALLBACK", response.getResolverStatus());
        assertEquals("REPORT_INTERFACE_CONFIG_NOT_FOUND", response.getUnavailableReason());
    }
}
