package com.company.sqlforge.common.queryexecution;

import java.util.List;

public class QueryExecutionAccelerationPlanApplyRequest {

    private String tenantId;
    private String planId;
    private String sqlFingerprint;
    private String datasourceType;
    private List<String> selectedSuggestionTypes;
    private String planSummary;
    private String primaryRecommendation;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public List<String> getSelectedSuggestionTypes() {
        return selectedSuggestionTypes;
    }

    public void setSelectedSuggestionTypes(List<String> selectedSuggestionTypes) {
        this.selectedSuggestionTypes = selectedSuggestionTypes;
    }

    public String getPlanSummary() {
        return planSummary;
    }

    public void setPlanSummary(String planSummary) {
        this.planSummary = planSummary;
    }

    public String getPrimaryRecommendation() {
        return primaryRecommendation;
    }

    public void setPrimaryRecommendation(String primaryRecommendation) {
        this.primaryRecommendation = primaryRecommendation;
    }
}
