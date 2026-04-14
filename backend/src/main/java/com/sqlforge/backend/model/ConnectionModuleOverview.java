package com.sqlforge.backend.model;

import java.util.List;

public class ConnectionModuleOverview {

    private final int totalConnections;
    private final int driverReadyCount;
    private final int driverMissingCount;
    private final int recentActivityCount;
    private final int lastProbeHealthyCount;
    private final int lastPreviewHealthyCount;
    private final List<ConnectionEngineOverview> engines;

    public ConnectionModuleOverview(
        int totalConnections,
        int driverReadyCount,
        int driverMissingCount,
        int recentActivityCount,
        int lastProbeHealthyCount,
        int lastPreviewHealthyCount,
        List<ConnectionEngineOverview> engines
    ) {
        this.totalConnections = totalConnections;
        this.driverReadyCount = driverReadyCount;
        this.driverMissingCount = driverMissingCount;
        this.recentActivityCount = recentActivityCount;
        this.lastProbeHealthyCount = lastProbeHealthyCount;
        this.lastPreviewHealthyCount = lastPreviewHealthyCount;
        this.engines = engines;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public int getDriverReadyCount() {
        return driverReadyCount;
    }

    public int getDriverMissingCount() {
        return driverMissingCount;
    }

    public int getRecentActivityCount() {
        return recentActivityCount;
    }

    public int getLastProbeHealthyCount() {
        return lastProbeHealthyCount;
    }

    public int getLastPreviewHealthyCount() {
        return lastPreviewHealthyCount;
    }

    public List<ConnectionEngineOverview> getEngines() {
        return engines;
    }
}
