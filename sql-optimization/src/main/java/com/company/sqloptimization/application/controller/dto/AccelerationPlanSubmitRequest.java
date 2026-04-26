package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

public class AccelerationPlanSubmitRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "sourceTaskId is required")
    private String sourceTaskId;

    private List<AccelerationSuggestionType> selectedSuggestionTypes;

    @AssertTrue(message = "selectedSuggestionTypes cannot contain ALL when creating a governed acceleration plan")
    public boolean isSelectedSuggestionTypesValid() {
        return selectedSuggestionTypes == null || !selectedSuggestionTypes.contains(AccelerationSuggestionType.ALL);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public List<AccelerationSuggestionType> getSelectedSuggestionTypes() {
        return selectedSuggestionTypes;
    }

    public void setSelectedSuggestionTypes(List<AccelerationSuggestionType> selectedSuggestionTypes) {
        this.selectedSuggestionTypes = selectedSuggestionTypes;
    }
}
