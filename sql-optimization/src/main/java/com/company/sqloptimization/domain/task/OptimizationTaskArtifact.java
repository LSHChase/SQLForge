package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizationTaskArtifact {

    private final String category;
    private final String name;
    private final String content;

    @JsonCreator
    public OptimizationTaskArtifact(@JsonProperty("category") String category,
                                    @JsonProperty("name") String name,
                                    @JsonProperty("content") String content) {
        this.category = category;
        this.name = name;
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
