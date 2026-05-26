package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.utils.SqlCatalogQualifierRewriteUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * 解析器构建 AST 前使用的轻量 SQL 方言归一化。
 */
final class SqlDialectNormalizer {

    private static final Set<String> SELECT_LIKE_START = new HashSet<String>(Arrays.asList("SELECT", "WITH"));

    private SqlDialectNormalizer() {
    }

    static String normalize(String sqlText) {
        if (sqlText == null) {
            return "";
        }
        boolean yonghongReportSql = looksLikeYonghongReportSql(sqlText);
        String normalized = stripLineComments(sqlText);
        if (yonghongReportSql || looksLikeYonghongReportSql(normalized)) {
            normalized = normalizeYonghongDerivedJoinSyntax(normalized);
        }
        normalized = removeTrailingSemicolons(normalized);
        normalized = SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(normalized);
        return normalized;
    }

    static String normalizeYonghongDerivedJoinSyntax(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return sqlText;
        }
        List<SqlToken> tokens = tokenize(sqlText);
        if (tokens.isEmpty()) {
            return sqlText;
        }
        Map<Integer, Integer> matchingCloseByOpen = matchingCloseByOpen(tokens);
        boolean[] removed = new boolean[sqlText.length()];
        boolean changed = false;

        for (int index = 0; index < tokens.size(); index++) {
            SqlToken outerOpen = tokens.get(index);
            if (outerOpen.kind != TokenKind.LEFT_PAREN || !isFromOrJoinOpen(tokens, index)) {
                continue;
            }
            int innerOpenIndex = nextTokenIndex(tokens, index);
            if (innerOpenIndex < 0 || tokens.get(innerOpenIndex).kind != TokenKind.LEFT_PAREN) {
                continue;
            }
            int selectStartIndex = nextTokenIndex(tokens, innerOpenIndex);
            if (selectStartIndex < 0 || !isKeyword(tokens.get(selectStartIndex), SELECT_LIKE_START)) {
                continue;
            }
            Integer outerCloseIndex = matchingCloseByOpen.get(Integer.valueOf(index));
            if (outerCloseIndex == null || outerCloseIndex.intValue() <= innerOpenIndex) {
                continue;
            }

            markRemoved(removed, tokens.get(innerOpenIndex));
            markRemoved(removed, tokens.get(outerCloseIndex.intValue()));
            changed = true;
        }

