package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

public class OptimizationTaskSuggestion {

    private final String summary;
    private final String primaryRecommendation;
    private final Integer confidenceScore;
    private final List<OptimizationTaskArtifact> artifacts;
    private final List<OptimizationTaskBenefit> benefits;
    private final List<OptimizationTaskCost> costs;
    private final List<OptimizationTaskRisk> risks;

    @JsonCreator
    public OptimizationTaskSuggestion(@JsonProperty("summary") String summary,
                                      @JsonProperty("primaryRecommendation") String primaryRecommendation,
                                      @JsonProperty("confidenceScore") Integer confidenceScore,
                                      @JsonProperty("artifacts") List<OptimizationTaskArtifact> artifacts,
                                      @JsonProperty("benefits") List<OptimizationTaskBenefit> benefits,
                                      @JsonProperty("costs") List<OptimizationTaskCost> costs,
                                      @JsonProperty("risks") List<OptimizationTaskRisk> risks) {
        this.summary = summary;
        this.primaryRecommendation = primaryRecommendation;
        this.confidenceScore = confidenceScore;
        this.artifacts = artifacts == null ? Collections.<OptimizationTaskArtifact>emptyList() : Collections.unmodifiableList(artifacts);
        this.benefits = benefits == null ? Collections.<OptimizationTaskBenefit>emptyList() : Collections.unmodifiableList(benefits);
        this.costs = costs == null ? Collections.<OptimizationTaskCost>emptyList() : Collections.unmodifiableList(costs);
        this.risks = risks == null ? Collections.<OptimizationTaskRisk>emptyList() : Collections.unmodifiableList(risks);
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

    public List<OptimizationTaskArtifact> getArtifacts() {
        return artifacts;
    }

    public List<OptimizationTaskBenefit> getBenefits() {
        return benefits;
    }

    public List<OptimizationTaskCost> getCosts() {
        return costs;
    }

    public List<OptimizationTaskRisk> getRisks() {
        return risks;
    }
}
