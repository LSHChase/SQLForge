package com.company.governance.application.controller.vo;

public class DatabaseViewDependencyVO {

    private String dependencyObjectType;
    private String dependencyObjectKey;
    private String dependencyObjectName;

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
}
