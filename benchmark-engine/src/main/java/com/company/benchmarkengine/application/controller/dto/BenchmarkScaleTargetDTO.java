package com.company.benchmarkengine.application.controller.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class BenchmarkScaleTargetDTO {

    @Min(value = 1L, message = "targetConcurrency 必须大于 0")
    private Integer targetConcurrency;

    @Size(max = 64, message = "targetDatasetSizeLabel 超过 64 个字符")
    private String targetDatasetSizeLabel;

    @Min(value = 1L, message = "targetDailyQueryVolume 必须大于 0")
    private Long targetDailyQueryVolume;

    @Size(max = 64, message = "targetComplexityProfile 超过 64 个字符")
    private String targetComplexityProfile;

    @Size(max = 128, message = "targetCostEfficiency 超过 128 个字符")
    private String targetCostEfficiency;

    public Integer getTargetConcurrency() {
        return targetConcurrency;
    }

    public void setTargetConcurrency(Integer targetConcurrency) {
        this.targetConcurrency = targetConcurrency;
    }

    public String getTargetDatasetSizeLabel() {
        return targetDatasetSizeLabel;
    }

    public void setTargetDatasetSizeLabel(String targetDatasetSizeLabel) {
        this.targetDatasetSizeLabel = targetDatasetSizeLabel;
    }

    public Long getTargetDailyQueryVolume() {
        return targetDailyQueryVolume;
    }

    public void setTargetDailyQueryVolume(Long targetDailyQueryVolume) {
        this.targetDailyQueryVolume = targetDailyQueryVolume;
    }

    public String getTargetComplexityProfile() {
        return targetComplexityProfile;
    }

    public void setTargetComplexityProfile(String targetComplexityProfile) {
        this.targetComplexityProfile = targetComplexityProfile;
    }

    public String getTargetCostEfficiency() {
        return targetCostEfficiency;
    }

    public void setTargetCostEfficiency(String targetCostEfficiency) {
        this.targetCostEfficiency = targetCostEfficiency;
    }
}
