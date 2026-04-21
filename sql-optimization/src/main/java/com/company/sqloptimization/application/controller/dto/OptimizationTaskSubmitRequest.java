package com.company.sqloptimization.application.controller.dto;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class OptimizationTaskSubmitRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotNull(message = "taskType is required")
    private OptimizationTaskType taskType;

    @Size(max = 10485760, message = "sqlText exceeds 10MB limit")
    private String sqlText;

    @Size(max = 128, message = "sqlFingerprint exceeds 128 characters")
    private String sqlFingerprint;

    @NotNull(message = "datasourceType is required")
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

    @AssertTrue(message = "Either sqlText or sqlFingerprint must be provided")
    public boolean isSqlIdentityProvided() {
        return hasText(sqlText) || hasText(sqlFingerprint);
    }

    @AssertTrue(message = "requestedSuggestionTypes are only supported for ACCELERATION_SUGGESTION tasks")
    public boolean isSuggestionTypeUsageValid() {
        return taskContext == null
            || taskContext.getRequestedSuggestionTypes().isEmpty()
            || taskType == OptimizationTaskType.ACCELERATION_SUGGESTION;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
