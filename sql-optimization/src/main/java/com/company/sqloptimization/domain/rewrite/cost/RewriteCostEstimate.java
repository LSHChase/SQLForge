package com.company.sqloptimization.domain.rewrite.cost;

import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.List;
import java.util.Map;

public class RewriteCostEstimate {

    private final String estimateId;
    private final String candidateId;
    private final RelationalRewriteRuleType ruleType;
    private final RewriteCostVector costVector;
    private final boolean paretoOptimal;
    private final boolean selected;
    private final int rank;
    private final String semanticGate;
    private final List<String> hetuAdjustments;
    private final List<String> selectionReasons;
    private final List<Map<String, Object>> evidence;
    private final Map<String, Object> attributes;

    public RewriteCostEstimate(String estimateId,
                               String candidateId,
                               RelationalRewriteRuleType ruleType,
                               RewriteCostVector costVector,
                               boolean paretoOptimal,
                               boolean selected,
                               int rank,
                               String semanticGate,
                               List<String> hetuAdjustments,
                               List<String> selectionReasons,
                               List<Map<String, Object>> evidence,
                               Map<String, Object> attributes) {
        this.estimateId = estimateId;
        this.candidateId = candidateId;
        this.ruleType = ruleType;
        this.costVector = costVector;
        this.paretoOptimal = paretoOptimal;
        this.selected = selected;
        this.rank = rank;
        this.semanticGate = semanticGate;
        this.hetuAdjustments = CostSelectionCollections.immutableStrings(hetuAdjustments);
        this.selectionReasons = CostSelectionCollections.immutableStrings(selectionReasons);
        this.evidence = CostSelectionCollections.immutableMaps(evidence);
        this.attributes = CostSelectionCollections.immutableMap(attributes);
    }

    public String getEstimateId() {
        return estimateId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public RelationalRewriteRuleType getRuleType() {
        return ruleType;
    }

    public RewriteCostVector getCostVector() {
        return costVector;
    }

    public boolean isParetoOptimal() {
        return paretoOptimal;
    }

    public boolean isSelected() {
        return selected;
    }

    public int getRank() {
        return rank;
    }

    public String getSemanticGate() {
        return semanticGate;
    }

    public List<String> getHetuAdjustments() {
        return hetuAdjustments;
    }

    public List<String> getSelectionReasons() {
        return selectionReasons;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
