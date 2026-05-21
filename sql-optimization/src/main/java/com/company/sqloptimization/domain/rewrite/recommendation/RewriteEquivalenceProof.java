package com.company.sqloptimization.domain.rewrite.recommendation;

import java.util.List;
import java.util.Map;

public class RewriteEquivalenceProof {

    private final String method;
    private final List<String> verifiedDimensions;
    private final List<String> edgeCases;
    private final List<Map<String, Object>> evidence;
    private final Map<String, Object> attributes;

    public RewriteEquivalenceProof(String method,
                                   List<String> verifiedDimensions,
                                   List<String> edgeCases,
                                   List<Map<String, Object>> evidence,
                                   Map<String, Object> attributes) {
        this.method = RewriteRecommendationCollections.text(method);
        this.verifiedDimensions = RewriteRecommendationCollections.immutableStrings(verifiedDimensions);
        this.edgeCases = RewriteRecommendationCollections.immutableStrings(edgeCases);
        this.evidence = RewriteRecommendationCollections.immutableMaps(evidence);
        this.attributes = RewriteRecommendationCollections.immutableMap(attributes);
    }

    public String getMethod() {
        return method;
    }

    public List<String> getVerifiedDimensions() {
        return verifiedDimensions;
    }

    public List<String> getEdgeCases() {
        return edgeCases;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
