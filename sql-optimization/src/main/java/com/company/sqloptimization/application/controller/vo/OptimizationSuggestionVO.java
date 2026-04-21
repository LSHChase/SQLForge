package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class OptimizationSuggestionVO {

    private final String summary;
    private final String primaryRecommendation;
    private final Integer confidenceScore;
    private final List<OptimizationArtifactVO> artifacts;
    private final List<OptimizationBenefitVO> benefits;
    private final List<OptimizationCostVO> costs;
    private final List<OptimizationRiskVO> risks;

    public OptimizationSuggestionVO(String summary,
                                    String primaryRecommendation,
                                    Integer confidenceScore,
                                    List<OptimizationArtifactVO> artifacts,
                                    List<OptimizationBenefitVO> benefits,
                                    List<OptimizationCostVO> costs,
                                    List<OptimizationRiskVO> risks) {
        this.summary = summary;
        this.primaryRecommendation = primaryRecommendation;
        this.confidenceScore = confidenceScore;
        this.artifacts = artifacts;
        this.benefits = benefits;
        this.costs = costs;
        this.risks = risks;
    }

    public String getSummary() {
        return summary;
    }

    public String getPrimaryRecommendation() {
        return primaryRecommendation;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public List<OptimizationArtifactVO> getArtifacts() {
        return artifacts;
    }

    public List<OptimizationBenefitVO> getBenefits() {
        return benefits;
    }

    public List<OptimizationCostVO> getCosts() {
        return costs;
    }

    public List<OptimizationRiskVO> getRisks() {
        return risks;
    }
}
