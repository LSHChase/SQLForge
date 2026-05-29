package com.company.sqlforge.common.rewrite;

import java.util.Locale;

final class RuntimeSqlRewriteSqlScanner {

    private RuntimeSqlRewriteSqlScanner() {
    }

    static int findKeywordAtDepth(String sql, String keyword, int start, Integer requiredDepth) {
        String lowerSql = sql.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int depth = 0;
        int index = Math.max(0, start);
        while (index <= sql.length() - lowerKeyword.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                index++;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktick) {
                if (inDoubleQuote && next == '"') {
                    index += 2;
                    continue;
                }
                inDoubleQuote = !inDoubleQuote;
                index++;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
                index++;
                continue;
            }
            if (inSingleQuote || inDoubleQuote || inBacktick) {
                index++;
                continue;
            }
            if (current == '(') {
                depth++;
                index++;
                continue;
            }
            if (current == ')') {
                depth = Math.max(0, depth - 1);
                index++;
                continue;
            }
            if ((requiredDepth == null || depth == requiredDepth.intValue())
                && lowerSql.startsWith(lowerKeyword, index)
                && hasKeywordBoundary(sql, index, lowerKeyword.length())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    static int findMatchingParen(String sql, int openIndex) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int depth = 0;
        int index = openIndex;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                index++;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktick) {
                if (inDoubleQuote && next == '"') {
                    index += 2;
                    continue;
                }
                inDoubleQuote = !inDoubleQuote;
                index++;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
                index++;
                continue;
            }
            if (inSingleQuote || inDoubleQuote || inBacktick) {
                index++;
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

    static int depthAt(String sql, int offset) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int depth = 0;
        int index = 0;
        while (index < offset && index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                index++;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktick) {
                if (inDoubleQuote && next == '"') {
                    index += 2;
                    continue;
                }
                inDoubleQuote = !inDoubleQuote;
                index++;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
                index++;
                continue;
            }
            if (inSingleQuote || inDoubleQuote || inBacktick) {
                index++;
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
            }
            index++;
        }
        return depth;
    }

    static boolean hasKeywordBoundary(String sql, int start, int length) {
        int before = start - 1;
        int after = start + length;
        return (before < 0 || !isIdentifierPart(sql.charAt(before)))
            && (after >= sql.length() || !isIdentifierPart(sql.charAt(after)));
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }
}
