package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.normalizeExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class RollupSqlTextSupport {

    private RollupSqlTextSupport() {
    }

    static boolean containsAggregateToken(String expression) {
        String upper = expression.toUpperCase(Locale.ROOT);
        return upper.contains("SUM(")
            || upper.contains("COUNT(")
            || upper.contains("MIN(")
            || upper.contains("MAX(")
            || upper.contains("AVG(");
    }

    static boolean isTimeFunctionExpression(String expression) {
        String upper = normalizeExpression(stripAlias(expression, ""));
        return upper.startsWith("DATE_TRUNC(")
            || upper.startsWith("TRUNC(")
            || upper.startsWith("DATE_FORMAT(")
            || upper.startsWith("YEAR(")
            || upper.startsWith("MONTH(")
            || upper.startsWith("DAY(");
    }

    static boolean containsTimezoneDependency(String expression) {
        String upper = normalizeExpression(expression);
        return upper.contains(" AT TIME ZONE ")
            || upper.contains("CONVERT_TIMEZONE(")
            || upper.contains("CONVERT_TZ(")
            || upper.contains("TIMEZONE(");
    }

    static boolean containsFiscalCalendar(String expression) {
        return normalizeExpression(expression).contains("FISCAL");
    }

    static boolean containsFiscalCalendar(List<String> values) {
        for (String value : values) {
            if (containsFiscalCalendar(value)) {
                return true;
            }
        }
        return false;
    }

    static String leftPredicateField(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String[] operators = {" BETWEEN ", " IN ", ">=", "<=", "<>", "!=", "=", ">", "<", " LIKE "};
        String upper = expression.toUpperCase(Locale.ROOT);
        int index = -1;
        for (String operator : operators) {
            index = upper.indexOf(operator);
            if (index >= 0) {
                break;
            }
        }
        return index < 0 ? "" : expression.substring(0, index).trim();
    }

    static List<String> splitArguments(String value) {
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
            if (ch == ',' && depth == 0 && !inSingleQuote && !inDoubleQuote) {
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

    static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        if (StringUtils.hasText(alias)) {
            result = result.replaceAll("(?is)\\s+AS\\s+" + Pattern.quote(alias.trim()) + "\\s*$", "");
        }
        return result.replaceAll("(?is)\\s+AS\\s+[A-Z_][A-Z0-9_]*\\s*$", "").trim();
    }

    static String stripOuterParentheses(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        while (result.startsWith("(") && result.endsWith(")") && wrapsWholeExpression(result)) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
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

    private static boolean wrapsWholeExpression(String expression) {
        int depth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0 && i < expression.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }
}
