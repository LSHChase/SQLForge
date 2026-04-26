package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizationTaskRisk {

    private final String level;
    private final String category;
    private final String summary;
    private final String mitigation;

    @JsonCreator
    public OptimizationTaskRisk(@JsonProperty("level") String level,
                                @JsonProperty("category") String category,
                                @JsonProperty("summary") String summary,
                                @JsonProperty("mitigation") String mitigation) {
        this.level = level;
        this.category = category;
        this.summary = summary;
        this.mitigation = mitigation;
    }

    public String getLevel() {
        return level;
    }

    public String getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public String getMitigation() {
        return mitigation;
    }
}
