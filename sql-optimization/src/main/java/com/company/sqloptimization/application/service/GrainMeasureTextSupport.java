package com.company.sqloptimization.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class GrainMeasureTextSupport {

    private static final Pattern ALIAS_PATTERN =
        Pattern.compile("(?is)\\s+AS\\s+([A-Z_][A-Z0-9_]*)\\s*$");

    private GrainMeasureTextSupport() {
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

    static void addTextList(List<String> target, Set<String> seen, List<String> values) {
        for (String value : values) {
            addText(target, seen, value);
        }
    }

    static void addText(List<String> target, Set<String> seen, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String cleaned = value.trim();
        String key = normalizeName(cleaned);
        if (!StringUtils.hasText(key) || seen.contains(key)) {
            return;
        }
        seen.add(key);
        target.add(cleaned);
    }

    static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        if (StringUtils.hasText(alias)) {
            result = result.replaceAll("(?is)\\s+AS\\s+" + Pattern.quote(alias.trim()) + "\\s*$", "");
        }
        Matcher matcher = ALIAS_PATTERN.matcher(result);
        if (matcher.find()) {
            result = result.substring(0, matcher.start()).trim();
        }
        return result;
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

    static String cleanName(String value) {
        String cleaned = StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
            : "";
        cleaned = cleaned.replaceAll("^_+", "").replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "measure";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "m_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }

    static String normalizeExpression(String expression) {
        return StringUtils.hasText(expression)
            ? expression.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
            : "";
    }

    static String normalizeName(String value) {
        return StringUtils.hasText(value)
            ? value.replace('`', ' ')
                .replace('"', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
            : "";
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
