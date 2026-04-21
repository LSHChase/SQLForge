package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;

public class BenchmarkTaskSubmitRequest {

    private String tenantId;
    private BenchmarkTaskType taskType;
    private String sqlText;
    private String sqlFingerprint;
    private BenchmarkTaskContextDTO taskContext;

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
        this.taskContext = taskContext;
    }
}
