package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class StructureParseLogicalObjectHitVO {

    private String objectType;
    private String objectName;
    private String matchSource;
    private Boolean resolved;
    private List<String> mappedPhysicalTargets;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getMatchSource() {
        return matchSource;
    }

    public void setMatchSource(String matchSource) {
        this.matchSource = matchSource;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public List<String> getMappedPhysicalTargets() {
        return mappedPhysicalTargets;
    }

    public void setMappedPhysicalTargets(List<String> mappedPhysicalTargets) {
        this.mappedPhysicalTargets = mappedPhysicalTargets;
    }
}
