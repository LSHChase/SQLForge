package com.company.sqloptimization.domain.rewrite.ra;

import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockHashGroup;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelationalRewritePlanBuilder {

    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern DATE_LITERAL_PATTERN =
        Pattern.compile("(?i)\\b(DATE|TIME|TIMESTAMP)\\s*'(?:''|[^'])*'");
    private static final Pattern NUMBER_LITERAL_PATTERN =
        Pattern.compile("(?<![A-Z0-9_$.])-?\\b\\d+(?:\\.\\d+)?\\b(?![A-Z0-9_$])", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGGREGATE_PATTERN =
        Pattern.compile("(?i)\\b(COUNT|SUM|AVG|MIN|MAX|APPROX_DISTINCT|GROUP_CONCAT|STRING_AGG|LISTAGG)\\s*\\(");
    private static final Pattern AVG_PATTERN = Pattern.compile("(?i)\\bAVG\\s*\\(");
    private static final Pattern COUNT_DISTINCT_PATTERN = Pattern.compile("(?i)\\bCOUNT\\s*\\(\\s*DISTINCT\\b|COUNT_DISTINCT\\s*\\(");
    private static final Pattern JOIN_KEY_PATTERN =
        Pattern.compile("(?i)\\bON\\s+([A-Z0-9_.$`\"]+)\\s*=\\s*([A-Z0-9_.$`\"]+)");

    public RelationalRewritePlan build(QueryBlockDag dag) {
        if (dag == null || dag.getBlocks().isEmpty()) {
            return emptyPlan("QBDAG_UNAVAILABLE");
        }
        List<RelationalRewriteCandidate> candidates = new ArrayList<RelationalRewriteCandidate>();
        int[] sequence = new int[] {1};
        candidates.addAll(detectCseCandidates(dag, sequence));
        candidates.addAll(detectVerticalFoldingCandidates(dag, sequence));
        candidates.addAll(detectHorizontalUnnestingCandidates(dag, sequence));

        List<String> unappliedRules = new ArrayList<String>();
        addUnappliedIfMissing(candidates, unappliedRules, RelationalRewriteRuleType.CSE_ELIMINATION);
        addUnappliedIfMissing(candidates, unappliedRules, RelationalRewriteRuleType.VERTICAL_FOLDING);
        addUnappliedIfMissing(candidates, unappliedRules, RelationalRewriteRuleType.HORIZONTAL_UNNESTING);

        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "QBDAG_L4_STATIC_ANALYSIS");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("candidateCount", Integer.valueOf(candidates.size()));
        attributes.put("candidateRules", candidateRules(candidates));
        attributes.put("rewriteStatus", candidates.isEmpty() ? "NO_STATIC_CANDIDATE" : "CANDIDATE_GENERATED");
        return new RelationalRewritePlan(
            RelationalRewritePlan.SCHEMA_VERSION,
            dag.getSchemaVersion(),
            candidates,
            unappliedRules,
            attributes
        );
    }

    private RelationalRewritePlan emptyPlan(String reason) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "QBDAG_L4_STATIC_ANALYSIS");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("candidateCount", Integer.valueOf(0));
        attributes.put("candidateRules", Collections.emptyList());
        attributes.put("rewriteStatus", "UNAVAILABLE");
        attributes.put("reason", reason);
        return new RelationalRewritePlan(
            RelationalRewritePlan.SCHEMA_VERSION,
            "",
            Collections.<RelationalRewriteCandidate>emptyList(),
            Arrays.asList(
                RelationalRewriteRuleType.CSE_ELIMINATION.name(),
                RelationalRewriteRuleType.VERTICAL_FOLDING.name(),
                RelationalRewriteRuleType.HORIZONTAL_UNNESTING.name()
            ),
            attributes
        );
    }

    private List<RelationalRewriteCandidate> detectCseCandidates(QueryBlockDag dag, int[] sequence) {
        List<RelationalRewriteCandidate> result = new ArrayList<RelationalRewriteCandidate>();
        for (QueryBlockHashGroup group : dag.getDuplicateStructuralGroups()) {
            List<QueryBlockNode> blocks = blocksForGroup(dag, group);
            if (blocks.size() < 2 || !outputsEquivalent(blocks)) {
                continue;
            }
            QueryBlockNode primary = broadestPredicateBlock(blocks);
            List<String> blockIds = blockIds(blocks);
            List<String> compensationPredicates = compensationPredicates(primary, blocks);
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("structuralHash", group.getStructuralHash());
            attributes.put("collisionStatus", group.getCollisionStatus());
            attributes.put("predicateRelation", predicateRelation(primary, blocks));
            attributes.put("outputColumnEquivalence", "POSITIONAL_COLUMN_MAPPING");
            result.add(new RelationalRewriteCandidate(
                candidateId(sequence),
                RelationalRewriteRuleType.CSE_ELIMINATION,
                primary.getBlockId(),
                blockIds,
                "CSE(master=" + primary.getBlockId() + ", replace=" + nonPrimaryIds(primary, blocks)
                    + ", pushCompensationPredicatesAtReference=true)",
                compensationPredicates,
                Arrays.asList(
                    "STRUCTURAL_HASH_MATCH_REQUIRED",
                    "OUTPUT_COLUMNS_POSITIONALLY_EQUIVALENT",
                    "PREDICATE_COMPENSATION_REQUIRED_WHEN_LITERAL_OR_RANGE_DIFFERS"
                ),
                Arrays.asList(
                    "STATIC_EQUIVALENCE_ONLY",
                    "SEMANTIC_EQUIVALENCE_2_3_2_REQUIRED_BEFORE_AUTO_MERGE"
                ),
                evidence(group, blocks),
                scanReductionBenefit(blocks.size(), "重复查询块可保留主副本，其余引用点增加补偿谓词。"),
                false,
                true,
                attributes
            ));
        }
        return result;
    }

    private List<RelationalRewriteCandidate> detectVerticalFoldingCandidates(QueryBlockDag dag, int[] sequence) {
        List<RelationalRewriteCandidate> result = new ArrayList<RelationalRewriteCandidate>();
        for (QueryBlockHashGroup group : dag.getDuplicateStructuralGroups()) {
            List<QueryBlockNode> blocks = aggregateBlocks(blocksForGroup(dag, group));
            if (blocks.size() < 2 || !sameGroupBy(blocks) || !outputsEquivalent(blocks)) {
                continue;
            }
            QueryBlockNode primary = broadestPredicateBlock(blocks);
            List<String> metrics = metricForms(blocks);
            List<String> preconditions = new ArrayList<String>();
            preconditions.add("AGGREGATION_DECOMPOSABILITY_REQUIRED");
            preconditions.add("GROUP_BY_COLUMNS_EQUIVALENT");
            preconditions.add("CASE_PREDICATE_OVERLAP_POLICY_REQUIRED");
            if (containsCountDistinct(blocks)) {
                preconditions.add("COUNT_DISTINCT_ARGUMENT_EQUIVALENCE_REQUIRED");
            }
            List<String> risks = new ArrayList<String>();
            risks.add("STATIC_EQUIVALENCE_ONLY");
            risks.add("OVERLAPPING_PREDICATES_MUST_REMAIN_INSIDE_CASE_EXPRESSION");
            if (containsAvg(blocks)) {
                risks.add("AVG_REQUIRES_SUM_COUNT_DECOMPOSITION");
            }
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("structuralHash", group.getStructuralHash());
            attributes.put("foldingMode", "CASE_AGGREGATION");
            attributes.put("predicateOverlapPolicy", "CONTROLLED_BY_CASE_WHEN");
            result.add(new RelationalRewriteCandidate(
                candidateId(sequence),
                RelationalRewriteRuleType.VERTICAL_FOLDING,
                primary.getBlockId(),
                blockIds(blocks),
                "VERTICAL_FOLD(source=" + primary.getFromClause() + ", groupBy=" + primary.getGroupBy()
                    + ", metrics=" + metrics + ")",
                compensationPredicates(primary, blocks),
                preconditions,
                risks,
                evidence(group, blocks),
                scanReductionBenefit(blocks.size(), "多指标聚合可合并为单个 GROUP BY，通过 CASE WHEN 区分日期、分层或 AUM 条件。"),
                false,
                true,
                attributes
            ));
        }
        return result;
    }

    private List<RelationalRewriteCandidate> detectHorizontalUnnestingCandidates(QueryBlockDag dag, int[] sequence) {
        List<RelationalRewriteCandidate> result = new ArrayList<RelationalRewriteCandidate>();
        Map<String, List<QueryBlockNode>> childrenByParent = childrenByParent(dag.getBlocks());
        for (Map.Entry<String, List<QueryBlockNode>> entry : childrenByParent.entrySet()) {
            QueryBlockNode parent = dag.getBlock(entry.getKey());
            if (parent == null || !parent.getFromClause().toUpperCase(Locale.ROOT).contains("LEFT JOIN")) {
                continue;
            }
            List<QueryBlockNode> children = aggregateBlocks(entry.getValue());
            if (children.size() < 2 || !sameGroupBy(children) || hasExternalReferences(children)) {
                continue;
            }
            String joinKey = commonJoinKey(parent.getFromClause());
            if (joinKey.isEmpty()) {
                continue;
            }
            QueryBlockNode primary = broadestPredicateBlock(children);
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("parentBlockId", parent.getBlockId());
            attributes.put("joinKeySignature", joinKey);
            attributes.put("unnestingMode", "AGGREGATION_PUSHDOWN_GROUP_BY_EXTENSION");
            result.add(new RelationalRewriteCandidate(
                candidateId(sequence),
                RelationalRewriteRuleType.HORIZONTAL_UNNESTING,
                primary.getBlockId(),
                blockIds(children),
                "HORIZONTAL_UNNEST(parent=" + parent.getBlockId() + ", joinKey=" + joinKey
                    + ", groupBy=" + primary.getGroupBy() + ", metrics=" + metricForms(children) + ")",
                compensationPredicates(primary, children),
                Arrays.asList(
                    "LEFT_JOIN_AGGREGATE_CHILDREN_REQUIRED",
                    "JOIN_KEY_EQUIVALENCE_REQUIRED",
                    "CHILDREN_MUST_NOT_DEPEND_ON_EACH_OTHER"
                ),
                Arrays.asList(
                    "STATIC_EQUIVALENCE_ONLY",
                    "KEEP_LATERAL_JOIN_IF_SOURCE_STRUCTURE_DIVERGES"
                ),
                evidence(null, children),
                scanReductionBenefit(children.size(), "多个 LEFT JOIN 聚合右表可下推合并为单个扩展 GROUP BY。"),
                false,
                true,
                attributes
            ));
        }
        return result;
    }

    private List<QueryBlockNode> blocksForGroup(QueryBlockDag dag, QueryBlockHashGroup group) {
        List<QueryBlockNode> result = new ArrayList<QueryBlockNode>();
        for (String blockId : group.getBlockIds()) {
            QueryBlockNode block = dag.getBlock(blockId);
            if (block != null) {
                result.add(block);
            }
        }
        return result;
    }

    private List<QueryBlockNode> aggregateBlocks(List<QueryBlockNode> blocks) {
        List<QueryBlockNode> result = new ArrayList<QueryBlockNode>();
        for (QueryBlockNode block : blocks) {
            if (isAggregateBlock(block)) {
                result.add(block);
            }
        }
        return result;
    }

    private boolean isAggregateBlock(QueryBlockNode block) {
        if (block == null) {
            return false;
        }
        if (!block.getGroupBy().isEmpty()) {
            return true;
        }
        for (String item : block.getSelectList()) {
            if (AGGREGATE_PATTERN.matcher(item).find()) {
                return true;
            }
        }
        return AGGREGATE_PATTERN.matcher(block.getNormalizedRelationalForm()).find();
    }

    private boolean outputsEquivalent(List<QueryBlockNode> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        int size = blocks.get(0).getOutputColumns().size();
        if (size == 0) {
            return false;
        }
        for (QueryBlockNode block : blocks) {
            if (block.getOutputColumns().size() != size) {
                return false;
            }
        }
        return true;
    }

    private boolean sameGroupBy(List<QueryBlockNode> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        String signature = groupBySignature(blocks.get(0));
        for (QueryBlockNode block : blocks) {
            if (!signature.equals(groupBySignature(block))) {
                return false;
            }
        }
        return true;
    }

    private String groupBySignature(QueryBlockNode block) {
        List<String> values = new ArrayList<String>();
        for (String item : block.getGroupBy()) {
            values.add(normalizeExpressionText(item));
        }
        Collections.sort(values);
        return values.toString();
    }

    private QueryBlockNode broadestPredicateBlock(List<QueryBlockNode> blocks) {
        List<QueryBlockNode> sorted = new ArrayList<QueryBlockNode>(blocks);
        Collections.sort(sorted, new Comparator<QueryBlockNode>() {
            @Override
            public int compare(QueryBlockNode left, QueryBlockNode right) {
                int leftCount = normalizedPredicateAtoms(left.getWhereClause()).size();
                int rightCount = normalizedPredicateAtoms(right.getWhereClause()).size();
                if (leftCount != rightCount) {
                    return leftCount - rightCount;
                }
                return left.getBlockId().compareTo(right.getBlockId());
            }
        });
        return sorted.get(0);
    }

    private String predicateRelation(QueryBlockNode primary, List<QueryBlockNode> blocks) {
        Set<String> primaryAtoms = normalizedPredicateAtoms(primary.getWhereClause());
        for (QueryBlockNode block : blocks) {
            Set<String> atoms = normalizedPredicateAtoms(block.getWhereClause());
            if (primaryAtoms.containsAll(atoms) && !atoms.containsAll(primaryAtoms)) {
                return "PRIMARY_PREDICATE_NARROWER_THAN_PEER";
            }
            if (atoms.containsAll(primaryAtoms) && !primaryAtoms.containsAll(atoms)) {
                return "PRIMARY_PREDICATE_BROADER_THAN_PEER";
            }
        }
        return "SAME_SHAPE_OR_LITERAL_DIFFERENCE";
    }

    private List<String> compensationPredicates(QueryBlockNode primary, List<QueryBlockNode> blocks) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        Set<String> primaryRaw = rawPredicateAtoms(primary.getWhereClause());
        for (QueryBlockNode block : blocks) {
            if (block.getBlockId().equals(primary.getBlockId())) {
                continue;
            }
            for (String atom : rawPredicateAtoms(block.getWhereClause())) {
                if (!primaryRaw.contains(atom)) {
                    result.add(block.getBlockId() + ": " + atom);
                }
            }
        }
        return new ArrayList<String>(result);
    }

    private Set<String> normalizedPredicateAtoms(String predicate) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String atom : rawPredicateAtoms(predicate)) {
            result.add(normalizeExpressionText(atom));
        }
        return result;
    }

    private Set<String> rawPredicateAtoms(String predicate) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        String text = RaRewriteCollections.text(predicate);
        if (text.isEmpty()) {
            return result;
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        String[] parts = normalized.split("(?i)\\s+AND\\s+");
        for (String part : parts) {
            String atom = part.trim();
            while (atom.startsWith("(") && atom.endsWith(")") && atom.length() > 2) {
                atom = atom.substring(1, atom.length() - 1).trim();
            }
            if (!atom.isEmpty()) {
                result.add(atom);
            }
        }
        return result;
    }

    private String normalizeExpressionText(String text) {
        String normalized = RaRewriteCollections.text(text)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replaceAll("\\s+", " ");
        normalized = DATE_LITERAL_PATTERN.matcher(normalized).replaceAll("<DATE>");
        normalized = STRING_LITERAL_PATTERN.matcher(normalized).replaceAll("<STRING>");
        normalized = NUMBER_LITERAL_PATTERN.matcher(normalized).replaceAll("<NUMBER>");
        return normalized.replace("\"", "").replace("`", "").trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsCountDistinct(List<QueryBlockNode> blocks) {
        for (QueryBlockNode block : blocks) {
            for (String item : block.getSelectList()) {
                if (COUNT_DISTINCT_PATTERN.matcher(item).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAvg(List<QueryBlockNode> blocks) {
        for (QueryBlockNode block : blocks) {
            for (String item : block.getSelectList()) {
                if (AVG_PATTERN.matcher(item).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasExternalReferences(List<QueryBlockNode> blocks) {
        for (QueryBlockNode block : blocks) {
            if (!block.getExternalReferences().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<QueryBlockNode>> childrenByParent(List<QueryBlockNode> blocks) {
        LinkedHashMap<String, List<QueryBlockNode>> result = new LinkedHashMap<String, List<QueryBlockNode>>();
        for (QueryBlockNode block : blocks) {
            String parent = block.getParentBlockId();
            if (parent == null || parent.isEmpty()) {
                continue;
            }
            List<QueryBlockNode> children = result.get(parent);
            if (children == null) {
                children = new ArrayList<QueryBlockNode>();
                result.put(parent, children);
            }
            children.add(block);
        }
        return result;
    }

    private String commonJoinKey(String fromClause) {
        Matcher matcher = JOIN_KEY_PATTERN.matcher(fromClause == null ? "" : fromClause.toUpperCase(Locale.ROOT));
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        while (matcher.find()) {
            keys.add(columnTail(matcher.group(1)) + "=" + columnTail(matcher.group(2)));
        }
        if (keys.isEmpty()) {
            return "";
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String key : keys) {
            String[] parts = key.split("=");
            if (parts.length == 2 && parts[0].equals(parts[1])) {
                normalized.add(parts[0] + "=" + parts[1]);
            } else {
                normalized.add(key);
            }
        }
        return normalized.size() == 1 ? normalized.iterator().next() : "";
    }

    private String columnTail(String value) {
        String clean = value == null ? "" : value.replace("\"", "").replace("`", "").trim();
        int dot = clean.lastIndexOf('.');
        return dot >= 0 ? clean.substring(dot + 1) : clean;
    }

    private List<String> metricForms(List<QueryBlockNode> blocks) {
        List<String> result = new ArrayList<String>();
        for (QueryBlockNode block : blocks) {
            List<String> metrics = new ArrayList<String>();
            for (String item : block.getSelectList()) {
                if (AGGREGATE_PATTERN.matcher(item).find()) {
                    metrics.add(item);
                }
            }
            if (metrics.isEmpty() && !block.getSelectList().isEmpty()) {
                metrics.add(block.getSelectList().get(block.getSelectList().size() - 1));
            }
            result.add(block.getBlockId() + " -> " + metrics + " WHEN " + block.getWhereClause());
        }
        return result;
    }

    private List<Map<String, Object>> evidence(QueryBlockHashGroup group, List<QueryBlockNode> blocks) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (group != null) {
            LinkedHashMap<String, Object> hashEvidence = new LinkedHashMap<String, Object>();
            hashEvidence.put("type", "STRUCTURAL_HASH_GROUP");
            hashEvidence.put("structuralHash", group.getStructuralHash());
            hashEvidence.put("collisionStatus", group.getCollisionStatus());
            hashEvidence.put("blockIds", group.getBlockIds());
            result.add(hashEvidence);
        }
        for (QueryBlockNode block : blocks) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("type", "QUERY_BLOCK");
            item.put("blockId", block.getBlockId());
            item.put("fromClause", block.getFromClause());
            item.put("whereClause", block.getWhereClause());
            item.put("groupBy", block.getGroupBy());
            item.put("outputColumns", block.getOutputColumns());
            item.put("structuralHash", block.getStructuralHash());
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> scanReductionBenefit(int sourceBlockCount, String explanation) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("claimBoundary", "STATIC_ESTIMATE_NOT_REAL_EXECUTION_GAIN");
        result.put("sourceBlockCount", Integer.valueOf(sourceBlockCount));
        result.put("estimatedScanReductionCount", Integer.valueOf(Math.max(0, sourceBlockCount - 1)));
        result.put("explanation", explanation);
        return result;
    }

    private List<String> blockIds(List<QueryBlockNode> blocks) {
        List<String> result = new ArrayList<String>();
        for (QueryBlockNode block : blocks) {
            result.add(block.getBlockId());
        }
        return result;
    }

    private List<String> nonPrimaryIds(QueryBlockNode primary, List<QueryBlockNode> blocks) {
        List<String> result = new ArrayList<String>();
        for (QueryBlockNode block : blocks) {
            if (!block.getBlockId().equals(primary.getBlockId())) {
                result.add(block.getBlockId());
            }
        }
        return result;
    }

    private String candidateId(int[] sequence) {
        String value = "RAR-" + sequence[0];
        sequence[0]++;
        return value;
    }

    private void addUnappliedIfMissing(List<RelationalRewriteCandidate> candidates,
                                       List<String> unappliedRules,
                                       RelationalRewriteRuleType ruleType) {
        for (RelationalRewriteCandidate candidate : candidates) {
            if (ruleType == candidate.getRuleType()) {
                return;
            }
        }
        unappliedRules.add(ruleType.name());
    }

    private List<String> candidateRules(List<RelationalRewriteCandidate> candidates) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (RelationalRewriteCandidate candidate : candidates) {
            result.add(candidate.getRuleType().name());
        }
        return new ArrayList<String>(result);
    }
}
