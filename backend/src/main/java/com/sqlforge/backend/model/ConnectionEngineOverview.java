package com.sqlforge.backend.model;

public class ConnectionEngineOverview {

    private final String engineCode;
    private final int connectionCount;

    public ConnectionEngineOverview(String engineCode, int connectionCount) {
        this.engineCode = engineCode;
        this.connectionCount = connectionCount;
    }

    public String getEngineCode() {
        return engineCode;
    }

    public int getConnectionCount() {
        return connectionCount;
    }
}
