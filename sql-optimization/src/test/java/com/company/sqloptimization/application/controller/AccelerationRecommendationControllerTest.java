package com.company.sqloptimization.application.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.application.controller.vo.RecommendationPageVO;
import com.company.sqloptimization.application.service.AccelerationRecommendationApplicationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AccelerationRecommendationController.class)
class AccelerationRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccelerationRecommendationApplicationService recommendationApplicationService;

    @Test
    void shouldExposeRecommendationListAndDetailEndpoints() throws Exception {
        AccelerationRecommendationVO recommendation = new AccelerationRecommendationVO();
        recommendation.setRecommendationId("rec-001");
        recommendation.setTenantId("tenant-a");
        recommendation.setRecommendationType("PREWARM");
        recommendation.setRecommendedSqlText("INSERT INTO agg_sales SELECT * FROM sales");
        recommendation.setExpectedGain("Warms report table before peak window");
        recommendation.setBenefitLevel("HIGH");
        recommendation.setRiskLevel("LOW");
        recommendation.setRequiresDispatch(Boolean.TRUE);
        recommendation.setStatus("RECOMMENDED");
        recommendation.setSourceType("PARSE");
        recommendation.setSourceKind("STRUCTURE_PARSE");
        recommendation.setEvidenceLevel("STATIC_PARSE");
        recommendation.setValidationStatus("NOT_VALIDATED");
        recommendation.setAutoApplyAllowed(Boolean.FALSE);
        recommendation.setManualReviewRequired(Boolean.TRUE);
        recommendation.setRuleChain(Collections.singletonList(rule("COUNT_ONE_TO_COUNT_STAR")));
        recommendation.setAccelerationArtifact(artifact());
        RecommendationDiffVO diff = new RecommendationDiffVO();
        diff.setRecommendationId("rec-001");
        diff.setTenantId("tenant-a");
        diff.setSourceType("QUERY");
        diff.setSourceKind("QUERY_HISTORY");
        diff.setSourceId("history-001");
        diff.setEvidenceLevel("RUNTIME_HISTORY");
        diff.setTextDiff(Collections.singletonList(textHunk("REPLACE")));
        diff.setAccelerationArtifact(artifact());
        diff.setDiffStatus("READY");
        diff.setImplementationStage("HARN_132_SQL_DIFF_SERVICE");
        when(recommendationApplicationService.listRecommendations())
            .thenReturn(Collections.singletonList(recommendation));
        when(recommendationApplicationService.getRecommendation("rec-001")).thenReturn(recommendation);
        when(recommendationApplicationService.getRecommendationDiff("rec-001")).thenReturn(diff);

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recommendationType").value("PREWARM"))
            .andExpect(jsonPath("$[0].benefitLevel").value("HIGH"))
            .andExpect(jsonPath("$[0].riskLevel").value("LOW"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations/rec-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendationId").value("rec-001"))
            .andExpect(jsonPath("$.requiresDispatch").value(true))
            .andExpect(jsonPath("$.ruleChain[0].rule").value("COUNT_ONE_TO_COUNT_STAR"))
            .andExpect(jsonPath("$.accelerationArtifact.mvType").value("PREJOIN_MV"))
            .andExpect(jsonPath("$.accelerationArtifact.artifactStatus").value("REVIEW_REQUIRED"))
            .andExpect(jsonPath("$.accelerationArtifact.reviewWarnings[0].code").value("ROW_AMPLIFICATION_METADATA_MISSING"))
            .andExpect(jsonPath("$.accelerationArtifact.grain[0]").value("customer_id"))
            .andExpect(jsonPath("$.accelerationArtifact.dimensions[0]").value("customer_id"))
            .andExpect(jsonPath("$.accelerationArtifact.measures[0].name").value("sum_amount"))
            .andExpect(jsonPath("$.accelerationArtifact.coverage.coversProjection").value(true))
            .andExpect(jsonPath("$.accelerationArtifact.joinGraph").isArray())
            .andExpect(jsonPath("$.accelerationArtifact.ddlSql").value("CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT customer_id, SUM(amount) AS sum_amount FROM orders GROUP BY customer_id"))
            .andExpect(jsonPath("$.accelerationArtifact.refreshSql").value("REFRESH MATERIALIZED VIEW mv_orders_customer"))
            .andExpect(jsonPath("$.accelerationArtifact.validationSql").value("SELECT COUNT(*) FROM mv_orders_customer"))
            .andExpect(jsonPath("$.accelerationArtifact.rollbackSql").value("DROP MATERIALIZED VIEW mv_orders_customer"))
            .andExpect(jsonPath("$.accelerationArtifact.rewriteSql").value("SELECT customer_id, SUM(sum_amount) AS total_amount FROM mv_orders_customer GROUP BY customer_id"))
            .andExpect(jsonPath("$.manualReviewRequired").value(true))
            .andExpect(jsonPath("$.autoApplyAllowed").value(false));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations/rec-001/diff")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceKind").value("QUERY_HISTORY"))
            .andExpect(jsonPath("$.evidenceLevel").value("RUNTIME_HISTORY"))
            .andExpect(jsonPath("$.accelerationArtifact.mvType").value("PREJOIN_MV"))
            .andExpect(jsonPath("$.accelerationArtifact.artifactStatus").value("REVIEW_REQUIRED"))
            .andExpect(jsonPath("$.accelerationArtifact.reviewWarnings[0].code").value("ROW_AMPLIFICATION_METADATA_MISSING"))
            .andExpect(jsonPath("$.accelerationArtifact.coverage.coversProjection").value(true))
            .andExpect(jsonPath("$.accelerationArtifact.rewriteSql").value("SELECT customer_id, SUM(sum_amount) AS total_amount FROM mv_orders_customer GROUP BY customer_id"))
            .andExpect(jsonPath("$.diffStatus").value("READY"))
            .andExpect(jsonPath("$.textDiff[0].type").value("REPLACE"));
    }

    @Test
    void shouldExposeRecommendationPageEndpointWithFiltersAndMetadata() throws Exception {
        AccelerationRecommendationVO recommendation = new AccelerationRecommendationVO();
        recommendation.setRecommendationId("rec-page-001");
        recommendation.setTenantId("tenant-a");
        recommendation.setRecommendationType("REWRITE");
        recommendation.setStatus("DISPATCH_READY");
        recommendation.setBenefitLevel("HIGH");
        recommendation.setRiskLevel("LOW");
        recommendation.setValidationStatus("DIVERGED");
        RecommendationPageVO page = new RecommendationPageVO(
            Collections.singletonList(recommendation),
            Integer.valueOf(2),
            Integer.valueOf(50),
            Integer.valueOf(123),
            Integer.valueOf(3),
            Boolean.TRUE
        );
        when(recommendationApplicationService.listRecommendationPage(
            eq("REWRITE"),
            eq("DISPATCH_READY"),
            eq("HIGH"),
            eq("LOW"),
            eq("DIVERGED"),
            eq(Boolean.TRUE),
            eq(Boolean.FALSE),
            eq("QUERY"),
            eq("QUERY_HISTORY"),
            eq("history-001"),
            eq("history-001"),
            eq("parse-task-001"),
            eq("batch-001"),
            eq("RPT_001"),
            eq("riskLevel"),
            eq("ASC"),
            eq(Integer.valueOf(2)),
            eq(Integer.valueOf(50))
        )).thenReturn(page);

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations/page")
                .param("pageNo", "2")
                .param("pageSize", "50")
                .param("sortBy", "riskLevel")
                .param("sortOrder", "ASC")
                .param("recommendationType", "REWRITE")
                .param("status", "DISPATCH_READY")
                .param("benefitLevel", "HIGH")
                .param("riskLevel", "LOW")
                .param("validationStatus", "DIVERGED")
                .param("requiresDispatch", "true")
                .param("manualReviewRequired", "false")
                .param("sourceType", "QUERY")
                .param("sourceKind", "QUERY_HISTORY")
                .param("sourceId", "history-001")
                .param("historyId", "history-001")
                .param("parseTaskId", "parse-task-001")
                .param("batchId", "batch-001")
                .param("reportCode", "RPT_001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].recommendationId").value("rec-page-001"))
            .andExpect(jsonPath("$.pageNo").value(2))
            .andExpect(jsonPath("$.pageSize").value(50))
            .andExpect(jsonPath("$.totalCount").value(123))
            .andExpect(jsonPath("$.pageCount").value(3))
            .andExpect(jsonPath("$.hasMore").value(true));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ACCESS_CHANNEL, "api")
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }

    private Map<String, Object> rule(String rule) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        return entry;
    }

    private Map<String, Object> textHunk(String type) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("hunkId", "hunk-1");
        entry.put("type", type);
        return entry;
    }

    private Map<String, Object> artifact() {
        Map<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("mvType", "PREJOIN_MV");
        artifact.put("artifactStatus", "REVIEW_REQUIRED");
        artifact.put("grain", Collections.singletonList("customer_id"));
        artifact.put("dimensions", Collections.singletonList("customer_id"));
        artifact.put("measures", Collections.singletonList(measure()));
        artifact.put("externalizedPredicates", Collections.emptyList());
        artifact.put("retainedPredicates", Collections.emptyList());
        artifact.put("securityPredicates", Collections.emptyList());
        artifact.put("blockedPredicates", Collections.emptyList());
        artifact.put("blockingReasons", Collections.emptyList());
        artifact.put("reviewWarnings", Collections.singletonList(warning()));
        artifact.put("coverage", coverage());
        artifact.put("joinGraph", Collections.emptyList());
        artifact.put(
            "ddlSql",
            "CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT customer_id, SUM(amount) AS sum_amount FROM orders GROUP BY customer_id"
        );
        artifact.put("refreshSql", "REFRESH MATERIALIZED VIEW mv_orders_customer");
        artifact.put("validationSql", "SELECT COUNT(*) FROM mv_orders_customer");
        artifact.put("rollbackSql", "DROP MATERIALIZED VIEW mv_orders_customer");
        artifact.put(
            "rewriteSql",
            "SELECT customer_id, SUM(sum_amount) AS total_amount FROM mv_orders_customer GROUP BY customer_id"
        );
        return artifact;
    }

    private Map<String, Object> warning() {
        Map<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", "ROW_AMPLIFICATION_METADATA_MISSING");
        warning.put("description", "missing uniqueness and selectivity metadata");
        return warning;
    }

    private Map<String, Object> measure() {
        Map<String, Object> measure = new LinkedHashMap<String, Object>();
        measure.put("name", "sum_amount");
        measure.put("sourceExpression", "SUM(amount)");
        measure.put("rewriteExpression", "SUM(sum_amount)");
        measure.put("mergeable", Boolean.TRUE);
        return measure;
    }

    private Map<String, Object> coverage() {
        Map<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("coversProjection", Boolean.TRUE);
        coverage.put("coversFilters", Boolean.TRUE);
        coverage.put("coversGrouping", Boolean.TRUE);
        coverage.put("coversMeasures", Boolean.TRUE);
        coverage.put("coversSecurity", Boolean.TRUE);
        return coverage;
    }
}
