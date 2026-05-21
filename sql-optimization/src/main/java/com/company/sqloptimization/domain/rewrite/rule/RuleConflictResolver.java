package com.company.sqloptimization.domain.rewrite.rule;

import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.cost.RewriteCostEstimate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RuleConflictResolver {

    private static final int BEAM_WIDTH = 4;

    public RuleConflictResolutionReport resolve(RelationalRewritePlan plan,
                                                CostBasedRewriteSelectionReport costReport) {
        if (plan == null || plan.getCandidates().isEmpty()) {
            return emptyReport(plan == null ? "" : plan.getSchemaVersion(), "NO_REWRITE_RULE_CANDIDATE");
        }
        List<RewriteRuleDefinition> matchedRules = matchedRules(plan);
        if (matchedRules.isEmpty()) {
            return emptyReport(plan.getSchemaVersion(), "NO_RULE_DSL_MATCH");
        }
        Map<String, RewriteRuleDefinition> rulesById = rulesById(matchedRules);
        Map<String, RelationalRewriteCandidate> candidatesByRuleId = candidatesByRuleId(plan, matchedRules);
        Map<String, RewriteCostEstimate> estimatesByRuleId = estimatesByRuleId(costReport, matchedRules);
        List<RuleDependencyEdge> dependencyEdges = dependencyEdges(matchedRules, candidatesByRuleId);
        TopologyResult topology = topologicalOrder(matchedRules, dependencyEdges);
        List<RuleConflict> conflicts = conflicts(
            matchedRules,
            candidatesByRuleId,
            estimatesByRuleId,
            dependencyEdges,
            topology.cycleDetected
        );
        boolean localSearchRequired = topology.cycleDetected || hasConflict(conflicts, "MUTUALLY_EXCLUSIVE_REWRITE");
        List<RuleSearchState> searchStates = localSearchRequired
            ? beamSearch(matchedRules, dependencyEdges, conflicts, estimatesByRuleId)
            : Collections.<RuleSearchState>emptyList();
        List<String> selectedRuleIds = localSearchRequired
            ? selectedRuleIds(searchStates)
            : topology.order;
        String status = status(matchedRules, localSearchRequired, topology.cycleDetected);
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("catalogRuleCount", Integer.valueOf(new RewriteRuleCatalog().defaultRules().size()));
        attributes.put("matchedRuleCount", Integer.valueOf(matchedRules.size()));
        attributes.put("dependencyEdgeCount", Integer.valueOf(dependencyEdges.size()));
        attributes.put("conflictCount", Integer.valueOf(conflicts.size()));
        attributes.put("cycleDetected", Boolean.valueOf(topology.cycleDetected));
        attributes.put("localSearchRequired", Boolean.valueOf(localSearchRequired));
        attributes.put("beamWidth", Integer.valueOf(BEAM_WIDTH));
        attributes.put("selectedRuleCount", Integer.valueOf(selectedRuleIds.size()));
        attributes.put("rdgNodeIds", new ArrayList<String>(rulesById.keySet()));
        return new RuleConflictResolutionReport(
            RuleConflictResolutionReport.SCHEMA_VERSION,
            sourceSchemaVersion(costReport, plan),
            status,
            matchedRules,
            dependencyEdges,
            conflicts,
            topology.order,
            searchStates,
            selectedRuleIds,
            attributes
        );
    }

    private RuleConflictResolutionReport emptyReport(String sourceSchemaVersion, String reason) {
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("catalogRuleCount", Integer.valueOf(new RewriteRuleCatalog().defaultRules().size()));
        attributes.put("matchedRuleCount", Integer.valueOf(0));
        attributes.put("dependencyEdgeCount", Integer.valueOf(0));
        attributes.put("conflictCount", Integer.valueOf(0));
        attributes.put("cycleDetected", Boolean.FALSE);
        attributes.put("localSearchRequired", Boolean.FALSE);
        attributes.put("beamWidth", Integer.valueOf(BEAM_WIDTH));
        attributes.put("selectedRuleCount", Integer.valueOf(0));
        attributes.put("reason", reason);
        return new RuleConflictResolutionReport(
            RuleConflictResolutionReport.SCHEMA_VERSION,
            sourceSchemaVersion,
            "NO_RULE",
            Collections.<RewriteRuleDefinition>emptyList(),
            Collections.<RuleDependencyEdge>emptyList(),
            Collections.<RuleConflict>emptyList(),
            Collections.<String>emptyList(),
            Collections.<RuleSearchState>emptyList(),
            Collections.<String>emptyList(),
            attributes
        );
    }

    private List<RewriteRuleDefinition> matchedRules(RelationalRewritePlan plan) {
        List<RewriteRuleDefinition> result = new ArrayList<RewriteRuleDefinition>();
        for (RewriteRuleDefinition rule : new RewriteRuleCatalog().defaultRules()) {
            if (plan.hasCandidate(rule.getRelationalRuleType())) {
                result.add(rule);
            }
        }
        return result;
    }

    private Map<String, RewriteRuleDefinition> rulesById(List<RewriteRuleDefinition> rules) {
        LinkedHashMap<String, RewriteRuleDefinition> result = new LinkedHashMap<String, RewriteRuleDefinition>();
        for (RewriteRuleDefinition rule : rules) {
            result.put(rule.getId(), rule);
        }
        return result;
    }

    private Map<String, RelationalRewriteCandidate> candidatesByRuleId(RelationalRewritePlan plan,
                                                                        List<RewriteRuleDefinition> rules) {
        LinkedHashMap<String, RelationalRewriteCandidate> result =
            new LinkedHashMap<String, RelationalRewriteCandidate>();
        for (RewriteRuleDefinition rule : rules) {
            List<RelationalRewriteCandidate> candidates = plan.candidatesOf(rule.getRelationalRuleType());
            if (!candidates.isEmpty()) {
                result.put(rule.getId(), candidates.get(0));
            }
        }
        return result;
    }

    private Map<String, RewriteCostEstimate> estimatesByRuleId(CostBasedRewriteSelectionReport costReport,
                                                                List<RewriteRuleDefinition> rules) {
        LinkedHashMap<String, RewriteCostEstimate> result = new LinkedHashMap<String, RewriteCostEstimate>();
        if (costReport == null) {
            return result;
        }
        for (RewriteRuleDefinition rule : rules) {
            RewriteCostEstimate estimate = costReport.firstEstimateOf(rule.getRelationalRuleType());
            if (estimate != null) {
                result.put(rule.getId(), estimate);
            }
        }
        return result;
    }

    private List<RuleDependencyEdge> dependencyEdges(List<RewriteRuleDefinition> rules,
                                                     Map<String, RelationalRewriteCandidate> candidatesByRuleId) {
        Set<String> matchedIds = new HashSet<String>();
        for (RewriteRuleDefinition rule : rules) {
            matchedIds.add(rule.getId());
        }
        List<RuleDependencyEdge> result = new ArrayList<RuleDependencyEdge>();
        for (RewriteRuleDefinition rule : rules) {
            for (String dependency : rule.getMustRunAfterRuleIds()) {
                if (matchedIds.contains(dependency)) {
                    result.add(edge(
                        dependency,
                        rule.getId(),
                        "ORDER_DEPENDENCY",
                        "DEPENDENCY_RULE_PRODUCES_REQUIRED_PATTERN",
                        sharedScopeEvidence(dependency, rule.getId(), candidatesByRuleId)
                    ));
                }
            }
        }
        addScopeOrdering(result, rules, candidatesByRuleId);
        return result;
    }

    private void addScopeOrdering(List<RuleDependencyEdge> result,
                                  List<RewriteRuleDefinition> rules,
                                  Map<String, RelationalRewriteCandidate> candidatesByRuleId) {
        if (!containsRule(rules, "CSE-DEDUP-001") || !containsRule(rules, "JOIN-HUNNEST-001")) {
            return;
        }
        if (!sharesSourceBlocks("CSE-DEDUP-001", "JOIN-HUNNEST-001", candidatesByRuleId)) {
            return;
        }
        result.add(edge(
            "CSE-DEDUP-001",
            "JOIN-HUNNEST-001",
            "ORDER_DEPENDENCY",
            "DEDUPLICATE_CHILD_BLOCKS_BEFORE_PARENT_JOIN_REWRITE",
            sharedScopeEvidence("CSE-DEDUP-001", "JOIN-HUNNEST-001", candidatesByRuleId)
        ));
    }

    private RuleDependencyEdge edge(String sourceRuleId,
                                    String targetRuleId,
                                    String edgeType,
                                    String reason,
                                    Map<String, Object> evidence) {
        return new RuleDependencyEdge(sourceRuleId, targetRuleId, edgeType, reason, evidence);
    }

    private List<RuleConflict> conflicts(List<RewriteRuleDefinition> rules,
                                         Map<String, RelationalRewriteCandidate> candidatesByRuleId,
                                         Map<String, RewriteCostEstimate> estimatesByRuleId,
                                         List<RuleDependencyEdge> dependencyEdges,
                                         boolean cycleDetected) {
        List<RuleConflict> result = new ArrayList<RuleConflict>();
        int[] sequence = new int[] {1};
        for (RuleDependencyEdge edge : dependencyEdges) {
            result.add(conflict(
                sequence,
                "ORDER_DEPENDENCY",
                Arrays.asList(edge.getSourceRuleId(), edge.getTargetRuleId()),
                edge.getReason(),
                "APPLY_TOPOLOGICAL_ORDER_WHEN_RDG_ACYCLIC",
                edge.getEvidence()
            ));
        }
        for (int leftIndex = 0; leftIndex < rules.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < rules.size(); rightIndex++) {
                RewriteRuleDefinition left = rules.get(leftIndex);
                RewriteRuleDefinition right = rules.get(rightIndex);
                if (isMutuallyExclusive(left, right, candidatesByRuleId)) {
                    result.add(conflict(
                        sequence,
                        "MUTUALLY_EXCLUSIVE_REWRITE",
                        Arrays.asList(left.getId(), right.getId()),
                        "OVERLAPPING_SCOPE_WITH_DIFFERENT_PARENT_CHILD_REWRITE_SHAPE",
                        "LOCAL_SEARCH_BEAM_WIDTH_4",
                        sharedScopeEvidence(left.getId(), right.getId(), candidatesByRuleId)
                    ));
                }
                if (hasCostContradiction(left.getId(), right.getId(), estimatesByRuleId)) {
                    result.add(conflict(
                        sequence,
                        "COST_CONTRADICTION",
                        Arrays.asList(left.getId(), right.getId()),
                        "SCAN_MEMORY_TRADEOFF_DETECTED",
                        "COMPARE_OBJECTIVE_COST_IN_LOCAL_OR_TOPOLOGICAL_SELECTION",
                        costTradeoffEvidence(left.getId(), right.getId(), estimatesByRuleId)
                    ));
                }
            }
        }
        if (cycleDetected) {
            result.add(conflict(
                sequence,
                "RDG_CYCLE",
                ruleIds(rules),
                "RULE_DEPENDENCY_GRAPH_CONTAINS_CYCLE",
                "LOCAL_SEARCH_BEAM_WIDTH_4",
                Collections.<String, Object>emptyMap()
            ));
        }
        return result;
    }

    private RuleConflict conflict(int[] sequence,
                                  String conflictType,
                                  List<String> ruleIds,
                                  String signal,
                                  String resolutionPolicy,
                                  Map<String, Object> evidence) {
        int value = sequence[0];
        sequence[0]++;
        return new RuleConflict(
            "RULE-CONFLICT-" + value,
            conflictType,
            ruleIds,
            signal,
            resolutionPolicy,
            evidence
        );
    }

    private boolean isMutuallyExclusive(RewriteRuleDefinition left,
                                        RewriteRuleDefinition right,
                                        Map<String, RelationalRewriteCandidate> candidatesByRuleId) {
        if (!sharesSourceBlocks(left.getId(), right.getId(), candidatesByRuleId)) {
            return false;
        }
        boolean parentRewrite = RelationalRewriteRuleType.HORIZONTAL_UNNESTING == left.getRelationalRuleType()
            || RelationalRewriteRuleType.HORIZONTAL_UNNESTING == right.getRelationalRuleType();
        boolean blockMerge = left.hasActionType("MERGE_BLOCKS") && right.hasActionType("MERGE_BLOCKS");
        return parentRewrite && blockMerge;
    }

    private boolean sharesSourceBlocks(String leftRuleId,
                                       String rightRuleId,
                                       Map<String, RelationalRewriteCandidate> candidatesByRuleId) {
        RelationalRewriteCandidate left = candidatesByRuleId.get(leftRuleId);
        RelationalRewriteCandidate right = candidatesByRuleId.get(rightRuleId);
        if (left == null || right == null) {
            return false;
        }
        for (String blockId : left.getSourceBlockIds()) {
            if (right.getSourceBlockIds().contains(blockId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCostContradiction(String leftRuleId,
                                         String rightRuleId,
                                         Map<String, RewriteCostEstimate> estimatesByRuleId) {
        RewriteCostEstimate left = estimatesByRuleId.get(leftRuleId);
        RewriteCostEstimate right = estimatesByRuleId.get(rightRuleId);
        if (left == null || right == null) {
            return false;
        }
        boolean leftScanForMemory = left.getCostVector().getScanCost() < right.getCostVector().getScanCost()
            && left.getCostVector().getMemoryCost() > right.getCostVector().getMemoryCost();
        boolean rightScanForMemory = right.getCostVector().getScanCost() < left.getCostVector().getScanCost()
            && right.getCostVector().getMemoryCost() > left.getCostVector().getMemoryCost();
        return leftScanForMemory || rightScanForMemory;
    }

    private TopologyResult topologicalOrder(List<RewriteRuleDefinition> rules,
                                            List<RuleDependencyEdge> dependencyEdges) {
        LinkedHashMap<String, Integer> incoming = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, List<String>> outgoing = new LinkedHashMap<String, List<String>>();
        for (RewriteRuleDefinition rule : rules) {
            incoming.put(rule.getId(), Integer.valueOf(0));
            outgoing.put(rule.getId(), new ArrayList<String>());
        }
        for (RuleDependencyEdge edge : dependencyEdges) {
            if (!incoming.containsKey(edge.getSourceRuleId()) || !incoming.containsKey(edge.getTargetRuleId())) {
                continue;
            }
            outgoing.get(edge.getSourceRuleId()).add(edge.getTargetRuleId());
            incoming.put(edge.getTargetRuleId(), Integer.valueOf(incoming.get(edge.getTargetRuleId()).intValue() + 1));
        }
        List<String> ready = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : incoming.entrySet()) {
            if (entry.getValue().intValue() == 0) {
                ready.add(entry.getKey());
            }
        }
        List<String> ordered = new ArrayList<String>();
        while (!ready.isEmpty()) {
            Collections.sort(ready);
            String current = ready.remove(0);
            ordered.add(current);
            for (String target : outgoing.get(current)) {
                int nextIncoming = incoming.get(target).intValue() - 1;
                incoming.put(target, Integer.valueOf(nextIncoming));
                if (nextIncoming == 0) {
                    ready.add(target);
                }
            }
        }
        boolean cycle = ordered.size() != rules.size();
        if (cycle) {
            for (RewriteRuleDefinition rule : rules) {
                if (!ordered.contains(rule.getId())) {
                    ordered.add(rule.getId());
                }
            }
        }
        return new TopologyResult(ordered, cycle);
    }

    private List<RuleSearchState> beamSearch(List<RewriteRuleDefinition> rules,
                                             List<RuleDependencyEdge> dependencyEdges,
                                             List<RuleConflict> conflicts,
                                             Map<String, RewriteCostEstimate> estimatesByRuleId) {
        List<String> orderedRuleIds = topologicalOrder(rules, dependencyEdges).order;
        List<MutableState> beam = Collections.singletonList(new MutableState(
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            "START"
        ));
        for (String ruleId : orderedRuleIds) {
            List<MutableState> expanded = new ArrayList<MutableState>();
            for (MutableState state : beam) {
                expanded.add(state.skip(ruleId));
                expanded.add(state.apply(ruleId));
            }
            rankStates(expanded, dependencyEdges, conflicts, estimatesByRuleId);
            beam = first(expanded, BEAM_WIDTH);
        }
        rankStates(beam, dependencyEdges, conflicts, estimatesByRuleId);
        MutableState selected = selectedMutableState(beam);
        List<RuleSearchState> result = new ArrayList<RuleSearchState>();
        int rank = 1;
        int sequence = 1;
        for (MutableState state : beam) {
            result.add(state.toImmutable("RULE-STATE-" + sequence, rank, state == selected));
            rank++;
            sequence++;
        }
        return result;
    }

    private void rankStates(List<MutableState> states,
                            List<RuleDependencyEdge> dependencyEdges,
                            List<RuleConflict> conflicts,
                            Map<String, RewriteCostEstimate> estimatesByRuleId) {
        for (MutableState state : states) {
            state.objectiveCost = objectiveCost(state, dependencyEdges, conflicts, estimatesByRuleId);
        }
        Collections.sort(states, new Comparator<MutableState>() {
            @Override
            public int compare(MutableState left, MutableState right) {
                int cost = Double.compare(left.objectiveCost, right.objectiveCost);
                if (cost != 0) {
                    return cost;
                }
                int applied = Integer.valueOf(right.appliedRuleIds.size()).compareTo(
                    Integer.valueOf(left.appliedRuleIds.size())
                );
                if (applied != 0) {
                    return applied;
                }
                return left.appliedRuleIds.toString().compareTo(right.appliedRuleIds.toString());
            }
        });
    }

    private double objectiveCost(MutableState state,
                                 List<RuleDependencyEdge> dependencyEdges,
                                 List<RuleConflict> conflicts,
                                 Map<String, RewriteCostEstimate> estimatesByRuleId) {
        double totalKnownCost = totalKnownCost(estimatesByRuleId.values());
        if (state.appliedRuleIds.isEmpty()) {
            return round(totalKnownCost * 1.25 + 100.0);
        }
        double objective = 0.0;
        for (String ruleId : state.appliedRuleIds) {
            objective += ruleCost(ruleId, estimatesByRuleId);
        }
        objective = objective / Math.max(1, state.appliedRuleIds.size());
        objective += dependencyPenalty(state, dependencyEdges, totalKnownCost);
        objective += conflictPenalty(state, conflicts, totalKnownCost);
        objective += semanticGatePenalty(state, estimatesByRuleId);
        objective -= dependencySynergyBonus(state, dependencyEdges, objective);
        return round(Math.max(1.0, objective));
    }

    private double dependencyPenalty(MutableState state,
                                     List<RuleDependencyEdge> dependencyEdges,
                                     double totalKnownCost) {
        double penalty = 0.0;
        for (RuleDependencyEdge edge : dependencyEdges) {
            boolean targetApplied = state.appliedRuleIds.contains(edge.getTargetRuleId());
            boolean sourceApplied = state.appliedRuleIds.contains(edge.getSourceRuleId());
            if (targetApplied && !sourceApplied) {
                penalty += Math.max(150.0, totalKnownCost * 0.20);
            }
        }
        return penalty;
    }

    private double conflictPenalty(MutableState state, List<RuleConflict> conflicts, double totalKnownCost) {
        double penalty = 0.0;
        for (RuleConflict conflict : conflicts) {
            if (!state.appliedRuleIds.containsAll(conflict.getRuleIds())) {
                continue;
            }
            if ("MUTUALLY_EXCLUSIVE_REWRITE".equals(conflict.getConflictType())) {
                penalty += Math.max(400.0, totalKnownCost * 0.75);
            } else if ("COST_CONTRADICTION".equals(conflict.getConflictType())) {
                penalty += Math.max(80.0, totalKnownCost * 0.08);
            } else if ("RDG_CYCLE".equals(conflict.getConflictType())) {
                penalty += Math.max(120.0, totalKnownCost * 0.12);
            }
        }
        return penalty;
    }

    private double semanticGatePenalty(MutableState state,
                                       Map<String, RewriteCostEstimate> estimatesByRuleId) {
        double penalty = 0.0;
        for (String ruleId : state.appliedRuleIds) {
            RewriteCostEstimate estimate = estimatesByRuleId.get(ruleId);
            if (estimate == null) {
                penalty += 70.0;
            } else if (!"PROVED".equals(estimate.getSemanticGate())) {
                penalty += 45.0;
            }
        }
        return penalty;
    }

    private double dependencySynergyBonus(MutableState state,
                                          List<RuleDependencyEdge> dependencyEdges,
                                          double objective) {
        int satisfied = 0;
        for (RuleDependencyEdge edge : dependencyEdges) {
            if (state.appliedRuleIds.contains(edge.getSourceRuleId())
                && state.appliedRuleIds.contains(edge.getTargetRuleId())) {
                satisfied++;
            }
        }
        return objective * Math.min(0.20, satisfied * 0.08);
    }

    private double totalKnownCost(Collection<RewriteCostEstimate> estimates) {
        double result = 0.0;
        for (RewriteCostEstimate estimate : estimates) {
            result += estimate.getCostVector().getWeightedCost();
        }
        return result <= 0.0 ? 3000.0 : result;
    }

    private double ruleCost(String ruleId, Map<String, RewriteCostEstimate> estimatesByRuleId) {
        RewriteCostEstimate estimate = estimatesByRuleId.get(ruleId);
        if (estimate == null) {
            return 1200.0;
        }
        return estimate.getCostVector().getWeightedCost();
    }

    private MutableState selectedMutableState(List<MutableState> states) {
        for (MutableState state : states) {
            if (!state.appliedRuleIds.isEmpty()) {
                return state;
            }
        }
        return states.isEmpty() ? null : states.get(0);
    }

    private List<MutableState> first(List<MutableState> states, int maxSize) {
        List<MutableState> result = new ArrayList<MutableState>();
        for (int index = 0; index < states.size() && index < maxSize; index++) {
            result.add(states.get(index));
        }
        return result;
    }

    private List<String> selectedRuleIds(List<RuleSearchState> searchStates) {
        RuleSearchState selected = null;
        for (RuleSearchState state : searchStates) {
            if (state.isSelected()) {
                selected = state;
                break;
            }
        }
        return selected == null ? Collections.<String>emptyList() : selected.getAppliedRuleIds();
    }

    private boolean hasConflict(List<RuleConflict> conflicts, String conflictType) {
        for (RuleConflict conflict : conflicts) {
            if (conflict.getConflictType().equals(conflictType)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRule(List<RewriteRuleDefinition> rules, String ruleId) {
        for (RewriteRuleDefinition rule : rules) {
            if (rule.getId().equals(ruleId)) {
                return true;
            }
        }
        return false;
    }

    private List<String> ruleIds(List<RewriteRuleDefinition> rules) {
        List<String> result = new ArrayList<String>();
        for (RewriteRuleDefinition rule : rules) {
            result.add(rule.getId());
        }
        return result;
    }

    private Map<String, Object> sharedScopeEvidence(String leftRuleId,
                                                    String rightRuleId,
                                                    Map<String, RelationalRewriteCandidate> candidatesByRuleId) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        RelationalRewriteCandidate left = candidatesByRuleId.get(leftRuleId);
        RelationalRewriteCandidate right = candidatesByRuleId.get(rightRuleId);
        evidence.put("leftRuleId", leftRuleId);
        evidence.put("rightRuleId", rightRuleId);
        evidence.put("leftCandidateId", left == null ? "" : left.getCandidateId());
        evidence.put("rightCandidateId", right == null ? "" : right.getCandidateId());
        evidence.put("sharedSourceBlockIds", sharedSourceBlockIds(left, right));
        evidence.put("matchScope", "STATIC_QBDAG_SOURCE_BLOCK_OVERLAP");
        return evidence;
    }

    private List<String> sharedSourceBlockIds(RelationalRewriteCandidate left, RelationalRewriteCandidate right) {
        if (left == null || right == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String blockId : left.getSourceBlockIds()) {
            if (right.getSourceBlockIds().contains(blockId)) {
                result.add(blockId);
            }
        }
        return new ArrayList<String>(result);
    }

    private Map<String, Object> costTradeoffEvidence(String leftRuleId,
                                                     String rightRuleId,
                                                     Map<String, RewriteCostEstimate> estimatesByRuleId) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        RewriteCostEstimate left = estimatesByRuleId.get(leftRuleId);
        RewriteCostEstimate right = estimatesByRuleId.get(rightRuleId);
        evidence.put("leftRuleId", leftRuleId);
        evidence.put("rightRuleId", rightRuleId);
        evidence.put("leftCandidateId", left == null ? "" : left.getCandidateId());
        evidence.put("rightCandidateId", right == null ? "" : right.getCandidateId());
        evidence.put("leftCostVector", left == null ? Collections.emptyMap() : left.getCostVector().toMap());
        evidence.put("rightCostVector", right == null ? Collections.emptyMap() : right.getCostVector().toMap());
        evidence.put("tradeoffDimensions", Arrays.asList("ScanCost", "MemoryCost"));
        return evidence;
    }

    private String sourceSchemaVersion(CostBasedRewriteSelectionReport costReport, RelationalRewritePlan plan) {
        if (costReport != null && !costReport.getSchemaVersion().isEmpty()) {
            return costReport.getSchemaVersion();
        }
        return plan == null ? "" : plan.getSchemaVersion();
    }

    private String status(List<RewriteRuleDefinition> matchedRules,
                          boolean localSearchRequired,
                          boolean cycleDetected) {
        if (matchedRules.isEmpty()) {
            return "NO_RULE";
        }
        if (cycleDetected) {
            return "LOCAL_SEARCH_SELECTED_FOR_RDG_CYCLE";
        }
        if (localSearchRequired) {
            return "LOCAL_SEARCH_SELECTED_FOR_CONFLICT";
        }
        return "TOPOLOGICAL_ORDER_READY";
    }

    private LinkedHashMap<String, Object> baseAttributes() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "RELATIONAL_REWRITE_PLAN_AND_COST_REPORT");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("dslStatus", "STATIC_RULE_DSL_V1");
        attributes.put("dependencyGraph", "RULE_DEPENDENCY_GRAPH");
        attributes.put("cycleDetection", "KAHN_TOPOLOGICAL_SORT");
        attributes.put("localSearchAlgorithm", "BEAM_SEARCH");
        attributes.put("localSearchBeamWidthRange", "3-5");
        attributes.put("objectiveFunction", "ABSTRACT_ESTIMATED_COST_C");
        attributes.put("stateSpace", "APPLIED_RULE_SUBSET");
        attributes.put("transitionModel", "APPLY_OR_SKIP_RULE");
        attributes.put("searchBoundary", "STATIC_RULE_PLANNING_NO_SQL_EXECUTION");
        return attributes;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class TopologyResult {
        private final List<String> order;
        private final boolean cycleDetected;

        private TopologyResult(List<String> order, boolean cycleDetected) {
            this.order = order;
            this.cycleDetected = cycleDetected;
        }
    }

    private final class MutableState {
        private final List<String> appliedRuleIds;
        private final List<String> skippedRuleIds;
        private final String transition;
        private double objectiveCost;

        private MutableState(List<String> appliedRuleIds, List<String> skippedRuleIds, String transition) {
            this.appliedRuleIds = new ArrayList<String>(appliedRuleIds);
            this.skippedRuleIds = new ArrayList<String>(skippedRuleIds);
            this.transition = transition;
        }

        private MutableState apply(String ruleId) {
            List<String> applied = new ArrayList<String>(appliedRuleIds);
            applied.add(ruleId);
            return new MutableState(applied, skippedRuleIds, "APPLY:" + ruleId);
        }

        private MutableState skip(String ruleId) {
            List<String> skipped = new ArrayList<String>(skippedRuleIds);
            skipped.add(ruleId);
            return new MutableState(appliedRuleIds, skipped, "SKIP:" + ruleId);
        }

        private RuleSearchState toImmutable(String stateId, int beamRank, boolean selected) {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("searchAlgorithm", "BEAM_SEARCH");
            attributes.put("beamWidth", Integer.valueOf(BEAM_WIDTH));
            attributes.put("objectiveCostRounded", Double.valueOf(objectiveCost));
            attributes.put("selectionBoundary", "RANKING_ONLY_NO_AUTO_APPLY");
            List<Map<String, Object>> evidence = new ArrayList<Map<String, Object>>();
            LinkedHashMap<String, Object> transitionEvidence = new LinkedHashMap<String, Object>();
            transitionEvidence.put("type", "RULE_STATE_TRANSITION");
            transitionEvidence.put("transition", transition.toUpperCase(Locale.ROOT));
            transitionEvidence.put("appliedRuleIds", new ArrayList<String>(appliedRuleIds));
            transitionEvidence.put("skippedRuleIds", new ArrayList<String>(skippedRuleIds));
            evidence.add(transitionEvidence);
            return new RuleSearchState(
                stateId,
                appliedRuleIds,
                skippedRuleIds,
                transition,
                objectiveCost,
                beamRank,
                selected,
                evidence,
                attributes
            );
        }
    }
}
