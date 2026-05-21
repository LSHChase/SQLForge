package com.company.sqloptimization.domain.rewrite.rule;

import java.util.Map;

public class RuleDependencyEdge {

    private final String sourceRuleId;
    private final String targetRuleId;
    private final String edgeType;
    private final String reason;
    private final Map<String, Object> evidence;

    public RuleDependencyEdge(String sourceRuleId,
                              String targetRuleId,
                              String edgeType,
                              String reason,
                              Map<String, Object> evidence) {
        this.sourceRuleId = RuleEngineCollections.text(sourceRuleId);
        this.targetRuleId = RuleEngineCollections.text(targetRuleId);
        this.edgeType = RuleEngineCollections.text(edgeType);
        this.reason = RuleEngineCollections.text(reason);
        this.evidence = RuleEngineCollections.immutableMap(evidence);
    }

    public String getSourceRuleId() {
        return sourceRuleId;
    }

    public String getTargetRuleId() {
        return targetRuleId;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }
}
