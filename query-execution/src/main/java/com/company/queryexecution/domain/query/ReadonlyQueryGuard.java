package com.company.queryexecution.domain.query;

import java.util.Locale;

/**
 * 对同步查询路径执行当前只读优先策略。
 */
public final class ReadonlyQueryGuard {

    private static final String[] READONLY_PREFIXES = {
        "SELECT",
        "WITH",
        "SHOW",
        "DESCRIBE",
        "EXPLAIN"
    };
    private static final String[] FORBIDDEN_TOKENS = {
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

    private ReadonlyQueryGuard() {
    }

    public static ReadonlyQueryAssessment assess(String actualSql) {
        String normalized = stripLeadingComments(actualSql == null ? "" : actualSql.trim());
        if (normalized.isEmpty()) {
            return ReadonlyQueryAssessment.reject("请提交非空的只读 SQL 语句。");
        }
        if (normalized.indexOf(';') >= 0) {
            return ReadonlyQueryAssessment.reject("请提交单条只读 SQL 语句，不要使用多语句批量执行。");
        }

        String upperSql = normalized.toUpperCase(Locale.ROOT);
        if (!hasReadonlyPrefix(upperSql)) {
            return ReadonlyQueryAssessment.reject("当前路径仅允许使用 SELECT、WITH、SHOW、DESCRIBE 或 EXPLAIN。");
        }

        String paddedSql = ' ' + upperSql + ' ';
        int i;
        for (i = 0; i < FORBIDDEN_TOKENS.length; i++) {
            if (paddedSql.contains(FORBIDDEN_TOKENS[i])) {
                return ReadonlyQueryAssessment.reject("请移除 SQL 中的写入、DDL、权限或加载类操作。");
            }
        }
        return ReadonlyQueryAssessment.allow();
    }

    private static boolean hasReadonlyPrefix(String upperSql) {
        int i;
        for (i = 0; i < READONLY_PREFIXES.length; i++) {
            String prefix = READONLY_PREFIXES[i];
            if (upperSql.equals(prefix)) {
                return true;
            }
            if (upperSql.startsWith(prefix) && Character.isWhitespace(upperSql.charAt(prefix.length()))) {
                return true;
            }
        }
        return false;
    }

    private static String stripLeadingComments(String normalized) {
        String remaining = normalized;
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
