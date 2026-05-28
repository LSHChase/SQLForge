package com.company.sqlforge.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SqlFingerprintUtilsTest {

    @Test
    void shouldIgnoreCommentsWhitespaceCaseAndLiteralValues() {
        String first = "--report_code=RPT_A\n"
            + "SELECT * FROM sales.orders WHERE dt = DATE '2026-04-01' AND tenant_id = 7;";
        String second = "/* imported sql */ select  *\n"
            + "from sales.orders where dt = DATE '2026-04-02' and tenant_id = 8";

        assertEquals(
            SqlFingerprintUtils.fingerprint(first),
            SqlFingerprintUtils.fingerprint(second)
        );
    }

    @Test
    void shouldKeepCommentMarkersInsideStringLiteralsSafe() {
        String first = "SELECT * FROM logs WHERE message = '--not-comment' AND id = 1";
        String second = "select * from logs where message = '/*not-comment*/' and id = 2";

        assertEquals(
            SqlFingerprintUtils.normalizeForFingerprint(first),
            SqlFingerprintUtils.normalizeForFingerprint(second)
        );
    }

    @Test
    void shouldNormalizeNamedParametersAndQuestionMarkParametersTogether() {
        String named = "SELECT * FROM orders WHERE tenant_id = :tenantId AND amount >= :minAmount";
        String positional = "select * from orders where tenant_id = ? and amount >= ?";

        assertEquals(
            SqlFingerprintUtils.fingerprint(named),
            SqlFingerprintUtils.fingerprint(positional)
        );
    }

    @Test
    void shouldFingerprintBiViewAndHetuCatalogQualifiersTogether() {
        String viewCatalog = "SELECT * FROM BI_SALES_V.orders WHERE tenant_id = 1";
        String hetuCatalog = "select * from BI_SALES_HETU.orders where tenant_id = 2";

        assertEquals(
            SqlFingerprintUtils.fingerprint(viewCatalog),
            SqlFingerprintUtils.fingerprint(hetuCatalog)
        );
    }

    @Test
    void shouldIgnoreFormatterWhitespaceAroundOperatorsAndPunctuation() {
        String compact = "SELECT SUM(a-b) AS delta FROM orders WHERE tenant_id=7 AND amount>=100";
        String formatted = "SELECT\n"
            + "  SUM( a - b ) AS delta\n"
            + "FROM orders\n"
            + "WHERE tenant_id = 8\n"
            + "  AND amount >= 200";

        assertEquals(
            SqlFingerprintUtils.fingerprint(compact),
            SqlFingerprintUtils.fingerprint(formatted)
        );
    }

    @Test
    void shouldKeepDifferentQueryShapesApart() {
        String byTenant = "SELECT * FROM orders WHERE tenant_id = 1";
        String byStatus = "SELECT * FROM orders WHERE status = 'PAID'";

        assertNotEquals(
            SqlFingerprintUtils.fingerprint(byTenant),
            SqlFingerprintUtils.fingerprint(byStatus)
        );
    }
}
