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
    private final String routeProfile;
    private final List<String> routeOrder;
    private final String routeEvidenceSource;
    private final String routeVerificationStatus;
    private final String cacheGovernanceStatus;
    private final String cacheGovernanceEvidence;

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
            Collections.singletonList("SIMULATED"),
            null,
            Collections.<String>emptyList(),
            null,
            null,
            null,
            null
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
        this(
            targetEngine,
            rows,
            elapsedMs,
            scannedRows,
            cacheHit,
            accelerationApplied,
            executionMode,
            attemptedModes,
            null,
            Collections.<String>emptyList(),
            null,
            null,
            null,
            null
        );
    }

    public QueryExecutionStep(DataSourceTypeEnum targetEngine,
                              List<Map<String, Object>> rows,
                              long elapsedMs,
                              long scannedRows,
                              boolean cacheHit,
                              boolean accelerationApplied,
                              String executionMode,
                              List<String> attemptedModes,
                              String routeProfile,
                              List<String> routeOrder,
                              String routeEvidenceSource,
                              String routeVerificationStatus,
                              String cacheGovernanceStatus,
                              String cacheGovernanceEvidence) {
        this.targetEngine = targetEngine;
        this.rows = rows;
        this.elapsedMs = elapsedMs;
        this.scannedRows = scannedRows;
        this.cacheHit = cacheHit;
        this.accelerationApplied = accelerationApplied;
        this.executionMode = executionMode;
        this.attemptedModes = attemptedModes == null ? Collections.<String>emptyList() : attemptedModes;
        this.routeProfile = routeProfile;
        this.routeOrder = routeOrder == null ? Collections.<String>emptyList() : routeOrder;
        this.routeEvidenceSource = routeEvidenceSource;
        this.routeVerificationStatus = routeVerificationStatus;
        this.cacheGovernanceStatus = cacheGovernanceStatus;
        this.cacheGovernanceEvidence = cacheGovernanceEvidence;
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

    public String getCacheGovernanceStatus() {
        return cacheGovernanceStatus;
    }

    public String getCacheGovernanceEvidence() {
        return cacheGovernanceEvidence;
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
            newAttemptedModes,
            routeProfile,
            routeOrder,
            routeEvidenceSource,
            routeVerificationStatus,
            cacheGovernanceStatus,
            cacheGovernanceEvidence
        );
    }

    public QueryExecutionStep withRouteCalibration(String newRouteProfile,
                                                   List<String> newRouteOrder,
                                                   String newRouteEvidenceSource,
                                                   String newRouteVerificationStatus) {
        return new QueryExecutionStep(
            targetEngine,
            rows,
            elapsedMs,
            scannedRows,
            cacheHit,
            accelerationApplied,
            executionMode,
            attemptedModes,
            newRouteProfile,
            newRouteOrder,
            newRouteEvidenceSource,
            newRouteVerificationStatus,
            cacheGovernanceStatus,
            cacheGovernanceEvidence
        );
    }

    public QueryExecutionStep withCacheGovernance(boolean newCacheHit,
                                                  String newCacheGovernanceStatus,
                                                  String newCacheGovernanceEvidence) {
        return new QueryExecutionStep(
            targetEngine,
            rows,
            elapsedMs,
            scannedRows,
            newCacheHit,
            accelerationApplied,
            executionMode,
            attemptedModes,
            routeProfile,
            routeOrder,
            routeEvidenceSource,
            routeVerificationStatus,
            newCacheGovernanceStatus,
            newCacheGovernanceEvidence
        );
    }
}
