package com.company.queryexecution.domain.query;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Captures the deterministic output of a single synchronous execution step.
 */
public class QueryExecutionStep {

    private final DataSourceTypeEnum targetEngine;
    private final List<Map<String, Object>> rows;
    private final long elapsedMs;
    private final long scannedRows;
    private final boolean cacheHit;
    private final boolean accelerationApplied;
    private final String executionMode;
    private final List<String> attemptedModes;

    public QueryExecutionStep(DataSourceTypeEnum targetEngine,
                              List<Map<String, Object>> rows,
                              long elapsedMs,
                              long scannedRows,
                              boolean cacheHit,
                              boolean accelerationApplied) {
        this(
            targetEngine,
            rows,
            elapsedMs,
            scannedRows,
            cacheHit,
            accelerationApplied,
            "SIMULATED",
            Collections.singletonList("SIMULATED")
        );
    }

    public QueryExecutionStep(DataSourceTypeEnum targetEngine,
                              List<Map<String, Object>> rows,
                              long elapsedMs,
                              long scannedRows,
                              boolean cacheHit,
                              boolean accelerationApplied,
                              String executionMode,
                              List<String> attemptedModes) {
        this.targetEngine = targetEngine;
        this.rows = rows;
        this.elapsedMs = elapsedMs;
        this.scannedRows = scannedRows;
        this.cacheHit = cacheHit;
        this.accelerationApplied = accelerationApplied;
        this.executionMode = executionMode;
        this.attemptedModes = attemptedModes == null ? Collections.<String>emptyList() : attemptedModes;
    }

    public DataSourceTypeEnum getTargetEngine() {
        return targetEngine;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public long getScannedRows() {
        return scannedRows;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public boolean isAccelerationApplied() {
        return accelerationApplied;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public List<String> getAttemptedModes() {
        return attemptedModes;
    }

    public QueryExecutionStep withAttemptedModes(List<String> newAttemptedModes) {
        return new QueryExecutionStep(
            targetEngine,
            rows,
            elapsedMs,
            scannedRows,
            cacheHit,
            accelerationApplied,
            executionMode,
            newAttemptedModes
        );
    }
}
