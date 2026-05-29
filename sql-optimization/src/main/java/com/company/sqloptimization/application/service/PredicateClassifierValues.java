package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class PredicateClassifierValues {

    private PredicateClassifierValues() {
    }

    static boolean containsToken(String text, String token) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(token)) {
            return false;
        }
        String normalizedText = text.toUpperCase(Locale.ROOT).replace('`', ' ').replace('"', ' ');
        String normalizedToken = token.toUpperCase(Locale.ROOT);
        Pattern pattern = Pattern.compile("(^|[^A-Z0-9_])" + Pattern.quote(normalizedToken) + "([^A-Z0-9_]|$)");
        return pattern.matcher(normalizedText).find();
    }

    static String upperText(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item));
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

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }
}
