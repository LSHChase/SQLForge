package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2MaterializedViewAmv016RegressionTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldKeepNonBlockingSqlBundleForAllAdvancedMvTypes() {
        List<AmvCase> cases = Arrays.asList(
            new AmvCase(
                "PARAMETERIZED_AGG_MV",
                "GENERATED",
                null,
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                    + "WHERE dt = DATE '2026-05-01' AND region = 'CN' GROUP BY customer_id"
            ),
            new AmvCase(
                "PREJOIN_MV",
                "REVIEW_REQUIRED",
                "ROW_AMPLIFICATION_METADATA_MISSING",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                    + "WHERE o.dt = DATE '2026-05-01' GROUP BY c.customer_level"
            ),
            new AmvCase(
                "STAR_AGG_MV",
                "REVIEW_REQUIRED",
                "STAR_SCHEMA_METADATA_MISSING",
                "SELECT o.dt, p.category, SUM(o.amount) AS total_amount "
                    + "FROM orders o "
                    + "JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id "
                    + "WHERE o.dt = DATE '2026-05-01' AND s.city = 'HZ' "
                    + "GROUP BY o.dt, p.category"
            ),
            new AmvCase(
                "ROLLUP_MV",
                "GENERATED",
                null,
                "SELECT DATE_TRUNC('month', order_date) AS order_month, region, "
                    + "SUM(amount) AS total_amount FROM orders "
                    + "WHERE order_date BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
                    + "AND region = 'CN' GROUP BY DATE_TRUNC('month', order_date), region"
            ),
            new AmvCase(
                "COMMON_SUBGRAPH_MV",
                "GENERATED",
                null,
                "WITH recent_orders AS (SELECT customer_id, amount, region FROM orders WHERE status = 'PAID') "
                    + "SELECT customer_id, SUM(amount) AS total_amount FROM recent_orders "
                    + "WHERE region = 'CN' GROUP BY customer_id"
            )
        );

        for (AmvCase item : cases) {
            Map<String, Object> artifact = artifact(item.sql);

            assertEquals(item.mvType, artifact.get("mvType"), String.valueOf(artifact));
            assertEquals(item.expectedStatus, artifact.get("artifactStatus"), String.valueOf(artifact));
            assertFalse("EXACT_QUERY_MV".equals(artifact.get("mvType")));
            assertTrue(maps(artifact.get("blockingReasons")).isEmpty(), String.valueOf(artifact));
            assertSqlBundle(artifact);
            assertTrue(String.valueOf(artifact.get("rewriteSql")).contains("FROM " + artifact.get("mvName")));
            assertFalse(String.valueOf(artifact.get("rewriteSql")).contains("SELECT * FROM " + artifact.get("mvName")));
            if (item.reviewWarningCode == null) {
                assertTrue(maps(artifact.get("reviewWarnings")).isEmpty(), String.valueOf(artifact));
            } else {
                assertTrue(hasCode(maps(artifact.get("reviewWarnings")), item.reviewWarningCode), String.valueOf(artifact));
            }
        }
    }

    @Test
    void shouldKeepBlockedSamplesForAllAdvancedMvTypesWithoutPublishableSqlBundle() {
        List<AmvCase> cases = Arrays.asList(
            new AmvCase(
                "PARAMETERIZED_AGG_MV",
                "COUNT_DISTINCT_MEASURE_NOT_MERGEABLE",
                "SELECT region, COUNT(DISTINCT customer_id) AS unique_customers FROM orders GROUP BY region"
            ),
            new AmvCase(
                "PREJOIN_MV",
                "OUTER_JOIN_PREJOIN_UNSUPPORTED",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o LEFT JOIN customers c ON o.customer_id = c.customer_id "
                    + "GROUP BY c.customer_level"
            ),
            new AmvCase(
                "STAR_AGG_MV",
                "OUTER_JOIN_STAR_AGG_UNSUPPORTED",
                "SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o "
                    + "LEFT JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id "
                    + "GROUP BY p.category, s.city"
            ),
            new AmvCase(
                "ROLLUP_MV",
                "TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE",
                "SELECT DATE_FORMAT(order_date, '%Y-%m') AS order_month, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_FORMAT(order_date, '%Y-%m')"
            ),
            new AmvCase(
                "COMMON_SUBGRAPH_MV",
                "RECURSIVE_CTE_COMMON_SUBGRAPH_UNSUPPORTED",
                "WITH RECURSIVE t(n) AS (SELECT 1 AS n) SELECT n FROM t"
            )
        );

        for (AmvCase item : cases) {
            Map<String, Object> artifact = artifact(item.sql);

            assertEquals(item.mvType, artifact.get("mvType"), String.valueOf(artifact));
            assertEquals("BLOCKED", artifact.get("artifactStatus"), String.valueOf(artifact));
            assertTrue(hasCode(maps(artifact.get("blockingReasons")), item.expectedReasonCode), String.valueOf(artifact));
            assertNull(artifact.get("ddlSql"), String.valueOf(artifact));
            assertNull(artifact.get("refreshSql"), String.valueOf(artifact));
            assertNull(artifact.get("validationSql"), String.valueOf(artifact));
            assertNull(artifact.get("rollbackSql"), String.valueOf(artifact));
            assertNull(artifact.get("rewriteSql"), String.valueOf(artifact));
        }
    }

    @Test
    void shouldRejectExactQueryMvSnapshotForAmv016ResponseBoundary() {
        LinkedHashMap<String, Object> exactQuery = new LinkedHashMap<String, Object>();
        exactQuery.put("mvType", "EXACT_QUERY_MV");
        exactQuery.put("artifactStatus", "GENERATED");
        exactQuery.put("ddlSql", "CREATE MATERIALIZED VIEW mv_exact AS SELECT * FROM orders");
        exactQuery.put("refreshSql", "REFRESH MATERIALIZED VIEW mv_exact");
        exactQuery.put("validationSql", "SELECT 1");
        exactQuery.put("rollbackSql", "DROP MATERIALIZED VIEW mv_exact");
        exactQuery.put("rewriteSql", "SELECT * FROM mv_exact");

        assertNull(AccelerationArtifactSnapshotSanitizer.sanitize(exactQuery));
    }

    private Map<String, Object> artifact(String sql) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
                "datasource-a",
                "fingerprint-amv-016",
                "sales-daily",
                null,
                null
            ),
            profile
        );
    }

    private static void assertSqlBundle(Map<String, Object> artifact) {
        assertText(artifact.get("ddlSql"));
        assertText(artifact.get("refreshSql"));
        assertText(artifact.get("validationSql"));
        assertText(artifact.get("rollbackSql"));
        assertText(artifact.get("rewriteSql"));
    }

    private static void assertText(Object value) {
        assertTrue(value != null && String.valueOf(value).trim().length() > 0);
    }

    private static boolean hasCode(List<Map<String, Object>> items, String code) {
        for (Map<String, Object> item : items) {
            if (code.equals(item.get("code"))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) value;
    }

    private static final class AmvCase {
        private final String mvType;
        private final String expectedStatus;
        private final String reviewWarningCode;
        private final String expectedReasonCode;
        private final String sql;

        private AmvCase(String mvType, String expectedStatus, String reviewWarningCode, String sql) {
            this.mvType = mvType;
            this.expectedStatus = expectedStatus;
            this.reviewWarningCode = reviewWarningCode;
            this.expectedReasonCode = null;
            this.sql = sql;
        }

        private AmvCase(String mvType, String expectedReasonCode, String sql) {
            this.mvType = mvType;
            this.expectedStatus = "BLOCKED";
            this.reviewWarningCode = null;
            this.expectedReasonCode = expectedReasonCode;
            this.sql = sql;
        }
    }
}
