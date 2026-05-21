package com.company.sqloptimization.domain.rewrite.recommendation;

import java.util.List;
import java.util.Map;

public class RewriteRecommendationReport {

    public static final String SCHEMA_VERSION = "rewrite-recommendation-report/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final String generationStatus;
    private final String selectedRecommendationId;
    private final List<RewriteRecommendation> recommendations;
    private final List<String> automationFilteredCandidateIds;
    private final List<String> manualReviewCandidateIds;
    private final List<Map<String, Object>> rankingFactors;
    private final Map<String, Object> weights;
    private final Map<String, Object> attributes;

    public RewriteRecommendationReport(String schemaVersion,
                                       String sourceSchemaVersion,
                                       String generationStatus,
                                       String selectedRecommendationId,
                                       List<RewriteRecommendation> recommendations,
                                       List<String> automationFilteredCandidateIds,
                                       List<String> manualReviewCandidateIds,
                                       List<Map<String, Object>> rankingFactors,
                                       Map<String, Object> weights,
                                       Map<String, Object> attributes) {
        this.schemaVersion = RewriteRecommendationCollections.text(schemaVersion);
        this.sourceSchemaVersion = RewriteRecommendationCollections.text(sourceSchemaVersion);
        this.generationStatus = RewriteRecommendationCollections.text(generationStatus);
        this.selectedRecommendationId = RewriteRecommendationCollections.text(selectedRecommendationId);
        this.recommendations = RewriteRecommendationCollections.immutableList(recommendations);
        this.automationFilteredCandidateIds =
            RewriteRecommendationCollections.immutableStrings(automationFilteredCandidateIds);
        this.manualReviewCandidateIds = RewriteRecommendationCollections.immutableStrings(manualReviewCandidateIds);
        this.rankingFactors = RewriteRecommendationCollections.immutableMaps(rankingFactors);
        this.weights = RewriteRecommendationCollections.immutableMap(weights);
        this.attributes = RewriteRecommendationCollections.immutableMap(attributes);
    }

    public RewriteRecommendation firstRecommendation() {
        return recommendations.isEmpty() ? null : recommendations.get(0);
    }

    public RewriteRecommendation recommendationByCandidateId(String candidateId) {
        String expected = RewriteRecommendationCollections.text(candidateId);
        for (RewriteRecommendation recommendation : recommendations) {
            if (recommendation.getCandidateId().equals(expected)) {
                return recommendation;
            }
        }
        return null;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSourceSchemaVersion() {
        return sourceSchemaVersion;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public String getSelectedRecommendationId() {
        return selectedRecommendationId;
    }

    public List<RewriteRecommendation> getRecommendations() {
        return recommendations;
    }

    public List<String> getAutomationFilteredCandidateIds() {
        return automationFilteredCandidateIds;
    }

    public List<String> getManualReviewCandidateIds() {
        return manualReviewCandidateIds;
    }

    public List<Map<String, Object>> getRankingFactors() {
        return rankingFactors;
    }

    public Map<String, Object> getWeights() {
        return weights;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
