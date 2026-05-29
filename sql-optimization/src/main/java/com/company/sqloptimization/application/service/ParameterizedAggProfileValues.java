package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class ParameterizedAggProfileValues {

    private ParameterizedAggProfileValues() {
    }

    static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    static Object mapValue(Object value, String key) {
        if (!(value instanceof Map<?, ?>)) {
            return null;
        }
        return ((Map<?, ?>) value).get(key);
    }

    static boolean booleanValue(Object value) {
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }
        return result;
    }

    static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (Iterable<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add(copyMap((Map<?, ?>) item));
            }
        }
        return result;
    }

    static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static List<Map<String, Object>> immutableMapList(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : source) {
            result.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(item)));
        }
        return Collections.unmodifiableList(result);
    }
}
