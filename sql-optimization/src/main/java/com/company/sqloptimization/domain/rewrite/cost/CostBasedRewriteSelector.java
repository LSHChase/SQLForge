package com.company.sqloptimization.domain.rewrite.cost;

import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheck;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CostBasedRewriteSelector {

    private static final Pattern AGGREGATE_PATTERN =
        Pattern.compile("(?i)\\b(COUNT|SUM|AVG|MIN|MAX|APPROX_DISTINCT|COUNT_DISTINCT)\\s*\\(");
    private static final Pattern DISTINCT_PATTERN = Pattern.compile("(?i)\\bDISTINCT\\b");
    private static final Pattern JOIN_PATTERN = Pattern.compile("(?i)\\bJOIN\\b");
    private static final Pattern UNION_PATTERN = Pattern.compile("(?i)\\bUNION\\b");

    public CostBasedRewriteSelectionReport select(QueryBlockDag dag,
                                                  RelationalRewritePlan plan,
                                                  SemanticEquivalenceReport semanticReport) {
        return select(dag, plan, semanticReport, CostSelectionStrategy.DEFAULT_WEIGHTED);
    }

    public CostBasedRewriteSelectionReport select(QueryBlockDag dag,
                                                  RelationalRewritePlan plan,
                                                  SemanticEquivalenceReport semanticReport,
                                                  CostSelectionStrategy strategy) {
        CostSelectionStrategy actualStrategy = strategy == null ? CostSelectionStrategy.DEFAULT_WEIGHTED : strategy;
        if (plan == null || plan.getCandidates().isEmpty()) {
            return emptyReport(plan == null ? "" : plan.getSchemaVersion(), actualStrategy, "NO_REWRITE_CANDIDATE");
        }
        LinkedHashMap<String, Object> weights = weights(actualStrategy);
        List<MutableEstimate> mutableEstimates = new ArrayList<MutableEstimate>();
        int[] sequence = new int[] {1};
        for (RelationalRewriteCandidate candidate : plan.getCandidates()) {
            mutableEstimates.add(estimate(sequence, candidate, dag, semanticReport, weights));
        }
        markParetoFrontier(mutableEstimates);
        sortAndRank(mutableEstimates, actualStrategy);
        RewriteCostEstimate selected = selectedEstimate(mutableEstimates);
        List<RewriteCostEstimate> estimates = immutableEstimates(mutableEstimates, selected);
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("estimateCount", Integer.valueOf(estimates.size()));
        attributes.put("paretoFrontierCount", Integer.valueOf(paretoFrontierIds(mutableEstimates).size()));
        attributes.put("selectionStatus", selectionStatus(selected));
        attributes.put("selectedCandidateId", selected == null ? "" : selected.getCandidateId());
        attributes.put("selectionBoundary", "STATIC_ABSTRACT_COST_NOT_REAL_EXECUTION_PLAN");
        return new CostBasedRewriteSelectionReport(
            CostBasedRewriteSelectionReport.SCHEMA_VERSION,
            plan.getSchemaVersion(),
            actualStrategy,
            selectionStatus(selected),
            selected == null ? "" : selected.getCandidateId(),
            paretoFrontierIds(mutableEstimates),
            estimates,
            weights,
            attributes
        );
    }

    private CostBasedRewriteSelectionReport emptyReport(String sourceSchemaVersion,
                                                       CostSelectionStrategy strategy,
                                                       String reason) {
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("estimateCount", Integer.valueOf(0));
        attributes.put("paretoFrontierCount", Integer.valueOf(0));
        attributes.put("selectionStatus", "NO_CANDIDATE");
        attributes.put("selectedCandidateId", "");
        attributes.put("reason", reason);
        return new CostBasedRewriteSelectionReport(
            CostBasedRewriteSelectionReport.SCHEMA_VERSION,
            sourceSchemaVersion,
            strategy,
            "NO_CANDIDATE",
            "",
            Collections.<String>emptyList(),
            Collections.<RewriteCostEstimate>emptyList(),
            weights(strategy),
            attributes
        );
    }

    private MutableEstimate estimate(int[] sequence,
                                     RelationalRewriteCandidate candidate,
                                     QueryBlockDag dag,
                                     SemanticEquivalenceReport semanticReport,
                                     Map<String, Object> weights) {
        List<QueryBlockNode> blocks = sourceBlocks(dag, candidate);
        CostFeatures features = features(candidate, blocks);
        List<String> hetuAdjustments = hetuAdjustments(candidate, features);
        RewriteCostVector costVector = costVector(candidate, features, weights, hetuAdjustments);
        String semanticGate = semanticGate(candidate, semanticReport);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("sourceBlockCount", Integer.valueOf(features.sourceBlockCount));
        attributes.put("aggregateCount", Integer.valueOf(features.aggregateCount));
        attributes.put("joinCount", Integer.valueOf(features.joinCount));
        attributes.put("predicateAtomCount", Integer.valueOf(features.predicateAtomCount));
        attributes.put("projectionCount", Integer.valueOf(features.projectionCount));
        attributes.put("estimatedIntermediateRows", Double.valueOf(features.intermediateRows));
        attributes.put("estimatedRowWidth", Double.valueOf(features.rowWidth));
        attributes.put("semanticGate", semanticGate);
        attributes.put("costBoundary", "ABSTRACT_STATIC_UNITS");
        return new MutableEstimate(
            "COST-" + next(sequence),
            candidate,
            costVector,
            semanticGate,
            hetuAdjustments,
            selectionReasons(candidate, features, hetuAdjustments, semanticGate),
            evidence(candidate, blocks, features),
            attributes
        );
    }

    private RewriteCostVector costVector(RelationalRewriteCandidate candidate,
                                         CostFeatures features,
                                         Map<String, Object> weights,
                                         List<String> hetuAdjustments) {
        double scanCost = scanCost(candidate, features);
        double shuffleCost = shuffleCost(candidate, features);
        double computeCost = computeCost(candidate, features);
        double memoryCost = memoryCost(candidate, features);
        if (hetuAdjustments.contains("HETU_DYNAMIC_FILTER_PUSHDOWN_PREFERRED")) {
            scanCost = scanCost * 0.88;
        }
        if (hetuAdjustments.contains("HETU_SHUFFLE_REDUCTION_PRIORITY")) {
            shuffleCost = shuffleCost * 0.75;
        }
        if (hetuAdjustments.contains("CTE_MATERIALIZATION_RECOMMENDED_FOR_REUSE")) {
            computeCost = computeCost * 0.85;
            memoryCost = memoryCost * 1.12;
        }
        double weightedCost = doubleWeight(weights, "scanWeight") * scanCost
            + doubleWeight(weights, "shuffleWeight") * shuffleCost
            + doubleWeight(weights, "computeWeight") * computeCost
            + doubleWeight(weights, "memoryWeight") * memoryCost;
        return new RewriteCostVector(
            round(scanCost),
            round(shuffleCost),
            round(computeCost),
            round(memoryCost),
            round(weightedCost)
        );
    }

    private double scanCost(RelationalRewriteCandidate candidate, CostFeatures features) {
        double base = 1000.0 + features.projectionCount * 42.0 + features.predicateAtomCount * 35.0;
        if (RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()) {
            return base + Math.max(0, features.sourceBlockCount - 1) * 180.0;
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return base + features.aggregateCount * 95.0;
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return base + features.joinCount * 140.0;
        }
        return base * Math.max(1, features.sourceBlockCount);
    }

    private double shuffleCost(RelationalRewriteCandidate candidate, CostFeatures features) {
        double networkFactor = 0.18;
        double base = features.intermediateRows * features.rowWidth * networkFactor;
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return base * 0.62 + features.groupByColumnCount * 80.0;
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return base * 0.78 + features.aggregateCount * 50.0;
        }
        if (RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()) {
            return base * 0.92;
        }
        return base;
    }

    private double computeCost(RelationalRewriteCandidate candidate, CostFeatures features) {
        double operationComplexity = 1.0
            + features.aggregateCount * 0.42
            + features.distinctCount * 0.35
            + features.predicateAtomCount * 0.08
            + features.joinCount * 0.28;
        double base = operationComplexity * Math.max(1.0, features.intermediateRows);
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return base * 1.08;
        }
        if (RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()) {
            return base * 0.72;
        }
        return base;
    }

    private double memoryCost(RelationalRewriteCandidate candidate, CostFeatures features) {
        double hashTableSize = Math.max(1, features.groupByColumnCount + features.joinCount) * features.rowWidth * 18.0;
        double sortBufferSize = features.distinctCount * features.rowWidth * 12.0;
        double base = hashTableSize + sortBufferSize + features.aggregateCount * 60.0;
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return base * 1.18;
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return base * 1.08;
        }
        return base;
    }

    private CostFeatures features(RelationalRewriteCandidate candidate, List<QueryBlockNode> blocks) {
        CostFeatures features = new CostFeatures();
        features.sourceBlockCount = Math.max(1, blocks.size());
        features.projectionCount = Math.max(1, totalSelectItems(blocks));
        features.groupByColumnCount = totalGroupByItems(blocks);
        features.aggregateCount = aggregateCount(blocks);
        features.distinctCount = distinctCount(blocks);
        features.joinCount = joinCount(blocks);
        features.unionCount = unionCount(blocks);
        features.predicateAtomCount = Math.max(
            1,
            predicateAtomCount(blocks) + candidate.getCompensationPredicates().size()
        );
        features.rowWidth = 24.0 + features.projectionCount * 10.0 + features.groupByColumnCount * 8.0;
        features.intermediateRows = 1000.0 * features.sourceBlockCount
            * (features.joinCount + 1)
            * (features.unionCount + 1)
            / Math.max(1.0, features.predicateAtomCount * 0.7);
        return features;
    }

    private List<String> hetuAdjustments(RelationalRewriteCandidate candidate, CostFeatures features) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.add("ABSTRACT_HETU_PRESTO_ENGINE_DECOUPLED");
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            result.add("HETU_SHUFFLE_REDUCTION_PRIORITY");
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()
            || RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()) {
            result.add("CTE_MATERIALIZATION_RECOMMENDED_FOR_REUSE");
        }
        if (!candidate.getCompensationPredicates().isEmpty() || features.predicateAtomCount > 1) {
            result.add("HETU_DYNAMIC_FILTER_PUSHDOWN_PREFERRED");
        }
        return new ArrayList<String>(result);
    }

    private List<String> selectionReasons(RelationalRewriteCandidate candidate,
                                          CostFeatures features,
                                          List<String> hetuAdjustments,
                                          String semanticGate) {
        List<String> reasons = new ArrayList<String>();
        reasons.add("ABSTRACT_COST_VECTOR_COMPUTED");
        reasons.add("SEMANTIC_GATE_" + semanticGate);
        if (candidate.getSourceBlockIds().size() > 1) {
            reasons.add("REPEATED_SOURCE_BLOCKS_CAN_REDUCE_SCAN_OR_COMPUTE");
        }
        if (features.joinCount > 0) {
            reasons.add("JOIN_SHUFFLE_COST_CONSIDERED");
        }
        reasons.addAll(hetuAdjustments);
        return reasons;
    }

    private List<QueryBlockNode> sourceBlocks(QueryBlockDag dag, RelationalRewriteCandidate candidate) {
        if (dag == null || candidate == null) {
            return Collections.emptyList();
        }
        List<QueryBlockNode> result = new ArrayList<QueryBlockNode>();
        for (String blockId : candidate.getSourceBlockIds()) {
            QueryBlockNode block = dag.getBlock(blockId);
            if (block != null) {
                result.add(block);
            }
        }
        return result;
    }

    private void markParetoFrontier(List<MutableEstimate> estimates) {
        for (MutableEstimate estimate : estimates) {
            boolean dominated = false;
            for (MutableEstimate other : estimates) {
                if (estimate != other && other.costVector.dominates(estimate.costVector)) {
                    dominated = true;
                    break;
                }
            }
            estimate.paretoOptimal = !dominated;
        }
    }

    private void sortAndRank(List<MutableEstimate> estimates, final CostSelectionStrategy strategy) {
        Collections.sort(estimates, new Comparator<MutableEstimate>() {
            @Override
            public int compare(MutableEstimate left, MutableEstimate right) {
                int frontier = Boolean.valueOf(!left.paretoOptimal).compareTo(Boolean.valueOf(!right.paretoOptimal));
                if (frontier != 0) {
                    return frontier;
                }
                int metric = Double.compare(
                    policyCost(left.costVector, strategy),
                    policyCost(right.costVector, strategy)
                );
                if (metric != 0) {
                    return metric;
                }
                return left.candidate.getCandidateId().compareTo(right.candidate.getCandidateId());
            }
        });
        int rank = 1;
        for (MutableEstimate estimate : estimates) {
            estimate.rank = rank;
            rank++;
        }
    }

    private RewriteCostEstimate selectedEstimate(List<MutableEstimate> estimates) {
        for (MutableEstimate estimate : estimates) {
            if (estimate.paretoOptimal) {
                return estimate.toImmutable(true);
            }
        }
        return estimates.isEmpty() ? null : estimates.get(0).toImmutable(true);
    }

    private List<RewriteCostEstimate> immutableEstimates(List<MutableEstimate> estimates,
                                                         RewriteCostEstimate selected) {
        List<RewriteCostEstimate> result = new ArrayList<RewriteCostEstimate>();
        String selectedCandidateId = selected == null ? "" : selected.getCandidateId();
        for (MutableEstimate estimate : estimates) {
            result.add(estimate.toImmutable(estimate.candidate.getCandidateId().equals(selectedCandidateId)));
        }
        return result;
    }

    private List<String> paretoFrontierIds(List<MutableEstimate> estimates) {
        List<String> result = new ArrayList<String>();
        for (MutableEstimate estimate : estimates) {
            if (estimate.paretoOptimal) {
                result.add(estimate.candidate.getCandidateId());
            }
        }
        return result;
    }

    private double policyCost(RewriteCostVector costVector, CostSelectionStrategy strategy) {
        if (CostSelectionStrategy.TIMEOUT_SENSITIVE == strategy) {
            return costVector.getScanCost();
        }
        if (CostSelectionStrategy.MEMORY_CONSTRAINED == strategy) {
            return costVector.getMemoryCost();
        }
        return costVector.getWeightedCost();
    }

    private String selectionStatus(RewriteCostEstimate selected) {
        if (selected == null) {
            return "NO_CANDIDATE";
        }
        if ("PROVED".equals(selected.getSemanticGate())) {
            return "SELECTED_PROVED_CANDIDATE";
        }
        return "RANKED_WITH_SEMANTIC_GATES";
    }

    private String semanticGate(RelationalRewriteCandidate candidate, SemanticEquivalenceReport semanticReport) {
        if (semanticReport == null || semanticReport.getChecks().isEmpty()) {
            return "NOT_VERIFIED";
        }
        SemanticEquivalenceStatus strongest = null;
        for (SemanticEquivalenceCheck check : semanticReport.getChecks()) {
            if (candidate.getCandidateId().equals(check.getCandidateId())) {
                strongest = stronger(strongest, check.getStatus());
            }
        }
        return strongest == null ? "NOT_VERIFIED" : strongest.name();
    }

    private SemanticEquivalenceStatus stronger(SemanticEquivalenceStatus left, SemanticEquivalenceStatus right) {
        if (left == null) {
            return right;
        }
        return statusRank(right) < statusRank(left) ? right : left;
    }

    private int statusRank(SemanticEquivalenceStatus status) {
        if (SemanticEquivalenceStatus.PROVED == status) {
            return 1;
        }
        if (SemanticEquivalenceStatus.CONDITIONALLY_PROVED == status) {
            return 2;
        }
        if (SemanticEquivalenceStatus.NEEDS_CONSTRAINTS == status) {
            return 3;
        }
        if (SemanticEquivalenceStatus.UNSUPPORTED == status) {
            return 4;
        }
        return 5;
    }

    private List<Map<String, Object>> evidence(RelationalRewriteCandidate candidate,
                                               List<QueryBlockNode> blocks,
                                               CostFeatures features) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Object> featureEvidence = new LinkedHashMap<String, Object>();
        featureEvidence.put("type", "ABSTRACT_COST_FEATURES");
        featureEvidence.put("candidateId", candidate.getCandidateId());
        featureEvidence.put("ruleType", candidate.getRuleType().name());
        featureEvidence.put("sourceBlockCount", Integer.valueOf(features.sourceBlockCount));
        featureEvidence.put("predicateAtomCount", Integer.valueOf(features.predicateAtomCount));
        featureEvidence.put("aggregateCount", Integer.valueOf(features.aggregateCount));
        featureEvidence.put("joinCount", Integer.valueOf(features.joinCount));
        featureEvidence.put("groupByColumnCount", Integer.valueOf(features.groupByColumnCount));
        result.add(featureEvidence);
        for (QueryBlockNode block : blocks) {
            LinkedHashMap<String, Object> blockEvidence = new LinkedHashMap<String, Object>();
            blockEvidence.put("type", "QUERY_BLOCK_COST_INPUT");
            blockEvidence.put("blockId", block.getBlockId());
            blockEvidence.put("fromClause", block.getFromClause());
            blockEvidence.put("whereClause", block.getWhereClause());
            blockEvidence.put("selectList", block.getSelectList());
            blockEvidence.put("groupBy", block.getGroupBy());
            result.add(blockEvidence);
        }
        return result;
    }

    private LinkedHashMap<String, Object> weights(CostSelectionStrategy strategy) {
        LinkedHashMap<String, Object> weights = new LinkedHashMap<String, Object>();
        if (CostSelectionStrategy.TIMEOUT_SENSITIVE == strategy) {
            weights.put("scanWeight", Double.valueOf(0.50));
            weights.put("shuffleWeight", Double.valueOf(0.25));
            weights.put("computeWeight", Double.valueOf(0.15));
            weights.put("memoryWeight", Double.valueOf(0.10));
            return weights;
        }
        if (CostSelectionStrategy.MEMORY_CONSTRAINED == strategy) {
            weights.put("scanWeight", Double.valueOf(0.25));
            weights.put("shuffleWeight", Double.valueOf(0.25));
            weights.put("computeWeight", Double.valueOf(0.15));
            weights.put("memoryWeight", Double.valueOf(0.35));
            return weights;
        }
        weights.put("scanWeight", Double.valueOf(0.35));
        weights.put("shuffleWeight", Double.valueOf(0.30));
        weights.put("computeWeight", Double.valueOf(0.20));
        weights.put("memoryWeight", Double.valueOf(0.15));
        return weights;
    }

    private LinkedHashMap<String, Object> baseAttributes() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "RELATIONAL_REWRITE_PLAN_AND_SEMANTIC_REPORT");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("costModel", "ABSTRACT_HETU_PRESTO_DECOUPLED");
        attributes.put("scanCostFormula", "SUM(table_cardinality * column_selectivity)");
        attributes.put("shuffleCostFormula", "SUM(intermediate_rows * row_width * network_factor)");
        attributes.put("computeCostFormula", "SUM(operation_complexity * input_cardinality)");
        attributes.put("memoryCostFormula", "SUM(hash_table_size + sort_buffer_size)");
        attributes.put("selectionAlgorithm", "PARETO_FRONTIER_THEN_SLA_POLICY");
        attributes.put("availableStrategies", Arrays.asList(
            CostSelectionStrategy.DEFAULT_WEIGHTED.name(),
            CostSelectionStrategy.TIMEOUT_SENSITIVE.name(),
            CostSelectionStrategy.MEMORY_CONSTRAINED.name()
        ));
        return attributes;
    }

    private int totalSelectItems(List<QueryBlockNode> blocks) {
        int count = 0;
        for (QueryBlockNode block : blocks) {
            count += block.getSelectList().size();
        }
        return count;
    }

    private int totalGroupByItems(List<QueryBlockNode> blocks) {
        int count = 0;
        for (QueryBlockNode block : blocks) {
            count += block.getGroupBy().size();
        }
        return count;
    }

    private int aggregateCount(List<QueryBlockNode> blocks) {
        return patternCount(blocks, AGGREGATE_PATTERN);
    }

    private int distinctCount(List<QueryBlockNode> blocks) {
        return patternCount(blocks, DISTINCT_PATTERN);
    }

    private int joinCount(List<QueryBlockNode> blocks) {
        int count = patternCount(blocks, JOIN_PATTERN);
        return count == 0 ? inferredJoinCount(blocks) : count;
    }

    private int unionCount(List<QueryBlockNode> blocks) {
        return patternCount(blocks, UNION_PATTERN);
    }

    private int patternCount(List<QueryBlockNode> blocks, Pattern pattern) {
        int count = 0;
        for (QueryBlockNode block : blocks) {
            count += countMatches(pattern, block.getFromClause());
            count += countMatches(pattern, block.getWhereClause());
            count += countMatches(pattern, block.getNormalizedRelationalForm());
            for (String item : block.getSelectList()) {
                count += countMatches(pattern, item);
            }
        }
        return count;
    }

    private int inferredJoinCount(List<QueryBlockNode> blocks) {
        int count = 0;
        for (QueryBlockNode block : blocks) {
            if (block.getExternalReferences().size() > 0) {
                count++;
            }
        }
        return count;
    }

    private int predicateAtomCount(List<QueryBlockNode> blocks) {
        int count = 0;
        for (QueryBlockNode block : blocks) {
            String predicate = CostSelectionCollections.text(block.getWhereClause());
            if (predicate.isEmpty()) {
                continue;
            }
            count += Math.max(1, predicate.split("(?i)\\s+AND\\s+").length);
        }
        return count;
    }

    private int countMatches(Pattern pattern, String text) {
        java.util.regex.Matcher matcher = pattern.matcher(CostSelectionCollections.text(text));
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private double doubleWeight(Map<String, Object> weights, String key) {
        Object value = weights.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int next(int[] sequence) {
        int value = sequence[0];
        sequence[0]++;
        return value;
    }

    private static final class CostFeatures {
        private int sourceBlockCount;
        private int projectionCount;
        private int groupByColumnCount;
        private int aggregateCount;
        private int distinctCount;
        private int joinCount;
        private int unionCount;
        private int predicateAtomCount;
        private double rowWidth;
        private double intermediateRows;
    }

    private static final class MutableEstimate {
        private final String estimateId;
        private final RelationalRewriteCandidate candidate;
        private final RewriteCostVector costVector;
        private final String semanticGate;
        private final List<String> hetuAdjustments;
        private final List<String> selectionReasons;
        private final List<Map<String, Object>> evidence;
        private final Map<String, Object> attributes;
        private boolean paretoOptimal;
        private int rank;

        private MutableEstimate(String estimateId,
                                RelationalRewriteCandidate candidate,
                                RewriteCostVector costVector,
                                String semanticGate,
                                List<String> hetuAdjustments,
                                List<String> selectionReasons,
                                List<Map<String, Object>> evidence,
                                Map<String, Object> attributes) {
            this.estimateId = estimateId;
            this.candidate = candidate;
            this.costVector = costVector;
            this.semanticGate = semanticGate;
            this.hetuAdjustments = hetuAdjustments;
            this.selectionReasons = selectionReasons;
            this.evidence = evidence;
            this.attributes = attributes;
        }

        private RewriteCostEstimate toImmutable(boolean selected) {
            return new RewriteCostEstimate(
                estimateId,
                candidate.getCandidateId(),
                candidate.getRuleType(),
                costVector,
                paretoOptimal,
                selected,
                rank,
                semanticGate,
                hetuAdjustments,
                selectionReasons,
                evidence,
                attributes
            );
        }
    }
}
