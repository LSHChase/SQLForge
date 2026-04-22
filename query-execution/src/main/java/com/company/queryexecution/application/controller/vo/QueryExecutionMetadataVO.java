package com.company.queryexecution.application.controller.vo;

import java.util.Collections;
import java.util.List;

public class QueryExecutionMetadataVO {

    private final String targetEngine;
    private final String actualSql;
    private final long elapsedMs;
    private final long scannedRows;
    private final boolean cacheHit;
    private final boolean accelerationApplied;
    private final String executionMode;
    private final List<String> attemptedModes;
    private final int rowCount;

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied) {
        this(
            targetEngine,
            actualSql,
            elapsedMs,
            scannedRows,
            cacheHit,
            accelerationApplied,
            "SIMULATED",
            Collections.singletonList("SIMULATED"),
            0
        );
    }

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied,
                                    String executionMode,
                                    List<String> attemptedModes,
                                    int rowCount) {
        this.targetEngine = targetEngine;
        this.actualSql = actualSql;
        this.elapsedMs = elapsedMs;
        this.scannedRows = scannedRows;
        this.cacheHit = cacheHit;
        this.accelerationApplied = accelerationApplied;
        this.executionMode = executionMode;
        this.attemptedModes = attemptedModes == null ? Collections.<String>emptyList() : attemptedModes;
        this.rowCount = rowCount;
    }

    public String getTargetEngine() {
        return targetEngine;
    }

    public String getActualSql() {
        return actualSql;
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

    public int getRowCount() {
        return rowCount;
    }
}
