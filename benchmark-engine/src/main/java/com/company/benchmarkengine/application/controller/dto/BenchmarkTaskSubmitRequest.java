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

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotNull(message = "taskType 为必填项")
    private BenchmarkTaskType taskType;

    @Size(max = 10485760, message = "sqlText 超过 10MB 限制")
    private String sqlText;

    @Size(max = 128, message = "sqlFingerprint 超过 128 个字符")
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

    @AssertTrue(message = "必须提供 sqlText 或 sqlFingerprint")
    public boolean isSqlIdentityProvided() {
        return hasText(sqlText) || hasText(sqlFingerprint);
    }

    @AssertTrue(message = "templateType 必须与压测 taskType 匹配")
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

    @AssertTrue(message = "对比模板至少需要两个目标引擎")
    public boolean isComparisonTemplateEngineSetCompatible() {
        if (taskContext == null || taskContext.getTemplateType() != BenchmarkTemplateType.CROSS_ENGINE_COMPARISON) {
            return true;
        }
        List<?> targetEngines = taskContext.getTargetEngines();
        return targetEngines != null && targetEngines.size() >= 2;
    }

    @AssertTrue(message = "回归防护模板至少需要一个阈值")
    public boolean isRegressionTemplateThresholdCompatible() {
        if (taskContext == null || taskContext.getTemplateType() != BenchmarkTemplateType.REGRESSION_GUARD) {
            return true;
        }
        List<?> thresholds = taskContext.getThresholds();
        return thresholds != null && !thresholds.isEmpty();
    }

    @AssertTrue(message = "派生测试集来源必须提供来源引用")
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

    @AssertTrue(message = "推荐生成的测试集必须提供推荐引用")
    public boolean isRecommendationTestSetReferenceCompatible() {
        if (taskContext == null || taskContext.getTestSetSource() != BenchmarkTestSetSource.RECOMMENDATION_GENERATION) {
            return true;
        }
        return hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.RECOMMENDATION);
    }

    @AssertTrue(message = "解析生成的测试集必须提供 parse、history、report 或 sql 引用")
    public boolean isParseGeneratedTestSetReferenceCompatible() {
        if (taskContext == null || taskContext.getTestSetSource() != BenchmarkTestSetSource.PARSE_RESULT_GENERATION) {
            return true;
        }
        return hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.PARSE_TASK)
            || hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.QUERY_HISTORY)
            || hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.REPORT)
            || hasReferenceType(taskContext.getTestSetSourceRefs(), BenchmarkSourceReferenceType.SQL_FINGERPRINT);
    }

    @AssertTrue(message = "批量导入测试集必须提供导入批次引用")
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
