package com.company.sqloptimization.application.controller.vo;

public class OptimizationCostVO {

    private final String category;
    private final String level;
    private final String summary;

    public OptimizationCostVO(String category, String level, String summary) {
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
