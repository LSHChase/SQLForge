package com.company.sqloptimization.domain.rewrite.recommendation;

import java.util.List;
import java.util.Map;

public class RewriteTransformation {

    private final String type;
    private final List<String> source;
    private final String target;
    private final String description;
    private final Map<String, Object> attributes;

    public RewriteTransformation(String type,
                                 List<String> source,
                                 String target,
                                 String description,
                                 Map<String, Object> attributes) {
        this.type = RewriteRecommendationCollections.text(type);
        this.source = RewriteRecommendationCollections.immutableStrings(source);
        this.target = RewriteRecommendationCollections.text(target);
        this.description = RewriteRecommendationCollections.text(description);
        this.attributes = RewriteRecommendationCollections.immutableMap(attributes);
    }

    public String getType() {
        return type;
    }

    public List<String> getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
