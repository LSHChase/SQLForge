package com.company.sqloptimization.domain.rewrite.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RuleEngineCollections {

    private RuleEngineCollections() {
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static Map<String, Object> immutableMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
    }

    static List<String> immutableStrings(Collection<String> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(value));
    }

    static List<Map<String, Object>> immutableMaps(Collection<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(value.size());
        for (Map<String, Object> item : value) {
            result.add(immutableMap(item));
        }
        return Collections.unmodifiableList(result);
    }

    static <T> List<T> immutableList(Collection<T> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(value));
    }
}
