package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizationTaskCost {

    private final String category;
    private final String level;
    private final String summary;

    @JsonCreator
    public OptimizationTaskCost(@JsonProperty("category") String category,
                                @JsonProperty("level") String level,
                                @JsonProperty("summary") String summary) {
        this.category = category;
        this.level = level;
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public String getLevel() {
        return level;
    }

    public String getSummary() {
        return summary;
    }
}
