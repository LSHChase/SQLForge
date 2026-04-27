package com.company.governance.application.controller.vo;

import java.util.List;

public class DatabaseViewRefVO {

    private String viewId;
    private String tenantId;
    private String datasourceCode;
    private String viewName;
    private String objectKey;
    private String schemaName;
    private String catalogName;
    private String ownerUser;
    private Boolean queryable;
    private List<DatabaseViewDependencyVO> dependencies;

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

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

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(String ownerUser) {
        this.ownerUser = ownerUser;
    }

    public Boolean getQueryable() {
        return queryable;
    }

    public void setQueryable(Boolean queryable) {
        this.queryable = queryable;
    }

    public List<DatabaseViewDependencyVO> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DatabaseViewDependencyVO> dependencies) {
        this.dependencies = dependencies;
    }
}
