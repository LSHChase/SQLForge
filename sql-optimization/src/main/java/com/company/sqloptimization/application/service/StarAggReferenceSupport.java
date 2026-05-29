package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.intValue;
import static com.company.sqloptimization.application.service.StarAggProfileValues.firstText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class StarAggReferenceSupport {

    private static final Pattern QUALIFIED_REFERENCE_PATTERN =
        Pattern.compile("(?i)\\b[A-Z_][A-Z0-9_$]*\\.[A-Z_][A-Z0-9_$]*\\b");

    private StarAggReferenceSupport() {
    }

    static List<String> qualifiedReferences(String expression) {
        if (!StringUtils.hasText(expression)) {
            return java.util.Collections.emptyList();
        }
        Matcher matcher = QUALIFIED_REFERENCE_PATTERN.matcher(expression);
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return new ArrayList<String>(result);
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

    static String relationKey(String value) {
        return cleanReference(value).toUpperCase(Locale.ROOT);
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

    static String columnName(String expression) {
        String candidate = isIdentifierReference(expression) ? unqualifiedName(expression) : expression;
        return cleanName(candidate);
    }

    static String normalizeName(String value) {
        return StringUtils.hasText(value)
            ? cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
            : "";
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

    static boolean sameExpression(String left, String right) {
        return normalizeExpression(left).equals(normalizeExpression(right));
    }

    static boolean containsSameExpression(List<String> expressions, String expected) {
        for (String expression : expressions) {
            if (sameExpression(expression, expected)) {
                return true;
            }
        }
        return false;
    }

    static LinkedHashMap<String, Integer> dimensionNameCounts(List<String> dimensions) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String dimension : dimensions) {
            String key = normalizeName(unqualifiedName(dimension));
            Integer current = counts.get(key);
            counts.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }
        return counts;
    }

    static String outputName(String sourceExpression,
                             StarAggRelationSpec relation,
                             Map<String, Integer> unqualifiedCounts,
                             Set<String> usedNames) {
        String base = columnName(sourceExpression);
        if (isIdentifierReference(sourceExpression)
            && intValue(unqualifiedCounts.get(normalizeName(unqualifiedName(sourceExpression)))) > 1) {
            base = cleanName(firstText(relation.alias, relation.tableName) + "_" + unqualifiedName(sourceExpression));
        }
        return uniqueName(base, usedNames);
    }
}
