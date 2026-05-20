package com.company.sqloptimization.application.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

final class L2MaterializedViewNamePolicy {

    private static final int MAX_NAME_LENGTH = 63;
    private static final int HASH_LENGTH = 8;

    private L2MaterializedViewNamePolicy() {
    }

    static String mvName(String logicalObjectKey,
                         String reportCode,
                         String sqlFingerprint,
                         String sourceSql,
                         SqlOptimizationPipelineService.ParsedSqlProfile profile,
                         L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        String suffix = "_h" + shortHash(sqlFingerprint, sourceSql);
        String body = joinSegments(segments(
            firstText(logicalObjectKey, reportCode, "query"),
            mainTable(profile),
            mvTypeShort(grainMeasureDerivation == null ? null : grainMeasureDerivation.getMvType()),
            firstDimensions(grainMeasureDerivation == null ? null : grainMeasureDerivation.getDimensions())
        ));
        if (body.length() + suffix.length() <= MAX_NAME_LENGTH) {
            return body + suffix;
        }
        int allowedBodyLength = MAX_NAME_LENGTH - suffix.length();
        String truncated = body.substring(0, Math.max("mv_q".length(), allowedBodyLength));
        truncated = truncated.replaceAll("_+$", "");
        if (truncated.length() + suffix.length() > MAX_NAME_LENGTH) {
            truncated = truncated.substring(0, MAX_NAME_LENGTH - suffix.length()).replaceAll("_+$", "");
        }
        if (!StringUtils.hasText(truncated)) {
            truncated = "mv_q";
        }
        return truncated + suffix;
    }

    private static List<String> segments(String logicalOrReport,
                                         String mainTable,
                                         String mvTypeShort,
                                         String firstDimensions) {
        List<String> segments = new ArrayList<String>();
        segments.add("mv");
        segments.add(cleanName(logicalOrReport, "query"));
        segments.add(cleanName(mainTable, "source"));
        segments.add(cleanName(mvTypeShort, "mv"));
        segments.add(cleanName(firstDimensions, "nograin"));
        return segments;
    }

    private static String joinSegments(List<String> segments) {
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('_');
            }
            builder.append(segment);
        }
        return builder.toString().replaceAll("_+", "_").replaceAll("_+$", "");
    }

    private static String mainTable(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile == null || profile.getTables().isEmpty()) {
            return "query";
        }
        return profile.getTables().get(0);
    }

    private static String mvTypeShort(String mvType) {
        if (L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG.equals(mvType)) {
            return "paramagg";
        }
        if (L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(mvType)) {
            return "prejoin";
        }
        if (L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(mvType)) {
            return "staragg";
        }
        if (L2GrainMeasureDeriver.MV_TYPE_ROLLUP.equals(mvType)) {
            return "rollup";
        }
        if (L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(mvType)) {
            return "subgraph";
        }
        return "mv";
    }

    private static String firstDimensions(List<String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return "nograin";
        }
        List<String> result = new ArrayList<String>();
        for (String dimension : dimensions) {
            String cleaned = cleanName(dimension, "");
            if (StringUtils.hasText(cleaned)) {
                result.add(limitSegment(cleaned, 18));
            }
            if (result.size() >= 2) {
                break;
            }
        }
        return result.isEmpty() ? "nograin" : joinSegments(result);
    }

    private static String cleanName(String value, String fallback) {
        String cleaned = StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT)
                .replace("`", "")
                .replace("\"", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "")
            : "";
        if (!StringUtils.hasText(cleaned)) {
            cleaned = fallback;
        }
        if (StringUtils.hasText(cleaned) && Character.isDigit(cleaned.charAt(0))) {
            cleaned = "n_" + cleaned;
        }
        return limitSegment(cleaned, 32);
    }

    private static String limitSegment(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).replaceAll("_+$", "");
    }

    private static String shortHash(String sqlFingerprint, String sourceSql) {
        String fingerprint = cleanHash(sqlFingerprint);
        if (fingerprint.length() >= HASH_LENGTH) {
            return fingerprint.substring(0, HASH_LENGTH);
        }
        if (StringUtils.hasText(sqlFingerprint)) {
            return sha256(sqlFingerprint).substring(0, HASH_LENGTH);
        }
        return sha256(firstText(sourceSql, "query")).substring(0, HASH_LENGTH);
    }

    private static String cleanHash(String value) {
        return StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "")
            : "";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", Integer.valueOf(item & 0xff)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
