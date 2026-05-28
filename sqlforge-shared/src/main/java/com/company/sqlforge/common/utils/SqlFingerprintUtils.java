package com.company.sqlforge.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 用于治理聚合的 SQL 指纹生成器。
 */
public final class SqlFingerprintUtils {

    private SqlFingerprintUtils() {
    }

    public static String fingerprint(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        String normalized = normalizeForFingerprint(sql);
        if (normalized.isEmpty()) {
            return "";
        }
        return md5Hex(normalized);
    }

    public static String normalizeForFingerprint(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        String rewrittenSql = SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sql);
        String withoutComments = stripComments(rewrittenSql);
        String parameterized = parameterizeLiterals(withoutComments);
        String normalized = canonicalizeInsignificantWhitespace(parameterized).toLowerCase(Locale.ROOT);
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static String stripComments(String sql) {
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '-' && next == '-') {
                builder.append(' ');
                index += 2;
                while (index < sql.length()) {
                    char item = sql.charAt(index);
                    if (item == '\n' || item == '\r') {
                        builder.append(' ');
                        break;
                    }
                    index++;
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '/' && next == '*') {
                builder.append(' ');
                index += 2;
                boolean closed = false;
                while (index + 1 < sql.length()) {
                    if (sql.charAt(index) == '*' && sql.charAt(index + 1) == '/') {
                        index += 2;
                        closed = true;
                        break;
                    }
                    index++;
                }
                if (!closed) {
                    index = sql.length();
                }
                continue;
            }
            builder.append(current);
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktick) {
                if (inDoubleQuote && next == '"') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }
            index++;
        }
        return builder.toString();
    }

    private static String canonicalizeInsignificantWhitespace(String sql) {
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean pendingWhitespace = false;
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (inSingleQuote || inDoubleQuote || inBacktick) {
                builder.append(current);
                if (current == '\'' && inSingleQuote) {
                    if (next == '\'') {
                        builder.append(next);
                        index += 2;
                        continue;
                    }
                    inSingleQuote = false;
                } else if (current == '"' && inDoubleQuote) {
                    if (next == '"') {
                        builder.append(next);
                        index += 2;
                        continue;
                    }
                    inDoubleQuote = false;
                } else if (current == '`' && inBacktick) {
                    inBacktick = false;
                }
                index++;
                continue;
            }
            if (current == '\'') {
                appendPendingWhitespace(builder, pendingWhitespace);
                pendingWhitespace = false;
                builder.append(current);
                inSingleQuote = true;
                index++;
                continue;
            }
            if (current == '"') {
                appendPendingWhitespace(builder, pendingWhitespace);
                pendingWhitespace = false;
                builder.append(current);
                inDoubleQuote = true;
                index++;
                continue;
            }
            if (current == '`') {
                appendPendingWhitespace(builder, pendingWhitespace);
                pendingWhitespace = false;
                builder.append(current);
                inBacktick = true;
                index++;
                continue;
            }
            if (Character.isWhitespace(current)) {
                pendingWhitespace = true;
                index++;
                continue;
            }
            if (isOperatorOrPunctuation(current)) {
                trimTrailingSpace(builder);
                builder.append(current);
                pendingWhitespace = false;
                index++;
                continue;
            }
            appendPendingWhitespace(builder, pendingWhitespace);
            pendingWhitespace = false;
            builder.append(current);
            index++;
        }
        trimTrailingSpace(builder);
        return builder.toString().trim();
    }

    private static void appendPendingWhitespace(StringBuilder builder, boolean pendingWhitespace) {
        if (!pendingWhitespace || builder.length() == 0 || isOperatorOrPunctuation(builder.charAt(builder.length() - 1))) {
            return;
        }
        builder.append(' ');
    }

    private static void trimTrailingSpace(StringBuilder builder) {
        while (builder.length() > 0 && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.deleteCharAt(builder.length() - 1);
        }
    }

    private static boolean isOperatorOrPunctuation(char value) {
        return value == '('
            || value == ')'
            || value == ','
            || value == '.'
            || value == ';'
            || value == '='
            || value == '<'
            || value == '>'
            || value == '+'
            || value == '-'
            || value == '*'
            || value == '/'
            || value == '%'
            || value == '|'
            || value == '&'
            || value == '!';
    }

    private static String parameterizeLiterals(String sql) {
        StringBuilder builder = new StringBuilder(sql.length());
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (current == '\'') {
                builder.append('?');
                index = skipSingleQuotedLiteral(sql, index + 1);
                continue;
            }
            if (isSignedNumberStart(sql, index)) {
                builder.append('?');
                index = skipNumber(sql, index + 1);
                continue;
            }
            if (isNumberStart(sql, index)) {
                builder.append('?');
                index = skipNumber(sql, index);
                continue;
            }
            if (isNamedParameterStart(sql, index)) {
                builder.append('?');
                index = skipNamedParameter(sql, index + 1);
                continue;
            }
            builder.append(current);
            index++;
        }
        return builder.toString();
    }

    private static int skipSingleQuotedLiteral(String sql, int index) {
        while (index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (current == '\'' && next == '\'') {
                index += 2;
                continue;
            }
            if (current == '\'') {
                return index + 1;
            }
            index++;
        }
        return index;
    }

    private static boolean isSignedNumberStart(String sql, int index) {
        if (index + 1 >= sql.length()) {
            return false;
        }
        char current = sql.charAt(index);
        if (current != '-' && current != '+') {
            return false;
        }
        return Character.isDigit(sql.charAt(index + 1)) && isBoundaryBefore(sql, index);
    }

    private static boolean isNumberStart(String sql, int index) {
        return Character.isDigit(sql.charAt(index)) && isBoundaryBefore(sql, index);
    }

    private static int skipNumber(String sql, int index) {
        int cursor = index;
        if (cursor < sql.length() && (sql.charAt(cursor) == '-' || sql.charAt(cursor) == '+')) {
            cursor++;
        }
        boolean exponentAllowed = true;
        while (cursor < sql.length()) {
            char current = sql.charAt(cursor);
            if (Character.isDigit(current) || current == '.') {
                cursor++;
                continue;
            }
            if (exponentAllowed && (current == 'e' || current == 'E') && cursor + 1 < sql.length()) {
                char next = sql.charAt(cursor + 1);
                if (Character.isDigit(next) || next == '-' || next == '+') {
                    exponentAllowed = false;
                    cursor += 2;
                    continue;
                }
            }
            break;
        }
        return cursor;
    }

    private static boolean isNamedParameterStart(String sql, int index) {
        if (sql.charAt(index) != ':' || index + 1 >= sql.length()) {
            return false;
        }
        if ((index > 0 && sql.charAt(index - 1) == ':') || sql.charAt(index + 1) == ':') {
            return false;
        }
        return isBoundaryBefore(sql, index) && isIdentifierStart(sql.charAt(index + 1));
    }

    private static int skipNamedParameter(String sql, int index) {
        int cursor = index;
        while (cursor < sql.length() && isIdentifierPart(sql.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static boolean isBoundaryBefore(String sql, int index) {
        return index == 0 || !isIdentifierPart(sql.charAt(index - 1));
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 算法不可用", ex);
        }
    }
}
