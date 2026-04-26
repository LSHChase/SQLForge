package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizationTaskBenefit {

    private final String category;
    private final Integer estimatedImprovementPercent;
    private final String summary;

    @JsonCreator
    public OptimizationTaskBenefit(@JsonProperty("category") String category,
                                   @JsonProperty("estimatedImprovementPercent") Integer estimatedImprovementPercent,
                                   @JsonProperty("summary") String summary) {
        this.category = category;
        this.estimatedImprovementPercent = estimatedImprovementPercent;
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public Integer getEstimatedImprovementPercent() {
        return estimatedImprovementPercent;
    }

    public String getSummary() {
        return summary;
    }
}
