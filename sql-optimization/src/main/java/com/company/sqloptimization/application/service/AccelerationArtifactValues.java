package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class AccelerationArtifactValues {

    private AccelerationArtifactValues() {
    }

    static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    static boolean containsRule(List<Map<String, Object>> ruleChain, String expectedRule) {
        if (ruleChain == null || ruleChain.isEmpty()) {
            return false;
        }
        for (Map<String, Object> item : ruleChain) {
            Object rule = item == null ? null : firstObject(item.get("rule"), item.get("ruleCode"));
            if (expectedRule.equals(String.valueOf(rule))) {
                return true;
            }
        }
        return false;
    }

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return null;
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    static List<String> steps() {
        return Arrays.asList(
            "检查表元数据、分区键、字段血缘",
            "生成物化视图 DDL",
            "执行只读 explain / 成本评估",
            "创建或刷新物化视图",
            "对比原 SQL 与 MV 查询结果",
            "激活 runtime rewrite binding",
            "异常时暂停绑定并保留恢复证据"
        );
    }

    static Map<String, Object> source(L2AccelerationArtifactBuilder.AccelerationRecommendationInput input) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("sqlFingerprint", input.sqlFingerprint);
        source.put("reportCode", input.reportCode);
        source.put("logicalObjectKey", input.logicalObjectKey);
        return source;
    }

    static Object mapValue(Object value, String key) {
        if (!(value instanceof Map<?, ?>)) {
            return null;
        }
        return ((Map<?, ?>) value).get(key);
    }

    static void addIfText(Set<String> values, String value) {
        if (values != null && StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
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

    private static Object firstObject(Object... values) {
        for (Object value : values) {
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return value;
            }
        }
        return null;
    }
}
