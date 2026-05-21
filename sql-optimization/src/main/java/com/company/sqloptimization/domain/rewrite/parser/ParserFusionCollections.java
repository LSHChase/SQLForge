package com.company.sqloptimization.domain.rewrite.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ParserFusionCollections {

    private ParserFusionCollections() {
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static int intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<Object>) value) {
            if (item instanceof Map) {
                result.add(new LinkedHashMap<String, Object>((Map<String, Object>) item));
            }
        }
        return result;
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
