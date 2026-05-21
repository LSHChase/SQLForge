package com.company.sqloptimization.domain.rewrite.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RuleConflictResolutionReport {

    public static final String SCHEMA_VERSION = "rewrite-rule-engine/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final String resolutionStatus;
    private final List<RewriteRuleDefinition> matchedRules;
    private final List<RuleDependencyEdge> dependencyEdges;
    private final List<RuleConflict> conflicts;
    private final List<String> topologicalOrder;
    private final List<RuleSearchState> searchStates;
    private final List<String> selectedRuleIds;
    private final Map<String, Object> attributes;

    public RuleConflictResolutionReport(String schemaVersion,
                                        String sourceSchemaVersion,
                                        String resolutionStatus,
                                        List<RewriteRuleDefinition> matchedRules,
                                        List<RuleDependencyEdge> dependencyEdges,
                                        List<RuleConflict> conflicts,
                                        List<String> topologicalOrder,
                                        List<RuleSearchState> searchStates,
                                        List<String> selectedRuleIds,
                                        Map<String, Object> attributes) {
        this.schemaVersion = RuleEngineCollections.text(schemaVersion);
        this.sourceSchemaVersion = RuleEngineCollections.text(sourceSchemaVersion);
        this.resolutionStatus = RuleEngineCollections.text(resolutionStatus);
        this.matchedRules = RuleEngineCollections.immutableList(matchedRules);
        this.dependencyEdges = RuleEngineCollections.immutableList(dependencyEdges);
        this.conflicts = RuleEngineCollections.immutableList(conflicts);
        this.topologicalOrder = RuleEngineCollections.immutableStrings(topologicalOrder);
        this.searchStates = RuleEngineCollections.immutableList(searchStates);
        this.selectedRuleIds = RuleEngineCollections.immutableStrings(selectedRuleIds);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public boolean hasMatchedRule(String ruleId) {
        for (RewriteRuleDefinition rule : matchedRules) {
            if (rule.getId().equals(ruleId)) {
                return true;
            }
        }
        return false;
    }

    public RewriteRuleDefinition matchedRule(String ruleId) {
        for (RewriteRuleDefinition rule : matchedRules) {
            if (rule.getId().equals(ruleId)) {
                return rule;
            }
        }
        return null;
    }

    public boolean hasConflictType(String conflictType) {
        for (RuleConflict conflict : conflicts) {
            if (conflict.getConflictType().equals(conflictType)) {
                return true;
            }
        }
        return false;
    }

    public RuleSearchState selectedState() {
        for (RuleSearchState state : searchStates) {
            if (state.isSelected()) {
                return state;
            }
        }
        return null;
    }

    public List<RuleConflict> conflictsOf(String conflictType) {
        if (conflictType == null) {
            return Collections.emptyList();
        }
        List<RuleConflict> result = new ArrayList<RuleConflict>();
        for (RuleConflict conflict : conflicts) {
            if (conflict.getConflictType().equals(conflictType)) {
                result.add(conflict);
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

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public List<RewriteRuleDefinition> getMatchedRules() {
        return matchedRules;
    }

    public List<RuleDependencyEdge> getDependencyEdges() {
        return dependencyEdges;
    }

    public List<RuleConflict> getConflicts() {
        return conflicts;
    }

    public List<String> getTopologicalOrder() {
        return topologicalOrder;
    }

    public List<RuleSearchState> getSearchStates() {
        return searchStates;
    }

    public List<String> getSelectedRuleIds() {
        return selectedRuleIds;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
