package com.company.sqloptimization.domain.rewrite.cost;

import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CostBasedRewriteSelectionReport {

    public static final String SCHEMA_VERSION = "cost-based-rewrite-selection/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final CostSelectionStrategy strategy;
    private final String selectionStatus;
    private final String selectedCandidateId;
    private final List<String> paretoFrontierCandidateIds;
    private final List<RewriteCostEstimate> estimates;
    private final Map<String, Object> weights;
    private final Map<String, Object> attributes;

    public CostBasedRewriteSelectionReport(String schemaVersion,
                                           String sourceSchemaVersion,
                                           CostSelectionStrategy strategy,
                                           String selectionStatus,
                                           String selectedCandidateId,
                                           List<String> paretoFrontierCandidateIds,
                                           List<RewriteCostEstimate> estimates,
                                           Map<String, Object> weights,
                                           Map<String, Object> attributes) {
        this.schemaVersion = schemaVersion;
        this.sourceSchemaVersion = sourceSchemaVersion;
        this.strategy = strategy;
        this.selectionStatus = selectionStatus;
        this.selectedCandidateId = selectedCandidateId;
        this.paretoFrontierCandidateIds = CostSelectionCollections.immutableStrings(paretoFrontierCandidateIds);
        this.estimates = CostSelectionCollections.immutableList(estimates);
        this.weights = CostSelectionCollections.immutableMap(weights);
        this.attributes = CostSelectionCollections.immutableMap(attributes);
    }

    public RewriteCostEstimate selectedEstimate() {
        for (RewriteCostEstimate estimate : estimates) {
            if (estimate.isSelected()) {
                return estimate;
            }
        }
        return null;
    }

    public RewriteCostEstimate firstEstimateOf(RelationalRewriteRuleType ruleType) {
        if (ruleType == null) {
            return null;
        }
        for (RewriteCostEstimate estimate : estimates) {
            if (ruleType == estimate.getRuleType()) {
                return estimate;
            }
        }
        return null;
    }

    public List<RewriteCostEstimate> estimatesOf(RelationalRewriteRuleType ruleType) {
        if (ruleType == null) {
            return Collections.emptyList();
        }
        List<RewriteCostEstimate> result = new ArrayList<RewriteCostEstimate>();
        for (RewriteCostEstimate estimate : estimates) {
            if (ruleType == estimate.getRuleType()) {
                result.add(estimate);
            }
        }
        return result;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSourceSchemaVersion() {
        return sourceSchemaVersion;
    }

    public CostSelectionStrategy getStrategy() {
        return strategy;
    }

    public String getSelectionStatus() {
        return selectionStatus;
    }

    public String getSelectedCandidateId() {
        return selectedCandidateId;
    }

    public List<String> getParetoFrontierCandidateIds() {
        return paretoFrontierCandidateIds;
    }

    public List<RewriteCostEstimate> getEstimates() {
        return estimates;
    }

    public Map<String, Object> getWeights() {
        return weights;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
