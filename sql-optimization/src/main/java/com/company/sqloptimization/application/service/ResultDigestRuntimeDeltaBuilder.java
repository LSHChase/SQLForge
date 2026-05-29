package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

final class ResultDigestRuntimeDeltaBuilder {

    private ResultDigestRuntimeDeltaBuilder() {
    }

    static Map<String, Object> runtimeDelta(QueryExecutionResultDigestResponse original,
                                            QueryExecutionResultDigestResponse recommended) {
        Map<String, Object> originalExecution = executionEvidence(original);
        Map<String, Object> recommendedExecution = executionEvidence(recommended);
        Long originalElapsedMs = longValue(originalExecution.get("elapsedMs"));
        Long recommendedElapsedMs = longValue(recommendedExecution.get("elapsedMs"));
        Long originalScannedRows = longValue(originalExecution.get("scannedRows"));
        Long recommendedScannedRows = longValue(recommendedExecution.get("scannedRows"));
        Double elapsedImprovementPercent = improvementPercent(originalElapsedMs, recommendedElapsedMs);
        Double scannedRowsImprovementPercent = improvementPercent(originalScannedRows, recommendedScannedRows);

        Map<String, Object> delta = new LinkedHashMap<String, Object>();
        putIfPresent(delta, "originalElapsedMs", originalElapsedMs);
        putIfPresent(delta, "recommendedElapsedMs", recommendedElapsedMs);
        putIfPresent(delta, "elapsedImprovementPercent", elapsedImprovementPercent);
        putIfPresent(delta, "originalScannedRows", originalScannedRows);
        putIfPresent(delta, "recommendedScannedRows", recommendedScannedRows);
        putIfPresent(delta, "scannedRowsImprovementPercent", scannedRowsImprovementPercent);
        delta.put("benefitStatus", benefitStatus(elapsedImprovementPercent, scannedRowsImprovementPercent));
        delta.put("claimBoundary", "DIGEST_EXECUTION_EVIDENCE_ONLY");
        return delta;
    }

    static Long longValue(Object value) {
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Map<String, Object> executionEvidence(QueryExecutionResultDigestResponse response) {
        if (response == null || response.getExecutionEvidence() == null) {
            return Collections.emptyMap();
        }
        return response.getExecutionEvidence();
    }

    private static Double improvementPercent(Long original, Long recommended) {
        if (original == null || recommended == null || original.longValue() <= 0L) {
            return null;
        }
        return Double.valueOf(((double) original.longValue() - (double) recommended.longValue())
            * 100D / (double) original.longValue());
    }

    private static String benefitStatus(Double elapsedImprovementPercent,
                                        Double scannedRowsImprovementPercent) {
        if (isRegression(elapsedImprovementPercent) || isRegression(scannedRowsImprovementPercent)) {
            return "REGRESSED";
        }
        if (isImprovement(elapsedImprovementPercent) || isImprovement(scannedRowsImprovementPercent)) {
            return "POSITIVE";
        }
        if (elapsedImprovementPercent != null || scannedRowsImprovementPercent != null) {
            return "NEUTRAL";
        }
        return "UNKNOWN";
    }

    private static boolean isImprovement(Double value) {
        return value != null && value.doubleValue() >= 5D;
    }

    private static boolean isRegression(Double value) {
        return value != null && value.doubleValue() <= -5D;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
