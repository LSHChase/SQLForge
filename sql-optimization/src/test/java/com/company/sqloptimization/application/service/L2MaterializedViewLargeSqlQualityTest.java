package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class L2MaterializedViewLargeSqlQualityTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldShipOneHundredComplexMvSqlFixturesWithRepresentativePlannerRuns() throws Exception {
        Path corpusDir = repositoryRoot().resolve("sql-optimization/src/test/resources/complex-mv-sql");
        List<Path> fixtures;
        try (Stream<Path> stream = Files.list(corpusDir)) {
            fixtures = stream
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .sorted()
                .collect(Collectors.toList());
        }

        assertEquals(100, fixtures.size(), fixtures.toString());
        int largeFixtureCount = 0;
        for (Path fixture : fixtures) {
            String sql = new String(Files.readAllBytes(fixture), StandardCharsets.UTF_8);
            int lineCount = sql.split("\n", -1).length - 1;
            int byteCount = sql.getBytes(StandardCharsets.UTF_8).length;
            assertTrue(lineCount >= 1316, fixture + " lineCount=" + lineCount);
            assertTrue(byteCount >= 79467, fixture + " byteCount=" + byteCount);
            assertTrue(sql.contains("-- nested_layers=10"), fixture.toString());
            if (lineCount >= 10000 || byteCount >= 100000) {
                largeFixtureCount++;
            }
        }
        assertTrue(largeFixtureCount >= 20, "largeFixtureCount=" + largeFixtureCount);

        for (Path fixture : representativeFixtures(fixtures)) {
            String sql = new String(Files.readAllBytes(fixture), StandardCharsets.UTF_8);
            SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
            Map<String, Object> artifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
                new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                    sql,
                    "HETU",
                    "datasource-corpus",
                    "fingerprint-" + fixture.getFileName().toString(),
                    "CORPUS",
                    "complex-mv-corpus",
                    null
                ),
                profile
            );
            assertNotNull(artifact, fixture.toString());
            assertTrue(String.valueOf(artifact.get("candidateId")).startsWith("mv_candidate_"), fixture.toString());
            assertTrue(String.valueOf(artifact.get("coverageProof")).contains("MV_COVERAGE_PROOF_ENGINE_V1"),
                fixture.toString());
            assertTrue(String.valueOf(artifact.get("metadataEvidence")).contains("METADATA_PARTIAL"),
                fixture.toString());
            assertFalse("EXACT_QUERY_MV".equals(artifact.get("mvType")), fixture.toString());
        }
    }

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

    @Test
    void shouldUseCalciteProjectionLineageForYonghongAliasSnapshotFields() {
        Map<String, Object> artifact = artifact(calciteAliasLineageSnapshotSql());

        assertNotNull(artifact);
        assertEquals("COMMON_SUBGRAPH_MV", artifact.get("mvType"), String.valueOf(artifact));
        assertEquals("GENERATED", artifact.get("artifactStatus"), String.valueOf(artifact));
        assertTrue(maps(artifact.get("blockingReasons")).isEmpty(), String.valueOf(artifact));
        assertTrue(String.valueOf(artifact.get("commonSubgraphEvidence"))
            .contains("CALCITE_AST_QBDAG_STRUCTURAL_REUSE"), String.valueOf(artifact));
        assertTrue(String.valueOf(artifact.get("commonSubgraphEvidence")).contains("sourceName=s"),
            String.valueOf(artifact.get("commonSubgraphEvidence")));
        assertTrue(String.valueOf(artifact.get("ddlSql")).contains("asset_value AS"),
            String.valueOf(artifact.get("ddlSql")));
        assertTrue(String.valueOf(artifact.get("rewriteSql")).contains("FROM " + artifact.get("mvName")),
            String.valueOf(artifact.get("rewriteSql")));
        assertFalse(String.valueOf(artifact.get("rewriteSql")).contains("FROM ledger_daily_assets"),
            String.valueOf(artifact.get("rewriteSql")));
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

    private List<Path> representativeFixtures(List<Path> fixtures) {
        return Arrays.asList(
            findFixture(fixtures, "_star_agg_"),
            findFixture(fixtures, "_prejoin_"),
            findFixture(fixtures, "_rollup_"),
            findFixture(fixtures, "_common_subgraph_"),
            findFixture(fixtures, "_blocked_complex_"),
            findFixture(fixtures, "_mixed_extreme_")
        );
    }

    private Path findFixture(List<Path> fixtures, String token) {
        for (Path fixture : fixtures) {
            if (fixture.getFileName().toString().contains(token)) {
                return fixture;
            }
        }
        throw new AssertionError("Missing fixture token " + token);
    }

    private Path repositoryRoot() {
        Path root = Paths.get("").toAbsolutePath();
        if (!Files.exists(root.resolve("docs/test01.sql"))) {
            root = root.resolve("..").normalize();
        }
        return root;
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

    private static String calciteAliasLineageSnapshotSql() {
        String first = snapshotCountSubquery("a", "20260430", "1000000");
        String second = snapshotCountSubquery("b", "20260519", "1000000");
        String third = snapshotCountSubquery("c", "20260430", "6000000");
        String fourth = snapshotCountSubquery("d", "20260519", "6000000");
        String fifth = snapshotCountSubquery("e", "20260519", "1000000");
        return "SELECT a.\"机构编码__第二层时点机构号\", a.\"机构编码__第二层机构简称\", a.\"org\",\n"
            + "  a.cnt AS \"基期100\",\n"
            + "  b.cnt AS \"当期100\",\n"
            + "  e.cnt - a.cnt AS \"新增100\",\n"
            + "  d.cnt - c.cnt AS \"新增100-600\",\n"
            + "  b.cnt - a.cnt AS \"Sum_增量100\"\n"
            + "FROM " + first + "\n"
            + "LEFT JOIN " + second + " ON a.\"机构编码__第二层时点机构号\" = b.\"机构编码__第二层时点机构号\"\n"
            + "LEFT JOIN " + third + " ON a.\"机构编码__第二层时点机构号\" = c.\"机构编码__第二层时点机构号\"\n"
            + "LEFT JOIN " + fourth + " ON a.\"机构编码__第二层时点机构号\" = d.\"机构编码__第二层时点机构号\"\n"
            + "LEFT JOIN " + fifth + " ON a.\"机构编码__第二层时点机构号\" = e.\"机构编码__第二层时点机构号\"\n"
            + "ORDER BY a.\"org\"";
    }

    private static String snapshotCountSubquery(String alias, String date, String threshold) {
        return "(\n"
            + "  SELECT s.\"机构编码__第二层时点机构号\", s.\"机构编码__第二层机构简称\", s.\"org\",\n"
            + "    COUNT(DISTINCT s.\"客户编号\") AS cnt\n"
            + "  FROM (\n"
            + "    SELECT\n"
            + "      dept_l0_cd AS \"机构编码__零层时点机构号\",\n"
            + "      dept_l1_cd AS \"机构编码__第一层时点机构号\",\n"
            + "      dept_l2_cd AS \"机构编码__第二层时点机构号\",\n"
            + "      dept_l3_cd AS \"机构编码__第三层时点机构号\",\n"
            + "      dept_l4_cd AS \"机构编码__第四层时点机构号\",\n"
            + "      dept_l2_nm AS \"机构编码__第二层机构简称\",\n"
            + "      dept_l3_nm AS \"机构编码__第三层机构简称\",\n"
            + "      dept_l4_nm AS \"机构编码__第四层机构简称\",\n"
            + "      dept_hier_rank AS \"机构层级\",\n"
            + "      acct_no AS \"客户编号\",\n"
            + "      book_day AS \"数据日期\",\n"
            + "      asset_value AS \"月日均AUM\",\n"
            + "      '深圳市分行' AS \"org\"\n"
            + "    FROM ledger_daily_assets\n"
            + "    WHERE dept_hier_rank = 4\n"
            + "      AND (dept_l2_cd = '41H006' OR dept_l3_cd = '41H006' OR dept_l4_cd = '41H006')\n"
            + "      AND (book_day = '20260430' OR book_day = '20260519')\n"
            + "    GROUP BY dept_l0_cd, dept_l1_cd, dept_l2_cd, dept_l3_cd, dept_l4_cd,\n"
            + "      dept_l2_nm, dept_l3_nm, dept_l4_nm, dept_hier_rank, acct_no, book_day, asset_value\n"
            + "  ) s\n"
            + "  WHERE s.\"数据日期\" = '" + date + "'\n"
            + "    AND s.\"月日均AUM\" >= " + threshold + "\n"
            + "  GROUP BY s.\"机构编码__第二层时点机构号\", s.\"机构编码__第二层机构简称\", s.\"org\"\n"
            + ") " + alias;
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
