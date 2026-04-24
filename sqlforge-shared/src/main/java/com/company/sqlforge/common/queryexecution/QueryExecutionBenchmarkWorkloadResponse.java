package com.company.sqlforge.common.queryexecution;

import java.util.List;

public class QueryExecutionBenchmarkWorkloadResponse {

    private String tenantId;
    private String benchmarkTaskId;
    private String sqlFingerprint;
    private String workloadDigest;
    private String workloadSource;
    private boolean backfillApplied;
    private boolean compensationApplied;
    private String compensationStrategy;
    private List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBenchmarkTaskId() {
        return benchmarkTaskId;
    }

    public void setBenchmarkTaskId(String benchmarkTaskId) {
        this.benchmarkTaskId = benchmarkTaskId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getWorkloadDigest() {
        return workloadDigest;
    }

    public void setWorkloadDigest(String workloadDigest) {
        this.workloadDigest = workloadDigest;
    }

    public String getWorkloadSource() {
        return workloadSource;
    }

    public void setWorkloadSource(String workloadSource) {
        this.workloadSource = workloadSource;
    }

    public boolean isBackfillApplied() {
        return backfillApplied;
    }

    public void setBackfillApplied(boolean backfillApplied) {
        this.backfillApplied = backfillApplied;
    }

    public boolean isCompensationApplied() {
        return compensationApplied;
    }

    public void setCompensationApplied(boolean compensationApplied) {
        this.compensationApplied = compensationApplied;
    }

    public String getCompensationStrategy() {
        return compensationStrategy;
    }

    public void setCompensationStrategy(String compensationStrategy) {
        this.compensationStrategy = compensationStrategy;
    }

    public List<QueryExecutionBenchmarkWorkloadEngineSnapshot> getEngineSnapshots() {
        return engineSnapshots;
    }

    public void setEngineSnapshots(List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots) {
        this.engineSnapshots = engineSnapshots;
    }

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
