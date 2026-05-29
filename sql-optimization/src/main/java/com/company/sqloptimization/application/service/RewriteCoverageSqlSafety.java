package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.containsToken;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripLeadingComments;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripSqlComments;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripStringLiterals;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.wordBoundary;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RewriteCoverageSqlSafety {

    private static final Set<String> READONLY_PREFIXES =
        new LinkedHashSet<String>(Arrays.asList("SELECT", "WITH"));
    private static final Set<String> FORBIDDEN_TOKENS =
        new LinkedHashSet<String>(Arrays.asList(
            "INSERT", "UPDATE", "DELETE", "MERGE", "UPSERT", "CREATE", "ALTER", "DROP", "TRUNCATE",
            "GRANT", "REVOKE", "CALL", "EXPORT", "IMPORT", "LOAD"
        ));

    private RewriteCoverageSqlSafety() {
    }

    static boolean isReadonly(String normalizedRewrite) {
        if (!StringUtils.hasText(normalizedRewrite) || normalizedRewrite.indexOf(';') >= 0) {
            return false;
        }
        String withoutLeadingComments = stripLeadingComments(normalizedRewrite);
        if (!StringUtils.hasText(withoutLeadingComments)) {
            return false;
        }
        String upper = withoutLeadingComments.toUpperCase(Locale.ROOT);
        if (!hasReadonlyPrefix(upper)) {
            return false;
        }
        String stripped = stripStringLiterals(stripSqlComments(withoutLeadingComments)).toUpperCase(Locale.ROOT);
        for (String token : FORBIDDEN_TOKENS) {
            if (containsToken(stripped, token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasReadonlyPrefix(String upper) {
        for (String prefix : READONLY_PREFIXES) {
            if ((upper.startsWith(prefix) && wordBoundary(upper, 0, prefix.length())) || upper.equals(prefix)) {
                return true;
            }
        }
        return false;
    }
}
