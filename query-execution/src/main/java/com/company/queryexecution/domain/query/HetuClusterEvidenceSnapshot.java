package com.company.queryexecution.domain.query;

public class HetuClusterEvidenceSnapshot {

    private final String evidenceSource;
    private final String environmentLabel;
    private final String clusterName;
    private final String coordinatorEndpoint;
    private final String runbookRef;
    private final String evidenceRef;
    private final String readonlyBoundary;
    private final String liveVerificationStatus;
    private final String operatorNotes;

    public HetuClusterEvidenceSnapshot(String evidenceSource,
                                       String environmentLabel,
                                       String clusterName,
                                       String coordinatorEndpoint,
                                       String runbookRef,
                                       String evidenceRef,
                                       String readonlyBoundary,
                                       String liveVerificationStatus,
                                       String operatorNotes) {
        this.evidenceSource = evidenceSource;
        this.environmentLabel = environmentLabel;
        this.clusterName = clusterName;
        this.coordinatorEndpoint = coordinatorEndpoint;
        this.runbookRef = runbookRef;
        this.evidenceRef = evidenceRef;
        this.readonlyBoundary = readonlyBoundary;
        this.liveVerificationStatus = liveVerificationStatus;
        this.operatorNotes = operatorNotes;
    }

    public String getEvidenceSource() {
        return evidenceSource;
    }

    public String getEnvironmentLabel() {
        return environmentLabel;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getCoordinatorEndpoint() {
        return coordinatorEndpoint;
    }

    public String getRunbookRef() {
        return runbookRef;
    }

    public String getEvidenceRef() {
        return evidenceRef;
    }

    public String getReadonlyBoundary() {
        return readonlyBoundary;
    }

    public String getLiveVerificationStatus() {
        return liveVerificationStatus;
    }

    public String getOperatorNotes() {
        return operatorNotes;
    }
}
