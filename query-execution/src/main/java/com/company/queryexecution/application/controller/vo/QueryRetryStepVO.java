package com.company.queryexecution.application.controller.vo;

public class QueryRetryStepVO {

    private final String engine;
    private final long elapsedMs;
    private final String resultStatus;
    private final String localRecoveryMarker;
    private final String localRecoveryAction;

    public QueryRetryStepVO(String engine,
                            long elapsedMs,
                            String resultStatus,
                            String localRecoveryMarker,
                            String localRecoveryAction) {
        this.engine = engine;
        this.elapsedMs = elapsedMs;
        this.resultStatus = resultStatus;
        this.localRecoveryMarker = localRecoveryMarker;
        this.localRecoveryAction = localRecoveryAction;
    }

    public String getEngine() {
        return engine;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public String getLocalRecoveryMarker() {
        return localRecoveryMarker;
    }

    public String getLocalRecoveryAction() {
        return localRecoveryAction;
    }
}
