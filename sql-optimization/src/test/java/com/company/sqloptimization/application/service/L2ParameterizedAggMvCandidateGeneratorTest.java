package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2ParameterizedAggMvCandidateGeneratorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGenerateSameMvShapeForDateRegionChannelAndCustomerParameterChanges() {
        String maySql = "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
            + "WHERE dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
            + "AND region = 'CN' AND channel = 'APP' AND customer_id = 1001 "
            + "AND status = 'PAID' AND access_domain = 'BI' GROUP BY customer_id";
        String juneSql = "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
            + "WHERE dt BETWEEN DATE '2026-06-01' AND DATE '2026-06-30' "
            + "AND region = 'US' AND channel = 'WEB' AND customer_id = 2002 "
            + "AND status = 'PAID' AND access_domain = 'BI' GROUP BY customer_id";

        Map<String, Object> mayArtifact = artifact(maySql);
        Map<String, Object> juneArtifact = artifact(juneSql);

        assertEquals("GENERATED", mayArtifact.get("artifactStatus"));
        assertEquals("PARAMETERIZED_AGG_MV", mayArtifact.get("mvType"));
        assertEquals(mayArtifact.get("ddlSql"), juneArtifact.get("ddlSql"));
        assertTrue(String.valueOf(mayArtifact.get("ddlSql")).contains("customer_id"));
        assertTrue(String.valueOf(mayArtifact.get("ddlSql")).contains("dt"));
        assertTrue(String.valueOf(mayArtifact.get("ddlSql")).contains("region"));
        assertTrue(String.valueOf(mayArtifact.get("ddlSql")).contains("channel"));
        assertTrue(String.valueOf(mayArtifact.get("ddlSql")).contains("access_domain"));
        assertTrue(String.valueOf(mayArtifact.get("ddlSql")).contains("status = 'PAID'"));
        assertFalse(String.valueOf(mayArtifact.get("ddlSql")).contains("region = 'CN'"));
        assertFalse(String.valueOf(mayArtifact.get("ddlSql")).contains("DATE '2026-05-01'"));
        assertFalse(String.valueOf(mayArtifact.get("ddlSql")).contains(maySql));

        String rewriteSql = String.valueOf(mayArtifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("FROM mv_sales_daily"));
        assertTrue(rewriteSql.contains("dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31'"));
        assertTrue(rewriteSql.contains("region = 'CN'"));
        assertTrue(rewriteSql.contains("channel = 'APP'"));
        assertTrue(rewriteSql.contains("customer_id = 1001"));
        assertTrue(rewriteSql.contains("access_domain = 'BI'"));
        assertTrue(rewriteSql.contains("SUM(total_amount) AS total_amount"));
        assertTrue(rewriteSql.contains("GROUP BY customer_id"));
        assertFalse(rewriteSql.contains("SELECT * FROM mv_sales_daily"));

        String validationSql = String.valueOf(mayArtifact.get("validationSql"));
        assertTrue(validationSql.contains("original_result"));
        assertTrue(validationSql.contains("rewrite_result"));
        assertTrue(validationSql.contains("FROM mv_sales_daily"));
    }

    @Test
    void shouldRenderDirectAvgAndRatioMeasureColumnsAndRewriteExpressions() {
        Map<String, Object> artifact = artifact(
            "SELECT region, SUM(amount) AS total_amount, COUNT(*) AS order_count, "
                + "MIN(pay_time) AS first_pay_time, MAX(pay_time) AS last_pay_time, "
                + "AVG(amount) AS avg_amount, SUM(pay_amount) / SUM(order_amount) AS pay_rate "
                + "FROM orders WHERE dt = DATE '2026-05-01' GROUP BY region"
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        String ddlSql = String.valueOf(artifact.get("ddlSql"));
        assertTrue(ddlSql.contains("SUM(amount) AS total_amount"));
        assertTrue(ddlSql.contains("COUNT(*) AS order_count"));
        assertTrue(ddlSql.contains("MIN(pay_time) AS first_pay_time"));
        assertTrue(ddlSql.contains("MAX(pay_time) AS last_pay_time"));
        assertTrue(ddlSql.contains("SUM(amount) AS avg_amount_sum"));
        assertTrue(ddlSql.contains("COUNT(amount) AS avg_amount_count"));
        assertTrue(ddlSql.contains("SUM(pay_amount) AS pay_rate_numerator"));
        assertTrue(ddlSql.contains("SUM(order_amount) AS pay_rate_denominator"));

        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("SUM(total_amount) AS total_amount"));
        assertTrue(rewriteSql.contains("SUM(order_count) AS order_count"));
        assertTrue(rewriteSql.contains("MIN(first_pay_time) AS first_pay_time"));
        assertTrue(rewriteSql.contains("MAX(last_pay_time) AS last_pay_time"));
        assertTrue(rewriteSql.contains("SUM(avg_amount_sum) / NULLIF(SUM(avg_amount_count), 0) AS avg_amount"));
        assertTrue(rewriteSql.contains(
            "SUM(pay_rate_numerator) / NULLIF(SUM(pay_rate_denominator), 0) AS pay_rate"
        ));
    }

    @Test
    void shouldBlockSelectStarNoMeasureAutoEngineAndNonMergeableMeasuresWithoutSql() {
        assertBlocked(
            artifact("SELECT *, SUM(amount) AS total_amount FROM orders GROUP BY customer_id"),
            "EXPLICIT_PROJECTION_REQUIRED"
        );
        assertBlocked(
            artifact("SELECT customer_id FROM orders GROUP BY customer_id"),
            "MEASURE_REQUIRED"
        );
        assertBlocked(
            artifactWithEngine(
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id",
                "AUTO"
            ),
            "TARGET_ENGINE_REQUIRED"
        );
        assertBlocked(
            artifact("SELECT region, COUNT(DISTINCT customer_id) AS unique_customers FROM orders GROUP BY region"),
            "COUNT_DISTINCT_MEASURE_NOT_MERGEABLE"
        );
        assertBlocked(
            artifact("SELECT region, APPROX_PERCENTILE(amount, 0.95) AS p95_amount FROM orders GROUP BY region"),
            "PERCENTILE_MEASURE_NOT_MERGEABLE"
        );
        assertBlocked(
            artifact("SELECT region, MEDIAN(amount) AS median_amount FROM orders GROUP BY region"),
            "MEDIAN_MEASURE_NOT_MERGEABLE"
        );
        assertBlocked(
            artifact("SELECT region, GROUP_CONCAT(product_id) AS products FROM orders GROUP BY region"),
            "COMPLEX_UDAF_MEASURE_NOT_MERGEABLE"
        );
        assertBlocked(
            artifact("SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt >= CURRENT_DATE GROUP BY customer_id"),
            "BLOCKED_UNSTABLE_PREDICATE"
        );
    }

    @Test
    void shouldBlockDeferredNonPrejoinMvShapesAndUnsafeRewriteCasesWithoutSql() {
        assertBlocked(
            artifact("SELECT region, SUM(amount) AS total_amount FROM orders "
                + "WHERE region = 'CN' OR region = 'US' GROUP BY region"),
            "OR_PREDICATE_REWRITE_UNSUPPORTED"
        );
        assertBlocked(
            artifact("SELECT region, SUM(amount) AS total_amount FROM orders "
                + "GROUP BY region ORDER BY total_amount DESC LIMIT 10"),
            "ORDER_LIMIT_REWRITE_UNSUPPORTED"
        );
    }

    @Test
    void shouldRouteJoinAggregateToPrejoinInsteadOfDeferredAmv005Block() {
        Map<String, Object> artifact = artifact("SELECT o.customer_id, SUM(o.amount) AS total_amount FROM orders o "
            + "JOIN customers c ON o.customer_id = c.customer_id GROUP BY o.customer_id");

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("PREJOIN_MV", artifact.get("mvType"));
        assertFalse(hasReason(maps(artifact.get("blockingReasons")), "JOIN_MV_TYPE_DEFERRED"));
        assertTrue(String.valueOf(artifact.get("rewriteSql")).contains("FROM mv_sales_daily"));
        assertFalse(String.valueOf(artifact.get("rewriteSql")).contains("JOIN customers"));
    }

    private Map<String, Object> artifact(String sql) {
        return artifactWithEngine(sql, "HETU");
    }

    private Map<String, Object> artifactWithEngine(String sql, String targetEngine) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                targetEngine,
                "datasource-a",
                "fingerprint-001",
                "sales-daily",
                null,
                null
            ),
            profile
        );
    }

    private static void assertBlocked(Map<String, Object> artifact, String reasonCode) {
        assertEquals("BLOCKED", artifact.get("artifactStatus"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), reasonCode));
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    private static boolean hasReason(List<Map<String, Object>> reasons, String code) {
        for (Map<String, Object> reason : reasons) {
            if (code.equals(reason.get("code"))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
