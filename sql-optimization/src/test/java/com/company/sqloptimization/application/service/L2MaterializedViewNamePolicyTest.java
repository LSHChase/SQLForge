package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import org.junit.jupiter.api.Test;

class L2MaterializedViewNamePolicyTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldBuildNameFromReportTableTypeDimensionAndFingerprintHash() {
        String sql = "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id";

        String mvName = mvName(sql, "fingerprint-001", "sales-daily", null);

        assertEquals("mv_sales_daily_orders_paramagg_customer_id_hfingerpr", mvName);
        assertTrue(mvName.matches("[a-z0-9_]+"));
        assertTrue(mvName.length() <= 63);
    }

    @Test
    void shouldKeepHashSuffixWhenLongNameIsTruncated() {
        String sql = "SELECT customer_id, region, SUM(amount) AS total_amount "
            + "FROM orders GROUP BY customer_id, region";

        String mvName = mvName(
            sql,
            "123456789abcdef",
            "sales-daily",
            "very-long-logical-object-key-for-monthly-sales-rollup-across-many-business-units"
        );

        assertTrue(mvName.length() <= 63);
        assertTrue(mvName.matches("[a-z0-9_]+"));
        assertTrue(mvName.endsWith("_h12345678"));
    }

    @Test
    void shouldAvoidCollisionForSameLogicalObjectWithDifferentSqlOrDimensions() {
        String customerSql = "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id";
        String regionSql = "SELECT region, SUM(amount) AS total_amount FROM orders GROUP BY region";

        String customerMvName = mvName(customerSql, "aaaaaaaa1111", "sales-daily", null);
        String regionMvName = mvName(regionSql, "bbbbbbbb2222", "sales-daily", null);

        assertNotEquals(customerMvName, regionMvName);
        assertTrue(customerMvName.contains("customer_id"));
        assertTrue(regionMvName.contains("region"));
    }

    @Test
    void shouldFallbackToSourceSqlShaWhenFingerprintIsMissing() {
        String sql = "SELECT region, SUM(amount) AS total_amount FROM orders GROUP BY region";

        String mvName = mvName(sql, null, "sales-daily", null);

        assertTrue(mvName.matches("mv_sales_daily_orders_paramagg_region_h[0-9a-f]{8}"));
    }

    private String mvName(String sql, String sqlFingerprint, String reportCode, String logicalObjectKey) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        L2PredicateClassifier.PredicateClassificationResult predicateClassification =
            L2PredicateClassifier.classify(profile.toAdvancedStructureProfile());
        L2GrainMeasureDeriver.DerivationResult derivation =
            L2GrainMeasureDeriver.derive(profile.toAdvancedStructureProfile(), predicateClassification);
        return L2MaterializedViewNamePolicy.mvName(
            logicalObjectKey,
            reportCode,
            sqlFingerprint,
            sql,
            profile,
            derivation
        );
    }
}
