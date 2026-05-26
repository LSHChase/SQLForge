package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;

class SqlDialectNormalizerTest {

    @Test
    void shouldNormalizeFormattedYonghongDerivedJoinWrappers() throws Exception {
        String sql =
            "-- YH_RPTID=RPT_TOKEN_NORMALIZER\n"
                + "SELECT *\n"
                + "FROM (\n"
                + "  (\n"
                + "    SELECT id, '(JOIN ((SELECT)) untouched)' AS marker FROM orders\n"
                + "  ) \"Sub1_分组和汇总\"\n"
                + ")\n"
                + "LEFT JOIN (\n"
                + "  (\n"
                + "    SELECT id FROM customers\n"
                + "  ) \"Sub2_分组和汇总\"\n"
                + "  ON \"Sub1_分组和汇总\".id = \"Sub2_分组和汇总\".id\n"
                + ")\n"
                + "WHERE \"Sub2_分组和汇总\".id IS NOT NULL;";

        String normalized = SqlDialectNormalizer.normalize(sql);

        assertFalse(normalized.contains("FROM (\n  ("));
        assertFalse(normalized.contains("JOIN (\n  ("));
        assertFalse(normalized.trim().endsWith(";"));
        assertTrue(normalized.contains("'(JOIN ((SELECT)) untouched)'"));
        Statement statement = CCJSqlParserUtil.parse(normalized);
        assertTrue(statement instanceof net.sf.jsqlparser.statement.select.Select);
    }

    @Test
    void shouldLeaveQuotedIdentifiersStringsAndCommentsOutOfTokenMatching() throws Exception {
        String sql =
            "/* YH_RPTID=RPT_TOKEN_NORMALIZER */\n"
                + "SELECT \"JOIN ((SELECT))\" AS quoted_keyword,\n"
                + "  '-- not a comment' AS literal_comment\n"
                + "FROM (\n"
                + "  (\n"
                + "    SELECT id FROM orders WHERE note = 'FROM ((SELECT kept))'\n"
                + "  ) \"Sub1_分组和汇总\"\n"
                + ")";

        String normalized = SqlDialectNormalizer.normalize(sql);

        assertTrue(normalized.contains("\"JOIN ((SELECT))\""));
        assertTrue(normalized.contains("'-- not a comment'"));
        assertTrue(normalized.contains("'FROM ((SELECT kept))'"));
        assertTrue(normalized.contains("/* YH_RPTID"));
        Statement statement = CCJSqlParserUtil.parse(normalized);
        assertTrue(statement instanceof net.sf.jsqlparser.statement.select.Select);
    }

    @Test
    void shouldNormalizeBiViewCatalogBeforeParserEntry() {
        String normalized = SqlDialectNormalizer.normalize(
            "SELECT * FROM BI_HQX00_V.orders "
                + "WHERE note = 'BI_HQX00_V.orders' AND comment_text = '-- BI_HQX00_V.hidden';"
        );

        assertTrue(normalized.contains("BI_HQX00_HETU.orders"));
        assertTrue(normalized.contains("'BI_HQX00_V.orders'"));
        assertTrue(normalized.contains("'-- BI_HQX00_V.hidden'"));
        assertFalse(normalized.trim().endsWith(";"));
        assertEquals(1, countOccurrences(normalized, "BI_HQX00_HETU"));
    }

    private int countOccurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while (index >= 0) {
            index = value.indexOf(token, index);
            if (index >= 0) {
                count++;
                index += token.length();
            }
        }
        return count;
    }
}
