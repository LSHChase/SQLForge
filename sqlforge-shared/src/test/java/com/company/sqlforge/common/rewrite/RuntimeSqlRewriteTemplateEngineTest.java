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
}
