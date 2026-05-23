package com.company.governance.application.controller.vo;

import java.util.ArrayList;
import java.util.List;

public class DatasourceAccessScopeChangeResponse {

    private String tenantId;
    private String datasourceId;
    private String state;
    private final List<String> actions = new ArrayList<String>();
    private String status;
    private String contractStage;
    private String implementationStage;

    public DatasourceAccessScopeChangeResponse() {
    }

    public DatasourceAccessScopeChangeResponse(String tenantId,
                                               String datasourceId,
                                               String state,
                                               List<String> actions,
                                               String status,
                                               String contractStage,
                                               String implementationStage) {
        this.tenantId = tenantId;
        this.datasourceId = datasourceId;
        this.state = state;
        if (actions != null) {
            this.actions.addAll(actions);
        }
        this.status = status;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getActions() {
        return actions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
