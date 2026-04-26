package com.company.sqloptimization.application.controller.vo;

public class CombinedParseConclusionVO {

    private String overallStatus;
    private String summary;
    private String recommendedAction;
    private Boolean structureAvailable;
    private Boolean accessAvailable;
    private String degradeReason;

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Boolean getStructureAvailable() {
        return structureAvailable;
    }

    public void setStructureAvailable(Boolean structureAvailable) {
        this.structureAvailable = structureAvailable;
    }

    public Boolean getAccessAvailable() {
        return accessAvailable;
    }

    public void setAccessAvailable(Boolean accessAvailable) {
        this.accessAvailable = accessAvailable;
    }

    public String getDegradeReason() {
        return degradeReason;
    }

    public void setDegradeReason(String degradeReason) {
        this.degradeReason = degradeReason;
    }
}
