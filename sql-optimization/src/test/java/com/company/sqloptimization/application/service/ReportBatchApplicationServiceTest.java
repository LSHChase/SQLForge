package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusResponse;
import com.company.sqloptimization.application.service.report.MockReportSqlFactory;
import com.company.sqloptimization.application.service.report.ReportSqlResolver;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchRepository;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    @Test
    void shouldImportWideCsvReportSqlColumnsAsSqlItems() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-002", "trace-002", "header", 1L, 2L);

        StringBuilder csv = new StringBuilder("report_code");
        for (int index = 1; index <= 100; index++) {
            csv.append(",sql_").append(index);
        }
        csv.append('\n').append("RPT_WIDE");
        for (int index = 1; index <= 100; index++) {
            csv.append(",\"SELECT ").append(index).append(" AS metric_value\"");
        }

        ReportBatchStatusResponse imported = service.importBatch(baseRequest("wide-csv", "CSV", csv.toString()));

        assertEquals(Integer.valueOf(1), imported.getTotalReports());
        assertEquals(Integer.valueOf(100), imported.getTotalSqls());
        assertEquals(Integer.valueOf(100), Integer.valueOf(imported.getReportItems().size()));
        assertEquals("sql_1", imported.getReportItems().get(0).getSqlColumnName());
        assertEquals(Integer.valueOf(1), imported.getReportItems().get(0).getSqlOrdinalInReport());
        assertEquals("sql_100", imported.getReportItems().get(99).getSqlColumnName());
    }

    @Test
    void shouldResolveInlineReportSqlsWithoutCallingMockResolver() {
        AtomicInteger resolverCalls = new AtomicInteger(0);
        ReportBatchApplicationService service = buildService(request -> {
            resolverCalls.incrementAndGet();
            return MockReportSqlFactory.resolve(request, "UNIT_TEST_MOCK_SOURCE");
        });
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-003", "trace-003", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "inline-csv",
            "CSV",
            "report_code,sql_1,sql_2\nRPT_INLINE,\"SELECT * FROM orders\",\"SELECT * FROM customers\""
        ));
        ReportBatchStatusResponse resolved = service.resolveSqls(imported.getBatchId());

        assertEquals(Integer.valueOf(0), Integer.valueOf(resolverCalls.get()));
        assertEquals("COMPLETED", resolved.getStatus());
        assertEquals(Integer.valueOf(1), resolved.getTotalReports());
        assertEquals(Integer.valueOf(2), resolved.getTotalSqls());
        assertEquals(Integer.valueOf(2), resolved.getResolvedSqls());
        assertEquals(Integer.valueOf(1), resolved.getResolvedReports());
    }

    @Test
    void shouldImportWideWorkbookReportSqlColumns() throws Exception {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-004", "trace-004", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "wide-xlsx",
            "XLSX",
            buildWideWorkbook()
        ));

        assertEquals(Integer.valueOf(1), imported.getTotalReports());
        assertEquals(Integer.valueOf(3), imported.getTotalSqls());
        assertEquals("sql_2", imported.getReportItems().get(1).getSqlColumnName());
        assertEquals("SELECT 3 AS metric_value", imported.getReportItems().get(2).getSqlText());
    }

    private ReportBatchImportRequest baseRequest(String batchName, String fileType, String content) {
        return baseRequest(batchName, fileType, content.getBytes(StandardCharsets.UTF_8));
    }

    private ReportBatchImportRequest baseRequest(String batchName, String fileType, byte[] content) {
        ReportBatchImportRequest request = new ReportBatchImportRequest();
        request.setTenantId("tenant-a");
        request.setBatchName(batchName);
        request.setFileType(fileType);
        request.setReportCodeField("report_code");
        request.setDatasourceCode("hetu_main");
        request.setStage("PROD");
        request.setPriority("high");
        request.setContentBase64(Base64.getEncoder().encodeToString(content));
        return request;
    }

    private byte[] buildWideWorkbook() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("reports");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("report_code");
        header.createCell(1).setCellValue("sql_1");
        header.createCell(2).setCellValue("sql_2");
        header.createCell(3).setCellValue("sql_3");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("RPT_XLSX_WIDE");
        row.createCell(1).setCellValue("SELECT 1 AS metric_value");
        row.createCell(2).setCellValue("SELECT 2 AS metric_value");
        row.createCell(3).setCellValue("SELECT 3 AS metric_value");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    private ReportBatchApplicationService buildService() {
        return buildService(request -> MockReportSqlFactory.resolve(request, "UNIT_TEST_MOCK_SOURCE"));
    }

    private ReportBatchApplicationService buildService(ReportSqlResolver reportSqlResolver) {
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
            reportSqlResolver
        );
    }
}
