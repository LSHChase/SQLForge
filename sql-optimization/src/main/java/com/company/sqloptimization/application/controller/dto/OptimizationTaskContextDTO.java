package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationParseDepth;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.Size;

public class OptimizationTaskContextDTO {

    private OptimizationParseDepth parseDepth = OptimizationParseDepth.DEEP;
    private OptimizationTaskPriority priority = OptimizationTaskPriority.NORMAL;

    @Size(max = 2048, message = "callbackUrl exceeds 2048 characters")
    private String callbackUrl;

    private List<AccelerationSuggestionType> requestedSuggestionTypes = Collections.emptyList();

    public OptimizationParseDepth getParseDepth() {
        return parseDepth;
    }

    public void setParseDepth(OptimizationParseDepth parseDepth) {
        this.parseDepth = parseDepth;
    }

    public OptimizationTaskPriority getPriority() {
        return priority;
    }

    public void setPriority(OptimizationTaskPriority priority) {
        this.priority = priority;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public List<AccelerationSuggestionType> getRequestedSuggestionTypes() {
        return requestedSuggestionTypes;
    }

    public void setRequestedSuggestionTypes(List<AccelerationSuggestionType> requestedSuggestionTypes) {
        if (requestedSuggestionTypes == null) {
            this.requestedSuggestionTypes = Collections.emptyList();
            return;
        }
        this.requestedSuggestionTypes = Collections.unmodifiableList(
            new ArrayList<AccelerationSuggestionType>(requestedSuggestionTypes)
        );
    }
}
