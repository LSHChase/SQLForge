package com.sqlforge.backend.model;

public class EngineDriverAudit {

    private final String engineCode;
    private final String engineName;
    private final String driverClassName;
    private final boolean available;

    public EngineDriverAudit(String engineCode, String engineName, String driverClassName, boolean available) {
        this.engineCode = engineCode;
        this.engineName = engineName;
        this.driverClassName = driverClassName;
        this.available = available;
    }

    public String getEngineCode() {
        return engineCode;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public boolean isAvailable() {
        return available;
    }
}
