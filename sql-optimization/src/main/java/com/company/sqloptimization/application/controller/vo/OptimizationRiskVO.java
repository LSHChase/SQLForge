package com.company.sqloptimization.application.controller.vo;

public class OptimizationRiskVO {

    private final String level;
    private final String category;
    private final String summary;
    private final String mitigation;

    public OptimizationRiskVO(String level, String category, String summary, String mitigation) {
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
