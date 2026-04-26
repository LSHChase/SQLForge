package com.company.sqloptimization.application.controller.vo;

public class AccessParseResponseVO {

    private String parseTaskId;
    private String parseType = "ACCESS";
    private String serviceStatus;
    private String connectionStatus;
    private String objectResolutionStatus;
    private String planSummary;
    private String partitionStatus;
    private String dataFreshnessStatus;
    private String slaStatus;
    private String compatibilityStatus;
    private String availabilityWarning;
    private String degradeReason;

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getParseType() {
        return parseType;
    }

    public void setParseType(String parseType) {
        this.parseType = parseType;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getObjectResolutionStatus() {
        return objectResolutionStatus;
    }

    public void setObjectResolutionStatus(String objectResolutionStatus) {
        this.objectResolutionStatus = objectResolutionStatus;
    }

    public String getPlanSummary() {
        return planSummary;
    }

    public void setPlanSummary(String planSummary) {
        this.planSummary = planSummary;
    }

    public String getPartitionStatus() {
        return partitionStatus;
    }

    public void setPartitionStatus(String partitionStatus) {
        this.partitionStatus = partitionStatus;
    }

    public String getDataFreshnessStatus() {
        return dataFreshnessStatus;
    }

    public void setDataFreshnessStatus(String dataFreshnessStatus) {
        this.dataFreshnessStatus = dataFreshnessStatus;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public String getCompatibilityStatus() {
        return compatibilityStatus;
    }

    public void setCompatibilityStatus(String compatibilityStatus) {
        this.compatibilityStatus = compatibilityStatus;
    }

    public String getAvailabilityWarning() {
        return availabilityWarning;
    }

    public void setAvailabilityWarning(String availabilityWarning) {
        this.availabilityWarning = availabilityWarning;
    }

    public String getDegradeReason() {
        return degradeReason;
    }

    public void setDegradeReason(String degradeReason) {
        this.degradeReason = degradeReason;
    }
}
