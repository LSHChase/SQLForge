package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2RollupMvCandidateGeneratorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGenerateDailyMvAndMonthlyRewriteOnMvOnly() {
        String sql = "SELECT DATE_TRUNC('month', order_date) AS order_month, region, "
            + "SUM(amount) AS total_amount FROM orders "
            + "WHERE order_date BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
            + "AND region = 'CN' AND status = 'PAID' AND access_domain = 'BI' "
            + "GROUP BY DATE_TRUNC('month', order_date), region";
        Map<String, Object> artifact = artifact(sql);

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("ROLLUP_MV", artifact.get("mvType"));
        Map<String, Object> evidence = map(artifact.get("timeRollupEvidence"));
        assertEquals("GENERATED", evidence.get("status"));
        assertEquals("order_date", evidence.get("timeSourceColumn"));
        assertEquals("DAY", evidence.get("mvFinestGrain"));
        assertEquals("MONTH", evidence.get("queryTargetGrain"));
        assertEquals("order_date_day", evidence.get("mvTimeColumn"));
        assertEquals("NATURAL_CALENDAR_DAY_TO_MONTH_QUARTER_YEAR", evidence.get("calendarPolicy"));

        String ddlSql = String.valueOf(artifact.get("ddlSql"));
        assertTrue(ddlSql.contains("CREATE MATERIALIZED VIEW mv_sales_daily AS"));
        assertTrue(ddlSql.contains("DATE_TRUNC('day', order_date) AS order_date_day"));
        assertTrue(ddlSql.contains("region"));
        assertTrue(ddlSql.contains("access_domain"));
        assertTrue(ddlSql.contains("SUM(amount) AS total_amount"));
        assertTrue(ddlSql.contains("WHERE status = 'PAID'"));
        assertTrue(ddlSql.contains("GROUP BY DATE_TRUNC('day', order_date), region, access_domain"));
        assertFalse(ddlSql.contains("DATE '2026-05-01'"));
        assertFalse(ddlSql.contains("region = 'CN'"));
        assertFalse(ddlSql.contains("access_domain = 'BI'"));
        assertFalse(ddlSql.contains(sql));

        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("DATE_TRUNC('month', order_date_day) AS order_month"));
        assertTrue(rewriteSql.contains("FROM mv_sales_daily"));
        assertTrue(rewriteSql.contains("order_date_day BETWEEN DATE '2026-05-01' AND DATE '2026-05-31'"));
        assertTrue(rewriteSql.contains("region = 'CN'"));
        assertTrue(rewriteSql.contains("access_domain = 'BI'"));
        assertTrue(rewriteSql.contains("SUM(total_amount) AS total_amount"));
        assertTrue(rewriteSql.contains("GROUP BY DATE_TRUNC('month', order_date_day), region"));
        assertFalse(rewriteSql.contains("FROM orders"));
    }

    @Test
    void shouldSplitAvgComponentsForQuarterRollupRewrite() {
        Map<String, Object> artifact = artifact(
            "SELECT DATE_TRUNC('quarter', order_date) AS order_quarter, AVG(amount) AS avg_amount "
                + "FROM orders GROUP BY DATE_TRUNC('quarter', order_date)"
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("ROLLUP_MV", artifact.get("mvType"));
        assertEquals("QUARTER", map(artifact.get("timeRollupEvidence")).get("queryTargetGrain"));

        String ddlSql = String.valueOf(artifact.get("ddlSql"));
        assertTrue(ddlSql.contains("SUM(amount) AS avg_amount_sum"));
        assertTrue(ddlSql.contains("COUNT(amount) AS avg_amount_count"));

        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("DATE_TRUNC('quarter', order_date_day) AS order_quarter"));
        assertTrue(rewriteSql.contains("SUM(avg_amount_sum) / NULLIF(SUM(avg_amount_count), 0) AS avg_amount"));
        assertFalse(rewriteSql.contains("AVG(avg_amount"));
    }

    @Test
    void shouldBlockWeekFiscalAndUnnormalizableTimeExpressionsWithoutSql() {
        assertBlocked(
            artifact("SELECT DATE_TRUNC('week', order_date) AS order_week, SUM(amount) AS total_amount "
                + "FROM orders GROUP BY DATE_TRUNC('week', order_date)"),
            "WEEK_ROLLUP_CALENDAR_POLICY_REQUIRED"
        );
        assertBlocked(
            artifact("SELECT DATE_TRUNC('month', fiscal_date) AS fiscal_month, SUM(amount) AS total_amount "
                + "FROM orders GROUP BY DATE_TRUNC('month', fiscal_date)"),
            "FISCAL_CALENDAR_ROLLUP_REQUIRES_REVIEW"
        );
        assertBlocked(
            artifact("SELECT DATE_FORMAT(order_date, '%Y-%m') AS order_month, SUM(amount) AS total_amount "
                + "FROM orders GROUP BY DATE_FORMAT(order_date, '%Y-%m')"),
            "TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE"
        );
    }

    @Test
    void shouldBlockNonMergeableRollupMeasuresWithoutPublishableSql() {
        assertBlocked(
            artifact("SELECT DATE_TRUNC('month', order_date) AS order_month, "
                + "COUNT(DISTINCT customer_id) AS unique_customers "
                + "FROM orders GROUP BY DATE_TRUNC('month', order_date)"),
            "COUNT_DISTINCT_MEASURE_NOT_MERGEABLE"
        );
        assertBlocked(
            artifact("SELECT DATE_TRUNC('month', order_date) AS order_month, "
                + "APPROX_PERCENTILE(amount, 0.95) AS p95_amount "
                + "FROM orders GROUP BY DATE_TRUNC('month', order_date)"),
            "PERCENTILE_MEASURE_NOT_MERGEABLE"
        );
        assertBlocked(
            artifact("SELECT DATE_TRUNC('month', order_date) AS order_month, "
                + "MEDIAN(amount) AS median_amount "
                + "FROM orders GROUP BY DATE_TRUNC('month', order_date)"),
            "MEDIAN_MEASURE_NOT_MERGEABLE"
        );
    }

    @Test
    void shouldKeepOrdinaryAggregateOnParameterizedAggPath() {
        Map<String, Object> artifact = artifact(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id"
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("PARAMETERIZED_AGG_MV", artifact.get("mvType"));
        assertNull(artifact.get("timeRollupEvidence"));
    }

    private Map<String, Object> artifact(String sql) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
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
        assertEquals("ROLLUP_MV", artifact.get("mvType"));
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
