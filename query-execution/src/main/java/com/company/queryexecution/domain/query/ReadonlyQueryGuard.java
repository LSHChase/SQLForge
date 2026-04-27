package com.company.queryexecution.domain.query;

import java.util.Locale;

/**
 * Enforces the current read-only-first policy for the synchronous query path.
 */
public final class ReadonlyQueryGuard {

    private static final String[] READONLY_PREFIXES = {
        "SELECT ",
        "WITH ",
        "SHOW ",
        "DESCRIBE ",
        "EXPLAIN "
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
            return ReadonlyQueryAssessment.reject("Submit a non-empty read-only SQL statement.");
        }
        if (normalized.indexOf(';') >= 0) {
            return ReadonlyQueryAssessment.reject("Submit a single read-only SQL statement without multi-statement batching.");
        }

        String upperSql = normalized.toUpperCase(Locale.ROOT);
        if (!hasReadonlyPrefix(upperSql)) {
            return ReadonlyQueryAssessment.reject("Use SELECT, WITH, SHOW, DESCRIBE, or EXPLAIN in the current path.");
        }

        String paddedSql = ' ' + upperSql + ' ';
        int i;
        for (i = 0; i < FORBIDDEN_TOKENS.length; i++) {
            if (paddedSql.contains(FORBIDDEN_TOKENS[i])) {
                return ReadonlyQueryAssessment.reject("Remove write, DDL, privilege, or load operations from the SQL.");
            }
        }
        return ReadonlyQueryAssessment.allow();
    }

    private static boolean hasReadonlyPrefix(String upperSql) {
        int i;
        for (i = 0; i < READONLY_PREFIXES.length; i++) {
            if (upperSql.startsWith(READONLY_PREFIXES[i])) {
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
