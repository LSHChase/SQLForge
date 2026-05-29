package com.company.sqloptimization.application.service;

import java.util.Set;
import org.apache.calcite.sql.SqlIdentifier;
import org.springframework.util.StringUtils;

final class CommonSubgraphRelationName {

    private CommonSubgraphRelationName() {
    }

    static void addRelationKey(Set<String> sources, String relation) {
        if (!StringUtils.hasText(relation)) {
            return;
        }
        sources.add(relationKey(relation));
        sources.add(relationKey(unqualifiedName(relation)));
    }

    static boolean containsRelationName(Set<String> names, String relationName) {
        if (names == null || !StringUtils.hasText(relationName)) {
            return false;
        }
        String expected = relationKey(relationName);
        String expectedUnqualified = relationKey(unqualifiedName(relationName));
        for (String name : names) {
            String actual = relationKey(name);
            if (actual.equals(expected) || relationKey(unqualifiedName(actual)).equals(expectedUnqualified)) {
                return true;
            }
        }
        return false;
    }

    static boolean relationNameMatches(SqlIdentifier identifier, String relationName) {
        if (identifier == null || !StringUtils.hasText(relationName)) {
            return false;
        }
        String actual = relationKey(identifier.toString());
        String expected = relationKey(relationName);
        return actual.equals(expected)
            || relationKey(unqualifiedName(actual)).equals(relationKey(unqualifiedName(expected)));
    }

    static String relationKey(String value) {
        return cleanReference(value).replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
    }

    static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    private static String cleanReference(String value) {
        return StringUtils.hasText(value)
            ? value.replace("`", "").replace("\"", "").replaceAll("\\s*\\.\\s*", ".").trim()
            : "";
    }
}
