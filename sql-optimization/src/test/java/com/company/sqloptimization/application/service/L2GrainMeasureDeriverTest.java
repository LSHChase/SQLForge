package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2GrainMeasureDeriverTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldExposeGrainDimensionsAndMergeableMeasuresOnArtifact() {
        Map<String, Object> artifact = artifact(
            "SELECT customer_id, SUM(amount) AS total_amount, COUNT(*) AS order_count, "
                + "MIN(pay_time) AS first_pay_time, MAX(pay_time) AS last_pay_time "
                + "FROM orders WHERE dt >= DATE '2026-05-01' AND region = 'CN' "
                + "AND access_domain = 'BI' GROUP BY customer_id"
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertEquals("PARAMETERIZED_AGG_MV", artifact.get("mvType"));
        assertContains(strings(artifact.get("grain")), "customer_id");
        assertContains(strings(artifact.get("grain")), "dt");
        assertContains(strings(artifact.get("grain")), "region");
        assertContains(strings(artifact.get("grain")), "access_domain");
        assertEquals(strings(artifact.get("grain")), strings(artifact.get("dimensions")));

        List<Map<String, Object>> measures = maps(artifact.get("measures"));
        assertMeasure(measures, "total_amount", "SUM", "SUM(amount)", "SUM(total_amount)");
        assertMeasure(measures, "order_count", "COUNT", "COUNT(*)", "SUM(order_count)");
        assertMeasure(measures, "first_pay_time", "MIN", "MIN(pay_time)", "MIN(first_pay_time)");
        assertMeasure(measures, "last_pay_time", "MAX", "MAX(pay_time)", "MAX(last_pay_time)");
        assertEquals(Boolean.TRUE, map(artifact.get("coverage")).get("coversMeasures"));
        assertEquals(Boolean.TRUE, map(artifact.get("coverage")).get("coversSecurity"));
    }

    @Test
    void shouldSplitAvgIntoSumAndCountComponents() {
        Map<String, Object> artifact = artifact(
            "SELECT customer_id, AVG(amount) AS avg_amount FROM orders GROUP BY customer_id"
        );

        Map<String, Object> measure = measureByName(maps(artifact.get("measures")), "avg_amount");

        assertEquals("AVG", measure.get("measureType"));
        assertEquals(Boolean.TRUE, measure.get("mergeable"));
        assertEquals("AVG(amount)", measure.get("sourceExpression"));
        assertEquals("SUM(avg_amount_sum) / NULLIF(SUM(avg_amount_count), 0)", measure.get("rewriteExpression"));
        List<Map<String, Object>> components = maps(measure.get("components"));
        assertEquals(2, components.size());
        assertMeasure(components, "avg_amount_sum", "SUM", "SUM(amount)", "SUM(avg_amount_sum)");
        assertMeasure(components, "avg_amount_count", "COUNT", "COUNT(amount)", "SUM(avg_amount_count)");
    }

    @Test
    void shouldSplitTopLevelRatioIntoNumeratorAndDenominatorComponents() {
        Map<String, Object> artifact = artifact(
            "SELECT customer_id, SUM(pay_amount) / SUM(order_amount) AS pay_rate "
                + "FROM orders GROUP BY customer_id"
        );

        List<Map<String, Object>> measures = maps(artifact.get("measures"));
        assertEquals(1, measures.size());
        Map<String, Object> measure = measures.get(0);
        assertEquals("pay_rate", measure.get("name"));
        assertEquals("RATIO", measure.get("measureType"));
        assertEquals(Boolean.TRUE, measure.get("mergeable"));
        assertEquals("SUM(pay_amount) / SUM(order_amount)", measure.get("sourceExpression"));
        assertEquals(
            "SUM(pay_rate_numerator) / NULLIF(SUM(pay_rate_denominator), 0)",
            measure.get("rewriteExpression")
        );
        List<Map<String, Object>> components = maps(measure.get("components"));
        assertMeasure(components, "pay_rate_numerator", "SUM", "SUM(pay_amount)", "SUM(pay_rate_numerator)");
        assertMeasure(components, "pay_rate_denominator", "SUM", "SUM(order_amount)", "SUM(pay_rate_denominator)");
    }

    @Test
    void shouldPreserveFineTimeColumnForRollupGrain() {
        Map<String, Object> artifact = artifact(
            "SELECT DATE_TRUNC('month', order_date) AS order_month, SUM(amount) AS total_amount "
                + "FROM orders GROUP BY DATE_TRUNC('month', order_date)"
        );

        assertEquals("ROLLUP_MV", artifact.get("mvType"));
        assertContains(strings(artifact.get("grain")), "DATE_TRUNC('month', order_date)");
        assertContains(strings(artifact.get("grain")), "order_date");
        assertEquals(Boolean.TRUE, map(artifact.get("coverage")).get("coversMeasures"));
    }

    @Test
    void shouldGenerateExactCountDistinctByRetainingDistinctKeyInGrain() {
        Map<String, Object> artifact = artifact(
            "SELECT region, COUNT(DISTINCT customer_id) AS unique_customers FROM orders GROUP BY region"
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertContains(strings(artifact.get("grain")), "customer_id");
        assertEquals(Boolean.TRUE, map(artifact.get("coverage")).get("coversMeasures"));
        assertTrue(maps(artifact.get("blockingReasons")).isEmpty());
        assertTrue(String.valueOf(artifact.get("ddlSql")).contains("GROUP BY region, customer_id"));
        assertTrue(String.valueOf(artifact.get("rewriteSql")).contains(
            "COUNT(DISTINCT customer_id) AS unique_customers"
        ));
        Map<String, Object> measure = measureByName(maps(artifact.get("measures")), "unique_customers");
        assertEquals("COUNT_DISTINCT", measure.get("measureType"));
        assertEquals(Boolean.TRUE, measure.get("mergeable"));
        assertEquals("EXACT_DISTINCT_KEY_IN_GRAIN", measure.get("safeReaggregateStrategy"));
    }

    @Test
    void shouldBlockPercentileMedianAndComplexAggregates() {
        assertBlockedMeasure(
            "SELECT region, APPROX_PERCENTILE(amount, 0.95) AS p95_amount FROM orders GROUP BY region",
            "PERCENTILE_MEASURE_NOT_MERGEABLE"
        );
        assertBlockedMeasure(
            "SELECT region, MEDIAN(amount) AS median_amount FROM orders GROUP BY region",
            "MEDIAN_MEASURE_NOT_MERGEABLE"
        );
        assertBlockedMeasure(
            "SELECT region, GROUP_CONCAT(product_id) AS products FROM orders GROUP BY region",
            "COMPLEX_UDAF_MEASURE_NOT_MERGEABLE"
        );
    }

    @Test
    void shouldKeepPredicateClassificationOnBlockedArtifact() {
        Map<String, Object> artifact = artifact(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt >= CURRENT_DATE GROUP BY customer_id"
        );

        assertEquals("BLOCKED", artifact.get("artifactStatus"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), "BLOCKED_UNSTABLE_PREDICATE"));
        assertTrue(hasExpression(maps(artifact.get("blockedPredicates")), "CURRENT_DATE"));
        assertFalse(maps(artifact.get("measures")).isEmpty());
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    private Map<String, Object> artifact(String sql) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                null,
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

    private void assertBlockedMeasure(String sql, String reasonCode) {
        Map<String, Object> artifact = artifact(sql);
        assertEquals("BLOCKED", artifact.get("artifactStatus"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), reasonCode));
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    private static void assertMeasure(List<Map<String, Object>> measures,
                                      String name,
                                      String measureType,
                                      String sourceExpression,
                                      String rewriteExpression) {
        Map<String, Object> measure = measureByName(measures, name);
        assertEquals(measureType, measure.get("measureType"));
        assertEquals(Boolean.TRUE, measure.get("mergeable"));
        assertEquals(sourceExpression, measure.get("sourceExpression"));
        assertEquals(rewriteExpression, measure.get("rewriteExpression"));
    }

    private static Map<String, Object> measureByName(List<Map<String, Object>> measures, String name) {
        for (Map<String, Object> measure : measures) {
            if (name.equals(measure.get("name"))) {
                return measure;
            }
        }
        throw new AssertionError("Missing measure " + name + " in " + measures);
    }

    private static void assertContains(List<String> values, String expected) {
        for (String value : values) {
            if (expected.equals(value)) {
                return;
            }
        }
        throw new AssertionError("Missing value " + expected + " in " + values);
    }

    private static boolean hasReason(List<Map<String, Object>> reasons, String code) {
        for (Map<String, Object> reason : reasons) {
            if (code.equals(reason.get("code"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExpression(List<Map<String, Object>> items, String expected) {
        for (Map<String, Object> item : items) {
            if (String.valueOf(item.get("expression")).contains(expected)) {
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

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        return (List<String>) value;
    }
}
