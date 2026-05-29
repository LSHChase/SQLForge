package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ValidationSqlValues.text;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class ValidationSqlText {

    private static final Pattern SAFE_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private ValidationSqlText() {
    }

    static String sqlIdentifier(String value) {
        String cleaned = cleanOutputIdentifier(value);
        if (isSafeIdentifier(cleaned)) {
            return cleaned;
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    static boolean isSafeIdentifier(String value) {
        return StringUtils.hasText(value) && SAFE_IDENTIFIER_PATTERN.matcher(value).matches();
    }

    static String sqlLiteral(String value) {
        return "'" + text(value).replace("'", "''") + "'";
    }

    static String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression) || !StringUtils.hasText(alias)) {
            return expression;
        }
        String suffix = " AS " + alias;
        if (expression.toUpperCase(Locale.ROOT).endsWith(suffix.toUpperCase(Locale.ROOT))) {
            return expression.substring(0, expression.length() - suffix.length()).trim();
        }
        return expression;
    }

    static boolean isSimpleColumnExpression(String expression) {
        return StringUtils.hasText(expression)
            && expression.trim().matches("(?i)[A-Z_][A-Z0-9_$]*(\\.[A-Z_][A-Z0-9_$]*)?");
    }

    static String unqualifiedName(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String trimmed = expression.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        return dotIndex >= 0 ? trimmed.substring(dotIndex + 1) : trimmed;
    }

    static String normalizeExpression(String value) {
        return text(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    static String cleanOutputIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
            result = result.substring(1, result.length() - 1);
        }
        while (result.startsWith("`") && result.endsWith("`") && result.length() > 1) {
            result = result.substring(1, result.length() - 1);
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
}
