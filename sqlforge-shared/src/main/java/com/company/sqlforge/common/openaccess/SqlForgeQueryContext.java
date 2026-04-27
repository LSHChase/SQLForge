package com.company.sqlforge.common.openaccess;

import java.util.Map;

public class SqlForgeQueryContext {

    private String databaseName;
    private String schemaVersion;
    private Map<String, String> sessionVariables;
    private Long timeoutMs;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, String> getSessionVariables() {
        return sessionVariables;
    }

    public void setSessionVariables(Map<String, String> sessionVariables) {
        this.sessionVariables = sessionVariables;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
