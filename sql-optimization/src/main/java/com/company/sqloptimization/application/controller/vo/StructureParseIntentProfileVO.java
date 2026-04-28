package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class StructureParseIntentProfileVO {

    private List<String> classificationLabels;
    private String scanMode;
    private String joinType;
    private String computeDensity;
    private String resourceType;
    private String slaLevel;
    private String confidence;

    public List<String> getClassificationLabels() { return classificationLabels; }
    public void setClassificationLabels(List<String> classificationLabels) { this.classificationLabels = classificationLabels; }
    public String getScanMode() { return scanMode; }
    public void setScanMode(String scanMode) { this.scanMode = scanMode; }
    public String getJoinType() { return joinType; }
    public void setJoinType(String joinType) { this.joinType = joinType; }
    public String getComputeDensity() { return computeDensity; }
    public void setComputeDensity(String computeDensity) { this.computeDensity = computeDensity; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getSlaLevel() { return slaLevel; }
    public void setSlaLevel(String slaLevel) { this.slaLevel = slaLevel; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
