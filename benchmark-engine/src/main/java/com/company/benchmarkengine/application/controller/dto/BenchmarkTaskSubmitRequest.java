package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTaskSubmitRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotNull(message = "taskType is required")
    private BenchmarkTaskType taskType;

    @Size(max = 10485760, message = "sqlText exceeds 10MB limit")
    private String sqlText;

    @Size(max = 128, message = "sqlFingerprint exceeds 128 characters")
    private String sqlFingerprint;

    @Valid
    private BenchmarkTaskContextDTO taskContext = new BenchmarkTaskContextDTO();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public BenchmarkTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(BenchmarkTaskType taskType) {
        this.taskType = taskType;
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

    public BenchmarkTaskContextDTO getTaskContext() {
        return taskContext;
    }

    public void setTaskContext(BenchmarkTaskContextDTO taskContext) {
        this.taskContext = taskContext == null ? new BenchmarkTaskContextDTO() : taskContext;
    }

    @AssertTrue(message = "Either sqlText or sqlFingerprint must be provided")
    public boolean isSqlIdentityProvided() {
        return hasText(sqlText) || hasText(sqlFingerprint);
    }

    @AssertTrue(message = "templateType must match benchmark taskType")
    public boolean isTemplateTypeCompatible() {
        if (taskType == null || taskContext == null || taskContext.getTemplateType() == null) {
            return true;
        }
        BenchmarkTemplateType templateType = taskContext.getTemplateType();
        switch (templateType) {
            case BASELINE_SNAPSHOT:
                return taskType == BenchmarkTaskType.BASELINE;
            case CROSS_ENGINE_COMPARISON:
                return taskType == BenchmarkTaskType.COMPARISON;
            case REGRESSION_GUARD:
                return taskType == BenchmarkTaskType.REGRESSION_GUARD;
            default:
                return false;
        }
    }

    @AssertTrue(message = "comparison template requires at least two target engines")
    public boolean isComparisonTemplateEngineSetCompatible() {
        if (taskContext == null || taskContext.getTemplateType() != BenchmarkTemplateType.CROSS_ENGINE_COMPARISON) {
            return true;
        }
        List<?> targetEngines = taskContext.getTargetEngines();
        return targetEngines != null && targetEngines.size() >= 2;
    }

    @AssertTrue(message = "regression guard template requires at least one threshold")
    public boolean isRegressionTemplateThresholdCompatible() {
        if (taskContext == null || taskContext.getTemplateType() != BenchmarkTemplateType.REGRESSION_GUARD) {
            return true;
        }
        List<?> thresholds = taskContext.getThresholds();
        return thresholds != null && !thresholds.isEmpty();
    }

    @AssertTrue(message = "derived testSet source requires source references")
    public boolean isDerivedTestSetReferenceComplete() {
        if (taskContext == null || taskContext.getTestSetSource() == null) {
            return true;
        }
        if (taskContext.getTestSetSource() == BenchmarkTestSetSource.MANUAL_CURATION) {
            return true;
        }
        List<BenchmarkSourceReferenceDTO> refs = taskContext.getTestSetSourceRefs();
        return refs != null && !refs.isEmpty();
    }

    @AssertTrue(message = "recommendation-generated testSet requires a recommendation reference")
    public boolean isRecommendationTestSetReferenceCompatible() {
        if (taskContext == null || taskContext.getTestSetSource() != BenchmarkTestSetSource.RECOMMENDATION_GENERATION) {
            return true;
        }
        return hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.RECOMMENDATION);
    }

    @AssertTrue(message = "parse-generated testSet requires parse/history/report/sql references")
    public boolean isParseGeneratedTestSetReferenceCompatible() {
        if (taskContext == null || taskContext.getTestSetSource() != BenchmarkTestSetSource.PARSE_RESULT_GENERATION) {
            return true;
        }
        return hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.PARSE_TASK)
            || hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.QUERY_HISTORY)
            || hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.REPORT)
            || hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.SQL_FINGERPRINT);
    }

    @AssertTrue(message = "batch-import testSet requires an import batch reference")
    public boolean isBatchImportTestSetReferenceCompatible() {
        if (taskContext == null || taskContext.getTestSetSource() != BenchmarkTestSetSource.BATCH_IMPORT) {
            return true;
        }
        return hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.IMPORT_BATCH);
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private boolean hasReferenceType(List<BenchmarkSourceReferenceDTO> refs, BenchmarkSourceReferenceType type) {
        if (refs == null || refs.isEmpty() || type == null) {
            return false;
        }
        for (BenchmarkSourceReferenceDTO ref : refs) {
            if (ref != null && type == ref.getType()) {
                return true;
            }
        }
        return false;
    }
}
