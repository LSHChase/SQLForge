package com.company.sqlforge.common.logicalobject;

import java.util.List;

public class LogicalObjectSurface {

    private String objectType;
    private String objectKey;
    private String objectName;
    private String catalogName;
    private String schemaName;
    private String matchSource;
    private Boolean resolved;
    private List<String> mappedPhysicalTargets;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
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
