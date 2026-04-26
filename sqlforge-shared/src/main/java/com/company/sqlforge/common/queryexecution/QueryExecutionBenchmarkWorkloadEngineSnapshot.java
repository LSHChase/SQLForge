package com.company.sqlforge.common.queryexecution;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;

public class QueryExecutionBenchmarkWorkloadEngineSnapshot {

    private DataSourceTypeEnum targetEngine;
    private String resultStatus;
    private String workloadSource;
    private String backfillSource;
    private String backfillReason;
    private String executionMode;
    private List<String> attemptedModes;
    private Long elapsedMs;
    private Long scannedRows;
    private Integer rowCount;
    private Boolean cacheHit;
    private String cacheGovernanceStatus;
    private String cacheGovernanceEvidence;
    private Boolean accelerationApplied;
    private String workloadDigest;
    private String evidence;
    private Boolean compensationApplied;
    private String compensationStrategy;
    private DataSourceTypeEnum compensationSourceEngine;
    private String compensationSourceWorkloadDigest;

    public DataSourceTypeEnum getTargetEngine() {
        return targetEngine;
    }

    public void setTargetEngine(DataSourceTypeEnum targetEngine) {
        this.targetEngine = targetEngine;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getWorkloadSource() {
        return workloadSource;
    }

    public void setWorkloadSource(String workloadSource) {
        this.workloadSource = workloadSource;
    }

    public String getBackfillSource() {
        return backfillSource;
    }

    public void setBackfillSource(String backfillSource) {
        this.backfillSource = backfillSource;
    }

    public String getBackfillReason() {
        return backfillReason;
    }

    public void setBackfillReason(String backfillReason) {
        this.backfillReason = backfillReason;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public List<String> getAttemptedModes() {
        return attemptedModes;
    }

    public void setAttemptedModes(List<String> attemptedModes) {
        this.attemptedModes = attemptedModes;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Long getScannedRows() {
        return scannedRows;
    }

    public void setScannedRows(Long scannedRows) {
        this.scannedRows = scannedRows;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public String getCacheGovernanceStatus() {
        return cacheGovernanceStatus;
    }

    public void setCacheGovernanceStatus(String cacheGovernanceStatus) {
        this.cacheGovernanceStatus = cacheGovernanceStatus;
    }

    public String getCacheGovernanceEvidence() {
        return cacheGovernanceEvidence;
    }

    public void setCacheGovernanceEvidence(String cacheGovernanceEvidence) {
        this.cacheGovernanceEvidence = cacheGovernanceEvidence;
    }

    public Boolean getAccelerationApplied() {
        return accelerationApplied;
    }

    public void setAccelerationApplied(Boolean accelerationApplied) {
        this.accelerationApplied = accelerationApplied;
    }

    public String getWorkloadDigest() {
        return workloadDigest;
    }

    public void setWorkloadDigest(String workloadDigest) {
        this.workloadDigest = workloadDigest;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public Boolean getCompensationApplied() {
        return compensationApplied;
    }

    public void setCompensationApplied(Boolean compensationApplied) {
        this.compensationApplied = compensationApplied;
    }

    public String getCompensationStrategy() {
        return compensationStrategy;
    }

    public void setCompensationStrategy(String compensationStrategy) {
        this.compensationStrategy = compensationStrategy;
    }

    public DataSourceTypeEnum getCompensationSourceEngine() {
        return compensationSourceEngine;
    }

    public void setCompensationSourceEngine(DataSourceTypeEnum compensationSourceEngine) {
        this.compensationSourceEngine = compensationSourceEngine;
    }

    public String getCompensationSourceWorkloadDigest() {
        return compensationSourceWorkloadDigest;
    }

    public void setCompensationSourceWorkloadDigest(String compensationSourceWorkloadDigest) {
        this.compensationSourceWorkloadDigest = compensationSourceWorkloadDigest;
    }
}
