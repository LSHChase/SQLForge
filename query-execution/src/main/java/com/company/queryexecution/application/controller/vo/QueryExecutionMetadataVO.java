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
    private final String routeProfile;
    private final List<String> routeOrder;
    private final String routeEvidenceSource;
    private final String routeVerificationStatus;

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
            0,
            null,
            Collections.<String>emptyList(),
            null,
            null
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
        this(
            targetEngine,
            actualSql,
            elapsedMs,
            scannedRows,
            cacheHit,
            accelerationApplied,
            executionMode,
            attemptedModes,
            rowCount,
            null,
            Collections.<String>emptyList(),
            null,
            null
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
                                    int rowCount,
                                    String routeProfile,
                                    List<String> routeOrder,
                                    String routeEvidenceSource,
                                    String routeVerificationStatus) {
        this.targetEngine = targetEngine;
        this.actualSql = actualSql;
        this.elapsedMs = elapsedMs;
        this.scannedRows = scannedRows;
        this.cacheHit = cacheHit;
        this.accelerationApplied = accelerationApplied;
        this.executionMode = executionMode;
        this.attemptedModes = attemptedModes == null ? Collections.<String>emptyList() : attemptedModes;
        this.rowCount = rowCount;
        this.routeProfile = routeProfile;
        this.routeOrder = routeOrder == null ? Collections.<String>emptyList() : routeOrder;
        this.routeEvidenceSource = routeEvidenceSource;
        this.routeVerificationStatus = routeVerificationStatus;
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

    public String getRouteProfile() {
        return routeProfile;
    }

    public List<String> getRouteOrder() {
        return routeOrder;
    }

    public String getRouteEvidenceSource() {
        return routeEvidenceSource;
    }

    public String getRouteVerificationStatus() {
        return routeVerificationStatus;
    }
}
