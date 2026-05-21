package com.company.sqloptimization.domain.rewrite.parser;

import java.util.List;
import java.util.Map;

public class HetuPlanHint {

    private final String hintId;
    private final String optimizationType;
    private final String target;
    private final String hintText;
    private final String reason;
    private final List<Map<String, Object>> evidence;
    private final Map<String, Object> attributes;

    public HetuPlanHint(String hintId,
                        String optimizationType,
                        String target,
                        String hintText,
                        String reason,
                        List<Map<String, Object>> evidence,
                        Map<String, Object> attributes) {
        this.hintId = ParserFusionCollections.text(hintId);
        this.optimizationType = ParserFusionCollections.text(optimizationType);
        this.target = ParserFusionCollections.text(target);
        this.hintText = ParserFusionCollections.text(hintText);
        this.reason = ParserFusionCollections.text(reason);
        this.evidence = ParserFusionCollections.immutableMaps(evidence);
        this.attributes = ParserFusionCollections.immutableMap(attributes);
    }

    public String getHintId() {
        return hintId;
    }

    public String getOptimizationType() {
        return optimizationType;
    }

    public String getTarget() {
        return target;
    }

    public String getHintText() {
        return hintText;
    }

    public String getReason() {
        return reason;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
