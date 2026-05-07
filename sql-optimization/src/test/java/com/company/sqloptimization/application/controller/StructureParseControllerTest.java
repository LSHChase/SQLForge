package com.company.sqloptimization.application.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.governance.GovernanceDbViewDependencyRef;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteResponse;
import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SqlOptimizationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructureParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @Test
    void shouldReturnStructureParseResultForValidReadSql() throws Exception {
        GovernanceDbViewDependencyRef dependency = new GovernanceDbViewDependencyRef();
        dependency.setObjectType("TABLE");
        dependency.setObjectKey("TABLE:sales.orders");
        dependency.setObjectName("sales.orders");
        GovernanceDbViewResolveResponse resolveResponse = new GovernanceDbViewResolveResponse();
        resolveResponse.setResolved(Boolean.TRUE);
        resolveResponse.setObjectKey("DB_VIEW:vw_sales_daily");
        resolveResponse.setDependencies(Collections.singletonList(dependency));
        when(governanceCapabilityClient.resolveDbView(any())).thenReturn(resolveResponse);
        GovernanceParseHistoryWriteResponse historyResponse = new GovernanceParseHistoryWriteResponse();
        historyResponse.setHistoryId("history-parse-001");
        historyResponse.setResultId("result-parse-001");
        when(governanceCapabilityClient.writeParseHistory(any())).thenReturn(historyResponse);
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id\","
                    + "\"datasourceCode\":\"hetu_main\",\"bindingMode\":\"POSITIONAL\","
                    + "\"commentContext\":{\"report_code\":\"RPT_SALES_DAILY\",\"stage\":\"PROD\"}}"))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andExpect(jsonPath("$.parseType").value("STRUCTURE"))
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStart").value("2026-04-01"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStatus").value("RESOLVED"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectType").value("DB_VIEW"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("DB_VIEW:vw_sales_daily"))
            .andExpect(jsonPath("$.logicalObjectHits[0].resolved").value(true))
            .andExpect(jsonPath("$.logicalObjectHits[0].mappedPhysicalTargets[0]").value("TABLE:sales.orders"))
            .andExpect(jsonPath("$.sqlFingerprint").isNotEmpty())
            .andExpect(jsonPath("$.intentProfile.scanMode").value("PARTITION_RANGE_SCAN"))
            .andExpect(jsonPath("$.intentProfile.computeDensity").value("LIGHT"))
            .andExpect(jsonPath("$.featureSummary.parserEngine").value("JSQLPARSER"))
            .andExpect(jsonPath("$.estimatedResourceCost.overall").value("HIGH"))
            .andExpect(jsonPath("$.riskChecklist[0].riskCode").value("LARGE_RESULT_SET_RISK"))
            .andExpect(jsonPath("$.riskTags[0]").value("SELECT_STAR"))
            .andExpect(jsonPath("$.rewriteCandidates[0]").value("DEDUPLICATE_WHERE_PREDICATES"))
            .andExpect(jsonPath("$.issues[0].issueCode").value("SELECT_STAR"))
            .andExpect(jsonPath("$.priorityLevel").value("P1"))
            .andExpect(jsonPath("$.historyId").value("history-parse-001"))
            .andExpect(jsonPath("$.historyPersisted").value(true))
            .andExpect(jsonPath("$.historyPersistenceStatus").value("SAVED"));
        verify(governanceCapabilityClient).writeParseHistory(any());
        verify(governanceCapabilityClient).resolveDbView(any());
    }

    @Test
    void shouldReturnInvalidStructureParseInsteadOfFailingHard() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT FROM\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parseType").value("STRUCTURE"))
            .andExpect(jsonPath("$.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.sqlType").value("UNKNOWN"))
            .andExpect(jsonPath("$.failureReason").isNotEmpty())
            .andExpect(jsonPath("$.failureLine").value(1))
            .andExpect(jsonPath("$.failureColumn").value(8))
            .andExpect(jsonPath("$.failureToken").value("FROM"))
            .andExpect(jsonPath("$.issues[0].issueCode").value("SQL_SYNTAX_INVALID"))
            .andExpect(jsonPath("$.issues[0].issueDomain").value("STRUCTURE"))
            .andExpect(jsonPath("$.issues[0].failureLine").value(1))
            .andExpect(jsonPath("$.issues[0].failureColumn").value(8))
            .andExpect(jsonPath("$.issues[0].failureToken").value("FROM"))
            .andExpect(jsonPath("$.issues[0].failureSnippet").isNotEmpty())
            .andExpect(jsonPath("$.intentProfile.confidence").value("LOW"))
            .andExpect(jsonPath("$.estimatedResourceCost.overall").value("UNKNOWN"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStatus").value("UNRESOLVED"))
            .andExpect(jsonPath("$.rewriteCandidates").isEmpty());
    }

    @Test
    void shouldParseStructureWithApacheCalciteWhenRequested() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"APACHE_CALCITE\",\"sqlText\":\"WITH recent_orders AS ("
                    + "SELECT customer_id, amount, dt FROM hive.sales.orders WHERE dt >= DATE '2026-04-01') "
                    + "SELECT customer_id, SUM(amount) AS total_amount FROM recent_orders "
                    + "WHERE dt <= DATE '2026-04-30' GROUP BY customer_id LIMIT 10\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.featureSummary.parserEngine").value("APACHE_CALCITE"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.featureSummary.predicateCount").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.rewriteCandidates").isEmpty());
    }

    @Test
    void shouldPropagateApacheCalciteIntoCombinedStructureParse() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/combined"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"APACHE_CALCITE\",\"sqlText\":\"SELECT customer_id, COUNT(*) "
                    + "FROM orders WHERE dt >= DATE '2026-04-01' GROUP BY customer_id LIMIT 20\","
                    + "\"datasourceCode\":\"hetu_main\",\"connectionRequired\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.structureParse.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.structureParse.featureSummary.parserEngine").value("APACHE_CALCITE"));
    }

    @Test
    void shouldRejectUnsupportedParserModeAsValidationError() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"TRINO\",\"sqlText\":\"SELECT * FROM orders\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("parserMode must be JSQLPARSER or APACHE_CALCITE"));
    }

    @Test
    void shouldExposeHistoryWriteFailureWithoutFailingStructureParse() throws Exception {
        when(governanceCapabilityClient.writeParseHistory(any())).thenThrow(new RuntimeException("route down"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT id FROM orders WHERE dt = '2026-04-01'\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.historyPersisted").value(false))
            .andExpect(jsonPath("$.historyPersistenceStatus").value("WRITE_FAILED"));
    }

    @Test
    void shouldParseSqlWithDashLineCommentsInsideStatement() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":" + JsonTestUtils.toJsonString(
                    "--report_code=RPT_COMMENTED\n"
                        + "SELECT customer_id, '--not-a-comment' AS marker FROM orders -- table comment\n"
                        + "WHERE dt = DATE '2026-04-01' -- date filter\n"
                        + "AND status = 'PAID'"
                ) + ",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectName").value("orders"))
            .andExpect(jsonPath("$.featureSummary.predicateCount").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void shouldReturnStableFingerprintForCommentedAndParameterizedEquivalentSql() throws Exception {
        MvcResult first = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"--report_code=RPT_A\\nSELECT * FROM orders "
                    + "WHERE dt = DATE '2026-04-01' AND tenant_id = 7\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andReturn();
        MvcResult second = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"/* imported */ select * from orders "
                    + "where dt = DATE '2026-04-02' and tenant_id = 8;\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andReturn();

        assertEquals(
            JsonTestUtils.readValue(first.getResponse().getContentAsString(), "$.sqlFingerprint"),
            JsonTestUtils.readValue(second.getResponse().getContentAsString(), "$.sqlFingerprint")
        );
    }

    @Test
    void shouldExposeComplexAntiPatternStructureSignals() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":" + JsonTestUtils.toJsonString(complexAntiPatternSql()) + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.complexityLevel").value("EXTREME"))
            .andExpect(jsonPath("$.intentProfile.computeDensity").value("HEAVY"))
            .andExpect(jsonPath("$.intentProfile.slaLevel").value("REPORT_LT_30S"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(5))
            .andExpect(jsonPath("$.featureSummary.subqueryCount").value(greaterThanOrEqualTo(9)))
            .andExpect(jsonPath("$.featureSummary.scalarSubqueryCount").value(3))
            .andExpect(jsonPath("$.featureSummary.nestedSubqueryDepth").value(greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.featureSummary.correlatedSubqueryCount").value(greaterThanOrEqualTo(6)))
            .andExpect(jsonPath("$.featureSummary.orPredicateCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.featureSummary.functionWrappedPredicateCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.featureSummary.leadingWildcardLikeCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.featureSummary.randomOrderCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.featureSummary.repeatedTableScanCount").value(greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.estimatedResourceCost.overall").value("HIGH"))
            .andExpect(jsonPath("$.riskTags").value(hasItem("SCALAR_SUBQUERY_IN_SELECT")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("NESTED_SUBQUERY_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("CORRELATED_SUBQUERY_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("FUNCTION_WRAPPED_PREDICATE")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("NOT_EXISTS_ANTI_JOIN_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("LEADING_WILDCARD_LIKE_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("ORDER_BY_RANDOM_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("REPEATED_TABLE_SCAN_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("COMPLEX_QUERY_GRAPH_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("SCALAR_SUBQUERY_IN_SELECT")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("ORDER_BY_RANDOM_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("COMPLEX_QUERY_GRAPH_RISK")))
            .andExpect(jsonPath("$.priorityLevel").value("P1"));
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

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-parse-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-parse-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ACCESS_CHANNEL, "api")
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
