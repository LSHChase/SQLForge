package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2MaterializedViewRewriteCoverageValidatorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGateSuccessfulRewriteCoverageForAllAdvancedMvTypes() {
        assertGeneratedCoverage(artifact(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt = DATE '2026-05-01' AND region = 'CN' GROUP BY customer_id"
        ));
        assertGeneratedCoverage(artifact(
            "SELECT c.customer_level, SUM(o.amount) AS total_amount "
                + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                + "WHERE o.dt = DATE '2026-05-01' GROUP BY c.customer_level"
        ));
        assertGeneratedCoverage(artifact(
            "SELECT o.dt, p.category, SUM(o.amount) AS total_amount "
                + "FROM orders o "
                + "JOIN products p ON o.product_id = p.product_id "
                + "JOIN shops s ON o.shop_id = s.shop_id "
                + "WHERE o.dt = DATE '2026-05-01' AND s.city = 'HZ' "
                + "GROUP BY o.dt, p.category"
        ));
        assertGeneratedCoverage(artifact(
            "SELECT DATE_TRUNC('month', order_date) AS order_month, region, SUM(amount) AS total_amount "
                + "FROM orders WHERE order_date BETWEEN DATE '2026-05-01' AND DATE '2026-05-31' "
                + "AND region = 'CN' GROUP BY DATE_TRUNC('month', order_date), region"
        ));
        assertGeneratedCoverage(artifact(
            "WITH recent_orders AS (SELECT customer_id, amount, region FROM orders WHERE status = 'PAID') "
                + "SELECT customer_id, SUM(amount) AS total_amount FROM recent_orders "
                + "WHERE region = 'CN' GROUP BY customer_id"
        ));
    }

    @Test
    void shouldBlockProjectionFilterGroupingMeasureAndSecurityCoverageGaps() {
        ValidationResultData projectionGap = validate(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id",
            "SELECT SUM(total_amount) AS total_amount FROM mv_sales;",
            Arrays.asList("total_amount"),
            Collections.<String>emptyList()
        );
        assertBlocked(projectionGap.result, L2MaterializedViewRewriteCoverageValidator.REWRITE_PROJECTION_NOT_COVERED);

        ValidationResultData filterGap = validate(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE region = 'CN' GROUP BY customer_id",
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales "
                + "WHERE region = 'CN' GROUP BY customer_id;",
            Arrays.asList("customer_id", "total_amount"),
            Collections.<String>emptyList()
        );
        assertBlocked(filterGap.result, L2MaterializedViewRewriteCoverageValidator.REWRITE_FILTER_NOT_COVERED);

        ValidationResultData groupingGap = validate(
            "SELECT SUM(amount) AS total_amount FROM orders GROUP BY customer_id",
            "SELECT SUM(total_amount) AS total_amount FROM mv_sales GROUP BY customer_id;",
            Arrays.asList("total_amount"),
            Collections.<String>emptyList()
        );
        assertBlocked(groupingGap.result, L2MaterializedViewRewriteCoverageValidator.REWRITE_GROUPING_NOT_COVERED);

        ValidationResultData measureGap = validate(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id",
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales GROUP BY customer_id;",
            Arrays.asList("customer_id"),
            Collections.<String>emptyList()
        );
        assertBlocked(measureGap.result, L2MaterializedViewRewriteCoverageValidator.REWRITE_MEASURE_NOT_COVERED);

        ValidationResultData securityGap = validate(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE access_domain = 'BI' GROUP BY customer_id",
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales "
                + "WHERE access_domain = 'BI' GROUP BY customer_id;",
            Arrays.asList("customer_id", "total_amount"),
            Collections.<String>emptyList()
        );
        assertBlocked(securityGap.result, L2MaterializedViewRewriteCoverageValidator.REWRITE_SECURITY_PREDICATE_NOT_COVERED);
    }

    @Test
    void shouldRejectNonReadonlyAndOriginalSourceRewriteSql() {
        assertBlocked(readonlyValidation("SELECT * FROM mv_sales; INSERT INTO audit_log SELECT * FROM mv_sales;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("INSERT INTO target SELECT * FROM mv_sales;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("UPDATE mv_sales SET amount = 0;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("DELETE FROM mv_sales;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("CREATE TABLE t AS SELECT * FROM mv_sales;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("DROP TABLE mv_sales;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("CALL refresh_mv('mv_sales');"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);
        assertBlocked(readonlyValidation("-- leading comment\nDROP TABLE mv_sales;"),
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY);

        ValidationResultData trailingSemicolon = validate(
            "SELECT customer_id FROM orders GROUP BY customer_id",
            "/* allowed */ SELECT customer_id FROM mv_sales;",
            Arrays.asList("customer_id"),
            Collections.<String>emptyList()
        );
        assertTrue(trailingSemicolon.result.isGenerated(), String.valueOf(trailingSemicolon.result.getBlockingReasons()));
        assertEquals("/* allowed */ SELECT customer_id FROM mv_sales;", trailingSemicolon.result.getRewriteSql());

        ValidationResultData replacementFailure = validate(
            "SELECT d.customer_id FROM (SELECT customer_id FROM orders) d",
            "SELECT d.customer_id FROM (SELECT customer_id FROM orders) d;",
            Arrays.asList("customer_id"),
            Collections.<String>emptyList()
        );
        assertBlocked(replacementFailure.result,
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_MV_REFERENCE_REQUIRED);
        assertBlocked(replacementFailure.result,
            L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE);
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

    private ValidationResultData validate(String sourceSql,
                                          String rewriteSql,
                                          List<String> mvFields,
                                          List<String> additionalReferences) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sourceSql, DataSourceTypeEnum.HETU);
        Map<String, Object> advancedProfile = profile.toAdvancedStructureProfile();
        L2PredicateClassifier.PredicateClassificationResult classification =
            L2PredicateClassifier.classify(advancedProfile);
        L2GrainMeasureDeriver.DerivationResult derivation =
            L2GrainMeasureDeriver.derive(advancedProfile, classification);
        return new ValidationResultData(L2MaterializedViewRewriteCoverageValidator.validate(
            new L2MaterializedViewRewriteCoverageValidator.ValidationInput(
                sourceSql,
                derivation.getMvType(),
                "mv_sales",
                rewriteSql,
                advancedProfile,
                classification,
                derivation.getMeasures(),
                mvFields,
                additionalReferences
            )
        ));
    }

    private L2MaterializedViewRewriteCoverageValidator.ValidationResult readonlyValidation(String rewriteSql) {
        ValidationResultData data = validate(
            "SELECT customer_id FROM orders GROUP BY customer_id",
            rewriteSql,
            Arrays.asList("customer_id"),
            Collections.<String>emptyList()
        );
        return data.result;
    }

    private static void assertGeneratedCoverage(Map<String, Object> artifact) {
        assertEquals("GENERATED", artifact.get("artifactStatus"), String.valueOf(artifact));
        Map<String, Object> coverage = map(artifact.get("coverage"));
        assertEquals(Boolean.TRUE, coverage.get("coversProjection"));
        assertEquals(Boolean.TRUE, coverage.get("coversFilters"));
        assertEquals(Boolean.TRUE, coverage.get("coversGrouping"));
        assertEquals(Boolean.TRUE, coverage.get("coversMeasures"));
        assertEquals(Boolean.TRUE, coverage.get("coversSecurity"));
        assertEquals(Boolean.TRUE, coverage.get("rewriteSqlReadonly"));
        assertEquals(Boolean.TRUE, coverage.get("rewriteSqlReferencesMv"));
        assertEquals(Boolean.TRUE, coverage.get("rewriteSqlAvoidsOriginalSources"));
        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("FROM " + artifact.get("mvName")), rewriteSql);
        assertFalse(rewriteSql.contains("FROM orders"), rewriteSql);
    }

    private static void assertBlocked(L2MaterializedViewRewriteCoverageValidator.ValidationResult result,
                                      String code) {
        assertFalse(result.isGenerated());
        assertNull(result.getRewriteSql());
        assertTrue(hasReason(result.getBlockingReasons(), code), String.valueOf(result.getBlockingReasons()));
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
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class ValidationResultData {
        private final L2MaterializedViewRewriteCoverageValidator.ValidationResult result;

        private ValidationResultData(L2MaterializedViewRewriteCoverageValidator.ValidationResult result) {
            this.result = result;
        }
    }
}
