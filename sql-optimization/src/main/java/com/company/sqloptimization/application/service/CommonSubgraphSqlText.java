package com.company.sqloptimization.application.service;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class CommonSubgraphSqlText {

    private CommonSubgraphSqlText() {
    }

    static String normalizeSubgraphSql(String sql) {
        String normalized = stripOuterParentheses(trimTrailingSemicolon(sql));
        return trimTrailingSemicolon(normalized);
    }

    static String removeTrailingAlias(String sql, String alias) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(alias)) {
            return sql;
        }
        return sql.replaceFirst("(?is)\\s+(?:AS\\s+)?" + Pattern.quote(alias) + "\\s*$", "");
    }

    static String stripOuterParentheses(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        while (normalized.startsWith("(") && normalized.endsWith(")") && wrapsWholeExpression(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    static boolean startsWithWord(String sql, int offset, String word) {
        if (sql == null || offset < 0 || offset + word.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, offset, word, 0, word.length())) {
            return false;
        }
        int end = offset + word.length();
        return end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
    }

    static boolean hasOrderOrLimit(String sql) {
        String normalized = stripStringLiterals(sql).toUpperCase(Locale.ROOT);
        return normalized.matches("(?is).*\\bORDER\\s+BY\\b.*")
            || normalized.matches("(?is).*\\bLIMIT\\b.*");
    }

    static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String upperText(Object value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    static String cleanIdentifier(String value) {
        return value == null ? "" : value.replace("\"", "").replace("`", "").trim();
    }

    static String normalizeIdentifier(String value) {
        return cleanIdentifier(value).toUpperCase(Locale.ROOT);
    }

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    static void addIfText(Collection<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value);
        }
    }

    private static boolean wrapsWholeExpression(String value) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0 && i < value.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static String stripStringLiterals(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    builder.append(' ');
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                builder.append(' ');
                continue;
            }
            builder.append(inSingleQuote ? ' ' : current);
        }
        return builder.toString();
    }
}
