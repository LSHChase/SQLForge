package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class L2MaterializedViewDialectRendererTest {

    @Test
    void shouldRenderHetuHiveAndSparkMaterializationSql() {
        String selectSql = "SELECT customer_id, SUM(amount) AS total_amount "
            + "FROM orders GROUP BY customer_id;";

        L2MaterializedViewDialectRenderer.RenderedSql hetu =
            L2MaterializedViewDialectRenderer.render("HETU", "mv_sales_daily", selectSql);
        L2MaterializedViewDialectRenderer.RenderedSql hive =
            L2MaterializedViewDialectRenderer.render("HIVE", "mv_sales_daily", selectSql);
        L2MaterializedViewDialectRenderer.RenderedSql spark =
            L2MaterializedViewDialectRenderer.render("SPARK", "mv_sales_daily", selectSql);

        assertEquals("CREATE MATERIALIZED VIEW mv_sales_daily AS\n"
            + "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id;",
            hetu.getDdlSql());
        assertEquals("REFRESH MATERIALIZED VIEW mv_sales_daily;", hetu.getRefreshSql());
        assertEquals("DROP MATERIALIZED VIEW mv_sales_daily;", hetu.getRollbackSql());

        assertEquals("CREATE MATERIALIZED VIEW mv_sales_daily AS\n"
            + "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id;",
            hive.getDdlSql());
        assertEquals("ALTER MATERIALIZED VIEW mv_sales_daily REBUILD;", hive.getRefreshSql());
        assertEquals("DROP MATERIALIZED VIEW mv_sales_daily;", hive.getRollbackSql());

        assertEquals("CREATE TABLE mv_sales_daily AS\n"
            + "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id;",
            spark.getDdlSql());
        assertEquals("INSERT OVERWRITE TABLE mv_sales_daily\n"
            + "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id;",
            spark.getRefreshSql());
        assertEquals("DROP TABLE IF EXISTS mv_sales_daily;", spark.getRollbackSql());

        assertNoPlaceholder(hetu);
        assertNoPlaceholder(hive);
        assertNoPlaceholder(spark);
    }

    @Test
    void shouldNotRenderUnsupportedEngineSql() {
        assertNull(L2MaterializedViewDialectRenderer.render("CLICKHOUSE", "mv_sales_daily", "SELECT 1"));
        assertNull(L2MaterializedViewDialectRenderer.render("AUTO", "mv_sales_daily", "SELECT 1"));
        assertFalse(L2MaterializedViewDialectRenderer.supports("GAUSSDB"));
        assertEquals("UNRESOLVED", L2MaterializedViewDialectRenderer.dialect("GAUSSDB"));
    }

    private static void assertNoPlaceholder(L2MaterializedViewDialectRenderer.RenderedSql renderedSql) {
        assertNoPlaceholder(renderedSql.getDdlSql());
        assertNoPlaceholder(renderedSql.getRefreshSql());
        assertNoPlaceholder(renderedSql.getRollbackSql());
    }

    private static void assertNoPlaceholder(String sql) {
        assertFalse(sql.contains("${"));
        assertFalse(sql.contains("{{"));
        assertFalse(sql.contains("<"));
        assertTrue(sql.endsWith(";"));
    }
}
