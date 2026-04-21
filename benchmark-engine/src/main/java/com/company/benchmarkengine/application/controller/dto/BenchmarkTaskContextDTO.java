package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;

public class BenchmarkTaskContextDTO {

    private BenchmarkTaskPriority priority;
    private List<DataSourceTypeEnum> targetEngines;
    private Integer concurrency;
    private Integer durationSeconds;
    private Integer rampUpSeconds;
    private String datasetSizeLabel;
    private Boolean readonlyRequired;
    private ShadowEnvironmentMode shadowEnvironmentMode;
    private DesensitizationRequirement desensitizationRequirement;
    private List<BenchmarkThresholdDTO> thresholds;

    public BenchmarkTaskPriority getPriority() {
        return priority;
    }

    public void setPriority(BenchmarkTaskPriority priority) {
        this.priority = priority;
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

    public ShadowEnvironmentMode getShadowEnvironmentMode() {
        return shadowEnvironmentMode;
    }

    public void setShadowEnvironmentMode(ShadowEnvironmentMode shadowEnvironmentMode) {
        this.shadowEnvironmentMode = shadowEnvironmentMode;
    }

    public DesensitizationRequirement getDesensitizationRequirement() {
        return desensitizationRequirement;
    }

    public void setDesensitizationRequirement(DesensitizationRequirement desensitizationRequirement) {
        this.desensitizationRequirement = desensitizationRequirement;
    }

    public List<BenchmarkThresholdDTO> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<BenchmarkThresholdDTO> thresholds) {
        this.thresholds = thresholds;
    }
}
