package com.company.benchmarkengine.domain.benchmark;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkTaskSubmission {

    private final String tenantId;
    private final BenchmarkTaskType taskType;
    private final String sqlText;
    private final String sqlFingerprint;
    private final BenchmarkTaskPriority priority;
    private final List<DataSourceTypeEnum> targetEngines;
    private final Integer concurrency;
    private final Integer durationSeconds;
    private final Integer rampUpSeconds;
    private final String datasetSizeLabel;
    private final BenchmarkScaleTarget scaleTarget;
    private final String templateId;
    private final BenchmarkTemplateType templateType;
    private final String templateVersion;
    private final String testSetId;
    private final BenchmarkTestSetSource testSetSource;
    private final List<BenchmarkTestSetLabel> testSetLabels;
    private final List<BenchmarkSourceReference> testSetSourceRefs;
    private final Boolean readonlyRequired;
    private final ShadowEnvironmentMode shadowEnvironmentMode;
    private final DesensitizationRequirement desensitizationRequirement;
    private final List<BenchmarkThreshold> thresholds;

    public BenchmarkTaskSubmission(String tenantId,
                                   BenchmarkTaskType taskType,
                                   String sqlText,
                                   String sqlFingerprint,
                                   BenchmarkTaskPriority priority,
                                   List<DataSourceTypeEnum> targetEngines,
                                   Integer concurrency,
                                   Integer durationSeconds,
                                   Integer rampUpSeconds,
                                   String datasetSizeLabel,
                                   BenchmarkScaleTarget scaleTarget,
                                   String templateId,
                                   BenchmarkTemplateType templateType,
                                   String templateVersion,
                                   String testSetId,
                                   BenchmarkTestSetSource testSetSource,
                                   List<BenchmarkTestSetLabel> testSetLabels,
                                   List<BenchmarkSourceReference> testSetSourceRefs,
                                   Boolean readonlyRequired,
                                   ShadowEnvironmentMode shadowEnvironmentMode,
                                   DesensitizationRequirement desensitizationRequirement,
                                   List<BenchmarkThreshold> thresholds) {
        this.tenantId = tenantId;
        this.taskType = taskType;
        this.sqlText = sqlText;
        this.sqlFingerprint = sqlFingerprint;
        this.priority = priority == null ? BenchmarkTaskPriority.NORMAL : priority;
        this.targetEngines = normalizeTargetEngines(targetEngines);
        this.concurrency = concurrency == null ? Integer.valueOf(8) : concurrency;
        this.durationSeconds = durationSeconds == null ? Integer.valueOf(300) : durationSeconds;
        this.rampUpSeconds = rampUpSeconds == null ? Integer.valueOf(30) : rampUpSeconds;
        this.datasetSizeLabel = datasetSizeLabel == null ? "UNSPECIFIED" : datasetSizeLabel;
        this.scaleTarget = normalizeScaleTarget(scaleTarget);
        this.templateId = normalizeText(templateId);
        this.templateType = templateType;
        this.templateVersion = templateType == null
            ? normalizeText(templateVersion)
            : normalizeText(templateVersion) == null ? "v1" : normalizeText(templateVersion);
        this.testSetId = normalizeText(testSetId);
        this.testSetSource = testSetSource;
        this.testSetLabels = normalizeTestSetLabels(testSetLabels);
        this.testSetSourceRefs = normalizeSourceRefs(testSetSourceRefs);
        this.readonlyRequired = readonlyRequired == null ? Boolean.TRUE : readonlyRequired;
        this.shadowEnvironmentMode = shadowEnvironmentMode == null
            ? ShadowEnvironmentMode.REQUIRED
            : shadowEnvironmentMode;
        this.desensitizationRequirement = desensitizationRequirement == null
            ? DesensitizationRequirement.REQUIRED
            : desensitizationRequirement;
        this.thresholds = normalizeThresholds(thresholds);
    }

    private List<DataSourceTypeEnum> normalizeTargetEngines(List<DataSourceTypeEnum> requestedEngines) {
        if (requestedEngines == null || requestedEngines.isEmpty()) {
            return Collections.singletonList(DataSourceTypeEnum.HETU);
        }
        return Collections.unmodifiableList(new ArrayList<DataSourceTypeEnum>(requestedEngines));
    }

    private List<BenchmarkThreshold> normalizeThresholds(List<BenchmarkThreshold> requestedThresholds) {
        if (requestedThresholds == null || requestedThresholds.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<BenchmarkThreshold>(requestedThresholds));
    }

    private List<BenchmarkTestSetLabel> normalizeTestSetLabels(List<BenchmarkTestSetLabel> requestedLabels) {
        if (requestedLabels == null || requestedLabels.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<BenchmarkTestSetLabel>(requestedLabels));
    }

    private List<BenchmarkSourceReference> normalizeSourceRefs(List<BenchmarkSourceReference> requestedRefs) {
        if (requestedRefs == null || requestedRefs.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<BenchmarkSourceReference>(requestedRefs));
    }

    private BenchmarkScaleTarget normalizeScaleTarget(BenchmarkScaleTarget requestedTarget) {
        if (requestedTarget == null || !requestedTarget.hasAnyTarget()) {
            return null;
        }
        return requestedTarget;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getTenantId() {
        return tenantId;
    }

    public BenchmarkTaskType getTaskType() {
        return taskType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public BenchmarkTaskPriority getPriority() {
        return priority;
    }

    public List<DataSourceTypeEnum> getTargetEngines() {
        return targetEngines;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Integer getRampUpSeconds() {
        return rampUpSeconds;
    }

    public String getDatasetSizeLabel() {
        return datasetSizeLabel;
    }

    public BenchmarkScaleTarget getScaleTarget() {
        return scaleTarget;
    }

    public String getTemplateId() {
        return templateId;
    }

    public BenchmarkTemplateType getTemplateType() {
        return templateType;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public String getTestSetId() {
        return testSetId;
    }

    public BenchmarkTestSetSource getTestSetSource() {
        return testSetSource;
    }

    public List<BenchmarkTestSetLabel> getTestSetLabels() {
        return testSetLabels;
    }

    public List<BenchmarkSourceReference> getTestSetSourceRefs() {
        return testSetSourceRefs;
    }

    public Boolean getReadonlyRequired() {
        return readonlyRequired;
    }

    public ShadowEnvironmentMode getShadowEnvironmentMode() {
        return shadowEnvironmentMode;
    }

    public DesensitizationRequirement getDesensitizationRequirement() {
        return desensitizationRequirement;
    }

    public List<BenchmarkThreshold> getThresholds() {
        return thresholds;
    }
}
