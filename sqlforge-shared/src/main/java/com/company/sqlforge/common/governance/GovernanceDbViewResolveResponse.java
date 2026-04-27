package com.company.sqlforge.common.governance;

import java.util.List;

public class GovernanceDbViewResolveResponse {

    private String tenantId;
    private String datasourceCode;
    private String viewName;
    private String objectKey;
    private Boolean resolved;
    private List<GovernanceDbViewDependencyRef> dependencies;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public List<GovernanceDbViewDependencyRef> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<GovernanceDbViewDependencyRef> dependencies) {
        this.dependencies = dependencies;
    }
}
