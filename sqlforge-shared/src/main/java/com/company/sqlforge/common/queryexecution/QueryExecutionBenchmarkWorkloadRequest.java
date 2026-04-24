package com.company.sqlforge.common.queryexecution;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;

public class QueryExecutionBenchmarkWorkloadRequest {

    private String tenantId;
    private String benchmarkTaskId;
    private String benchmarkTaskType;
    private String sqlText;
    private String sqlFingerprint;
    private List<DataSourceTypeEnum> targetEngines;
    private Integer concurrency;
    private Integer durationSeconds;
    private Integer rampUpSeconds;
    private String datasetSizeLabel;
    private Boolean readonlyRequired;

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

    public String getBenchmarkTaskType() {
        return benchmarkTaskType;
    }

    public void setBenchmarkTaskType(String benchmarkTaskType) {
        this.benchmarkTaskType = benchmarkTaskType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public List<DataSourceTypeEnum> getTargetEngines() {
        return targetEngines;
    }

    public void setTargetEngines(List<DataSourceTypeEnum> targetEngines) {
        this.targetEngines = targetEngines;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getRampUpSeconds() {
        return rampUpSeconds;
    }

    public void setRampUpSeconds(Integer rampUpSeconds) {
        this.rampUpSeconds = rampUpSeconds;
    }

    public String getDatasetSizeLabel() {
        return datasetSizeLabel;
    }

    public void setDatasetSizeLabel(String datasetSizeLabel) {
        this.datasetSizeLabel = datasetSizeLabel;
    }

    public Boolean getReadonlyRequired() {
        return readonlyRequired;
    }

    public void setReadonlyRequired(Boolean readonlyRequired) {
        this.readonlyRequired = readonlyRequired;
    }
}
