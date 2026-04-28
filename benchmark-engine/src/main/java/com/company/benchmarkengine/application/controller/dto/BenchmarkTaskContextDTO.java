package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
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
    @Size(max = 64, message = "templateId exceeds 64 characters")
    private String templateId;
    private BenchmarkTemplateType templateType;
    @Size(max = 32, message = "templateVersion exceeds 32 characters")
    private String templateVersion;
    @Size(max = 64, message = "testSetId exceeds 64 characters")
    private String testSetId;
    private BenchmarkTestSetSource testSetSource;
    @Valid
    private List<BenchmarkTestSetLabelDTO> testSetLabels;
    @Valid
    private List<BenchmarkSourceReferenceDTO> testSetSourceRefs;
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

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public BenchmarkTemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(BenchmarkTemplateType templateType) {
        this.templateType = templateType;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(String testSetId) {
        this.testSetId = testSetId;
    }

    public BenchmarkTestSetSource getTestSetSource() {
        return testSetSource;
    }

    public void setTestSetSource(BenchmarkTestSetSource testSetSource) {
        this.testSetSource = testSetSource;
    }

    public List<BenchmarkTestSetLabelDTO> getTestSetLabels() {
        return testSetLabels;
    }

    public void setTestSetLabels(List<BenchmarkTestSetLabelDTO> testSetLabels) {
        this.testSetLabels = testSetLabels;
    }

    public List<BenchmarkSourceReferenceDTO> getTestSetSourceRefs() {
        return testSetSourceRefs;
    }

    public void setTestSetSourceRefs(List<BenchmarkSourceReferenceDTO> testSetSourceRefs) {
        this.testSetSourceRefs = testSetSourceRefs;
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

    @javax.validation.constraints.AssertTrue(message = "templateType is required when template metadata is provided")
    public boolean isTemplateContractConsistent() {
        if (!hasText(templateId) && templateType == null && !hasText(templateVersion)) {
            return true;
        }
        return templateType != null;
    }

    @javax.validation.constraints.AssertTrue(message = "testSetSource is required when test set metadata is provided")
    public boolean isTestSetContractConsistent() {
        if (!hasText(testSetId)
            && testSetSource == null
            && isEmpty(testSetLabels)
            && isEmpty(testSetSourceRefs)) {
            return true;
        }
        return testSetSource != null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private boolean isEmpty(List<?> value) {
        return value == null || value.isEmpty();
    }
}
