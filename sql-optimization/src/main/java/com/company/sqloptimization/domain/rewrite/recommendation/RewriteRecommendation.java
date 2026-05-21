package com.company.sqloptimization.domain.rewrite.recommendation;

import java.util.List;
import java.util.Map;

public class RewriteRecommendation {

    private final String rewriteId;
    private final String candidateId;
    private final double confidence;
    private final String category;
    private final String severity;
    private final String beforeSummary;
    private final String afterSummary;
    private final List<RewriteTransformation> transformations;
    private final RewriteEquivalenceProof equivalenceProof;
    private final RewritePerformanceEstimate performance;
    private final String executableSql;
    private final double score;
    private final int rank;
    private final boolean autoApplyAllowed;
    private final boolean manualReviewRequired;
    private final List<String> reviewRequirements;
    private final Map<String, Object> scoreBreakdown;
    private final Map<String, Object> attributes;

    public RewriteRecommendation(String rewriteId,
                                 String candidateId,
                                 double confidence,
                                 String category,
                                 String severity,
                                 String beforeSummary,
                                 String afterSummary,
                                 List<RewriteTransformation> transformations,
                                 RewriteEquivalenceProof equivalenceProof,
                                 RewritePerformanceEstimate performance,
                                 String executableSql,
                                 double score,
                                 int rank,
                                 boolean autoApplyAllowed,
                                 boolean manualReviewRequired,
                                 List<String> reviewRequirements,
                                 Map<String, Object> scoreBreakdown,
                                 Map<String, Object> attributes) {
        this.rewriteId = RewriteRecommendationCollections.text(rewriteId);
        this.candidateId = RewriteRecommendationCollections.text(candidateId);
        this.confidence = confidence;
        this.category = RewriteRecommendationCollections.text(category);
        this.severity = RewriteRecommendationCollections.text(severity);
        this.beforeSummary = RewriteRecommendationCollections.text(beforeSummary);
        this.afterSummary = RewriteRecommendationCollections.text(afterSummary);
        this.transformations = RewriteRecommendationCollections.immutableList(transformations);
        this.equivalenceProof = equivalenceProof;
        this.performance = performance;
        this.executableSql = RewriteRecommendationCollections.text(executableSql);
        this.score = score;
        this.rank = rank;
        this.autoApplyAllowed = autoApplyAllowed;
        this.manualReviewRequired = manualReviewRequired;
        this.reviewRequirements = RewriteRecommendationCollections.immutableStrings(reviewRequirements);
        this.scoreBreakdown = RewriteRecommendationCollections.immutableMap(scoreBreakdown);
        this.attributes = RewriteRecommendationCollections.immutableMap(attributes);
    }

    public boolean hasTransformationType(String transformationType) {
        for (RewriteTransformation transformation : transformations) {
            if (transformation.getType().equals(RewriteRecommendationCollections.text(transformationType))) {
                return true;
            }
        }
        return false;
    }

    public String getRewriteId() {
        return rewriteId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getBeforeSummary() {
        return beforeSummary;
    }

    public String getAfterSummary() {
        return afterSummary;
    }

    public List<RewriteTransformation> getTransformations() {
        return transformations;
    }

    public RewriteEquivalenceProof getEquivalenceProof() {
        return equivalenceProof;
    }

    public RewritePerformanceEstimate getPerformance() {
        return performance;
    }

    public String getExecutableSql() {
        return executableSql;
    }

    public double getScore() {
        return score;
    }

    public int getRank() {
        return rank;
    }

    public boolean isAutoApplyAllowed() {
        return autoApplyAllowed;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public List<String> getReviewRequirements() {
        return reviewRequirements;
    }

    public Map<String, Object> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
