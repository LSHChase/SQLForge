package com.company.sqloptimization.application.service;

final class CommonSubgraphAliasScanner {

    private CommonSubgraphAliasScanner() {
    }

    static int matchingParen(String sql, int openIndex) {
        int depth = 0;
        char quote = 0;
        for (int index = openIndex; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                        index++;
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }
            if (current == '\'' || current == '"' || current == '`') {
                quote = current;
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
        }
        return -1;
    }

    static CommonSubgraphAliasMatch aliasAfter(String sql, int start, String expectedAlias) {
        CommonSubgraphAliasMatch match = aliasAfter(sql, start);
        if (!match.matched) {
            return match;
        }
        int index = skipWhitespace(sql, start);
        int aliasStart = index;
        if (startsWithWord(sql, index, "AS")) {
            aliasStart = skipWhitespace(sql, index + 2);
        }
        CommonSubgraphAliasToken token = readAliasToken(sql, aliasStart);
        return normalizeIdentifier(token.value).equals(normalizeIdentifier(expectedAlias))
            ? match
            : CommonSubgraphAliasMatch.none();
    }

    static CommonSubgraphAliasMatch aliasAfter(String sql, int start) {
        int index = skipWhitespace(sql, start);
        int aliasStart = index;
        if (startsWithWord(sql, index, "AS")) {
            aliasStart = skipWhitespace(sql, index + 2);
        }
        CommonSubgraphAliasToken token = readAliasToken(sql, aliasStart);
        return token.present
            ? new CommonSubgraphAliasMatch(true, token.endIndex)
            : CommonSubgraphAliasMatch.none();
    }

    static String renderAlias(String alias) {
        String cleaned = cleanIdentifier(alias);
        if (cleaned.matches("(?i)[A-Z_][A-Z0-9_$]*")) {
            return cleaned;
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    private static int skipWhitespace(String sql, int start) {
        int index = start;
        while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
            index++;
        }
        return index;
    }

    private static CommonSubgraphAliasToken readAliasToken(String sql, int start) {
        if (start >= sql.length()) {
            return CommonSubgraphAliasToken.none();
        }
        char first = sql.charAt(start);
        if (first == '"' || first == '`') {
            return readQuotedAlias(sql, start, first);
        }
        int end = start;
        while (end < sql.length() && isIdentifierPart(sql.charAt(end))) {
            end++;
        }
        return end == start
            ? CommonSubgraphAliasToken.none()
            : new CommonSubgraphAliasToken(true, sql.substring(start, end), end);
    }

    private static CommonSubgraphAliasToken readQuotedAlias(String sql, int start, char quote) {
        StringBuilder value = new StringBuilder();
        for (int index = start + 1; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == quote) {
                if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                    value.append(current);
                    index++;
                    continue;
                }
                return new CommonSubgraphAliasToken(true, value.toString(), index + 1);
            }
            value.append(current);
        }
        return CommonSubgraphAliasToken.none();
    }

    private static boolean startsWithWord(String sql, int offset, String word) {
        if (offset < 0 || offset + word.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, offset, word, 0, word.length())) {
            return false;
        }
        int before = offset - 1;
        int after = offset + word.length();
        boolean beforeBoundary = before < 0 || !isIdentifierPart(sql.charAt(before));
        boolean afterBoundary = after >= sql.length() || !isIdentifierPart(sql.charAt(after));
        return beforeBoundary && afterBoundary;
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private static String cleanIdentifier(String value) {
        return value == null ? "" : value.replace("\"", "").replace("`", "").trim();
    }

    private static String normalizeIdentifier(String value) {
        return cleanIdentifier(value).toUpperCase(java.util.Locale.ROOT);
    }
}
