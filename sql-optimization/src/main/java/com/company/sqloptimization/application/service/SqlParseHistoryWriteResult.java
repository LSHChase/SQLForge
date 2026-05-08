package com.company.sqloptimization.application.service;

public class SqlParseHistoryWriteResult {

    private final String parseHistoryId;
    private final Boolean persisted;
    private final String persistenceStatus;

    public SqlParseHistoryWriteResult(String parseHistoryId, Boolean persisted, String persistenceStatus) {
        this.parseHistoryId = parseHistoryId;
        this.persisted = persisted;
        this.persistenceStatus = persistenceStatus;
    }

    public String getParseHistoryId() {
        return parseHistoryId;
    }

    public Boolean getPersisted() {
        return persisted;
    }

    public String getPersistenceStatus() {
        return persistenceStatus;
    }
}
