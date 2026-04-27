package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusResponse;
import com.company.sqloptimization.application.service.report.MockReportSqlFactory;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchRepository;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReportBatchApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldImportMockReportCatalogAndResolveSqls() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        ReportBatchImportRequest request = new ReportBatchImportRequest();
        request.setTenantId("tenant-a");
        request.setBatchName("report-batch-alpha");
        request.setFileType("TXT");
        request.setReportCodeField("report_code");
        request.setDatasourceCode("hetu_main");
        request.setStage("PROD");
        request.setPriority("high");
        request.setContentBase64(Base64.getEncoder().encodeToString((
            "RPT_A|Revenue Report|hetu_main|PROD|high\n"
                + "RPT_B|Ops Report|hetu_main|PROD|medium\n"
        ).getBytes(StandardCharsets.UTF_8)));

        ReportBatchStatusResponse imported = service.importBatch(request);
        assertEquals("READY", imported.getStatus());
        assertEquals(Integer.valueOf(2), imported.getTotalReports());

        ReportBatchStatusResponse resolved = service.resolveSqls(imported.getBatchId());
        assertEquals("COMPLETED", resolved.getStatus());
        assertEquals(Integer.valueOf(2), resolved.getResolvedReports());
        assertEquals("RPT_A", resolved.getReportItems().get(0).getReportCode());
        assertEquals("VALID", resolved.getReportItems().get(0).getStructureSyntaxStatus());
        assertEquals("AVAILABLE", resolved.getReportItems().get(0).getAccessServiceStatus());
    }

    private ReportBatchApplicationService buildService() {
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        StructureParseApplicationService structureService = new StructureParseApplicationService(
            new SqlOptimizationPipelineService(),
            governanceCapabilityClient
        );
        AccessParseApplicationService accessService = new AccessParseApplicationService();
        return new ReportBatchApplicationService(
            new InMemoryReportBatchRepository(),
            new InMemoryReportBatchItemRepository(),
            structureService,
            accessService,
            request -> MockReportSqlFactory.resolve(request, "UNIT_TEST_MOCK_SOURCE")
        );
    }
}
