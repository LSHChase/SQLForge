package com.company.queryexecution.application.controller.vo;

public class QueryExecutionMetadataVO {

    private final String targetEngine;
    private final String actualSql;
    private final long elapsedMs;
    private final long scannedRows;
    private final boolean cacheHit;
    private final boolean accelerationApplied;

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied) {
        this.targetEngine = targetEngine;
        this.actualSql = actualSql;
        this.elapsedMs = elapsedMs;
        this.scannedRows = scannedRows;
        this.cacheHit = cacheHit;
        this.accelerationApplied = accelerationApplied;
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
}
