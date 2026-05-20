package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2StarAggMvCandidateGeneratorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGenerateStarAggregateMvAndRewriteDimensionSliceOnMvOnly() {
        String sql = "SELECT o.dt, p.category, SUM(o.amount) AS total_amount, COUNT(*) AS order_count "
            + "FROM orders o "
            + "JOIN products p ON o.product_id = p.product_id "
            + "JOIN shops s ON o.shop_id = s.shop_id "
            + "WHERE o.dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
            + "AND s.city = 'HZ' AND o.status = 'PAID' "
            + "GROUP BY o.dt, p.category";
        Map<String, Object> artifact = artifact(sql);

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("STAR_AGG_MV", artifact.get("mvType"));
        assertEquals("orders", map(artifact.get("factTable")).get("tableName"));
        assertEquals("o", map(artifact.get("factTable")).get("alias"));
        assertTrue(hasTable(maps(artifact.get("dimensionTables")), "products"));
        assertTrue(hasTable(maps(artifact.get("dimensionTables")), "shops"));
        assertTrue(maps(artifact.get("joinKeys")).size() >= 2);
        assertTrue(hasDimension(maps(artifact.get("dimensionSources")), "p.category", "category"));
        assertTrue(hasDimension(maps(artifact.get("dimensionSources")), "s.city", "city"));
        assertTrue(hasMeasure(maps(artifact.get("measureSources")), "total_amount"));
        assertEquals("MEASURE_SOURCE_AND_JOIN_TOPOLOGY", map(artifact.get("starSchemaEvidence")).get("factInference"));

        String mvName = String.valueOf(artifact.get("mvName"));
        String ddlSql = String.valueOf(artifact.get("ddlSql"));
        assertTrue(ddlSql.contains("CREATE MATERIALIZED VIEW " + mvName + " AS"));
        assertTrue(ddlSql.contains("FROM orders o"));
        assertTrue(ddlSql.contains("JOIN products p ON"));
        assertTrue(ddlSql.contains("JOIN shops s ON"));
        assertTrue(ddlSql.contains("o.dt AS dt"));
        assertTrue(ddlSql.contains("p.category AS category"));
        assertTrue(ddlSql.contains("s.city AS city"));
        assertTrue(ddlSql.contains("SUM(o.amount) AS total_amount"));
        assertTrue(ddlSql.contains("COUNT(*) AS order_count"));
        assertTrue(ddlSql.contains("WHERE o.status = 'PAID'"));
        assertTrue(ddlSql.contains("GROUP BY o.dt, p.category, s.city"));
        assertFalse(ddlSql.contains(sql));

        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("FROM " + mvName));
        assertTrue(rewriteSql.contains("dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31'"));
        assertTrue(rewriteSql.contains("city = 'HZ'"));
        assertTrue(rewriteSql.contains("SUM(total_amount) AS total_amount"));
        assertTrue(rewriteSql.contains("SUM(order_count) AS order_count"));
        assertTrue(rewriteSql.contains("GROUP BY dt, category"));
        assertFalse(rewriteSql.contains("JOIN products"));
        assertFalse(rewriteSql.contains("JOIN shops"));
        assertFalse(rewriteSql.contains("orders o"));

        String validationSql = String.valueOf(artifact.get("validationSql"));
        assertTrue(validationSql.contains("original_result"));
        assertTrue(validationSql.contains("rewrite_result"));
        assertTrue(validationSql.contains("ROW_COUNT_CHECK"));
        assertTrue(validationSql.contains("JOIN_ROW_COUNT_CHECK"));
        assertTrue(validationSql.contains("MEASURE_DIFF"));
        assertTrue(validationSql.contains("GROUP_MEASURE_DIFF"));
        assertTrue(validationSql.contains("GROUP_KEY_DIFF"));
    }

    @Test
    void shouldKeepSingleJoinAggregateOnPrejoinPath() {
        Map<String, Object> artifact = artifact("SELECT c.customer_level, SUM(o.amount) AS total_amount "
            + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
            + "GROUP BY c.customer_level");

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("PREJOIN_MV", artifact.get("mvType"));
        assertTrue(String.valueOf(artifact.get("rewriteSql")).contains("FROM " + artifact.get("mvName")));
    }

    @Test
    void shouldBlockWhenFactTableCannotBeIdentified() {
        assertBlocked(
            artifact("SELECT c.customer_level, r.region_name, SUM(amount) AS total_amount "
                + "FROM orders o "
                + "JOIN customers c ON o.customer_id = c.customer_id "
                + "JOIN regions r ON c.region_id = r.region_id "
                + "GROUP BY c.customer_level, r.region_name"),
            "FACT_TABLE_UNRESOLVED"
        );
    }

    @Test
    void shouldBlockOuterAndNonEquiJoinForms() {
        assertBlocked(
            artifact("SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                + "FROM orders o "
                + "LEFT JOIN products p ON o.product_id = p.product_id "
                + "JOIN shops s ON o.shop_id = s.shop_id "
                + "GROUP BY p.category, s.city"),
            "OUTER_JOIN_STAR_AGG_UNSUPPORTED"
        );
        assertBlocked(
            artifact("SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                + "FROM orders o "
                + "JOIN products p ON o.amount > p.min_amount "
                + "JOIN shops s ON o.shop_id = s.shop_id "
                + "GROUP BY p.category, s.city"),
            "NON_EQUI_JOIN_STAR_AGG_UNSUPPORTED"
        );
    }

    @Test
    void shouldBlockFactNonTimeGroupByGrain() {
        assertBlocked(
            artifact("SELECT o.customer_id, p.category, SUM(o.amount) AS total_amount "
                + "FROM orders o "
                + "JOIN products p ON o.product_id = p.product_id "
                + "JOIN shops s ON o.shop_id = s.shop_id "
                + "GROUP BY o.customer_id, p.category"),
            "STAR_AGG_GRAIN_NOT_COVERED"
        );
    }

    @Test
    void shouldBlockNonMergeableMeasuresBeforeSqlGeneration() {
        Map<String, Object> artifact = artifact("SELECT p.category, s.city, COUNT(DISTINCT o.customer_id) AS users "
            + "FROM orders o "
            + "JOIN products p ON o.product_id = p.product_id "
            + "JOIN shops s ON o.shop_id = s.shop_id "
            + "GROUP BY p.category, s.city");

        assertEquals("BLOCKED", artifact.get("artifactStatus"));
        assertEquals("STAR_AGG_MV", artifact.get("mvType"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), "COUNT_DISTINCT_MEASURE_NOT_MERGEABLE"));
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    @Test
    void shouldBlockWhenSecurityPredicateCannotBeMappedToMvDimension() {
        assertBlocked(
            artifact("SELECT p.category, s.city, SUM(o.amount) AS total_amount "
                + "FROM orders o "
                + "JOIN products p ON o.product_id = p.product_id "
                + "JOIN shops s ON o.shop_id = s.shop_id "
                + "WHERE access_domain = 'BI' "
                + "GROUP BY p.category, s.city"),
            "STAR_AGG_SECURITY_PREDICATE_NOT_COVERED"
        );
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
        assertEquals("STAR_AGG_MV", artifact.get("mvType"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), reasonCode));
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    private static boolean hasTable(List<Map<String, Object>> tables, String tableName) {
        for (Map<String, Object> table : tables) {
            if (tableName.equals(table.get("tableName"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDimension(List<Map<String, Object>> dimensions, String sourceExpression, String mvColumn) {
        for (Map<String, Object> dimension : dimensions) {
            if (sourceExpression.equals(dimension.get("sourceExpression")) && mvColumn.equals(dimension.get("mvColumn"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMeasure(List<Map<String, Object>> measures, String name) {
        for (Map<String, Object> measure : measures) {
            if (name.equals(measure.get("name"))) {
                return true;
            }
        }
        return false;
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
