package com.company.sqlforge.common.openaccess;

public class SqlForgeQueryRetryStep {

    private String engine;
    private Long elapsedMs;
    private String resultStatus;
    private String localRecoveryMarker;
    private String localRecoveryAction;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getLocalRecoveryMarker() {
        return localRecoveryMarker;
    }

    public void setLocalRecoveryMarker(String localRecoveryMarker) {
        this.localRecoveryMarker = localRecoveryMarker;
    }

    public String getLocalRecoveryAction() {
        return localRecoveryAction;
    }

    public void setLocalRecoveryAction(String localRecoveryAction) {
        this.localRecoveryAction = localRecoveryAction;
    }
}
