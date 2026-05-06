package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
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
        for (int index = 1; index <= 120; index++) {
            csv.append(",sql_").append(index);
        }
        csv.append('\n').append("RPT_WIDE");
        for (int index = 1; index <= 120; index++) {
            csv.append(",\"SELECT ").append(index).append(" AS metric_value\"");
        }

        ReportBatchStatusResponse imported = service.importBatch(baseRequest("wide-csv", "CSV", csv.toString()));

        assertEquals(Integer.valueOf(1), imported.getTotalReports());
        assertEquals(Integer.valueOf(120), imported.getTotalSqls());
        assertEquals(Integer.valueOf(120), Integer.valueOf(imported.getReportItems().size()));
        assertEquals("sql_1", imported.getReportItems().get(0).getSqlColumnName());
        assertEquals(Integer.valueOf(1), imported.getReportItems().get(0).getSqlOrdinalInReport());
        assertEquals("sql_120", imported.getReportItems().get(119).getSqlColumnName());
    }

    @Test
    void shouldKeepLargeReportImportSummaryFullWhileCappingReturnedItemPreview() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-009", "trace-009", "header", 1L, 2L);

        StringBuilder csv = new StringBuilder("report_code");
        for (int index = 1; index <= 650; index++) {
            csv.append(",sql_").append(index);
        }
        csv.append('\n').append("RPT_BIG");
        for (int index = 1; index <= 650; index++) {
            csv.append(",\"SELECT ").append(index).append(" AS metric_value\"");
        }

        ReportBatchStatusResponse imported = service.importBatch(baseRequest("large-csv", "CSV", csv.toString()));

        assertEquals(Integer.valueOf(1), imported.getTotalReports());
        assertEquals(Integer.valueOf(650), imported.getTotalSqls());
        assertEquals(Integer.valueOf(500), Integer.valueOf(imported.getReportItems().size()));
        assertEquals(Integer.valueOf(500), imported.getItemPreviewLimit());
        assertEquals(Boolean.TRUE, imported.getItemPreviewTruncated());
        assertEquals(Integer.valueOf(150), imported.getOmittedItemCount());
        assertEquals("sql_500", imported.getReportItems().get(499).getSqlColumnName());
    }

    @Test
    void shouldImportEveryNonEmptyCsvCellAfterFirstColumnAsSql() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-005", "trace-005", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "arbitrary-header-csv",
            "CSV",
            "报表代码,任意列A,空列,任意列B\n"
                + "RPT_ARBITRARY,\"SELECT * FROM orders\",,\"SELECT count(*) FROM revenue\""
        ));

        assertEquals(Integer.valueOf(1), imported.getTotalReports());
        assertEquals(Integer.valueOf(2), imported.getTotalSqls());
        assertEquals("RPT_ARBITRARY", imported.getReportItems().get(0).getReportCode());
        assertEquals("任意列a", imported.getReportItems().get(0).getSqlColumnName());
        assertEquals("SELECT * FROM orders", imported.getReportItems().get(0).getSqlText());
        assertEquals(Integer.valueOf(2), imported.getReportItems().get(1).getSqlOrdinalInReport());
        assertEquals("SELECT count(*) FROM revenue", imported.getReportItems().get(1).getSqlText());
    }

    @Test
    void shouldImportHeaderlessCsvRowsByFirstColumnPosition() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-006", "trace-006", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "headerless-csv",
            "CSV",
            "RPT_RAW,\"SELECT 1\",,\"SELECT 3\"\n"
                + "RPT_NEXT,\"SELECT 4\""
        ));

        assertEquals(Integer.valueOf(2), imported.getTotalReports());
        assertEquals(Integer.valueOf(3), imported.getTotalSqls());
        assertEquals("RPT_RAW", imported.getReportItems().get(0).getReportCode());
        assertEquals("sql_1", imported.getReportItems().get(0).getSqlColumnName());
        assertEquals("sql_3", imported.getReportItems().get(1).getSqlColumnName());
        assertEquals(Integer.valueOf(2), imported.getReportItems().get(1).getSqlOrdinalInReport());
        assertEquals("RPT_NEXT", imported.getReportItems().get(2).getReportCode());
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
        ReportBatchParseStatisticsVO statistics = resolved.getParseStatistics();
        assertNotNull(statistics);
        assertEquals(Integer.valueOf(2), statistics.getOverview().getTotalSqlCount());
        assertEquals(Integer.valueOf(2), statistics.getOverview().getIssueSqlCount());
        assertEquals(Integer.valueOf(2), Integer.valueOf(statistics.getSqlStatistics().size()));
        assertEquals("RPT_INLINE", statistics.getReportStatistics().get(0).getReportCode());
        assertTrue(statistics.getLogicalObjectStatistics().stream()
            .anyMatch(item -> "TABLE:orders".equals(item.getObjectKey())));
        assertNotNull(service.getBatchParseStatistics(imported.getBatchId()).getOverview());
    }

    @Test
    void shouldExposeFailedReportSqlDetailForResultInspection() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-008", "trace-008", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "failed-report-detail",
            "CSV",
            "report_code,sql_1\nRPT_BAD,\"SELECT FROM\""
        ));
        ReportBatchStatusResponse resolved = service.resolveSqls(imported.getBatchId());

        assertEquals("PARTIAL_COMPLETED", resolved.getStatus());
        assertEquals(Integer.valueOf(1), resolved.getFailedReports());
        assertEquals(Integer.valueOf(1), resolved.getFailedSqls());
        assertEquals("FAILED", resolved.getReportItems().get(0).getStatus());
        assertEquals("STRUCTURE_PARSE_INVALID", resolved.getReportItems().get(0).getFailureReason());
        assertEquals("INVALID", resolved.getReportItems().get(0).getStructureSyntaxStatus());
        assertEquals("AVAILABLE", resolved.getReportItems().get(0).getAccessServiceStatus());
        assertEquals("CONNECTED", resolved.getReportItems().get(0).getAccessConnectionStatus());
        assertEquals("sql_1", resolved.getReportItems().get(0).getSqlColumnName());
        assertEquals(Integer.valueOf(1), resolved.getReportItems().get(0).getSqlOrdinalInReport());
        assertEquals("SELECT FROM", resolved.getReportItems().get(0).getSqlText());
        assertNotNull(resolved.getReportItems().get(0).getParseTaskId());
    }

    @Test
    void shouldKeepSingleSqlIssueCodesForComplexReportBatchSql() {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-010", "trace-010", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "complex-report-csv",
            "CSV",
            "report_code,sql_1\nRPT_COMPLEX," + csvEscape(complexAntiPatternSql())
        ));
        ReportBatchStatusResponse resolved = service.resolveSqls(imported.getBatchId());

        assertEquals(Integer.valueOf(1), resolved.getTotalSqls());
        assertEquals(Integer.valueOf(1), resolved.getResolvedSqls());
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().size() >= 10);
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("SCALAR_SUBQUERY_IN_SELECT"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("NESTED_SUBQUERY_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("CORRELATED_SUBQUERY_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("FUNCTION_WRAPPED_PREDICATE"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("NOT_EXISTS_ANTI_JOIN_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("LEADING_WILDCARD_LIKE_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("OR_PREDICATE_INDEX_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("ORDER_BY_RANDOM_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("REPEATED_TABLE_SCAN_RISK"));
        assertTrue(resolved.getReportItems().get(0).getIssueScenes().contains("COMPLEX_QUERY_GRAPH_RISK"));
        assertTrue(resolved.getParseStatistics().getOverview().getTotalIssueCount().intValue() >= 10);
        assertTrue(resolved.getParseStatistics().getSqlStatistics().get(0).getIssueScenes()
            .contains("ORDER_BY_RANDOM_RISK"));
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

    @Test
    void shouldImportWorkbookRowsByFirstColumnPositionWhenHeaderIsLocalized() throws Exception {
        ReportBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-007", "trace-007", "header", 1L, 2L);

        ReportBatchStatusResponse imported = service.importBatch(baseRequest(
            "localized-xlsx",
            "XLSX",
            buildLocalizedWorkbook()
        ));

        assertEquals(Integer.valueOf(1), imported.getTotalReports());
        assertEquals(Integer.valueOf(2), imported.getTotalSqls());
        assertEquals("RPT_XLSX_CN", imported.getReportItems().get(0).getReportCode());
        assertEquals("任意列a", imported.getReportItems().get(0).getSqlColumnName());
        assertEquals("任意列c", imported.getReportItems().get(1).getSqlColumnName());
        assertEquals(Integer.valueOf(2), imported.getReportItems().get(1).getSqlOrdinalInReport());
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

    private String csvEscape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String complexAntiPatternSql() {
        return "-- complex anti-pattern query\n"
            + "SELECT c.customer_id, c.customer_name, c.state,\n"
            + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS total_orders,\n"
            + "(SELECT SUM(order_amount) FROM orders o WHERE o.customer_id = c.customer_id) AS total_spent,\n"
            + "(SELECT GROUP_CONCAT(product_name) FROM order_items oi JOIN products p ON oi.product_id = p.product_id "
            + "WHERE oi.customer_id = c.customer_id) AS all_products\n"
            + "FROM customers c\n"
            + "WHERE c.is_active = 1 AND c.customer_id IN (\n"
            + "SELECT o1.customer_id FROM orders o1 WHERE YEAR(o1.order_date) = 2025\n"
            + "AND NOT EXISTS (SELECT 1 FROM customer_tags ct WHERE ct.customer_id = o1.customer_id AND ct.tag_name = 'VIP')\n"
            + "AND o1.order_amount > (SELECT AVG(o2.order_amount) FROM orders o2 "
            + "WHERE o2.state = (SELECT state FROM customers WHERE customer_id = o1.customer_id))\n"
            + "AND EXISTS (SELECT 1 FROM order_items oi2 WHERE oi2.order_id = o1.order_id "
            + "AND oi2.product_id IN (SELECT product_id FROM products WHERE category LIKE '%电子%')))\n"
            + "OR c.customer_id IN (SELECT customer_id FROM orders WHERE order_amount > 10000)\n"
            + "ORDER BY RAND() LIMIT 10";
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

    private byte[] buildLocalizedWorkbook() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("reports");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("报表代码");
        header.createCell(1).setCellValue("任意列A");
        header.createCell(2).setCellValue("空列B");
        header.createCell(3).setCellValue("任意列C");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("RPT_XLSX_CN");
        row.createCell(1).setCellValue("SELECT 10 AS metric_value");
        row.createCell(3).setCellValue("SELECT 30 AS metric_value");
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
