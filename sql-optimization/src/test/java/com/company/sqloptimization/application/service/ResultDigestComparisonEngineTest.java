package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResultDigestComparisonEngineTest {

    private final ResultDigestComparisonEngine engine = new ResultDigestComparisonEngine();

    @Test
    void shouldMarkEquivalentDigestsAsEquivalent() {
        ResultDigestComparisonResult result = engine.compare(
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1")),
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1")),
            Collections.<String, Object>emptyMap()
        );

        assertEquals(ValidationRunStatus.SUCCEEDED, result.getValidationRunStatus());
        assertEquals(ComparisonStatus.EQUIVALENT, result.getComparisonStatus());
        assertEquals(DifferenceType.NONE, result.getDifferenceType());
        assertFalse(result.isAutoApplyPaused());
    }

    @Test
    void shouldExposeRuntimeDeltaBenefitStatusFromExecutionEvidence() {
        ResultDigestComparisonResult result = engine.compare(
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1"), 100L, 1000L),
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1"), 60L, 500L),
            Collections.<String, Object>emptyMap()
        );

        Map<String, Object> runtimeDelta = nestedMap(result.getExecutionEvidence(), "runtimeDelta");
        assertEquals("POSITIVE", runtimeDelta.get("benefitStatus"));
        assertEquals(Double.valueOf(40D), runtimeDelta.get("elapsedImprovementPercent"));
        assertEquals(Double.valueOf(50D), runtimeDelta.get("scannedRowsImprovementPercent"));
    }

    @Test
    void shouldDetectSchemaDiffBeforeRowDiff() {
        ResultDigestComparisonResult result = engine.compare(
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1")),
            response("schema-b", Long.valueOf(3L), "key-a", "order-a", "checksum-a", row("id", "1")),
            Collections.<String, Object>emptyMap()
        );

        assertEquals(ComparisonStatus.DIVERGED, result.getComparisonStatus());
        assertEquals(DifferenceType.SCHEMA_DIFF, result.getDifferenceType());
        assertTrue(result.isAutoApplyPaused());
    }

    @Test
    void shouldDetectRowCountAndKeyDiffs() {
        ResultDigestComparisonResult rowCountResult = engine.compare(
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1")),
            response("schema-a", Long.valueOf(3L), "key-a", "order-a", "checksum-a", row("id", "1")),
            Collections.<String, Object>emptyMap()
        );
        ResultDigestComparisonResult keyResult = engine.compare(
            response("schema-a", Long.valueOf(2L), "key-a", "order-a", "checksum-a", row("id", "1")),
            response("schema-a", Long.valueOf(2L), "key-b", "order-a", "checksum-a", row("id", "1")),
            Collections.<String, Object>emptyMap()
        );

        assertEquals(DifferenceType.ROW_COUNT_DIFF, rowCountResult.getDifferenceType());
        assertEquals(DifferenceType.KEY_SET_DIFF, keyResult.getDifferenceType());
    }

    @Test
    void shouldDetectOrderSensitiveAndValueDiffs() {
        Map<String, Object> orderPolicy = new LinkedHashMap<String, Object>();
        orderPolicy.put("orderSensitive", Boolean.TRUE);
        ResultDigestComparisonResult orderResult = engine.compare(
            response("schema-a", Long.valueOf(2L), "", "order-a", "checksum-a", row("id", "1")),
            response("schema-a", Long.valueOf(2L), "", "order-b", "checksum-a", row("id", "1")),
            orderPolicy
        );
        ResultDigestComparisonResult valueResult = engine.compare(
            response("schema-a", Long.valueOf(2L), "", "order-a", "checksum-a", row("id", "1")),
            response("schema-a", Long.valueOf(2L), "", "order-a", "checksum-b", row("id", "2")),
            Collections.<String, Object>emptyMap()
        );

        assertEquals(DifferenceType.ORDER_DIFF, orderResult.getDifferenceType());
        assertEquals(DifferenceType.VALUE_DIFF, valueResult.getDifferenceType());
    }

    @Test
    void shouldMarkExecutionFailureAsFailedComparison() {
        QueryExecutionResultDigestResponse failed = response(
            "schema-a",
            Long.valueOf(0L),
            "",
            "",
            "",
            row("id", "1")
        );
        failed.setStatus("FAILED");

        ResultDigestComparisonResult result = engine.compare(
            failed,
            response("schema-a", Long.valueOf(0L), "", "", "", row("id", "1")),
            Collections.<String, Object>emptyMap()
        );

        assertEquals(ValidationRunStatus.FAILED, result.getValidationRunStatus());
        assertEquals(ComparisonStatus.FAILED, result.getComparisonStatus());
        assertEquals(DifferenceType.UNKNOWN, result.getDifferenceType());
    }

    private QueryExecutionResultDigestResponse response(String schemaDigest,
                                                        Long rowCount,
                                                        String keySetDigest,
                                                        String orderDigest,
                                                        String checksumDigest,
                                                        Map<String, Object> sampleRow) {
        QueryExecutionResultDigestResponse response = new QueryExecutionResultDigestResponse();
        response.setStatus("SUCCESS");
        response.setResultDigest(digest(schemaDigest, rowCount, keySetDigest, orderDigest, checksumDigest));
        response.setLimitedSample(Collections.singletonList(sampleRow));
        response.setExecutionEvidence(Collections.<String, Object>singletonMap("readonlyDigestOnly", Boolean.TRUE));
        return response;
    }

    private QueryExecutionResultDigestResponse response(String schemaDigest,
                                                        Long rowCount,
                                                        String keySetDigest,
                                                        String orderDigest,
                                                        String checksumDigest,
                                                        Map<String, Object> sampleRow,
                                                        long elapsedMs,
                                                        long scannedRows) {
        QueryExecutionResultDigestResponse response = response(
            schemaDigest,
            rowCount,
            keySetDigest,
            orderDigest,
            checksumDigest,
            sampleRow
        );
        Map<String, Object> executionEvidence = new LinkedHashMap<String, Object>();
        executionEvidence.put("readonlyDigestOnly", Boolean.TRUE);
        executionEvidence.put("elapsedMs", Long.valueOf(elapsedMs));
        executionEvidence.put("scannedRows", Long.valueOf(scannedRows));
        response.setExecutionEvidence(executionEvidence);
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> value, String key) {
        return (Map<String, Object>) value.get(key);
    }

    private Map<String, Object> digest(String schemaDigest,
                                       Long rowCount,
                                       String keySetDigest,
                                       String orderDigest,
                                       String checksumDigest) {
        Map<String, Object> digest = new LinkedHashMap<String, Object>();
        digest.put("schemaDigest", schemaDigest);
        digest.put("rowCount", rowCount);
        digest.put("keySetDigest", keySetDigest);
        digest.put("orderDigest", orderDigest);
        digest.put("checksumDigest", checksumDigest);
        return digest;
    }

    private Map<String, Object> row(String key, String value) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put(key, value);
        return row;
    }
}
