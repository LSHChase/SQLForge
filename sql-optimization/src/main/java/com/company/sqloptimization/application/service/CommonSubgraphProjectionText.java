package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class CommonSubgraphProjectionText {

    private CommonSubgraphProjectionText() {
    }

    static String firstSelectList(String sql) {
        String normalized = CommonSubgraphSqlText.trimTrailingSemicolon(
            CommonSubgraphSqlText.stripOuterParentheses(sql)
        );
        int selectIndex = findKeywordAtDepth(normalized, "SELECT", 0);
        if (selectIndex < 0) {
            return "";
        }
        int fromIndex = findKeywordAtDepth(normalized, "FROM", selectIndex + "SELECT".length());
        if (fromIndex < 0 || fromIndex <= selectIndex) {
            return "";
        }
        return normalized.substring(selectIndex + "SELECT".length(), fromIndex).trim();
    }

    static List<String> splitTopLevelComma(String value) {
        List<String> result = new ArrayList<String>();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktickQuote = !inBacktickQuote;
            } else if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote) {
                if (current == '(') {
                    depth++;
                } else if (current == ')') {
                    depth = Math.max(0, depth - 1);
                } else if (current == ',' && depth == 0) {
                    result.add(value.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        result.add(value.substring(start).trim());
        return result;
    }

    static String trailingAlias(String item) {
        Matcher quoted = Pattern.compile("(?is)\\s+AS\\s+(?:\"([^\"]+)\"|`([^`]+)`)\\s*$")
            .matcher(item == null ? "" : item);
        if (quoted.find()) {
            return CommonSubgraphSqlText.firstText(quoted.group(1), quoted.group(2));
        }
        Matcher unquoted = Pattern.compile("(?is)\\s+AS\\s+([\\p{L}_][\\p{L}\\p{N}_$]*)\\s*$")
            .matcher(item == null ? "" : item);
        return unquoted.find() ? unquoted.group(1) : "";
    }

    static String trailingColumnName(String item) {
        Matcher quoted = Pattern.compile("(?:\"([^\"]+)\"|`([^`]+)`)\\s*$").matcher(item == null ? "" : item);
        if (quoted.find()) {
            return CommonSubgraphSqlText.firstText(quoted.group(1), quoted.group(2));
        }
        Matcher unquoted = Pattern.compile("(?is)([\\p{L}_][\\p{L}\\p{N}_$]*)\\s*$")
            .matcher(item == null ? "" : item);
        return unquoted.find() ? unquoted.group(1) : "";
    }

    static String quotedIdentifierPattern(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return "\"[^\"]+\"|[A-Z_][A-Z0-9_$]*";
        }
        return "(?:\"" + Pattern.quote(identifier) + "\"|" + Pattern.quote(identifier) + ")";
    }

    private static int findKeywordAtDepth(String sql, String keyword, int offset) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        for (int i = Math.max(0, offset); i <= sql.length() - keyword.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktickQuote = !inBacktickQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote || inBacktickQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && CommonSubgraphSqlText.startsWithWord(sql, i, keyword)) {
                return i;
            }
        }
        return -1;
    }
}
