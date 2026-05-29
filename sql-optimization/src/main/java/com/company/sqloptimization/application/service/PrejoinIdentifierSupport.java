package com.company.sqloptimization.application.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class PrejoinIdentifierSupport {

    private PrejoinIdentifierSupport() {
    }

    static String replaceIdentifier(String expression, String source, String target) {
        if (!StringUtils.hasText(expression) || !StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return expression;
        }
        String normalizedSource = cleanReference(source);
        Pattern pattern = Pattern.compile(
            "(?i)(^|[^A-Z0-9_$.])" + Pattern.quote(normalizedSource) + "([^A-Z0-9_]|$)"
        );
        return pattern.matcher(expression).replaceAll("$1" + target + "$2");
    }

    static boolean isIdentifierReference(String expression) {
        return StringUtils.hasText(expression)
            && cleanReference(expression).matches("[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*)*");
    }

    static String qualifier(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? "" : cleaned.substring(0, index);
    }

    static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    static String cleanReference(String expression) {
        return StringUtils.hasText(expression)
            ? expression.replace("`", "").replace("\"", "").trim()
            : "";
    }

    static String cleanName(String value) {
        String cleaned = StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
            : "";
        cleaned = cleaned.replaceAll("^_+", "").replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "field";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "f_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }

    static String uniqueName(String candidate, Set<String> usedNames) {
        String base = StringUtils.hasText(candidate) ? candidate : "field";
        String result = base;
        int sequence = 2;
        while (usedNames.contains(result)) {
            result = base + "_" + sequence;
            sequence++;
        }
        usedNames.add(result);
        return result;
    }

    static String relationKey(String value) {
        return cleanReference(value).toUpperCase(Locale.ROOT);
    }

    static String normalizeName(String value) {
        return StringUtils.hasText(value)
            ? cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
            : "";
    }
}
