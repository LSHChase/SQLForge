package com.company.queryexecution.application.controller.vo;

public class QueryRetryStepVO {

    private final String engine;
    private final long elapsedMs;

    public QueryRetryStepVO(String engine, long elapsedMs) {
        this.engine = engine;
        this.elapsedMs = elapsedMs;
    }

    public String getEngine() {
        return engine;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
