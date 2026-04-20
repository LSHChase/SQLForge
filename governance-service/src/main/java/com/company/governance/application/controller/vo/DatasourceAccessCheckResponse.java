package com.company.governance.application.controller.vo;

public class DatasourceAccessCheckResponse {

    private final String tenantId;
    private final String datasourceId;
    private final boolean allowed;
    private final String reason;
    private final Integer errorCode;
    private final String contractStage;
    private final String implementationStage;

    public DatasourceAccessCheckResponse(String tenantId,
                                         String datasourceId,
                                         boolean allowed,
                                         String reason,
                                         Integer errorCode,
                                         String contractStage,
                                         String implementationStage) {
        this.tenantId = tenantId;
        this.datasourceId = datasourceId;
        this.allowed = allowed;
        this.reason = reason;
        this.errorCode = errorCode;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
