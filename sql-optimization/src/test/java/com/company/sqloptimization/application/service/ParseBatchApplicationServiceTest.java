package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchIngestRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchRetryAccessRequest;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataRequest;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataResponse;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlParseHistoryRepository;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParseBatchApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateReadyParseBatchWithTemplateContract() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        ParseBatchStatusResponse response = service.createBatch(baseRequest("TABULAR_FILE", "XLSX"));

        assertEquals("READY", response.getStatus());
        assertEquals("FILE_UPLOAD", response.getSourceType());
        assertEquals("JSQLPARSER", response.getParserMode());
        assertEquals(Integer.valueOf(3), Integer.valueOf(response.getStatusHistory().size()));
        assertEquals("sql_text", response.getTemplateColumns().get(8).getColumnKey());
    }

    @Test
    void shouldCreateAndIngestApacheCalciteParseBatch() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-005", "trace-005", "header", 1L, 2L);

        ParseBatchCreateRequest request = baseRequest("SQL_FILE", "SQL");
        request.setParserMode("APACHE_CALCITE");
        request.setStructureParseOnly(Boolean.TRUE);
        ParseBatchStatusResponse created = service.createBatch(request);

        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString(
            "SELECT customer_id, COUNT(*) FROM orders WHERE dt >= DATE '2026-04-01' GROUP BY customer_id LIMIT 10"
                .getBytes(StandardCharsets.UTF_8)
        ));
        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals("APACHE_CALCITE", ingested.getParserMode());
        assertEquals("COMPLETED", ingested.getStatus());
        assertEquals("VALID", ingested.getImportedRecords().get(0).getStructureSyntaxStatus());
    }

    @Test
    void shouldPersistPlanAnalysisStatusForWithPlanParseBatch() {
        ParseBatchApplicationService service = buildService(
            DatasourceViewMetadataClient.unavailable(),
            (sqlText, tenantId, datasourceCode) -> HetuPlanAnalysisResult.success(
                datasourceCode,
                "Fragment 0 [SINGLE]",
                5L,
                Collections.singletonList("sqlExecution=EXPLAIN_ONLY")
            )
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-007", "trace-007", "header", 1L, 2L);

        ParseBatchCreateRequest request = baseRequest("SQL_FILE", "SQL");
        request.setParserMode("JSQLPARSER_WITH_PLAN");
        request.setStructureParseOnly(Boolean.TRUE);
        ParseBatchStatusResponse created = service.createBatch(request);

        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString(
            "SELECT id FROM orders WHERE dt = DATE '2026-04-01'".getBytes(StandardCharsets.UTF_8)
        ));
        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals("COMPLETED", ingested.getStatus());
        assertEquals("SUCCESS", ingested.getImportedRecords().get(0).getPlanAnalysisStatus());
        assertEquals("SUCCESS", ingested.getImportedRecords().get(0).getAnalysisStatus());
        assertEquals(Integer.valueOf(1), ingested.getPlanAnalysisStatistics().getSuccessRecords());
    }

    @Test
    void shouldMarkParseBatchItemPartialWhenPlanFails() {
        ParseBatchApplicationService service = buildService(
            DatasourceViewMetadataClient.unavailable(),
            (sqlText, tenantId, datasourceCode) -> HetuPlanAnalysisResult.failed(
                datasourceCode,
                "HETU_JDBC_CONFIG_NOT_FOUND",
                4L,
                Collections.singletonList("datasourceCode=" + datasourceCode)
            )
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-008", "trace-008", "header", 1L, 2L);

        ParseBatchCreateRequest request = baseRequest("SQL_FILE", "SQL");
        request.setParserMode("JSQLPARSER_WITH_PLAN");
        request.setStructureParseOnly(Boolean.TRUE);
        ParseBatchStatusResponse created = service.createBatch(request);
        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString(
            "SELECT id FROM orders WHERE dt = DATE '2026-04-01'".getBytes(StandardCharsets.UTF_8)
        ));

        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals("PARTIAL_COMPLETED", ingested.getStatus());
        assertEquals("PARTIAL_SUCCESS", ingested.getImportedRecords().get(0).getStatus());
        assertEquals("FAILED", ingested.getImportedRecords().get(0).getPlanAnalysisStatus());
        assertEquals("PARTIAL_SUCCESS", ingested.getImportedRecords().get(0).getAnalysisStatus());
        assertEquals(Integer.valueOf(1), ingested.getPlanAnalysisStatistics().getFailedRecords());
    }

    @Test
    void shouldPersistFinalTableKeysForLiveViewDefinitionsInBatchParse() {
        DatasourceViewMetadataClient metadataClient = request -> {
            DatasourceViewMetadataRequest metadataRequest = request;
            if ("vw_sales_daily".equals(metadataRequest.getObjectName())) {
                return DatasourceViewMetadataResponse.view("SELECT * FROM sales.orders");
            }
            return DatasourceViewMetadataResponse.table();
        };
        ParseBatchApplicationService service = buildService(metadataClient);
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-006", "trace-006", "header", 1L, 2L);

        ParseBatchStatusResponse created = service.createBatch(baseRequest("SQL_FILE", "SQL"));
        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString(
            "SELECT * FROM vw_sales_daily".getBytes(StandardCharsets.UTF_8)
        ));

        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals("COMPLETED", ingested.getStatus());
        assertEquals(Collections.singletonList("TABLE:sales.orders"), ingested.getImportedRecords().get(0).getLogicalObjectKeys());
    }

    @Test
    void shouldRejectUnsupportedFileTypeForSqlFileMode() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BizException exception = assertThrows(BizException.class, () -> service.createBatch(baseRequest("SQL_FILE", "XLSX")));

        assertEquals(10001, exception.getCode());
    }

    @Test
    void shouldIngestSqlFileAndRetryPartialAccessRows() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        ParseBatchStatusResponse created = service.createBatch(baseRequest("SQL_FILE", "SQL"));

        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString((
            "SELECT * FROM orders WHERE dt = '2026-04-01';\n"
                + "SELECT * FROM vw_orders WHERE dt = '2026-04-02';"
        ).getBytes(StandardCharsets.UTF_8)));
        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals(Integer.valueOf(2), ingested.getTotalRecords());
        assertEquals("COMPLETED", ingested.getStatus());
        assertEquals(Integer.valueOf(2), ingested.getImportedRecords().size());
        assertEquals("VALID", ingested.getImportedRecords().get(0).getStructureSyntaxStatus());
        assertEquals("AVAILABLE", ingested.getImportedRecords().get(0).getAccessServiceStatus());

        ParseBatchRetryAccessRequest retryRequest = new ParseBatchRetryAccessRequest();
        retryRequest.setFailureFilter("ALL");
        ParseBatchStatusResponse retried = service.retryAccess(created.getBatchId(), retryRequest);

        assertEquals("COMPLETED", retried.getStatus());
        assertEquals(Integer.valueOf(2), retried.getSuccessRecords());
    }

    @Test
    void shouldSplitSqlFileWithoutBreakingSemicolonsInsideSqlText() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-002", "trace-002", "header", 1L, 2L);

        ParseBatchStatusResponse created = service.createBatch(baseRequest("SQL_FILE", "SQL"));
        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString((
            "SELECT ';' AS literal_value;\n"
                + "-- this comment contains ; and belongs to the next statement\n"
                + "SELECT * FROM orders WHERE note = 'a;b' AND dt = '2026-04-01';\n"
                + "/* trailing ; comment should not create an empty statement */"
        ).getBytes(StandardCharsets.UTF_8)));

        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals(Integer.valueOf(2), ingested.getTotalRecords());
        assertEquals(Integer.valueOf(2), ingested.getImportedRecords().size());
        assertEquals("VALID", ingested.getImportedRecords().get(0).getStructureSyntaxStatus());
        assertEquals("VALID", ingested.getImportedRecords().get(1).getStructureSyntaxStatus());
        assertEquals("SELECT ';' AS literal_value", ingested.getImportedRecords().get(0).getSqlText());
        assertEquals(Boolean.FALSE, ingested.getItemPreviewTruncated());
    }

    @Test
    void shouldExposeBatchSqlFailureLocationForInvalidSql() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-004", "trace-004", "header", 1L, 2L);

        ParseBatchStatusResponse created = service.createBatch(baseRequest("SQL_FILE", "SQL"));
        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString("SELECT FROM;".getBytes(StandardCharsets.UTF_8)));

        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals("FAILED", ingested.getStatus());
        assertEquals(Integer.valueOf(1), ingested.getFailedRecords());
        assertEquals("FAILED", ingested.getImportedRecords().get(0).getStatus());
        assertEquals("INVALID", ingested.getImportedRecords().get(0).getStructureSyntaxStatus());
        assertTrue(ingested.getImportedRecords().get(0).getFailureReason().contains("line=1"));
        assertTrue(ingested.getImportedRecords().get(0).getFailureReason().contains("col=8"));
        assertEquals(Integer.valueOf(1), ingested.getImportedRecords().get(0).getFailureLine());
        assertEquals(Integer.valueOf(8), ingested.getImportedRecords().get(0).getFailureColumn());
        assertEquals("FROM", ingested.getImportedRecords().get(0).getFailureToken());
        assertTrue(ingested.getImportedRecords().get(0).getFailureSnippet().contains("SELECT FROM"));
        assertTrue(ingested.getImportedRecords().get(0).getDiagnosticSummary().contains("sequence=1"));
        assertTrue(ingested.getImportedRecords().get(0).getDiagnosticSummary().contains("token=FROM"));
    }

    @Test
    void shouldKeepSingleSqlIssueCodesForComplexBatchSql() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-003", "trace-003", "header", 1L, 2L);

        ParseBatchStatusResponse created = service.createBatch(baseRequest("SQL_FILE", "SQL"));
        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString(
            (complexAntiPatternSql() + ";").getBytes(StandardCharsets.UTF_8)
        ));

        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals(Integer.valueOf(1), ingested.getTotalRecords());
        assertEquals("VALID", ingested.getImportedRecords().get(0).getStructureSyntaxStatus());
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().size() >= 10);
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("SCALAR_SUBQUERY_IN_SELECT"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("NESTED_SUBQUERY_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("CORRELATED_SUBQUERY_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("FUNCTION_WRAPPED_PREDICATE"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("NOT_EXISTS_ANTI_JOIN_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("LEADING_WILDCARD_LIKE_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("OR_PREDICATE_INDEX_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("ORDER_BY_RANDOM_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("REPEATED_TABLE_SCAN_RISK"));
        assertTrue(ingested.getImportedRecords().get(0).getIssueScenes().contains("COMPLEX_QUERY_GRAPH_RISK"));
        assertTrue(ingested.getIssueStatistics().stream()
            .anyMatch(item -> "ORDER_BY_RANDOM_RISK".equals(item.getIssueScene())));
    }

    private ParseBatchApplicationService buildService() {
        return buildService(DatasourceViewMetadataClient.unavailable());
    }

    private ParseBatchApplicationService buildService(DatasourceViewMetadataClient datasourceViewMetadataClient) {
        return buildService(datasourceViewMetadataClient, HetuPlanAnalysisClient.unavailable());
    }

    private ParseBatchApplicationService buildService(DatasourceViewMetadataClient datasourceViewMetadataClient,
                                                      HetuPlanAnalysisClient hetuPlanAnalysisClient) {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        SqlParseHistoryApplicationService sqlParseHistoryApplicationService =
            new SqlParseHistoryApplicationService(new InMemorySqlParseHistoryRepository());
        StructureParseApplicationService structureService = new StructureParseApplicationService(
            new SqlOptimizationPipelineService(),
            governanceCapabilityClient,
            sqlParseHistoryApplicationService,
            datasourceViewMetadataClient,
            hetuPlanAnalysisClient
        );
        AccessParseApplicationService accessService = new AccessParseApplicationService();
        return new ParseBatchApplicationService(
            new InMemoryParseBatchRepository(),
            new InMemoryParseBatchItemRepository(),
            structureService,
            accessService
        );
    }

    private ParseBatchCreateRequest baseRequest(String importMode, String fileType) {
        ParseBatchCreateRequest request = new ParseBatchCreateRequest();
        request.setTenantId("tenant-a");
        request.setBatchName("batch-alpha");
        request.setImportMode(importMode);
        request.setFileType(fileType);
        request.setTemplateVersion("v1");
        request.setDatasourceCode("hetu_main");
        request.setStructureParseOnly(Boolean.FALSE);
        return request;
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
}
