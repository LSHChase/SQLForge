package com.company.governance.application.controller.dto;

import java.util.ArrayList;
import java.util.List;

public class DatasourceAuthorizationChangeRequest {

    private String tenantId;
    private String datasourceId;
    private String state;
    private final List<String> actions = new ArrayList<String>();
    private String changeReason;

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

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
