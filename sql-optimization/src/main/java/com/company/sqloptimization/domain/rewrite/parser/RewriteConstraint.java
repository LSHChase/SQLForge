package com.company.sqloptimization.domain.rewrite.parser;

import java.util.List;
import java.util.Map;

public class RewriteConstraint {

    private final String constraintId;
    private final String sourceTagId;
    private final String constraintType;
    private final String policy;
    private final List<String> targetBlockIds;
    private final String reason;
    private final Map<String, Object> attributes;

    public RewriteConstraint(String constraintId,
                             String sourceTagId,
                             String constraintType,
                             String policy,
                             List<String> targetBlockIds,
                             String reason,
                             Map<String, Object> attributes) {
        this.constraintId = ParserFusionCollections.text(constraintId);
        this.sourceTagId = ParserFusionCollections.text(sourceTagId);
        this.constraintType = ParserFusionCollections.text(constraintType);
        this.policy = ParserFusionCollections.text(policy);
        this.targetBlockIds = ParserFusionCollections.immutableStrings(targetBlockIds);
        this.reason = ParserFusionCollections.text(reason);
        this.attributes = ParserFusionCollections.immutableMap(attributes);
    }

    public String getConstraintId() {
        return constraintId;
    }

    public String getSourceTagId() {
        return sourceTagId;
    }

    public String getConstraintType() {
        return constraintType;
    }

    public String getPolicy() {
        return policy;
    }

    public List<String> getTargetBlockIds() {
        return targetBlockIds;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
