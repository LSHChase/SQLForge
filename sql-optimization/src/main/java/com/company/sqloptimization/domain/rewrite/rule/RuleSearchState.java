package com.company.sqloptimization.domain.rewrite.rule;

import java.util.List;
import java.util.Map;

public class RuleSearchState {

    private final String stateId;
    private final List<String> appliedRuleIds;
    private final List<String> skippedRuleIds;
    private final String transition;
    private final double objectiveCost;
    private final int beamRank;
    private final boolean selected;
    private final List<Map<String, Object>> evidence;
    private final Map<String, Object> attributes;

    public RuleSearchState(String stateId,
                           List<String> appliedRuleIds,
                           List<String> skippedRuleIds,
                           String transition,
                           double objectiveCost,
                           int beamRank,
                           boolean selected,
                           List<Map<String, Object>> evidence,
                           Map<String, Object> attributes) {
        this.stateId = RuleEngineCollections.text(stateId);
        this.appliedRuleIds = RuleEngineCollections.immutableStrings(appliedRuleIds);
        this.skippedRuleIds = RuleEngineCollections.immutableStrings(skippedRuleIds);
        this.transition = RuleEngineCollections.text(transition);
        this.objectiveCost = objectiveCost;
        this.beamRank = beamRank;
        this.selected = selected;
        this.evidence = RuleEngineCollections.immutableMaps(evidence);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public String getStateId() {
        return stateId;
    }

    public List<String> getAppliedRuleIds() {
        return appliedRuleIds;
    }

    public List<String> getSkippedRuleIds() {
        return skippedRuleIds;
    }

    public String getTransition() {
        return transition;
    }

    public double getObjectiveCost() {
        return objectiveCost;
    }

    public int getBeamRank() {
        return beamRank;
    }

    public boolean isSelected() {
        return selected;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
