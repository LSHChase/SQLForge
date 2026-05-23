package com.company.sqlforge.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SqlCatalogQualifierRewriteUtilsTest {

    @Test
    void shouldRewriteUnquotedBiViewCatalogQualifierAndPreserveCaseStyle() {
        String sql = "SELECT * FROM BI_SALES_V.orders JOIN bi_Mart_v.customers ON orders.id = customers.id";

        assertEquals(
            "SELECT * FROM BI_SALES_HETU.orders JOIN bi_Mart_hetu.customers ON orders.id = customers.id",
            SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sql)
        );
    }

    @Test
    void shouldRewriteQuotedCatalogQualifiersBeforeDot() {
        String sql = "SELECT * FROM \"BI_SALES_V\" . orders JOIN `bi_finance_v`.ledger ON orders.id = ledger.id";

        assertEquals(
            "SELECT * FROM \"BI_SALES_HETU\" . orders JOIN `bi_finance_hetu`.ledger ON orders.id = ledger.id",
            SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sql)
        );
    }

    @Test
    void shouldRewriteThreePartReferencesWithWhitespaceBeforeDot() {
        String sql = "SELECT * FROM BI_RISK_V \n . mart.orders";

        assertEquals(
            "SELECT * FROM BI_RISK_HETU \n . mart.orders",
            SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sql)
        );
    }

    @Test
    void shouldSkipStringLiteralsAndComments() {
        String sql = "SELECT 'BI_SALES_V.orders' AS marker FROM fact_orders "
            + "-- BI_SALES_V.orders\n"
            + "/* BI_RISK_V.orders */ JOIN BI_FINANCE_V.orders ON fact_orders.id = orders.id";

        assertEquals(
            "SELECT 'BI_SALES_V.orders' AS marker FROM fact_orders "
                + "-- BI_SALES_V.orders\n"
                + "/* BI_RISK_V.orders */ JOIN BI_FINANCE_HETU.orders ON fact_orders.id = orders.id",
            SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sql)
        );
    }

    @Test
    void shouldIgnoreNonBiViewCatalogTokens() {
        String sql = "SELECT * FROM ABI_SALES_V.orders "
            + "JOIN BI_V.orders ON 1 = 1 "
            + "JOIN BI_SALES_VIEW.orders ON 1 = 1 "
            + "JOIN BI_SALES_VX.orders ON 1 = 1 "
            + "JOIN BI_SALES_V alias ON 1 = 1";

        assertEquals(sql, SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sql));
    }
}
