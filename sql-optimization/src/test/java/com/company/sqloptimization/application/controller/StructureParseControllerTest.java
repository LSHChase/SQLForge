package com.company.sqloptimization.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.governance.GovernanceDbViewDependencyRef;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqloptimization.SqlOptimizationTestPersistenceConfiguration;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataRequest;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataResponse;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {SqlOptimizationApplication.class, SqlOptimizationTestPersistenceConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructureParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @MockBean
    private DatasourceViewMetadataClient datasourceViewMetadataClient;

    @MockBean
    private HetuPlanAnalysisClient hetuPlanAnalysisClient;

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
        MvcResult parseResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
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
            .andExpect(jsonPath("$.historyId").value(startsWith("parse-history-")))
            .andExpect(jsonPath("$.historyPersisted").value(true))
            .andExpect(jsonPath("$.historyPersistenceStatus").value("SAVED"))
            .andReturn();
        String historyId = JsonTestUtils.readValue(parseResult.getResponse().getContentAsString(), "$.historyId");
        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse-history/{historyId}", historyId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parseHistoryId").value(historyId))
            .andExpect(jsonPath("$.sourceType").value("STRUCTURE_PARSE"))
            .andExpect(jsonPath("$.historyType").value("SQL_PARSE_RECORD"));
        verify(governanceCapabilityClient).resolveDbView(any());
    }

    @Test
    void shouldExpandLiveDatabaseViewDefinitionToLeafTables() throws Exception {
        when(datasourceViewMetadataClient.resolveView(any())).thenAnswer(invocation -> {
            DatasourceViewMetadataRequest request = invocation.getArgument(0);
            if ("vw_sales_daily".equals(request.getObjectName())) {
                return DatasourceViewMetadataResponse.view(
                    "CREATE VIEW vw_sales_daily AS SELECT order_id, dt FROM sales.orders WHERE dt >= DATE '2026-04-01'"
                );
            }
            return DatasourceViewMetadataResponse.table();
        });

        MvcResult parseResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT order_id FROM vw_sales_daily WHERE dt = DATE '2026-04-01'\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectType").value("DB_VIEW"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("DB_VIEW:vw_sales_daily"))
            .andExpect(jsonPath("$.logicalObjectHits[0].matchSource").value("LIVE_DB_VIEW_METADATA"))
            .andExpect(jsonPath("$.logicalObjectHits[0].resolved").value(true))
            .andExpect(jsonPath("$.logicalObjectHits[0].mappedPhysicalTargets[0]").value("TABLE:sales.orders"))
            .andExpect(jsonPath("$.logicalObjectHits[1].objectType").value("TABLE"))
            .andExpect(jsonPath("$.logicalObjectHits[1].objectKey").value("TABLE:sales.orders"))
            .andExpect(jsonPath("$.logicalObjectHits[1].matchSource").value("DB_VIEW_DEFINITION"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1))
            .andExpect(jsonPath("$.riskTags").value(not(hasItem("DB_VIEW_DEFINITION_UNRESOLVED"))))
            .andReturn();

        String historyId = JsonTestUtils.readValue(parseResult.getResponse().getContentAsString(), "$.historyId");
        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse-history/{historyId}", historyId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logicalObjectKeys[0]").value("TABLE:sales.orders"));
        verify(governanceCapabilityClient, never()).resolveDbView(any());
    }

    @Test
    void shouldRecursivelyExpandNestedLiveDatabaseViews() throws Exception {
        when(datasourceViewMetadataClient.resolveView(any())).thenAnswer(invocation -> {
            DatasourceViewMetadataRequest request = invocation.getArgument(0);
            if ("vw_outer".equals(request.getObjectName())) {
                return DatasourceViewMetadataResponse.view("SELECT * FROM vw_inner");
            }
            if ("vw_inner".equals(request.getObjectName())) {
                return DatasourceViewMetadataResponse.view("SELECT * FROM warehouse.fact_orders");
            }
            return DatasourceViewMetadataResponse.table();
        });

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM vw_outer\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("DB_VIEW:vw_outer"))
            .andExpect(jsonPath("$.logicalObjectHits[0].mappedPhysicalTargets[0]").value("TABLE:warehouse.fact_orders"))
            .andExpect(jsonPath("$.logicalObjectHits[1].objectKey").value("DB_VIEW:vw_inner"))
            .andExpect(jsonPath("$.logicalObjectHits[1].mappedPhysicalTargets[0]").value("TABLE:warehouse.fact_orders"))
            .andExpect(jsonPath("$.logicalObjectHits[2].objectKey").value("TABLE:warehouse.fact_orders"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1));
        verify(governanceCapabilityClient, never()).resolveDbView(any());
    }

    @Test
    void shouldDegradeOnCircularLiveDatabaseViewDefinition() throws Exception {
        when(datasourceViewMetadataClient.resolveView(any())).thenReturn(
            DatasourceViewMetadataResponse.view("SELECT * FROM vw_loop")
        );

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM vw_loop\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("DB_VIEW:vw_loop"))
            .andExpect(jsonPath("$.logicalObjectHits[0].resolved").value(false))
            .andExpect(jsonPath("$.riskTags").value(hasItem("DB_VIEW_DEFINITION_UNRESOLVED")))
            .andExpect(jsonPath("$.issues[*].issueCode").value(hasItem("DB_VIEW_DEFINITION_UNRESOLVED")));
    }

    @Test
    void shouldReturnInvalidStructureParseInsteadOfFailingHard() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT FROM orders WHERE dt = '2026-05-08'\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parseType").value("STRUCTURE"))
            .andExpect(jsonPath("$.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.failureReason").isNotEmpty())
            .andExpect(jsonPath("$.failureLine").value(1))
            .andExpect(jsonPath("$.failureColumn").value(8))
            .andExpect(jsonPath("$.failureToken").value("FROM"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("TABLE:orders"))
            .andExpect(jsonPath("$.logicalObjectHits[0].matchSource").value("HEURISTIC_FALLBACK"))
            .andExpect(jsonPath("$.riskTags").value(hasItem("SQL_SYNTAX_INVALID")))
            .andExpect(jsonPath("$.issues[0].issueCode").value("SQL_SYNTAX_INVALID"))
            .andExpect(jsonPath("$.issues[0].issueDomain").value("STRUCTURE"))
            .andExpect(jsonPath("$.issues[0].failureLine").value(1))
            .andExpect(jsonPath("$.issues[0].failureColumn").value(8))
            .andExpect(jsonPath("$.issues[0].failureToken").value("FROM"))
            .andExpect(jsonPath("$.issues[0].failureSnippet").isNotEmpty())
            .andExpect(jsonPath("$.intentProfile.confidence").value("LOW"))
            .andExpect(jsonPath("$.featureSummary.parserEngine").value("HEURISTIC_FALLBACK"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1))
            .andExpect(jsonPath("$.featureSummary.predicateCount").value(1))
            .andExpect(jsonPath("$.estimatedResourceCost.overall").value("UNKNOWN"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStart").value("2026-05-08"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStatus").value("RESOLVED"))
            .andExpect(jsonPath("$.rewriteCandidates").isEmpty());
    }

    @Test
    void shouldReturnHeuristicJoinEvidenceForIncompleteInvalidSql() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM orders o JOIN customers c ON o.customer_id =\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.logicalObjectHits[*].objectKey").value(hasItem("TABLE:orders")))
            .andExpect(jsonPath("$.logicalObjectHits[*].objectKey").value(hasItem("TABLE:customers")))
            .andExpect(jsonPath("$.featureSummary.parserEngine").value("HEURISTIC_FALLBACK"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(2))
            .andExpect(jsonPath("$.featureSummary.joinCount").value(1))
            .andExpect(jsonPath("$.riskTags").value(hasItem("SQL_SYNTAX_INVALID")));
    }

    @Test
    void shouldIgnoreDiagnosticNoiseInInvalidSqlFallbackScan() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"@@@@ 中文 SELECT * FROM orders o "
                    + "JOIN customers c ON o.customer_id = c.id "
                    + "WHERE o.dt = DATE '2026-05-08'\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.failureToken").value("SQL_SYNTAX_INVALID"))
            .andExpect(jsonPath("$.failureReason").value(not(containsString("@"))))
            .andExpect(jsonPath("$.failureReason").value(not(containsString("中文"))))
            .andExpect(jsonPath("$.failureSnippet").value(not(containsString("@"))))
            .andExpect(jsonPath("$.failureSnippet").value(not(containsString("中文"))))
            .andExpect(jsonPath("$.logicalObjectHits[*].objectKey").value(hasItem("TABLE:orders")))
            .andExpect(jsonPath("$.logicalObjectHits[*].objectKey").value(hasItem("TABLE:customers")))
            .andExpect(jsonPath("$.featureSummary.parserEngine").value("HEURISTIC_FALLBACK"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(2))
            .andExpect(jsonPath("$.featureSummary.joinCount").value(1))
            .andExpect(jsonPath("$.featureSummary.predicateCount").value(1))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStart").value("2026-05-08"))
            .andExpect(jsonPath("$.issues[0].detail").value(not(containsString("@"))))
            .andExpect(jsonPath("$.issues[0].detail").value(not(containsString("中文"))));
    }

    @Test
    void shouldReturnBoundedDiagnosticsForSqlOverDefaultLimit() throws Exception {
        String sqlText = overlongSql();
        MvcResult result = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":" + JsonTestUtils.toJsonString(sqlText) + ",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.failureReason").value("SQL 过长；语法解析器已失败或为限制诊断范围而跳过。"))
            .andExpect(jsonPath("$.failureToken").value("SQL_TOO_LONG"))
            .andExpect(jsonPath("$.riskTags").value(hasItem("SQL_SYNTAX_INVALID")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("SQL_TOO_LONG")))
            .andExpect(jsonPath("$.issues[*].issueCode").value(hasItem("SQL_TOO_LONG")))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("TABLE:orders"))
            .andExpect(jsonPath("$.featureSummary.parserEngine").value("HEURISTIC_FALLBACK"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1))
            .andExpect(jsonPath("$.featureSummary.predicateCount").value(1))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStart").value("2026-05-08"))
            .andReturn();

        String failureSnippet = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.failureSnippet");
        String failureDetail = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString())
            .get("issues")
            .get(1)
            .get("detail")
            .asText();
        assertTrue(failureSnippet.length() <= 120);
        assertTrue(failureDetail.length() <= 512);
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
    void shouldExposeAdvancedStructureProfileForSingleTableAggregation() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT o.customer_id, SUM(o.amount) AS total_amount "
                    + "FROM orders o WHERE o.dt >= DATE '2026-04-01' "
                    + "GROUP BY o.customer_id ORDER BY total_amount DESC LIMIT 20\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.advancedStructureProfile.profileStatus").value("AVAILABLE"))
            .andExpect(jsonPath("$.advancedStructureProfile.tables[0].tableName").value("orders"))
            .andExpect(jsonPath("$.advancedStructureProfile.tables[0].alias").value("o"))
            .andExpect(jsonPath("$.advancedStructureProfile.projections[1].alias").value("total_amount"))
            .andExpect(jsonPath("$.advancedStructureProfile.predicates[0].clause").value("WHERE"))
            .andExpect(jsonPath("$.advancedStructureProfile.predicates[0].expression").value(containsString("o.dt")))
            .andExpect(jsonPath("$.advancedStructureProfile.aggregations[0].functionName").value("SUM"))
            .andExpect(jsonPath("$.advancedStructureProfile.groupBy[0].expression").value("o.customer_id"))
            .andExpect(jsonPath("$.advancedStructureProfile.orderBy[0].direction").value("DESC"))
            .andExpect(jsonPath("$.advancedStructureProfile.limit.present").value(true))
            .andExpect(jsonPath("$.advancedStructureProfile.limit.rowCount").value("20"));
    }

    @Test
    void shouldExposeAdvancedStructureProfileForJoinCteSubqueryAndTimeFunction() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"WITH recent_orders AS ("
                    + "SELECT order_id, customer_id, amount, order_date FROM orders o "
                    + "WHERE o.order_date >= DATE '2026-04-01') "
                    + "SELECT c.region, DATE_TRUNC('day', r.order_date) AS order_day, "
                    + "SUM(r.amount) AS total_amount FROM recent_orders r "
                    + "JOIN customers c ON r.customer_id = c.customer_id "
                    + "WHERE c.status = 'ACTIVE' AND r.amount > "
                    + "(SELECT AVG(amount) FROM orders WHERE status = 'PAID') "
                    + "GROUP BY c.region, DATE_TRUNC('day', r.order_date) "
                    + "ORDER BY order_day LIMIT 10\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.advancedStructureProfile.ctes[0].name").value("recent_orders"))
            .andExpect(jsonPath("$.advancedStructureProfile.tables[*].sourceType").value(hasItem("CTE_REFERENCE")))
            .andExpect(jsonPath("$.advancedStructureProfile.tables[*].alias").value(hasItem("c")))
            .andExpect(jsonPath("$.advancedStructureProfile.joinGraph[0].right").value("customers"))
            .andExpect(jsonPath("$.advancedStructureProfile.joinGraph[0].condition")
                .value(containsString("r.customer_id = c.customer_id")))
            .andExpect(jsonPath("$.advancedStructureProfile.subqueries[*].location").value(hasItem("EXPRESSION")))
            .andExpect(jsonPath("$.advancedStructureProfile.aggregations[*].functionName").value(hasItem("SUM")))
            .andExpect(jsonPath("$.advancedStructureProfile.aggregations[*].functionName").value(hasItem("AVG")))
            .andExpect(jsonPath("$.advancedStructureProfile.timeFunctions[*].functionName").value(hasItem("DATE_TRUNC")));
    }

    @Test
    void shouldExposeAdvancedStructureProfileForNonDeterministicFunctions() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT order_id, CURRENT_TIMESTAMP AS parsed_at "
                    + "FROM orders ORDER BY RAND() LIMIT 5\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.advancedStructureProfile.timeFunctions[*].functionName")
                .value(hasItem("CURRENT_TIMESTAMP")))
            .andExpect(jsonPath("$.advancedStructureProfile.nonDeterministicFunctions[*].functionName")
                .value(hasItem("CURRENT_TIMESTAMP")))
            .andExpect(jsonPath("$.advancedStructureProfile.nonDeterministicFunctions[*].functionName")
                .value(hasItem("RAND")))
            .andExpect(jsonPath("$.featureSummary.randomOrderCount").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldRunHetuExplainPlanWhenPlanParserModeRequested() throws Exception {
        when(hetuPlanAnalysisClient.explain(any(), any(), any(), any())).thenReturn(
            HetuPlanAnalysisResult.success(
                "hetu_main",
                "Fragment 0 [SINGLE]\nOutput[_col0]",
                7L,
                Collections.singletonList("sqlExecution=EXPLAIN_ONLY")
            )
        );

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"JSQLPARSER_WITH_PLAN\",\"sqlText\":\"SELECT * FROM orders WHERE dt = DATE '2026-04-01'\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.analysisStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.structureAnalysisStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.planAnalysis.status").value("SUCCESS"))
            .andExpect(jsonPath("$.planAnalysis.planText").value(org.hamcrest.Matchers.containsString("Fragment 0")))
            .andExpect(jsonPath("$.planAnalysis.evidence[0]").value("sqlExecution=EXPLAIN_ONLY"));

        verify(hetuPlanAnalysisClient).explain(any(), any(), any(), any());
    }

    @Test
    void shouldReturnPartialSuccessWhenHetuPlanFailsAfterStructureSuccess() throws Exception {
        when(hetuPlanAnalysisClient.explain(any(), any(), any(), any())).thenReturn(
            HetuPlanAnalysisResult.failed(
                "hetu_main",
                "HETU_JDBC_CONFIG_NOT_FOUND",
                3L,
                Collections.singletonList("datasourceCode=hetu_main")
            )
        );

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"APACHE_CALCITE_WITH_PLAN\",\"sqlText\":\"SELECT id FROM orders WHERE dt = DATE '2026-04-01'\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.analysisStatus").value("PARTIAL_SUCCESS"))
            .andExpect(jsonPath("$.planAnalysis.status").value("FAILED"))
            .andExpect(jsonPath("$.planAnalysis.failureReason").value("HETU_JDBC_CONFIG_NOT_FOUND"));
    }

    @Test
    void shouldSkipHetuPlanForLocalOnlyParserModes() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"JSQLPARSER\",\"sqlText\":\"SELECT id FROM orders WHERE dt = DATE '2026-04-01'\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.analysisStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.planAnalysis.status").value("SKIPPED"));

        verify(hetuPlanAnalysisClient, never()).explain(any(), any(), any(), any());
    }

    @Test
    void shouldPropagateApacheCalciteIntoCombinedStructureParse() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/combined"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"APACHE_CALCITE\",\"sqlText\":\"SELECT customer_id, COUNT(*) "
                    + "FROM orders WHERE dt >= DATE '2026-04-01' GROUP BY customer_id LIMIT 20\","
                    + "\"datasourceCode\":\"hetu_main\",\"connectionRequired\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.structureParse.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.structureParse.featureSummary.parserEngine").value("APACHE_CALCITE"))
            .andReturn();

        String parseTaskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.parseTaskId");
        waitForCombinedStatus(parseTaskId, "PARTIAL_SUCCEEDED");
    }

    @Test
    void shouldExposeHeuristicFallbackEvidenceInCombinedInvalidParse() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/combined"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT FROM orders WHERE dt = '2026-05-08'\","
                    + "\"datasourceCode\":\"hetu_main\",\"connectionRequired\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.degradeReason").value("STRUCTURE_PARSE_INVALID"))
            .andExpect(jsonPath("$.structureParse.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.structureParse.featureSummary.parserEngine").value("HEURISTIC_FALLBACK"))
            .andExpect(jsonPath("$.structureParse.logicalObjectHits[*].objectKey").value(hasItem("TABLE:orders")))
            .andExpect(jsonPath("$.structureParse.riskTags").value(hasItem("SQL_SYNTAX_INVALID")));
    }

    @Test
    void shouldRejectUnsupportedParserModeAsValidationError() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parserMode\":\"TRINO\",\"sqlText\":\"SELECT * FROM orders\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("parserMode 必须为 JSQLPARSER、APACHE_CALCITE、JSQLPARSER_WITH_PLAN 或 APACHE_CALCITE_WITH_PLAN"));
    }

    @Test
    void shouldPersistStructureParseHistoryWithoutGovernanceQueryHistoryRoute() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT id FROM orders WHERE dt = '2026-04-01'\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.historyId").value(startsWith("parse-history-")))
            .andExpect(jsonPath("$.historyPersisted").value(true))
            .andExpect(jsonPath("$.historyPersistenceStatus").value("SAVED"));
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
    void shouldNotFlagRepeatedRisksForTablePrefixedColumnNames() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT orders_status, orders_amount, orders_count "
                    + "FROM orders "
                    + "WHERE dt = DATE '2026-04-01' "
                    + "GROUP BY orders_status, orders_amount, orders_count "
                    + "ORDER BY orders_status "
                    + "LIMIT 50\","
                    + "\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1))
            .andExpect(jsonPath("$.featureSummary.repeatedTableScanCount").value(0))
            .andExpect(jsonPath("$.featureSummary.repeatedExpressionCount").value(0))
            .andExpect(jsonPath("$.riskTags").value(not(hasItem("REPEATED_TABLE_SCAN_RISK"))))
            .andExpect(jsonPath("$.riskTags").value(not(hasItem("REPEATED_EXPRESSION_COMPUTE"))))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(not(hasItem("REPEATED_TABLE_SCAN_RISK"))))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(not(hasItem("REPEATED_EXPRESSION_RISK"))));
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
    void shouldNotEscalateSimpleGroupedLookupAsHeavyOrComplex() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT customer_id, COUNT(*) FROM orders "
                    + "WHERE dt >= DATE '2026-04-01' GROUP BY customer_id LIMIT 20\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.complexityLevel").value("MODERATE"))
            .andExpect(jsonPath("$.intentProfile.computeDensity").value("MODERATE"))
            .andExpect(jsonPath("$.intentProfile.slaLevel").value("INTERACTIVE_LT_3S"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1))
            .andExpect(jsonPath("$.featureSummary.joinCount").value(0))
            .andExpect(jsonPath("$.featureSummary.subqueryCount").value(0))
            .andExpect(jsonPath("$.riskTags").value(not(hasItem("COMPLEX_QUERY_GRAPH_RISK"))))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(not(hasItem("COMPLEX_QUERY_GRAPH_RISK"))))
            .andExpect(jsonPath("$.priorityLevel").value("P4"));
    }

    @Test
    void shouldKeepFilteredLimitedMultiKeyGroupedReportNonHeavy() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT region, customer_id, status, COUNT(*), SUM(amount) "
                    + "FROM orders WHERE dt >= DATE '2026-04-01' AND tenant_id = 7 "
                    + "GROUP BY region, customer_id, status ORDER BY region LIMIT 50\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.complexityLevel").value("MODERATE"))
            .andExpect(jsonPath("$.intentProfile.computeDensity").value("MODERATE"))
            .andExpect(jsonPath("$.intentProfile.slaLevel").value("INTERACTIVE_LT_3S"))
            .andExpect(jsonPath("$.featureSummary.tableCount").value(1))
            .andExpect(jsonPath("$.featureSummary.joinCount").value(0))
            .andExpect(jsonPath("$.featureSummary.subqueryCount").value(0))
            .andExpect(jsonPath("$.riskTags").value(not(hasItem("COMPLEX_QUERY_GRAPH_RISK"))))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(not(hasItem("COMPLEX_QUERY_GRAPH_RISK"))));
    }

    @Test
    void shouldDescribeLargeJoinPairRiskAsStaticEvidenceRisk() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT o.id FROM orders o "
                    + "JOIN customers c ON o.customer_id = c.id "
                    + "JOIN regions r ON c.region_id = r.id "
                    + "WHERE o.dt = DATE '2026-05-08' LIMIT 20\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.riskTags").value(hasItem("LARGE_JOIN_PAIR_RISK")))
            .andExpect(jsonPath("$.issues[?(@.issueCode == 'LARGE_JOIN_PAIR_RISK')].summary")
                .value(hasItem("该语句关于 join 条件或选择性的静态证据不足。")))
            .andExpect(jsonPath("$.issues[?(@.issueCode == 'LARGE_JOIN_PAIR_RISK')].detail")
                .value(hasItem(not(containsString("large table")))))
            .andExpect(jsonPath("$.issues[?(@.issueCode == 'LARGE_JOIN_PAIR_RISK')].detail")
                .value(hasItem(not(containsString("large-table")))))
            .andExpect(jsonPath("$.issues[?(@.issueCode == 'LARGE_JOIN_PAIR_RISK')].suggestedAction")
                .value(hasItem(containsString("访问解析或压测证据"))))
            .andExpect(jsonPath("$.riskChecklist[?(@.riskCode == 'LARGE_TABLE_JOIN_RISK')].summary")
                .value(hasItem("静态 join 证据风险")))
            .andExpect(jsonPath("$.riskChecklist[?(@.riskCode == 'LARGE_TABLE_JOIN_RISK')].suggestedAction")
                .value(hasItem(containsString("访问解析或压测证据"))))
            .andExpect(jsonPath("$.riskChecklist[?(@.riskCode == 'LARGE_TABLE_JOIN_RISK')].summary")
                .value(hasItem(not(containsString("Large table")))));
    }

    @Test
    void shouldExposeOrderGroupRedundancyStaticRiskSignals() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT status FROM orders "
                    + "WHERE dt = DATE '2026-04-01' "
                    + "GROUP BY status, status "
                    + "ORDER BY status, status, customer_id\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.featureSummary.orderByExpressionCount").value(3))
            .andExpect(jsonPath("$.featureSummary.duplicateOrderByKeyCount").value(1))
            .andExpect(jsonPath("$.featureSummary.duplicateGroupByKeyCount").value(1))
            .andExpect(jsonPath("$.featureSummary.groupByWithoutAggregate").value(true))
            .andExpect(jsonPath("$.featureSummary.evidence").value(hasItem("staticOnly=true")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("ORDER_BY_COMPLEXITY_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("GROUP_BY_WITHOUT_AGGREGATE_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("DUPLICATE_GROUP_OR_ORDER_KEY_RISK")))
            .andExpect(jsonPath("$.issues[*].issueCode").value(hasItem("ORDER_BY_COMPLEXITY_RISK")))
            .andExpect(jsonPath("$.issues[*].issueCode").value(hasItem("GROUP_BY_WITHOUT_AGGREGATE_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("ORDER_BY_COMPLEXITY_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("DUPLICATE_GROUP_OR_ORDER_KEY_RISK")));
    }

    @Test
    void shouldExposeStringAggregationAndRepeatedSubqueryStaticRiskSignals() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT c.customer_id, "
                    + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_a, "
                    + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_b, "
                    + "GROUP_CONCAT(CONCAT(c.customer_name, ':', c.status)) AS customer_labels, "
                    + "COUNT(*), SUM(c.amount), AVG(c.amount) "
                    + "FROM customers c WHERE c.dt = DATE '2026-04-01' "
                    + "GROUP BY c.customer_id ORDER BY c.customer_id\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.featureSummary.aggregateFunctionCount").value(greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.featureSummary.stringConcatenationCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.featureSummary.largeStringAggregateCount").value(1))
            .andExpect(jsonPath("$.featureSummary.repeatedSubqueryCount").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.estimatedResourceCost.resultSize").value("HIGH"))
            .andExpect(jsonPath("$.riskTags").value(hasItem("AGGREGATION_COMPLEXITY_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("LARGE_STRING_RESULT_RISK")))
            .andExpect(jsonPath("$.riskTags").value(hasItem("REPEATED_SUBQUERY_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("AGGREGATION_COMPLEXITY_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("LARGE_STRING_RESULT_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].riskCode").value(hasItem("REPEATED_SUBQUERY_RISK")))
            .andExpect(jsonPath("$.riskChecklist[*].evidence").value(hasItem(org.hamcrest.Matchers.containsString("staticOnly=true"))));
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
            .andExpect(jsonPath("$.featureSummary.repeatedTableScanCount").value(greaterThanOrEqualTo(1)))
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

    private String overlongSql() {
        String prefix = "SELECT * FROM orders WHERE dt = '2026-05-08' ";
        StringBuilder builder = new StringBuilder(10 * 1024 * 1024 + 128);
        builder.append(prefix).append("/*");
        while (builder.length() <= 10 * 1024 * 1024) {
            builder.append(" bounded diagnostics filler ");
        }
        builder.append("*/");
        return builder.toString();
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-parse-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-parse-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ACCESS_CHANNEL, "api")
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }

    private void waitForCombinedStatus(String parseTaskId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult result = mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse/{parseTaskId}", parseTaskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.status");
            if (expectedStatus.equals(status)) {
                return;
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Combined parse did not reach expected status " + expectedStatus);
    }
}