        if (!changed) {
            return sqlText;
        }
        StringBuilder builder = new StringBuilder(sqlText.length());
        for (int index = 0; index < sqlText.length(); index++) {
            if (!removed[index]) {
                builder.append(sqlText.charAt(index));
            }
        }
        return builder.toString();
    }

    private static boolean looksLikeYonghongReportSql(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return false;
        }
        return sqlText.contains("YH_RPT")
            || sqlText.contains("YH_QUERYID")
            || sqlText.contains("分组和汇总");
    }

    private static String stripLineComments(String sqlText) {
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        for (int index = 0; index < sqlText.length(); index++) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    builder.append(current);
                } else {
                    builder.append(' ');
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '-' && next == '-') {
                inLineComment = true;
                builder.append(' ');
                builder.append(' ');
                index++;
                continue;
            }
            builder.append(current);
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
            } else if (current == '"' && !inSingleQuote && !inBacktick) {
                if (inDoubleQuote && next == '"') {
                    builder.append(next);
                    index++;
                } else {
                    inDoubleQuote = !inDoubleQuote;
                }
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                if (inBacktick && next == '`') {
                    builder.append(next);
                    index++;
                } else {
                    inBacktick = !inBacktick;
                }
            }
        }
        return builder.toString();
    }

    private static String removeTrailingSemicolons(String sqlText) {
        String normalized = sqlText == null ? "" : sqlText;
        int end = normalized.length();
        while (end > 0) {
            while (end > 0 && Character.isWhitespace(normalized.charAt(end - 1))) {
                end--;
            }
            if (end > 0 && normalized.charAt(end - 1) == ';') {
                normalized = normalized.substring(0, end - 1);
                end = normalized.length();
            } else {
                break;
            }
        }
        return normalized;
    }

    private static List<SqlToken> tokenize(String sqlText) {
        List<SqlToken> tokens = new ArrayList<SqlToken>();
        int index = 0;
        while (index < sqlText.length()) {
            char current = sqlText.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (current == '-' && next == '-') {
                index = skipLineComment(sqlText, index);
                continue;
            }
            if (current == '/' && next == '*') {
                index = skipBlockComment(sqlText, index);
                continue;
            }
            if (current == '(') {
                tokens.add(new SqlToken(TokenKind.LEFT_PAREN, current, index, index + 1));
                index++;
                continue;
            }
            if (current == ')') {
                tokens.add(new SqlToken(TokenKind.RIGHT_PAREN, current, index, index + 1));
                index++;
                continue;
            }
            if (current == '\'') {
                index = appendDelimitedToken(tokens, TokenKind.STRING, sqlText, index, '\'');
                continue;
            }
            if (current == '"' || current == '`') {
                index = appendDelimitedToken(tokens, TokenKind.QUOTED_IDENTIFIER, sqlText, index, current);
                continue;
            }
            if (isIdentifierStart(current)) {
                int start = index;
                index++;
                while (index < sqlText.length() && isIdentifierPart(sqlText.charAt(index))) {
                    index++;
                }
                tokens.add(new SqlToken(TokenKind.WORD, sqlText.substring(start, index), start, index));
                continue;
            }
            tokens.add(new SqlToken(TokenKind.OTHER, current, index, index + 1));
            index++;
        }
        return tokens;
    }

    private static int appendDelimitedToken(List<SqlToken> tokens, TokenKind kind, String sqlText, int start, char quote) {
        int cursor = start + 1;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            char next = cursor + 1 < sqlText.length() ? sqlText.charAt(cursor + 1) : '\0';
            if (current == quote && next == quote) {
                cursor += 2;
                continue;
            }
            if (current == quote) {
                cursor++;
                break;
            }
            cursor++;
        }
        tokens.add(new SqlToken(kind, sqlText.substring(start, cursor), start, cursor));
        return cursor;
    }

    private static int skipLineComment(String sqlText, int start) {
        int cursor = start + 2;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            cursor++;
            if (current == '\n' || current == '\r') {
                break;
            }
        }
        return cursor;
    }

    private static int skipBlockComment(String sqlText, int start) {
        int cursor = start + 2;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            char next = cursor + 1 < sqlText.length() ? sqlText.charAt(cursor + 1) : '\0';
            cursor++;
            if (current == '*' && next == '/') {
                cursor++;
                break;
            }
        }
        return cursor;
    }

    private static Map<Integer, Integer> matchingCloseByOpen(List<SqlToken> tokens) {
        Map<Integer, Integer> matching = new HashMap<Integer, Integer>();
        Deque<Integer> openStack = new ArrayDeque<Integer>();
        for (int index = 0; index < tokens.size(); index++) {
            SqlToken token = tokens.get(index);
            if (token.kind == TokenKind.LEFT_PAREN) {
                openStack.push(Integer.valueOf(index));
            } else if (token.kind == TokenKind.RIGHT_PAREN && !openStack.isEmpty()) {
                matching.put(openStack.pop(), Integer.valueOf(index));
            }
        }
        return matching;
    }

    private static boolean isFromOrJoinOpen(List<SqlToken> tokens, int openIndex) {
        int previousIndex = previousTokenIndex(tokens, openIndex);
        if (previousIndex < 0) {
            return false;
        }
        SqlToken previous = tokens.get(previousIndex);
        return isKeyword(previous, "FROM") || isKeyword(previous, "JOIN");
    }

    private static int previousTokenIndex(List<SqlToken> tokens, int index) {
        return index - 1 >= 0 ? index - 1 : -1;
    }

    private static int nextTokenIndex(List<SqlToken> tokens, int index) {
        return index + 1 < tokens.size() ? index + 1 : -1;
    }

    private static boolean isKeyword(SqlToken token, String keyword) {
        return token.kind == TokenKind.WORD && keyword.equals(token.upperText);
    }

    private static boolean isKeyword(SqlToken token, Set<String> keywords) {
        return token.kind == TokenKind.WORD && keywords.contains(token.upperText);
    }

    private static void markRemoved(boolean[] removed, SqlToken token) {
        for (int index = token.start; index < token.end; index++) {
            removed[index] = true;
        }
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private enum TokenKind {
        WORD,
        QUOTED_IDENTIFIER,
        STRING,
        LEFT_PAREN,
        RIGHT_PAREN,
        OTHER
    }

    private static final class SqlToken {
        private final TokenKind kind;
        private final String text;
        private final String upperText;
        private final int start;
        private final int end;

        private SqlToken(TokenKind kind, char text, int start, int end) {
            this(kind, String.valueOf(text), start, end);
        }

        private SqlToken(TokenKind kind, String text, int start, int end) {
            this.kind = kind;
            this.text = text;
            this.upperText = text.toUpperCase(Locale.ROOT);
            this.start = start;
            this.end = end;
        }
    }
}
