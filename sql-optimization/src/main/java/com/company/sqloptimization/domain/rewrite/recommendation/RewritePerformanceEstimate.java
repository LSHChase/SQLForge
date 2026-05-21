package com.company.sqloptimization.domain.rewrite.recommendation;

import java.util.Map;

public class RewritePerformanceEstimate {

    private final String scanReduction;
    private final String estimatedSpeedup;
    private final String memoryImpact;
    private final String riskLevel;
    private final Map<String, Object> attributes;

    public RewritePerformanceEstimate(String scanReduction,
                                      String estimatedSpeedup,
                                      String memoryImpact,
                                      String riskLevel,
                                      Map<String, Object> attributes) {
        this.scanReduction = RewriteRecommendationCollections.text(scanReduction);
        this.estimatedSpeedup = RewriteRecommendationCollections.text(estimatedSpeedup);
        this.memoryImpact = RewriteRecommendationCollections.text(memoryImpact);
        this.riskLevel = RewriteRecommendationCollections.text(riskLevel);
        this.attributes = RewriteRecommendationCollections.immutableMap(attributes);
    }

    public String getScanReduction() {
        return scanReduction;
    }

    public String getEstimatedSpeedup() {
        return estimatedSpeedup;
    }

    public String getMemoryImpact() {
        return memoryImpact;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
