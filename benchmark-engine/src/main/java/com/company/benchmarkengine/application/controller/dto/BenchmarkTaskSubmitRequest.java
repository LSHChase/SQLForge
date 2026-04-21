package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
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

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
