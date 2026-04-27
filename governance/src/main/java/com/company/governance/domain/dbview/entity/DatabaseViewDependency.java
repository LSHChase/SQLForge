package com.company.governance.domain.dbview.entity;

import java.time.LocalDateTime;

public class DatabaseViewDependency {

    private String id;
    private String tenantId;
    private String dbViewId;
    private String dependencyObjectType;
    private String dependencyObjectKey;
    private String dependencyObjectName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDbViewId() {
        return dbViewId;
    }

    public void setDbViewId(String dbViewId) {
        this.dbViewId = dbViewId;
    }

    public String getDependencyObjectType() {
        return dependencyObjectType;
    }

    public void setDependencyObjectType(String dependencyObjectType) {
        this.dependencyObjectType = dependencyObjectType;
    }

    public String getDependencyObjectKey() {
        return dependencyObjectKey;
    }

    public void setDependencyObjectKey(String dependencyObjectKey) {
        this.dependencyObjectKey = dependencyObjectKey;
    }

    public String getDependencyObjectName() {
        return dependencyObjectName;
    }

    public void setDependencyObjectName(String dependencyObjectName) {
        this.dependencyObjectName = dependencyObjectName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
