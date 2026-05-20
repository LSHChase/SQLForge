package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class AccelerationArtifactSnapshotSanitizer {

    private static final Set<String> ALLOWED_MV_TYPES = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "PARAMETERIZED_AGG_MV",
            "PREJOIN_MV",
            "STAR_AGG_MV",
            "ROLLUP_MV",
            "COMMON_SUBGRAPH_MV"
        ))
    );
    private static final List<String> LIST_FIELDS = Collections.unmodifiableList(Arrays.asList(
        "grain",
        "dimensions",
        "measures",
        "joinGraph",
        "requiredEvidence",
        "externalizedPredicates",
        "retainedPredicates",
        "securityPredicates",
        "blockedPredicates",
        "blockingReasons",
        "reviewWarnings"
    ));

    private AccelerationArtifactSnapshotSanitizer() {
    }

    static Map<String, Object> sanitize(Map<String, Object> artifact) {
        if (artifact == null || artifact.isEmpty()) {
            return null;
        }
        String mvType = text(artifact.get("mvType"));
        if (!ALLOWED_MV_TYPES.contains(mvType)) {
            return null;
        }
        LinkedHashMap<String, Object> normalized = deepCopyMap(artifact);
        normalized.put("mvType", mvType);
        normalizeListFields(normalized);
        normalizeMapField(normalized, "coverage");
        normalizeMapField(normalized, "source");
        String artifactStatus = text(normalized.get("artifactStatus"));
        if ("GENERATED".equals(artifactStatus) && !hasGeneratedSqlBundle(normalized)) {
            return null;
        }
        return normalized;
    }

    static boolean hasStoredSnapshot(Map<String, Object> artifact) {
        return artifact != null && !artifact.isEmpty();
    }

    private static void normalizeListFields(Map<String, Object> artifact) {
        for (String field : LIST_FIELDS) {
            Object value = artifact.get(field);
            if (value == null) {
                artifact.put(field, Collections.emptyList());
            } else if (!(value instanceof List<?>)) {
                artifact.put(field, Collections.singletonList(deepCopyValue(value)));
            }
        }
    }

    private static void normalizeMapField(Map<String, Object> artifact, String field) {
        Object value = artifact.get(field);
        if (value == null) {
            artifact.put(field, Collections.emptyMap());
        } else if (!(value instanceof Map<?, ?>)) {
            LinkedHashMap<String, Object> wrapped = new LinkedHashMap<String, Object>();
            wrapped.put("value", deepCopyValue(value));
            artifact.put(field, wrapped);
        }
    }

    private static boolean hasGeneratedSqlBundle(Map<String, Object> artifact) {
        return StringUtils.hasText(text(artifact.get("ddlSql")))
            && StringUtils.hasText(text(artifact.get("refreshSql")))
            && StringUtils.hasText(text(artifact.get("validationSql")))
            && StringUtils.hasText(text(artifact.get("rollbackSql")))
            && StringUtils.hasText(text(artifact.get("rewriteSql")));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return deepCopyMap((Map<?, ?>) value);
        }
        if (value instanceof List<?>) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (List<?>) value) {
                result.add(deepCopyValue(item));
            }
            return result;
        }
        return value;
    }

    private static LinkedHashMap<String, Object> deepCopyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        if (source == null) {
            return target;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
            }
        }
        return target;
    }
}
