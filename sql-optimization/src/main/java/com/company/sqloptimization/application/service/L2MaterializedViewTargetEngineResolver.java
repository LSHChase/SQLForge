package com.company.sqloptimization.application.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2MaterializedViewTargetEngineResolver {

    private static final String DEFAULT_ENGINE = "HETU";
    private static final Pattern REPORT_SEARCH_MODE_PATTERN =
        Pattern.compile("(?im)^\\s*--\\s*YH_RPTSEARCHMODE\\s*=\\s*([A-Z0-9_\\-]+)\\s*$");

    private L2MaterializedViewTargetEngineResolver() {
    }

    static Resolution resolve(String targetEngine, String targetDatasource, String sourceSql) {
        String requested = normalize(targetEngine);
        if (StringUtils.hasText(requested) && !"AUTO".equals(requested)) {
            if (L2MaterializedViewDialectRenderer.supports(requested)) {
                return new Resolution(requested, "EXPLICIT_TARGET_ENGINE", requested, false);
            }
            return new Resolution(requested, "EXPLICIT_TARGET_ENGINE_UNSUPPORTED", requested, true);
        }
        String datasourceEngine = inferFromDatasource(targetDatasource);
        if (StringUtils.hasText(datasourceEngine)) {
            return new Resolution(datasourceEngine, "TARGET_DATASOURCE_HINT", requested, false);
        }
        String sqlHintEngine = inferFromSqlHint(sourceSql);
        if (StringUtils.hasText(sqlHintEngine)) {
            return new Resolution(sqlHintEngine, "SQL_REPORT_SEARCH_MODE_HINT", requested, false);
        }
        return new Resolution(DEFAULT_ENGINE, "DEFAULT_ENGINE", requested, false);
    }

    private static String inferFromDatasource(String targetDatasource) {
        String normalized = normalize(targetDatasource);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.contains("HETU") || normalized.contains("PRESTO") || normalized.contains("TRINO")) {
            return "HETU";
        }
        if (normalized.contains("HIVE")) {
            return "HIVE";
        }
        if (normalized.contains("SPARK")) {
            return "SPARK";
        }
        return "";
    }

    private static String inferFromSqlHint(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return "";
        }
        Matcher matcher = REPORT_SEARCH_MODE_PATTERN.matcher(sourceSql);
        if (!matcher.find()) {
            return "";
        }
        String mode = normalize(matcher.group(1));
        if ("HETU".equals(mode) || "PRESTO".equals(mode) || "TRINO".equals(mode)) {
            return "HETU";
        }
        if ("HIVE".equals(mode)) {
            return "HIVE";
        }
        if ("SPARK".equals(mode)) {
            return "SPARK";
        }
        return "";
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    static final class Resolution {

        private final String targetEngine;
        private final String source;
        private final String requestedEngine;
        private final boolean unsupportedExplicitEngine;

        private Resolution(String targetEngine,
                           String source,
                           String requestedEngine,
                           boolean unsupportedExplicitEngine) {
            this.targetEngine = targetEngine;
            this.source = source;
            this.requestedEngine = requestedEngine;
            this.unsupportedExplicitEngine = unsupportedExplicitEngine;
        }

        String getTargetEngine() {
            return targetEngine;
        }

        boolean isUnsupportedExplicitEngine() {
            return unsupportedExplicitEngine;
        }

        Map<String, Object> toEvidence() {
            LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("requestedEngine", requestedEngine);
            evidence.put("resolvedEngine", targetEngine);
            evidence.put("resolutionSource", source);
            evidence.put("unsupportedExplicitEngine", Boolean.valueOf(unsupportedExplicitEngine));
            evidence.put("autoAllowed", Boolean.TRUE);
            return evidence;
        }
    }
}
