package com.company.governance.application.controller.vo;

public class LogicalObjectMappingVO {

    private String targetObjectType;
    private String targetObjectKey;
    private String targetObjectName;
    private String mappingRole;

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
}
