package com.company.benchmarkengine.application.service;

import java.util.Locale;

final class BenchmarkReadonlySqlSupport {

    private BenchmarkReadonlySqlSupport() {
    }

    static String validateReadonlySql(String sqlText) {
        String normalized = stripLeadingComments(sqlText);
        if (normalized.isEmpty()) {
            return "sqlText must be a non-empty read-only SQL statement";
        }
        if (normalized.indexOf(';') >= 0) {
            return "sqlText must not contain multi-statement batching";
        }
        String upperSql = normalized.toUpperCase(Locale.ROOT);
        if (!(upperSql.startsWith("SELECT ")
            || upperSql.startsWith("WITH ")
            || upperSql.startsWith("SHOW ")
            || upperSql.startsWith("DESCRIBE ")
            || upperSql.startsWith("EXPLAIN "))) {
            return "sqlText must remain within the read-only benchmark boundary";
        }
        String padded = ' ' + upperSql + ' ';
        String[] forbiddenTokens = {
            " INSERT ",
            " UPDATE ",
            " DELETE ",
            " MERGE ",
            " UPSERT ",
            " CREATE ",
            " ALTER ",
            " DROP ",
            " TRUNCATE ",
            " GRANT ",
            " REVOKE ",
            " CALL ",
            " EXPORT ",
            " IMPORT ",
            " LOAD "
        };
        for (String token : forbiddenTokens) {
            if (padded.contains(token)) {
                return "sqlText contains write, DDL, privilege, or load operations";
            }
        }
        return null;
    }

    private static String stripLeadingComments(String sqlText) {
        String remaining = sqlText == null ? "" : sqlText.trim();
        boolean stripped = true;
        while (stripped) {
            stripped = false;
            remaining = remaining.trim();
            if (remaining.startsWith("--")) {
                int lineBreak = remaining.indexOf('\n');
                remaining = lineBreak >= 0 ? remaining.substring(lineBreak + 1) : "";
                stripped = true;
                continue;
            }
            if (remaining.startsWith("/*")) {
                int commentEnd = remaining.indexOf("*/");
                if (commentEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(commentEnd + 2);
                stripped = true;
            }
        }
        return remaining.trim();
    }
}
