package com.company.benchmarkengine.application.service;

import java.util.Locale;

final class BenchmarkReadonlySqlSupport {

    private BenchmarkReadonlySqlSupport() {
    }

    static String validateReadonlySql(String sqlText) {
        String normalized = stripLeadingComments(sqlText);
        if (normalized.isEmpty()) {
            return "sqlText 必须是非空只读 SQL 语句";
        }
        if (normalized.indexOf(';') >= 0) {
            return "sqlText 不能包含多语句批处理";
        }
        String upperSql = normalized.toUpperCase(Locale.ROOT);
        if (!(upperSql.startsWith("SELECT ")
            || upperSql.startsWith("WITH ")
            || upperSql.startsWith("SHOW ")
            || upperSql.startsWith("DESCRIBE ")
            || upperSql.startsWith("EXPLAIN "))) {
            return "sqlText 必须保持在只读压测边界内";
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
                return "sqlText 包含写入、DDL、权限或加载操作";
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
