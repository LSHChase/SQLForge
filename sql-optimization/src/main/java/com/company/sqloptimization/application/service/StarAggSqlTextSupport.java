package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.cleanReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class StarAggSqlTextSupport {

    private StarAggSqlTextSupport() {
    }

    static int topLevelEqualsIndex(String expression) {
        int depth = 0;
        int result = -1;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            } else if (ch == '=' && depth == 0) {
                char before = i == 0 ? '\0' : expression.charAt(i - 1);
                char after = i == expression.length() - 1 ? '\0' : expression.charAt(i + 1);
                if (before == '<' || before == '>' || before == '!' || after == '=' || result >= 0) {
                    return -1;
                }
                result = i;
            } else if (depth == 0 && (ch == '<' || ch == '>')) {
                return -1;
            }
        }
        return result;
    }

    static List<String> splitAndConditions(String condition) {
        if (!StringUtils.hasText(condition)) {
            return java.util.Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        String upper = condition.toUpperCase(Locale.ROOT);
        for (int i = 0; i < condition.length(); i++) {
            char ch = condition.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0 && upper.startsWith(" AND ", i)) {
                addConditionPart(result, condition.substring(start, i));
                start = i + 5;
                i += 4;
            }
        }
        addConditionPart(result, condition.substring(start));
        return result;
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

    static String tableWithAlias(Map<String, Object> table) {
        String tableName = text(table.get("tableName"));
        String alias = text(table.get("alias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
            return tableName + " " + alias;
        }
        return tableName;
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

    static String withAlias(String expression, String alias) {
        if (!StringUtils.hasText(alias) || alias.equalsIgnoreCase(expression)) {
            return expression;
        }
        return expression + " AS " + alias;
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

    static String replaceIdentifier(String expression, String source, String target) {
        if (!StringUtils.hasText(expression) || !StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return expression;
        }
        Pattern pattern = Pattern.compile(
            "(?i)(^|[^A-Z0-9_$.])" + Pattern.quote(cleanReference(source)) + "([^A-Z0-9_]|$)"
        );
        return pattern.matcher(expression).replaceAll("$1" + target + "$2");
    }

    static boolean containsAggregateToken(String expression) {
        String upper = expression.toUpperCase(Locale.ROOT);
        return upper.contains("SUM(")
            || upper.contains("COUNT(")
            || upper.contains("MIN(")
            || upper.contains("MAX(")
            || upper.contains("AVG(");
    }

    private static void addConditionPart(List<String> target, String value) {
        String cleaned = stripOuterParentheses(value);
        if (StringUtils.hasText(cleaned)) {
            target.add(cleaned);
        }
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
