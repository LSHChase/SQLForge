package com.company.governance.domain.logicalview.entity;

import java.time.LocalDateTime;

public class LogicalObjectMapping {

    private String id;
    private String tenantId;
    private String logicalViewId;
    private String targetObjectType;
    private String targetObjectKey;
    private String targetObjectName;
    private String mappingRole;
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

    public String getLogicalViewId() {
        return logicalViewId;
    }

    public void setLogicalViewId(String logicalViewId) {
        this.logicalViewId = logicalViewId;
    }

    public String getTargetObjectType() {
        return targetObjectType;
    }

    public void setTargetObjectType(String targetObjectType) {
        this.targetObjectType = targetObjectType;
    }

    public String getTargetObjectKey() {
        return targetObjectKey;
    }

    public void setTargetObjectKey(String targetObjectKey) {
        this.targetObjectKey = targetObjectKey;
    }

    public String getTargetObjectName() {
        return targetObjectName;
    }

    public void setTargetObjectName(String targetObjectName) {
        this.targetObjectName = targetObjectName;
    }

    public String getMappingRole() {
        return mappingRole;
    }

    public void setMappingRole(String mappingRole) {
        this.mappingRole = mappingRole;
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
