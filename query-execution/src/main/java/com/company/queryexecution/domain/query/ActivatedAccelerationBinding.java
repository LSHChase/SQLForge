package com.company.queryexecution.domain.query;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class ActivatedAccelerationBinding {

    private final String tenantId;
    private final String planId;
    private final String sqlFingerprint;
    private final String datasourceType;
    private final List<String> selectedSuggestionTypes;
    private final String planSummary;
    private final String primaryRecommendation;
    private final Instant activatedAt;

    public ActivatedAccelerationBinding(String tenantId,
                                       String planId,
                                       String sqlFingerprint,
                                       String datasourceType,
                                       List<String> selectedSuggestionTypes,
                                       String planSummary,
                                       String primaryRecommendation,
                                       Instant activatedAt) {
        this.tenantId = tenantId;
        this.planId = planId;
        this.sqlFingerprint = sqlFingerprint;
        this.datasourceType = datasourceType;
        this.selectedSuggestionTypes = selectedSuggestionTypes == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(selectedSuggestionTypes);
        this.planSummary = planSummary;
        this.primaryRecommendation = primaryRecommendation;
        this.activatedAt = activatedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public List<String> getSelectedSuggestionTypes() {
        return selectedSuggestionTypes;
    }

    public String getPlanSummary() {
        return planSummary;
    }

    public String getPrimaryRecommendation() {
        return primaryRecommendation;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }
}
