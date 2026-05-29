package com.company.sqlforge.common.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

final class NestedWhereCteScanner {

    private NestedWhereCteScanner() {
    }

    static List<String> names(String sql) {
        List<String> names = new ArrayList<String>();
        if (!StringUtils.hasText(sql) || !sql.trim().toLowerCase(Locale.ROOT).startsWith("with ")) {
            return names;
        }
        int cursor = sql.toLowerCase(Locale.ROOT).indexOf("with") + "with".length();
        while (cursor < sql.length()) {
            cursor = skipWhitespaceAndComma(sql, cursor);
            int nameStart = cursor;
            if (nameStart >= sql.length()) {
                break;
            }
            String name;
            if (sql.charAt(nameStart) == '"') {
                int nameEnd = sql.indexOf('"', nameStart + 1);
                if (nameEnd < 0) {
                    break;
                }
                name = sql.substring(nameStart + 1, nameEnd);
                cursor = nameEnd + 1;
            } else {
                while (cursor < sql.length() && isIdentifierChar(sql.charAt(cursor))) {
                    cursor++;
                }
                name = sql.substring(nameStart, cursor);
            }
            if (!StringUtils.hasText(name)) {
                break;
            }
            int asIndex = RuntimeSqlRewriteSqlScanner.findKeywordAtDepth(sql, "as", cursor, null);
            if (asIndex < 0) {
                break;
            }
            int openIndex = sql.indexOf('(', asIndex);
            if (openIndex < 0) {
                break;
            }
            int closeIndex = RuntimeSqlRewriteSqlScanner.findMatchingParen(sql, openIndex);
            if (closeIndex < 0) {
                break;
            }
            names.add(name);
            cursor = closeIndex + 1;
            int next = skipWhitespaceAndComma(sql, cursor);
            if (!startsWithWord(sql, next, "SELECT") && next < sql.length() && sql.charAt(next - 1) != ',') {
                cursor = next;
            }
            if (startsWithWord(sql, next, "SELECT")) {
                break;
            }
        }
        return names;
    }

    private static int skipWhitespaceAndComma(String sql, int cursor) {
        int index = cursor;
        while (index < sql.length() && (Character.isWhitespace(sql.charAt(index)) || sql.charAt(index) == ',')) {
            index++;
        }
        return index;
    }

    private static boolean isIdentifierChar(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private static boolean startsWithWord(String sql, int offset, String word) {
        if (sql == null || offset < 0 || offset + word.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, offset, word, 0, word.length())) {
            return false;
        }
        int end = offset + word.length();
        return end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
    }
}
