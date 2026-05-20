package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2PrejoinMvCandidateGeneratorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGeneratePrejoinWideTableAndRewriteAggregatesOnMvOnly() {
        String sql = "SELECT c.customer_level, SUM(o.amount) AS total_amount "
            + "FROM orders o "
            + "JOIN customers c ON o.customer_id = c.customer_id "
            + "WHERE o.dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
            + "GROUP BY c.customer_level";
        Map<String, Object> artifact = artifact(sql);

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("PREJOIN_MV", artifact.get("mvType"));
        assertTrue(hasWarning(maps(artifact.get("reviewWarnings")), "ROW_AMPLIFICATION_METADATA_MISSING"));
        assertEquals("REVIEW_REQUIRED", map(artifact.get("rowAmplificationRisk")).get("status"));
        assertFalse(maps(artifact.get("joinKeys")).isEmpty());
        assertTrue(hasMapping(maps(artifact.get("fieldMappings")), "c.customer_level", "customer_level"));

        String mvName = String.valueOf(artifact.get("mvName"));
        String ddlSql = String.valueOf(artifact.get("ddlSql"));
        assertTrue(ddlSql.contains("CREATE MATERIALIZED VIEW " + mvName + " AS"));
        assertTrue(ddlSql.contains("FROM orders o"));
        assertTrue(ddlSql.contains("JOIN customers c ON"));
        assertTrue(ddlSql.contains("o.customer_id = c.customer_id"));
        assertTrue(ddlSql.contains("c.customer_level AS customer_level"));
        assertTrue(ddlSql.contains("o.amount AS amount"));
        assertTrue(ddlSql.contains("o.dt AS dt"));
        assertFalse(ddlSql.contains("GROUP BY"));
        assertFalse(ddlSql.contains("SUM(o.amount)"));

        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("FROM " + mvName));
        assertTrue(rewriteSql.contains("SUM(amount) AS total_amount"));
        assertTrue(rewriteSql.contains("dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31'"));
        assertTrue(rewriteSql.contains("GROUP BY customer_level"));
        assertFalse(rewriteSql.contains("JOIN customers"));
        assertFalse(rewriteSql.contains("orders o"));
    }

    @Test
    void shouldReuseSamePrejoinDdlShapeWhenParametersChange() {
        String maySql = "SELECT c.customer_level, SUM(o.amount) AS total_amount "
            + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
            + "WHERE o.dt BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
            + "AND c.region = 'CN' GROUP BY c.customer_level";
        String juneSql = "SELECT c.customer_level, SUM(o.amount) AS total_amount "
            + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
            + "WHERE o.dt BETWEEN DATE '2026-06-01' AND DATE '2026-06-30' "
            + "AND c.region = 'US' GROUP BY c.customer_level";

        Map<String, Object> mayArtifact = artifact(maySql);
        Map<String, Object> juneArtifact = artifact(juneSql);

        assertEquals("GENERATED", mayArtifact.get("artifactStatus"));
        assertEquals("PREJOIN_MV", mayArtifact.get("mvType"));
        assertEquals(mayArtifact.get("ddlSql"), juneArtifact.get("ddlSql"));
        assertFalse(String.valueOf(mayArtifact.get("ddlSql")).contains("DATE '2026-05-01'"));
        assertFalse(String.valueOf(mayArtifact.get("ddlSql")).contains("c.region = 'CN'"));
        assertTrue(String.valueOf(mayArtifact.get("rewriteSql")).contains("region = 'CN'"));
        assertTrue(String.valueOf(juneArtifact.get("rewriteSql")).contains("region = 'US'"));
    }

    @Test
    void shouldBlockUnsafeJoinFormsWithoutPublishableSql() {
        assertBlocked(
            artifact("SELECT c.customer_level, SUM(o.amount) AS total_amount "
                + "FROM orders o CROSS JOIN customers c GROUP BY c.customer_level"),
            "CROSS_JOIN_PREJOIN_UNSUPPORTED"
        );
        assertBlocked(
            artifact("SELECT c.customer_level, SUM(o.amount) AS total_amount "
                + "FROM orders o LEFT JOIN customers c ON o.customer_id = c.customer_id "
                + "GROUP BY c.customer_level"),
            "OUTER_JOIN_PREJOIN_UNSUPPORTED"
        );
        assertBlocked(
            artifact("SELECT c.customer_level, SUM(o.amount) AS total_amount "
                + "FROM orders o JOIN customers c ON o.amount > c.credit_limit "
                + "GROUP BY c.customer_level"),
            "NON_EQUI_JOIN_PREJOIN_UNSUPPORTED"
        );
        assertBlocked(
            artifact("SELECT c.customer_level, SUM(o.amount) AS total_amount "
                + "FROM orders o JOIN customers c ON CAST(o.customer_id AS VARCHAR) = c.customer_id "
                + "GROUP BY c.customer_level"),
            "COMPLEX_JOIN_KEY_PREJOIN_UNSUPPORTED"
        );
        assertBlocked(
            artifact("SELECT customer_id, SUM(amount) AS total_amount "
                + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                + "GROUP BY customer_id"),
            "FIELD_AMBIGUITY_UNRESOLVED"
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
        assertEquals("PREJOIN_MV", artifact.get("mvType"));
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

    private static boolean hasWarning(List<Map<String, Object>> warnings, String code) {
        for (Map<String, Object> warning : warnings) {
            if (code.equals(warning.get("code"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMapping(List<Map<String, Object>> mappings, String source, String target) {
        for (Map<String, Object> mapping : mappings) {
            if (source.equals(mapping.get("sourceColumn")) && target.equals(mapping.get("mvColumn"))) {
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
