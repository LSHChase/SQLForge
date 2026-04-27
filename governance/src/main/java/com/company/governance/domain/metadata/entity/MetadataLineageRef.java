package com.company.governance.domain.metadata.entity;

public class MetadataLineageRef {

    private String objectType;
    private String objectKey;
    private String objectName;
    private String relationshipType;

    public MetadataLineageRef() {
    }

    public MetadataLineageRef(String objectType, String objectKey, String objectName, String relationshipType) {
        this.objectType = objectType;
        this.objectKey = objectKey;
        this.objectName = objectName;
        this.relationshipType = relationshipType;
    }

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

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }
}
