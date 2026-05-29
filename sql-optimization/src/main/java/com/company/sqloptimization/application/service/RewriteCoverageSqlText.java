package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class RewriteCoverageSqlText {

    private RewriteCoverageSqlText() {
    }

    static String normalizeSingleStatement(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    static String normalizeExpression(String expression) {
        return normalizeSql(expression);
    }

    static String normalizeSql(String sql) {
        return StringUtils.hasText(sql)
            ? sql.replace("`", "")
                .replace("\"", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
            : "";
    }

    static String cleanReference(String value) {
        return StringUtils.hasText(value)
            ? value.replace("`", "").replace("\"", "").replaceAll("\\s*\\.\\s*", ".").trim()
            : "";
    }

    static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        if (StringUtils.hasText(alias)) {
            result = result.replaceAll("(?is)\\s+AS\\s+" + Pattern.quote(alias.trim()) + "\\s*$", "");
        }
        return result.replaceAll("(?is)\\s+AS\\s+[A-Z_][A-Z0-9_$]*\\s*$", "").trim();
    }

    static int indexOfTopLevelKeyword(String sql, String keyword, int startIndex) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        String upper = sql.toUpperCase(Locale.ROOT);
        for (int i = Math.max(0, startIndex); i <= sql.length() - keyword.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && upper.startsWith(keyword, i) && wordBoundary(sql, i, keyword.length())) {
                return i;
            }
        }
        return -1;
    }

    static boolean wordBoundary(String sql, int start, int length) {
        int before = start - 1;
        int after = start + length;
        return (before < 0 || !isIdentifierChar(sql.charAt(before)))
            && (after >= sql.length() || !isIdentifierChar(sql.charAt(after)));
    }

    static List<String> splitTopLevel(String value, char delimiter) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '(' && !inSingleQuote && !inDoubleQuote) {
                depth++;
            } else if (ch == ')' && !inSingleQuote && !inDoubleQuote) {
                depth = Math.max(0, depth - 1);
            }
            if (ch == delimiter && depth == 0 && !inSingleQuote && !inDoubleQuote) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    static List<String> splitWhitespace(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.trim().split("\\s+");
        List<String> result = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                result.add(part);
            }
        }
        return result;
    }

    static boolean containsTopLevelFunction(String value) {
        return StringUtils.hasText(value) && value.matches("(?is).*\\b[A-Z_][A-Z0-9_]*\\s*\\(.*");
    }

    static boolean containsToken(String text, String token) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(token)) {
            return false;
        }
        Pattern pattern = Pattern.compile("(^|[^A-Z0-9_])" + Pattern.quote(token) + "([^A-Z0-9_]|$)");
        return pattern.matcher(text).find();
    }

    static String stripLeadingComments(String sql) {
        String remaining = sql == null ? "" : sql.trim();
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

    static String stripSqlComments(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        return sql.replaceAll("(?is)/\\*.*?\\*/", " ").replaceAll("(?m)--.*?$", " ");
    }

    static String stripStringLiterals(String sql) {
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

    private static boolean isIdentifierChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
    }
}
