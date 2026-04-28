package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationSqlRole;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class BenchmarkRecommendationComparisonCreateRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @Size(max = 128, message = "testSetName exceeds 128 characters")
    private String testSetName;

    @Size(max = 64, message = "templateId exceeds 64 characters")
    private String templateId;

    @Size(max = 32, message = "templateVersion exceeds 32 characters")
    private String templateVersion;

    private BenchmarkRecommendationSqlRole benchmarkSqlRole = BenchmarkRecommendationSqlRole.RECOMMENDED_SQL;

    @NotEmpty(message = "targetEngines are required")
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

    @Valid
    private List<BenchmarkThresholdDTO> thresholds;

    @Valid
    private List<BenchmarkTestSetLabelDTO> testSetLabels;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTestSetName() {
        return testSetName;
    }

    public void setTestSetName(String testSetName) {
        this.testSetName = testSetName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public BenchmarkRecommendationSqlRole getBenchmarkSqlRole() {
        return benchmarkSqlRole;
    }

    public void setBenchmarkSqlRole(BenchmarkRecommendationSqlRole benchmarkSqlRole) {
        this.benchmarkSqlRole = benchmarkSqlRole == null
            ? BenchmarkRecommendationSqlRole.RECOMMENDED_SQL
            : benchmarkSqlRole;
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

    public List<BenchmarkThresholdDTO> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<BenchmarkThresholdDTO> thresholds) {
        this.thresholds = thresholds;
    }

    public List<BenchmarkTestSetLabelDTO> getTestSetLabels() {
        return testSetLabels;
    }

    public void setTestSetLabels(List<BenchmarkTestSetLabelDTO> testSetLabels) {
        this.testSetLabels = testSetLabels;
    }

    @AssertTrue(message = "comparison benchmark requires at least two target engines")
    public boolean isComparisonTargetEngineSetValid() {
        return targetEngines != null && targetEngines.size() >= 2;
    }
}
