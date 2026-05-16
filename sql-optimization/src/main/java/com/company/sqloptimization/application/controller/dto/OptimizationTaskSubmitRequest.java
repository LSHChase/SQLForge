package com.company.sqloptimization.application.controller.dto;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class OptimizationTaskSubmitRequest {

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotNull(message = "taskType 为必填项")
    private OptimizationTaskType taskType;

    @Size(max = 10485760, message = "sqlText 超过 10MB 限制")
    private String sqlText;

    @Size(max = 128, message = "sqlFingerprint 超过 128 个字符")
    private String sqlFingerprint;

    @NotNull(message = "datasourceType 为必填项")
    private DataSourceTypeEnum datasourceType;

    @Valid
    private OptimizationTaskContextDTO taskContext = new OptimizationTaskContextDTO();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public OptimizationTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(OptimizationTaskType taskType) {
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

    public DataSourceTypeEnum getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(DataSourceTypeEnum datasourceType) {
        this.datasourceType = datasourceType;
    }

    public OptimizationTaskContextDTO getTaskContext() {
        return taskContext;
    }

    public void setTaskContext(OptimizationTaskContextDTO taskContext) {
        this.taskContext = taskContext == null ? new OptimizationTaskContextDTO() : taskContext;
    }

    @AssertTrue(message = "必须提供 sqlText 或 sqlFingerprint")
    public boolean isSqlIdentityProvided() {
        return hasText(sqlText) || hasText(sqlFingerprint);
    }

    @AssertTrue(message = "requestedSuggestionTypes 仅支持 ACCELERATION_SUGGESTION 任务")
    public boolean isSuggestionTypeUsageValid() {
        return taskContext == null
            || taskContext.getRequestedSuggestionTypes().isEmpty()
            || taskType == OptimizationTaskType.ACCELERATION_SUGGESTION;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
