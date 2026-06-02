package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResultDigestComparisonEngine {

    public ResultDigestComparisonResult compare(QueryExecutionResultDigestResponse original,
                                                QueryExecutionResultDigestResponse recommended,
                                                Map<String, Object> comparisonPolicy) {
        if (!isSuccessfulDigest(original) || !isSuccessfulDigest(recommended)) {
            return result(
                ValidationRunStatus.FAILED,
                ComparisonStatus.FAILED,
                DifferenceType.UNKNOWN,
                false,
                "只读摘要执行失败",
                original,
                recommended,
                comparisonPolicy
            );
        }
        if (!sameDigestValue(original, recommended, "schemaDigest")) {
            return diverged(DifferenceType.SCHEMA_DIFF, "schema 摘要不一致", original, recommended, comparisonPolicy);
        }
        if (!sameLongValue(original, recommended, "rowCount")) {
            return diverged(DifferenceType.ROW_COUNT_DIFF, "行数不一致", original, recommended, comparisonPolicy);
        }
        if (hasDigestValue(original, "keySetDigest") && !sameDigestValue(original, recommended, "keySetDigest")) {
            return diverged(DifferenceType.KEY_SET_DIFF, "键集合摘要不一致", original, recommended, comparisonPolicy);
        }
        if (isOrderSensitive(comparisonPolicy)
            && !sameDigestValue(original, recommended, "orderDigest")) {
            return diverged(DifferenceType.ORDER_DIFF, "顺序敏感摘要不一致", original, recommended, comparisonPolicy);
        }
        if (!sameDigestValue(original, recommended, "checksumDigest")) {
            DifferenceType type = sameLimitedSample(original, recommended)
                ? DifferenceType.CHECKSUM_DIFF
                : DifferenceType.VALUE_DIFF;
            return diverged(type, "checksum 摘要不一致", original, recommended, comparisonPolicy);
        }
        return result(
            ValidationRunStatus.SUCCEEDED,
            ComparisonStatus.EQUIVALENT,
            DifferenceType.NONE,
            false,
            "结果摘要等价",
            original,
            recommended,
            comparisonPolicy
        );
    }

    private ResultDigestComparisonResult diverged(DifferenceType type,
                                                  String reason,
                                                  QueryExecutionResultDigestResponse original,
                                                  QueryExecutionResultDigestResponse recommended,
                                                  Map<String, Object> comparisonPolicy) {
        return result(
            ValidationRunStatus.SUCCEEDED,
            ComparisonStatus.DIVERGED,
            type,
            true,
            reason,
            original,
            recommended,
            comparisonPolicy
        );
    }

    private ResultDigestComparisonResult result(ValidationRunStatus validationRunStatus,
                                                ComparisonStatus comparisonStatus,
                                                DifferenceType differenceType,
                                                boolean autoApplyPaused,
                                                String reason,
                                                QueryExecutionResultDigestResponse original,
                                                QueryExecutionResultDigestResponse recommended,
                                                Map<String, Object> comparisonPolicy) {
        Map<String, Object> sample = new LinkedHashMap<String, Object>();
        sample.put("reason", reason);
        sample.put("differenceType", differenceType.name());
        sample.put("originalDigest", safeDigest(original));
        sample.put("recommendedDigest", safeDigest(recommended));
        sample.put("originalSample", original == null ? Collections.emptyList() : original.getLimitedSample());
        sample.put("recommendedSample", recommended == null ? Collections.emptyList() : recommended.getLimitedSample());

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("comparisonPolicy", comparisonPolicy == null ? Collections.emptyMap() : comparisonPolicy);
        evidence.put("originalExecution", original == null ? Collections.emptyMap() : original.getExecutionEvidence());
        evidence.put("recommendedExecution", recommended == null ? Collections.emptyMap() : recommended.getExecutionEvidence());
        evidence.put("originalStatus", original == null ? null : original.getStatus());
        evidence.put("recommendedStatus", recommended == null ? null : recommended.getStatus());
        evidence.put("runtimeDelta", runtimeDelta(original, recommended));
        evidence.put("readonlyDigestOnly", Boolean.TRUE);
        return new ResultDigestComparisonResult(
            validationRunStatus,
            comparisonStatus,
            differenceType,
            autoApplyPaused,
            sample,
            evidence
        );
    }

    private boolean isSuccessfulDigest(QueryExecutionResultDigestResponse response) {
        if (response == null || !StringUtils.hasText(response.getStatus())) {
            return false;
        }
        return "SUCCESS".equals(response.getStatus()) || "PARTIAL".equals(response.getStatus());
    }

    private boolean hasDigestValue(QueryExecutionResultDigestResponse response, String key) {
        Object value = value(response, key);
        return value != null && StringUtils.hasText(String.valueOf(value));
    }

    private boolean sameDigestValue(QueryExecutionResultDigestResponse original,
                                    QueryExecutionResultDigestResponse recommended,
                                    String key) {
        Object originalValue = value(original, key);
        Object recommendedValue = value(recommended, key);
        if (originalValue == null) {
            return recommendedValue == null;
        }
        return originalValue.equals(recommendedValue);
    }

    private boolean sameLongValue(QueryExecutionResultDigestResponse original,
                                  QueryExecutionResultDigestResponse recommended,
                                  String key) {
        Long originalValue = longValue(value(original, key));
        Long recommendedValue = longValue(value(recommended, key));
        if (originalValue == null) {
            return recommendedValue == null;
        }
        return originalValue.equals(recommendedValue);
    }

    private boolean sameLimitedSample(QueryExecutionResultDigestResponse original,
                                      QueryExecutionResultDigestResponse recommended) {
        List<Map<String, Object>> originalSample = original == null ? null : original.getLimitedSample();
        List<Map<String, Object>> recommendedSample = recommended == null ? null : recommended.getLimitedSample();
        if (originalSample == null) {
            return recommendedSample == null;
        }
        return originalSample.equals(recommendedSample);
    }

    private Object value(QueryExecutionResultDigestResponse response, String key) {
        if (response == null || response.getResultDigest() == null) {
            return null;
        }
        return response.getResultDigest().get(key);
    }

    private Long longValue(Object value) {
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

    private boolean isOrderSensitive(Map<String, Object> comparisonPolicy) {
        if (comparisonPolicy == null) {
            return false;
        }
        Object value = comparisonPolicy.get("orderSensitive");
        return value instanceof Boolean
            ? ((Boolean) value).booleanValue()
            : value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private Map<String, Object> runtimeDelta(QueryExecutionResultDigestResponse original,
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

    private Map<String, Object> executionEvidence(QueryExecutionResultDigestResponse response) {
        if (response == null || response.getExecutionEvidence() == null) {
            return Collections.emptyMap();
        }
        return response.getExecutionEvidence();
    }

    private Double improvementPercent(Long original, Long recommended) {
        if (original == null || recommended == null || original.longValue() <= 0L) {
            return null;
        }
        return Double.valueOf(((double) original.longValue() - (double) recommended.longValue())
            * 100D / (double) original.longValue());
    }

    private String benefitStatus(Double elapsedImprovementPercent,
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

    private boolean isImprovement(Double value) {
        return value != null && value.doubleValue() >= 5D;
    }

    private boolean isRegression(Double value) {
        return value != null && value.doubleValue() <= -5D;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private Map<String, Object> safeDigest(QueryExecutionResultDigestResponse response) {
        if (response == null || response.getResultDigest() == null) {
            return Collections.emptyMap();
        }
        return response.getResultDigest();
    }
}
