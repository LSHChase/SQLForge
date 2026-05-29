package com.company.sqlforge.common.utils;

import org.springframework.util.StringUtils;

final class SqlCatalogQualifierRewriteScanner {

    private static final String BI_PREFIX = "BI_";
    private static final String VIEW_SUFFIX = "_V";
    private static final String HETU_SUFFIX_UPPER = "HETU";
    private static final String HETU_SUFFIX_LOWER = "hetu";

    private SqlCatalogQualifierRewriteScanner() {
    }

    static String rewriteBiViewCatalogQualifier(String sql) {
        StringBuilder builder = new StringBuilder(sql.length());
        int index = 0;
        boolean changed = false;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (current == '\'') {
                index = appendSingleQuoted(sql, index, builder);
                continue;
            }
            if (current == '-' && next == '-') {
                index = appendLineComment(sql, index, builder);
                continue;
            }
            if (current == '/' && next == '*') {
                index = appendBlockComment(sql, index, builder);
                continue;
            }
            if (current == '"' || current == '`') {
                RewriteCandidate quoted = tryRewriteQuoted(sql, index, current);
                if (quoted != null) {
                    builder.append(quoted.rewrittenText);
                    index = quoted.nextIndex;
                    changed = true;
                    continue;
                }
                index = appendQuotedIdentifier(sql, index, current, builder);
                continue;
            }
            RewriteCandidate unquoted = tryRewriteUnquoted(sql, index);
            if (unquoted != null) {
                builder.append(unquoted.rewrittenText);
                index = unquoted.nextIndex;
                changed = true;
                continue;
            }
            builder.append(current);
            index++;
        }
        return changed ? builder.toString() : sql;
    }

    private static RewriteCandidate tryRewriteUnquoted(String sql, int start) {
        if (!isIdentifierBoundaryBefore(sql, start) || !startsWithIgnoreCase(sql, start, BI_PREFIX)) {
            return null;
        }
        int end = start;
        while (end < sql.length() && isIdentifierPart(sql.charAt(end))) {
            end++;
        }
        String token = sql.substring(start, end);
        String rewritten = rewriteCatalogToken(token);
        if (rewritten == null || !isQualifierPrefix(sql, end)) {
            return null;
        }
        return new RewriteCandidate(rewritten, end);
    }

    private static RewriteCandidate tryRewriteQuoted(String sql, int start, char quote) {
        int end = findQuotedIdentifierEnd(sql, start, quote);
        if (end < 0 || !isQualifierPrefix(sql, end)) {
            return null;
        }
        String token = sql.substring(start + 1, end - 1);
        String rewritten = rewriteCatalogToken(token);
        if (rewritten == null) {
            return null;
        }
        return new RewriteCandidate(String.valueOf(quote) + rewritten + quote, end);
    }

    private static String rewriteCatalogToken(String token) {
        if (!StringUtils.hasText(token) || token.length() <= BI_PREFIX.length() + VIEW_SUFFIX.length()) {
            return null;
        }
        if (!startsWithIgnoreCase(token, 0, BI_PREFIX)
            || !endsWithIgnoreCase(token, VIEW_SUFFIX)
            || !isCatalogToken(token)) {
            return null;
        }
        char suffixCase = token.charAt(token.length() - 1);
        String hetuSuffix = Character.isLowerCase(suffixCase) ? HETU_SUFFIX_LOWER : HETU_SUFFIX_UPPER;
        return token.substring(0, token.length() - 1) + hetuSuffix;
    }

    private static boolean isQualifierPrefix(String sql, int index) {
        int cursor = index;
        while (cursor < sql.length() && Character.isWhitespace(sql.charAt(cursor))) {
            cursor++;
        }
        return cursor < sql.length() && sql.charAt(cursor) == '.';
    }

    private static int appendSingleQuoted(String sql, int start, StringBuilder builder) {
        int cursor = start;
        while (cursor < sql.length()) {
            char current = sql.charAt(cursor);
            char next = cursor + 1 < sql.length() ? sql.charAt(cursor + 1) : '\0';
            builder.append(current);
            cursor++;
            if (current == '\'' && next == '\'') {
                builder.append(next);
                cursor++;
                continue;
            }
            if (current == '\'' && cursor > start + 1) {
                break;
            }
        }
        return cursor;
    }

    private static int appendLineComment(String sql, int start, StringBuilder builder) {
        int cursor = start;
        while (cursor < sql.length()) {
            char current = sql.charAt(cursor);
            builder.append(current);
            cursor++;
            if (current == '\n' || current == '\r') {
                break;
            }
        }
        return cursor;
    }

    private static int appendBlockComment(String sql, int start, StringBuilder builder) {
        int cursor = start;
        while (cursor < sql.length()) {
            char current = sql.charAt(cursor);
            char next = cursor + 1 < sql.length() ? sql.charAt(cursor + 1) : '\0';
            builder.append(current);
            cursor++;
            if (current == '*' && next == '/') {
                builder.append(next);
                cursor++;
                break;
            }
        }
        return cursor;
    }

    private static int appendQuotedIdentifier(String sql, int start, char quote, StringBuilder builder) {
        int end = findQuotedIdentifierEnd(sql, start, quote);
        if (end < 0) {
            builder.append(sql.substring(start));
            return sql.length();
        }
        builder.append(sql.substring(start, end));
        return end;
    }

    private static int findQuotedIdentifierEnd(String sql, int start, char quote) {
        int cursor = start + 1;
        while (cursor < sql.length()) {
            char current = sql.charAt(cursor);
            char next = cursor + 1 < sql.length() ? sql.charAt(cursor + 1) : '\0';
            if (current == quote && next == quote) {
                cursor += 2;
                continue;
            }
            if (current == quote) {
                return cursor + 1;
            }
            cursor++;
        }
        return -1;
    }

    private static boolean startsWithIgnoreCase(String value, int start, String prefix) {
        if (value == null || prefix == null || start < 0 || start + prefix.length() > value.length()) {
            return false;
        }
        return value.regionMatches(true, start, prefix, 0, prefix.length());
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        if (value == null || suffix == null || suffix.length() > value.length()) {
            return false;
        }
        return value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    private static boolean isCatalogToken(String token) {
        for (int index = 0; index < token.length(); index++) {
            char current = token.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '_')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierBoundaryBefore(String sql, int index) {
        return index == 0 || !isIdentifierPart(sql.charAt(index - 1));
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

}
