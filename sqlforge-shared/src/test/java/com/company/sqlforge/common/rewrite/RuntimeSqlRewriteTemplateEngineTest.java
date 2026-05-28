package com.company.sqlforge.common.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuntimeSqlRewriteTemplateEngineTest {

    @Test
    void shouldReplayCurrentParametersAndAdditionalConditions() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT * FROM orders WHERE tenant_id = 1 AND status = 'PAID'",
            "SELECT id, status FROM orders WHERE tenant_id = 1 AND status = 'PAID'",
            "SELECT * FROM orders WHERE tenant_id = 8 AND status = 'CANCELLED' AND dt = '2026-05-22'"
        );

        assertTrue(result.isApplied());
        assertEquals(
            "SELECT id, status FROM orders WHERE tenant_id = 8 AND status = 'CANCELLED' AND dt = '2026-05-22'",
            result.getRewrittenSql()
        );
        assertTrue(result.getProgramJson().contains("template-replay-v1"));
    }

    @Test
    void shouldReplayCurrentConditionDeletion() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT * FROM orders WHERE tenant_id = 1 AND status = 'PAID'",
            "SELECT id FROM orders WHERE tenant_id = 1 AND status = 'PAID'",
            "SELECT * FROM orders WHERE tenant_id = 8"
        );

        assertTrue(result.isApplied());
        assertEquals("SELECT id FROM orders WHERE tenant_id = 8", result.getRewrittenSql());
    }

    @Test
    void shouldReplayFormatterEquivalentSqlByCanonicalFingerprint() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT SUM(a-b) AS delta FROM orders WHERE tenant_id=1 AND amount>=100",
            "WITH metric AS (SELECT SUM(a-b) AS delta FROM orders WHERE tenant_id=1 AND amount>=100) "
                + "SELECT delta FROM metric",
            "SELECT\n"
                + "  SUM( a - b ) AS delta\n"
                + "FROM orders\n"
                + "WHERE tenant_id = 2\n"
                + "  AND amount >= 200"
        );

        assertTrue(result.isApplied());
        assertTrue(result.getRewrittenSql().contains("tenant_id=2"));
        assertTrue(result.getRewrittenSql().contains("amount>=200"));
    }

    @Test
    void shouldRejectDifferentSourceShape() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT * FROM orders WHERE tenant_id = 1",
            "SELECT id FROM orders WHERE tenant_id = 1",
            "SELECT * FROM payments WHERE tenant_id = 1"
        );

        assertFalse(result.isApplied());
        assertEquals("SOURCE_HEAD_MISMATCH", result.getFailureReason());
    }

    @Test
    void shouldPreserveCompatibleTail() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT * FROM orders WHERE tenant_id = 1 ORDER BY created_at DESC LIMIT 10",
            "SELECT id FROM orders WHERE tenant_id = 1 ORDER BY created_at DESC LIMIT 10",
            "SELECT * FROM orders WHERE tenant_id = 2 AND dt = '2026-05-22' ORDER BY created_at DESC LIMIT 20"
        );

        assertTrue(result.isApplied());
        assertEquals(
            "SELECT id FROM orders WHERE tenant_id = 2 AND dt = '2026-05-22' ORDER BY created_at DESC LIMIT 20",
            result.getRewrittenSql()
        );
    }

    @Test
    void shouldReplayLiteralsIntoWithRecommendationWhenSourceHasNestedWhereOnly() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT org_no FROM (SELECT org_no FROM fact WHERE 1 = 1 AND org_no = '41H006' "
                + "AND dte >= '20260430' GROUP BY org_no) s GROUP BY org_no",
            "WITH raw_customer_snapshot AS (SELECT org_no FROM fact WHERE org_level = 4 "
                + "AND org_no = '41H006' AND dte >= '20260430' GROUP BY org_no) SELECT org_no FROM raw_customer_snapshot",
            "SELECT org_no FROM (SELECT org_no FROM fact WHERE 1 = 1 AND org_no = '41H007' "
                + "AND dte >= '20260501' GROUP BY org_no) s GROUP BY org_no"
        );

        assertTrue(result.isApplied());
        assertTrue(result.getRewrittenSql().contains("org_no = '41H007'"));
        assertTrue(result.getRewrittenSql().contains("dte >= '20260501'"));
    }

    @Test
    void shouldInjectPortableNestedResidualPredicateIntoFirstInjectableCte() {
        RuntimeSqlRewriteTemplateResult result = RuntimeSqlRewriteTemplateEngine.rewrite(
            "SELECT org_no FROM (SELECT org_no FROM fact WHERE 1 = 1 AND org_no = '41H006' "
                + "GROUP BY org_no) s GROUP BY org_no",
            "WITH mv_seed AS (SELECT org_no FROM fact WHERE org_level = 4 "
                + "AND org_no = '41H006' GROUP BY org_no) SELECT org_no FROM mv_seed",
            "SELECT org_no FROM (SELECT org_no FROM fact WHERE 1 = 1 AND channel_code = 'MOBILE' "
                + "AND org_no = '41H006' GROUP BY org_no) s GROUP BY org_no"
        );

        assertTrue(result.isApplied());
        assertTrue(result.getRewrittenSql().contains("AND channel_code = 'MOBILE'"));
        assertTrue(result.getRewrittenSql().indexOf("channel_code") < result.getRewrittenSql().indexOf("GROUP BY"));
    }
}
