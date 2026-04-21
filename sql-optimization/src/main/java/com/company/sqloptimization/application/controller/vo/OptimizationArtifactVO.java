package com.company.sqloptimization.application.controller.vo;

public class OptimizationArtifactVO {

    private final String category;
    private final String name;
    private final String content;

    public OptimizationArtifactVO(String category, String name, String content) {
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
