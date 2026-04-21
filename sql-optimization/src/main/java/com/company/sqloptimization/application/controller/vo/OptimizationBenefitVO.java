package com.company.sqloptimization.application.controller.vo;

public class OptimizationBenefitVO {

    private final String category;
    private final Integer estimatedImprovementPercent;
    private final String summary;

    public OptimizationBenefitVO(String category, Integer estimatedImprovementPercent, String summary) {
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
