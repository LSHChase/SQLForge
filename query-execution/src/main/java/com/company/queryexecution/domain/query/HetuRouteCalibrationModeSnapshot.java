package com.company.queryexecution.domain.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HetuRouteCalibrationModeSnapshot {

    private final QueryExecutionAccessMode mode;
    private final int priority;
    private final boolean allowed;
    private final boolean calibrationPreferred;
    private final boolean adapterAvailable;
    private final boolean configured;
    private final boolean ready;
    private final boolean willAttemptInCurrentPolicy;
    private final String readinessStatus;
    private final String readinessReason;
    private final Map<String, Object> routeParameters;

    public HetuRouteCalibrationModeSnapshot(QueryExecutionAccessMode mode,
                                            int priority,
                                            boolean allowed,
                                            boolean calibrationPreferred,
                                            boolean adapterAvailable,
                                            boolean configured,
                                            boolean ready,
                                            boolean willAttemptInCurrentPolicy,
                                            String readinessStatus,
                                            String readinessReason,
                                            Map<String, Object> routeParameters) {
        this.mode = mode;
        this.priority = priority;
        this.allowed = allowed;
        this.calibrationPreferred = calibrationPreferred;
        this.adapterAvailable = adapterAvailable;
        this.configured = configured;
        this.ready = ready;
        this.willAttemptInCurrentPolicy = willAttemptInCurrentPolicy;
        this.readinessStatus = readinessStatus;
        this.readinessReason = readinessReason;
        this.routeParameters = routeParameters == null
            ? Collections.<String, Object>emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(routeParameters));
    }

    public QueryExecutionAccessMode getMode() {
        return mode;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isCalibrationPreferred() {
        return calibrationPreferred;
    }

    public boolean isAdapterAvailable() {
        return adapterAvailable;
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isWillAttemptInCurrentPolicy() {
        return willAttemptInCurrentPolicy;
    }

    public String getReadinessStatus() {
        return readinessStatus;
    }

    public String getReadinessReason() {
        return readinessReason;
    }

    public Map<String, Object> getRouteParameters() {
        return routeParameters;
    }
}
