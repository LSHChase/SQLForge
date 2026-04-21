package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class BenchmarkTaskContextDTO {

    private BenchmarkTaskPriority priority;

    @Size(max = 8, message = "targetEngines exceeds 8 items")
    private List<DataSourceTypeEnum> targetEngines;

    @Min(value = 1L, message = "concurrency must be greater than 0")
    private Integer concurrency;

    @Min(value = 1L, message = "durationSeconds must be greater than 0")
    private Integer durationSeconds;

    @Min(value = 0L, message = "rampUpSeconds must not be negative")
    private Integer rampUpSeconds;

    @Size(max = 64, message = "datasetSizeLabel exceeds 64 characters")
    private String datasetSizeLabel;
    private Boolean readonlyRequired;
    private ShadowEnvironmentMode shadowEnvironmentMode;
    private DesensitizationRequirement desensitizationRequirement;

    @Valid
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
