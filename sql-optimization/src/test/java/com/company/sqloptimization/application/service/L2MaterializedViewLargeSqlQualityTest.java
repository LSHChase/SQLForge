package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2MaterializedViewLargeSqlQualityTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldCoverFiftyMaterializedViewSqlShapesWithVaryingSize() {
        List<MvCase> cases = Arrays.asList(
            generated("parameterized-basic", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id"),
            generated("parameterized-date-region", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                    + "WHERE dt = DATE '2026-05-01' AND region = 'CN' GROUP BY customer_id"),
            generated("parameterized-retained-status", "PARAMETERIZED_AGG_MV",
                "SELECT channel, COUNT(*) AS order_count FROM orders WHERE status = 'PAID' GROUP BY channel"),
            generated("parameterized-tenant-status", "PARAMETERIZED_AGG_MV",
                "SELECT product_id, SUM(amount) AS total_amount FROM orders "
                    + "WHERE tenant_id = 't1' AND status = 'PAID' GROUP BY product_id"),
            generated("parameterized-avg", "PARAMETERIZED_AGG_MV",
                "SELECT region, AVG(amount) AS avg_amount FROM orders GROUP BY region"),
            generated("parameterized-ratio", "PARAMETERIZED_AGG_MV",
                "SELECT region, SUM(profit) / COUNT(*) AS profit_per_order FROM orders GROUP BY region"),
            generated("parameterized-min-max", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, MIN(amount) AS min_amount, MAX(amount) AS max_amount "
                    + "FROM orders GROUP BY customer_id"),
            generated("parameterized-having-expression", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                    + "GROUP BY customer_id HAVING SUM(amount) > 1000"),
            generated("parameterized-having-alias", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                    + "GROUP BY customer_id HAVING total_amount > 1000"),
            generated("parameterized-security", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                    + "WHERE access_domain = 'BI' GROUP BY customer_id"),
            generated("parameterized-count-column", "PARAMETERIZED_AGG_MV",
                "SELECT region, COUNT(order_id) AS order_count FROM orders GROUP BY region"),
            generated("parameterized-case-sum", "PARAMETERIZED_AGG_MV",
                "SELECT customer_id, SUM(CASE WHEN status = 'PAID' THEN amount ELSE 0 END) AS paid_amount "
                    + "FROM orders GROUP BY customer_id"),
            generated("parameterized-count-distinct", "PARAMETERIZED_AGG_MV",
                "SELECT region, COUNT(DISTINCT customer_id) AS unique_customers FROM orders GROUP BY region"),
            blocked("parameterized-select-star", "PARAMETERIZED_AGG_MV", "EXPLICIT_PROJECTION_REQUIRED",
                "SELECT *, SUM(amount) AS total_amount FROM orders GROUP BY customer_id"),
            generated("parameterized-large-multiline", "PARAMETERIZED_AGG_MV", largeParameterizedAggSql(620)),
            generated("prejoin-basic", "PREJOIN_MV",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id GROUP BY c.customer_level",
                "ROW_AMPLIFICATION_METADATA_MISSING"),
            generated("prejoin-group-only", "PREJOIN_MV",
                "SELECT c.customer_level FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                    + "GROUP BY c.customer_level",
                "ROW_AMPLIFICATION_METADATA_MISSING"),
            generated("prejoin-avg", "PREJOIN_MV",
                "SELECT c.customer_level, AVG(o.amount) AS avg_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id GROUP BY c.customer_level",
                "ROW_AMPLIFICATION_METADATA_MISSING"),
            generated("prejoin-security", "PREJOIN_MV",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                    + "WHERE o.access_domain = 'BI' GROUP BY c.customer_level",
                "ROW_AMPLIFICATION_METADATA_MISSING"),
            generated("prejoin-retained-dimension-status", "PREJOIN_MV",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                    + "WHERE c.state = 'CA' GROUP BY c.customer_level",
                "ROW_AMPLIFICATION_METADATA_MISSING"),
            generated("prejoin-having", "PREJOIN_MV",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                    + "GROUP BY c.customer_level HAVING SUM(o.amount) > 1000",
                "ROW_AMPLIFICATION_METADATA_MISSING"),
            blocked("prejoin-left-join", "PREJOIN_MV", "OUTER_JOIN_PREJOIN_UNSUPPORTED",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o LEFT JOIN customers c ON o.customer_id = c.customer_id GROUP BY c.customer_level"),
            blocked("prejoin-non-equi", "PREJOIN_MV", "NON_EQUI_JOIN_PREJOIN_UNSUPPORTED",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.amount > c.min_amount GROUP BY c.customer_level"),
            blocked("prejoin-cast-key", "PREJOIN_MV", "COMPLEX_JOIN_KEY_PREJOIN_UNSUPPORTED",
                "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON CAST(o.customer_id AS VARCHAR) = c.customer_id "
                    + "GROUP BY c.customer_level"),
            blocked("prejoin-ambiguous-field", "PREJOIN_MV", "FIELD_AMBIGUITY_UNRESOLVED",
                "SELECT customer_id, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id GROUP BY customer_id"),
            generated("star-basic", "STAR_AGG_MV",
                "SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id GROUP BY p.category, s.city",
                "STAR_SCHEMA_METADATA_MISSING"),
            generated("star-avg", "STAR_AGG_MV",
                "SELECT p.category, s.city, AVG(o.amount) AS avg_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id GROUP BY p.category, s.city",
                "STAR_SCHEMA_METADATA_MISSING"),
            generated("star-time-dimension", "STAR_AGG_MV",
                "SELECT o.dt, p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id GROUP BY o.dt, p.category, s.city",
                "STAR_SCHEMA_METADATA_MISSING"),
            generated("star-security", "STAR_AGG_MV",
                "SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id "
                    + "WHERE o.access_domain = 'BI' GROUP BY p.category, s.city",
                "STAR_SCHEMA_METADATA_MISSING"),
            generated("star-large-multiline", "STAR_AGG_MV", largeStarSql(72), "STAR_SCHEMA_METADATA_MISSING"),
            generated("star-retained-status", "STAR_AGG_MV",
                "SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id "
                    + "WHERE o.status = 'PAID' GROUP BY p.category, s.city",
                "STAR_SCHEMA_METADATA_MISSING"),
            blocked("star-left-join", "STAR_AGG_MV", "OUTER_JOIN_STAR_AGG_UNSUPPORTED",
                "SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o LEFT JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id GROUP BY p.category, s.city"),
            blocked("star-non-equi", "STAR_AGG_MV", "NON_EQUI_JOIN_STAR_AGG_UNSUPPORTED",
                "SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.order_date BETWEEN s.start_date AND s.end_date GROUP BY p.category, s.city"),
            blocked("star-unqualified-measure", "STAR_AGG_MV", "FACT_TABLE_UNRESOLVED",
                "SELECT p.category, s.city, SUM(amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id GROUP BY p.category, s.city"),
            blocked("star-fact-non-time-group", "STAR_AGG_MV", "STAR_AGG_GRAIN_NOT_COVERED",
                "SELECT o.customer_id, p.category, SUM(o.amount) AS total_amount "
                    + "FROM orders o JOIN products p ON o.product_id = p.product_id "
                    + "JOIN shops s ON o.shop_id = s.shop_id GROUP BY o.customer_id, p.category"),
            generated("rollup-month", "ROLLUP_MV",
                "SELECT DATE_TRUNC('month', order_date) AS order_month, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_TRUNC('month', order_date)"),
            generated("rollup-quarter", "ROLLUP_MV",
                "SELECT DATE_TRUNC('quarter', order_date) AS order_quarter, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_TRUNC('quarter', order_date)"),
            generated("rollup-year", "ROLLUP_MV",
                "SELECT YEAR(order_date) AS order_year, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY YEAR(order_date)"),
            generated("rollup-avg", "ROLLUP_MV",
                "SELECT DATE_TRUNC('month', order_date) AS order_month, AVG(amount) AS avg_amount "
                    + "FROM orders GROUP BY DATE_TRUNC('month', order_date)"),
            generated("rollup-security", "ROLLUP_MV",
                "SELECT DATE_TRUNC('month', order_date) AS order_month, SUM(amount) AS total_amount "
                    + "FROM orders WHERE access_domain = 'BI' GROUP BY DATE_TRUNC('month', order_date)"),
            blocked("rollup-week", "ROLLUP_MV", "WEEK_ROLLUP_CALENDAR_POLICY_REQUIRED",
                "SELECT DATE_TRUNC('week', order_date) AS order_week, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_TRUNC('week', order_date)"),
            blocked("rollup-day", "ROLLUP_MV", "ROLLUP_TARGET_GRAIN_NOT_COARSER_THAN_DAY",
                "SELECT DATE_TRUNC('day', order_date) AS order_day, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_TRUNC('day', order_date)"),
            blocked("rollup-date-format", "ROLLUP_MV", "TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE",
                "SELECT DATE_FORMAT(order_date, '%Y-%m') AS order_month, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_FORMAT(order_date, '%Y-%m')"),
            blocked("rollup-fiscal-calendar", "ROLLUP_MV", "FISCAL_CALENDAR_ROLLUP_REQUIRES_REVIEW",
                "SELECT DATE_TRUNC('month', fiscal_order_date) AS fiscal_month, SUM(amount) AS total_amount "
                    + "FROM orders GROUP BY DATE_TRUNC('month', fiscal_order_date)"),
            blocked("rollup-multiple-time", "ROLLUP_MV", "MULTIPLE_TIME_ROLLUP_EXPRESSIONS_UNSUPPORTED",
                "SELECT DATE_TRUNC('month', order_date) AS order_month, DATE_TRUNC('year', order_date) AS order_year, "
                    + "SUM(amount) AS total_amount FROM orders "
                    + "GROUP BY DATE_TRUNC('month', order_date), DATE_TRUNC('year', order_date)"),
            generated("common-cte", "COMMON_SUBGRAPH_MV",
                "WITH recent_orders AS (SELECT customer_id, amount, region FROM orders WHERE status = 'PAID') "
                    + "SELECT customer_id, SUM(amount) AS total_amount FROM recent_orders "
                    + "WHERE region = 'CN' GROUP BY customer_id"),
            generated("common-derived-table", "COMMON_SUBGRAPH_MV",
                "SELECT d.customer_id, SUM(d.amount) AS total_amount FROM "
                    + "(SELECT customer_id, amount, region FROM orders WHERE status = 'PAID') d "
                    + "WHERE d.region = 'CN' GROUP BY d.customer_id"),
            generated("common-large-multiline", "COMMON_SUBGRAPH_MV", largeCommonSubgraphSql(90)),
            blocked("common-recursive", "COMMON_SUBGRAPH_MV", "RECURSIVE_CTE_COMMON_SUBGRAPH_UNSUPPORTED",
                "WITH RECURSIVE t(n) AS (SELECT 1 AS n) SELECT n FROM t"),
            blocked("common-select-star", "COMMON_SUBGRAPH_MV", "EXPLICIT_PROJECTION_REQUIRED",
                "WITH recent_orders AS (SELECT * FROM orders WHERE status = 'PAID') "
                    + "SELECT customer_id FROM recent_orders"),
            blocked("common-order-limit", "COMMON_SUBGRAPH_MV", "SUBGRAPH_ORDER_LIMIT_UNSUPPORTED",
                "WITH ranked_orders AS (SELECT customer_id, amount FROM orders ORDER BY amount DESC LIMIT 10) "
                    + "SELECT customer_id, amount FROM ranked_orders")
        );

        assertTrue(cases.size() >= 50, "样例集合必须至少包含 50 类 SQL 形态");
        boolean sawLargeSql = false;
        boolean sawManyLines = false;
        for (MvCase item : cases) {
            Map<String, Object> artifact = artifact(item.sql);
            assertNotNull(artifact, item.name);
            assertEquals(item.mvType, artifact.get("mvType"), item.name + " " + artifact);
            assertFalse("EXACT_QUERY_MV".equals(artifact.get("mvType")), item.name);
            if (item.expectedStatus == null) {
                assertUsableBundle(item, artifact);
            } else {
                assertEquals("BLOCKED", artifact.get("artifactStatus"), item.name + " " + artifact);
                assertTrue(hasCode(maps(artifact.get("blockingReasons")), item.expectedStatus), item.name + " " + artifact);
                assertNull(artifact.get("rewriteSql"), item.name);
            }
            sawLargeSql = sawLargeSql || item.sql.length() > 12000;
            sawManyLines = sawManyLines || item.sql.split("\n", -1).length > 180;
        }
        assertTrue(sawLargeSql, "样例集合必须包含超过 12000 字符的 SQL");
        assertTrue(sawManyLines, "样例集合必须包含超过 180 行的 SQL");
    }

    private void assertUsableBundle(MvCase item, Map<String, Object> artifact) {
        if (item.reviewCode == null) {
            assertEquals("GENERATED", artifact.get("artifactStatus"), item.name + " " + artifact);
            assertTrue(maps(artifact.get("reviewWarnings")).isEmpty(), item.name + " " + artifact);
        } else {
            assertEquals("REVIEW_REQUIRED", artifact.get("artifactStatus"), item.name + " " + artifact);
            assertTrue(hasCode(maps(artifact.get("reviewWarnings")), item.reviewCode), item.name + " " + artifact);
        }
        assertTrue(maps(artifact.get("blockingReasons")).isEmpty(), item.name + " " + artifact);
        assertText(artifact.get("ddlSql"), item.name);
        assertText(artifact.get("refreshSql"), item.name);
        assertText(artifact.get("validationSql"), item.name);
        assertText(artifact.get("rollbackSql"), item.name);
        assertText(artifact.get("rewriteSql"), item.name);
        assertTrue(String.valueOf(artifact.get("ddlSql")).contains("CREATE"), item.name);
        assertTrue(String.valueOf(artifact.get("rewriteSql")).contains("FROM " + artifact.get("mvName")), item.name);
        assertFalse(String.valueOf(artifact.get("rewriteSql")).contains("SELECT * FROM " + artifact.get("mvName")), item.name);
        if ("parameterized-having-alias".equals(item.name)) {
            assertTrue(String.valueOf(artifact.get("rewriteSql")).contains("HAVING SUM(total_amount) > 1000"),
                String.valueOf(artifact.get("rewriteSql")));
        }
    }

    private Map<String, Object> artifact(String sql) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
                "datasource-a",
                "fingerprint-large-quality",
                "sales-daily",
                null,
                null
            ),
            profile
        );
    }

    private static MvCase generated(String name, String mvType, String sql) {
        return new MvCase(name, mvType, sql, null, null);
    }

    private static MvCase generated(String name, String mvType, String sql, String reviewCode) {
        return new MvCase(name, mvType, sql, null, reviewCode);
    }

    private static MvCase blocked(String name, String mvType, String expectedReasonCode, String sql) {
        return new MvCase(name, mvType, sql, expectedReasonCode, null);
    }

    private static String largeParameterizedAggSql(int dimensions) {
        StringBuilder select = new StringBuilder("SELECT\n");
        StringBuilder groupBy = new StringBuilder("GROUP BY\n");
        for (int i = 1; i <= dimensions; i++) {
            String column = "dim_" + i;
            select.append("  ").append(column).append(",\n");
            groupBy.append("  ").append(column).append(i == dimensions ? "\n" : ",\n");
        }
        select.append("  SUM(amount) AS total_amount,\n");
        select.append("  COUNT(*) AS order_count\n");
        return select
            + "FROM orders\n"
            + "WHERE dt = DATE '2026-05-01'\n"
            + "  AND region = 'CN'\n"
            + groupBy.toString();
    }

    private static String largeStarSql(int dimensions) {
        StringBuilder select = new StringBuilder("SELECT\n");
        StringBuilder groupBy = new StringBuilder("GROUP BY\n");
        for (int i = 1; i <= dimensions; i++) {
            String column = i % 2 == 0 ? "p.attr_" + i : "s.attr_" + i;
            select.append("  ").append(column).append(" AS attr_").append(i).append(",\n");
            groupBy.append("  ").append(column).append(",\n");
        }
        select.append("  SUM(o.amount) AS total_amount\n");
        groupBy.append("  p.category,\n  s.city\n");
        return select
            + "FROM orders o\n"
            + "JOIN products p ON o.product_id = p.product_id\n"
            + "JOIN shops s ON o.shop_id = s.shop_id\n"
            + groupBy.toString();
    }

    private static String largeCommonSubgraphSql(int columns) {
        StringBuilder cteSelect = new StringBuilder("WITH wide_orders AS (\nSELECT\n");
        StringBuilder mainSelect = new StringBuilder("SELECT\n");
        for (int i = 1; i <= columns; i++) {
            cteSelect.append("  metric_").append(i).append(",\n");
            mainSelect.append("  metric_").append(i).append(i == columns ? "\n" : ",\n");
        }
        cteSelect.append("  customer_id,\n  amount,\n  region\nFROM orders\nWHERE status = 'PAID'\n)\n");
        return cteSelect.toString()
            + mainSelect.toString()
            + "FROM wide_orders\n"
            + "WHERE region = 'CN'\n"
            + "GROUP BY customer_id";
    }

    private static void assertText(Object value, String name) {
        assertTrue(value != null && String.valueOf(value).trim().length() > 0, name);
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

    private static final class MvCase {
        private final String name;
        private final String mvType;
        private final String sql;
        private final String expectedStatus;
        private final String reviewCode;

        private MvCase(String name, String mvType, String sql, String expectedStatus, String reviewCode) {
            this.name = name;
            this.mvType = mvType;
            this.sql = sql;
            this.expectedStatus = expectedStatus;
            this.reviewCode = reviewCode;
        }
    }
}
