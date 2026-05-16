package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

public class AccelerationPlanSubmitRequest {

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotBlank(message = "sourceTaskId 为必填项")
    private String sourceTaskId;

    private List<AccelerationSuggestionType> selectedSuggestionTypes;

    @AssertTrue(message = "创建受治理加速方案时 selectedSuggestionTypes 不能包含 ALL")
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
