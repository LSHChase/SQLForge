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
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchRepository;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
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
        assertEquals(Integer.valueOf(3), Integer.valueOf(response.getStatusHistory().size()));
        assertEquals("sql_text", response.getTemplateColumns().get(8).getColumnKey());
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
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        StructureParseApplicationService structureService = new StructureParseApplicationService(
            new SqlOptimizationPipelineService(),
            governanceCapabilityClient
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
