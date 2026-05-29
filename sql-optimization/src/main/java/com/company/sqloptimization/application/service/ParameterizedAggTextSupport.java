package com.company.sqloptimization.application.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class ParameterizedAggTextSupport {

    private ParameterizedAggTextSupport() {
    }

    static String replaceIdentifier(String expression, String source, String target) {
        if (!StringUtils.hasText(expression) || !StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return expression;
        }
        String normalizedSource = source.replace("`", "").replace("\"", "").trim();
        Pattern pattern = Pattern.compile(
            "(?i)(^|[^A-Z0-9_$.])" + Pattern.quote(normalizedSource) + "([^A-Z0-9_]|$)"
        );
        return pattern.matcher(expression).replaceAll("$1" + target + "$2");
    }

    static String withAlias(String expression, String alias) {
        if (!StringUtils.hasText(alias) || alias.equalsIgnoreCase(expression)) {
            return expression;
        }
        return expression + " AS " + alias;
    }

    static boolean sameExpression(String left, String right) {
        return normalizeExpression(left).equals(normalizeExpression(right));
    }

    static boolean containsAggregateToken(String expression) {
        String upper = expression.toUpperCase(Locale.ROOT);
        return upper.contains("SUM(")
            || upper.contains("COUNT(")
            || upper.contains("MIN(")
            || upper.contains("MAX(")
            || upper.contains("AVG(");
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

    static String uniqueName(String candidate, Set<String> usedNames) {
        String base = StringUtils.hasText(candidate) ? candidate : "dimension";
        String result = base;
        int sequence = 2;
        while (usedNames.contains(result)) {
            result = base + "_" + sequence;
            sequence++;
        }
        usedNames.add(result);
        return result;
    }

    static String columnName(String expression) {
        String candidate = expression;
        if (isIdentifierReference(candidate)) {
            candidate = unqualifiedName(candidate);
        }
        String cleaned = candidate.toLowerCase(Locale.ROOT)
            .replace("`", "")
            .replace("\"", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+", "")
            .replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "dimension";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "d_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }

    static boolean isIdentifierReference(String expression) {
        return StringUtils.hasText(expression)
            && expression.replace("`", "").replace("\"", "").trim()
                .matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    }

    static String unqualifiedName(String expression) {
        String cleaned = expression.replace("`", "").replace("\"", "").trim();
        int index = cleaned.lastIndexOf('.');
        return index >= 0 ? cleaned.substring(index + 1) : cleaned;
    }

    static String normalizeExpression(String expression) {
        return StringUtils.hasText(expression)
            ? expression.replace("`", "")
                .replace("\"", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
            : "";
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
}
