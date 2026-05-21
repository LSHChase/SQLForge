package com.company.sqloptimization.domain.rewrite.semantic;

import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class SemanticEquivalenceVerifier {

    private static final Pattern NULL_TEXT_PATTERN =
        Pattern.compile("(?i)\\b(NULL|IS\\s+NULL|IS\\s+NOT\\s+NULL|NOT\\s+IN|<>|!=)\\b");
    private static final Pattern COUNT_DISTINCT_PATTERN =
        Pattern.compile("(?i)\\bCOUNT\\s*\\(\\s*DISTINCT\\b|COUNT_DISTINCT\\s*\\(");
    private static final Pattern AVG_PATTERN = Pattern.compile("(?i)\\bAVG\\s*\\(");
    private static final Pattern COUNT_OR_SUM_PATTERN = Pattern.compile("(?i)\\b(COUNT|SUM)\\s*\\(");
    private static final Pattern FALSE_PREDICATE_PATTERN =
        Pattern.compile("(?i)(^|\\s)(FALSE|1\\s*=\\s*0|0\\s*=\\s*1)($|\\s)");

    public SemanticEquivalenceReport verify(QueryBlockDag dag, RelationalRewritePlan plan) {
        if (plan == null || plan.getCandidates().isEmpty()) {
            return emptyReport(plan == null ? "" : plan.getSchemaVersion(), "NO_RELATIONAL_REWRITE_CANDIDATE");
        }
        List<SemanticEquivalenceCheck> checks = new ArrayList<SemanticEquivalenceCheck>();
        int[] sequence = new int[] {1};
        for (RelationalRewriteCandidate candidate : plan.getCandidates()) {
            List<QueryBlockNode> blocks = sourceBlocks(dag, candidate);
            checks.add(constraintCheck(candidate, blocks, sequence));
            if (requiresAggregationCheck(candidate, blocks)) {
                checks.add(aggregationCheck(candidate, blocks, sequence));
            }
        }
        List<String> unverifiedCandidateIds = unverifiedCandidateIds(checks);
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("candidateCount", Integer.valueOf(plan.getCandidates().size()));
        attributes.put("checkCount", Integer.valueOf(checks.size()));
        attributes.put("unverifiedCandidateCount", Integer.valueOf(unverifiedCandidateIds.size()));
        attributes.put("verificationStatus", overallStatus(checks).name());
        attributes.put("proofScope", "CANDIDATE_LEVEL_NOT_FULL_SQL_TEXT");
        return new SemanticEquivalenceReport(
            SemanticEquivalenceReport.SCHEMA_VERSION,
            plan.getSchemaVersion(),
            overallStatus(checks),
            checks,
            unverifiedCandidateIds,
            attributes
        );
    }

    private SemanticEquivalenceReport emptyReport(String sourceSchemaVersion, String reason) {
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("candidateCount", Integer.valueOf(0));
        attributes.put("checkCount", Integer.valueOf(0));
        attributes.put("unverifiedCandidateCount", Integer.valueOf(0));
        attributes.put("verificationStatus", SemanticEquivalenceStatus.NO_CANDIDATE.name());
        attributes.put("reason", reason);
        return new SemanticEquivalenceReport(
            SemanticEquivalenceReport.SCHEMA_VERSION,
            sourceSchemaVersion,
            SemanticEquivalenceStatus.NO_CANDIDATE,
            Collections.<SemanticEquivalenceCheck>emptyList(),
            Collections.<String>emptyList(),
            attributes
        );
    }

    private SemanticEquivalenceCheck constraintCheck(RelationalRewriteCandidate candidate,
                                                    List<QueryBlockNode> blocks,
                                                    int[] sequence) {
        String originalExpression = originalExpression(blocks, candidate);
        String rewrittenExpression = candidate.getReplacementForm();
        String differenceExpression = "DELTA_Q1_MINUS_Q2(" + candidate.getCandidateId() + ") = "
            + originalExpression + " - " + rewrittenExpression;
        String reverseDifferenceExpression = "DELTA_Q2_MINUS_Q1(" + candidate.getCandidateId() + ") = "
            + rewrittenExpression + " - " + originalExpression;

        List<String> preconditions = new ArrayList<String>(candidate.getPreconditions());
        preconditions.add("OUTPUT_SCHEMA_EQUIVALENCE_REQUIRED");
        preconditions.add("BAG_OR_SET_SEMANTICS_MUST_MATCH_QUERY_DISTINCTNESS");

        List<String> proofObligations = new ArrayList<String>();
        proofObligations.add("DELTA_Q1_MINUS_Q2_EMPTY");
        proofObligations.add("DELTA_Q2_MINUS_Q1_EMPTY");
        if (!candidate.getCompensationPredicates().isEmpty()) {
            proofObligations.add("COMPENSATION_PREDICATES_APPLIED_AT_REFERENCE");
        }
        if (possibleNullInteraction(blocks, candidate)) {
            proofObligations.add("NULL_SAFE_COMPARISON_WITH_IS_NOT_DISTINCT_FROM");
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            proofObligations.add("LEFT_JOIN_NULL_EXTENSION_PRESERVED");
            proofObligations.add("RIGHT_AGGREGATE_ONE_ROW_PER_JOIN_KEY");
        }

        List<String> nullSemantics = nullSemantics(blocks, candidate);
        List<String> bagSemantics = bagSemantics(candidate);
        List<String> risks = new ArrayList<String>(candidate.getSemanticRisks());
        if (!allOutputColumnsStaticallyNotNull(blocks)) {
            risks.add("NOT_NULL_CONSTRAINTS_NOT_AVAILABLE_IN_STATIC_PROFILE");
        }

        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("method", "DIFFERENCE_EXPRESSION_EMPTYNESS");
        attributes.put("solver", "STATIC_RULE_ENGINE");
        attributes.put("smtSolverStatus", "NOT_INTEGRATED");
        attributes.put("deltaPredicateStatus", deltaPredicateStatus(blocks));
        attributes.put("nullExtendedComparison", possibleNullInteraction(blocks, candidate));
        attributes.put("candidateReplacementForm", candidate.getReplacementForm());

        return new SemanticEquivalenceCheck(
            checkId(sequence),
            candidate.getCandidateId(),
            candidate.getRuleType(),
            SemanticEquivalenceCheckType.CONSTRAINT_BASED_EQUIVALENCE,
            constraintStatus(candidate, blocks),
            originalExpression,
            rewrittenExpression,
            differenceExpression,
            reverseDifferenceExpression,
            preconditions,
            proofObligations,
            nullSemantics,
            bagSemantics,
            risks,
            constraintEvidence(candidate, blocks, differenceExpression, reverseDifferenceExpression),
            attributes
        );
    }

    private SemanticEquivalenceCheck aggregationCheck(RelationalRewriteCandidate candidate,
                                                     List<QueryBlockNode> blocks,
                                                     int[] sequence) {
        List<String> preconditions = new ArrayList<String>();
        List<String> proofObligations = new ArrayList<String>();
        List<String> nullSemantics = new ArrayList<String>();
        List<String> bagSemantics = new ArrayList<String>();
        List<String> risks = new ArrayList<String>();
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();

        attributes.put("method", "AGGREGATE_EQUIVALENCE_LAWS");
        attributes.put("solver", "STATIC_RULE_ENGINE");
        attributes.put("smtSolverStatus", "NOT_INTEGRATED");
        attributes.put("equivalenceLaw", aggregationLaw(candidate, blocks));

        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            preconditions.add("COUNT_DISTINCT_CASE_FILTER_EQUIVALENCE");
            preconditions.add("DISTINCT_ARGUMENT_EQUIVALENCE_REQUIRED");
            preconditions.add("CASE_FALSE_BRANCH_RETURNS_NULL");
            preconditions.add("PREDICATE_APPLIED_INSIDE_CASE_OR_FILTER");
            proofObligations.add("VERIFY_CASE_DOES_NOT_INTRODUCE_NON_NULL_SENTINEL");
            proofObligations.add("VERIFY_TRUE_BRANCH_ARGUMENT_EQUALS_DISTINCT_ARGUMENT");
            nullSemantics.add("COUNT_DISTINCT_IGNORES_NULL");
            nullSemantics.add("FILTER_AND_CASE_FALSE_NULL_ARE_EQUIVALENT");
            bagSemantics.add("DISTINCT_DEDUPLICATION_DOMAIN_MUST_MATCH");
            bagSemantics.add("OVERLAPPING_PREDICATES_ALLOWED_ONLY_INSIDE_CASE_BRANCHES");
        } else {
            preconditions.add("GROUP_BY_KEY_CARDINALITY_PRESERVED");
            preconditions.add("AGGREGATE_FUNCTION_DECOMPOSABILITY_REQUIRED");
            preconditions.add("LEFT_JOIN_ABSENCE_REMAINS_NULL_EXTENDED");
            proofObligations.add("VERIFY_EACH_RIGHT_AGGREGATE_HAS_ONE_ROW_PER_JOIN_KEY");
            proofObligations.add("VERIFY_GROUP_BY_KEY_EQUALS_JOIN_KEY");
            nullSemantics.add("LEFT_JOIN_NULL_EXTENSION_MUST_MATCH_ORIGINAL");
            bagSemantics.add("JOIN_MULTIPLICITY_MUST_NOT_CHANGE");
        }

        if (containsAvg(blocks)) {
            risks.add("AVG_REQUIRES_SUM_COUNT_DECOMPOSITION");
        }
        if (containsCountDistinct(blocks)) {
            risks.add("COUNT_DISTINCT_ARGUMENT_AND_NULL_SEMANTICS_REQUIRE_REVIEW");
        }
        if (!containsCountOrSum(blocks) && !containsCountDistinct(blocks)) {
            risks.add("NON_DECOMPOSABLE_AGGREGATE_REQUIRES_REVIEW");
        }

        return new SemanticEquivalenceCheck(
            checkId(sequence),
            candidate.getCandidateId(),
            candidate.getRuleType(),
            SemanticEquivalenceCheckType.STATISTICAL_AGGREGATION_EQUIVALENCE,
            aggregationStatus(candidate, blocks),
            aggregateOriginalExpression(blocks),
            candidate.getReplacementForm(),
            "AGG_DELTA_Q1_MINUS_Q2(" + candidate.getCandidateId() + ")",
            "AGG_DELTA_Q2_MINUS_Q1(" + candidate.getCandidateId() + ")",
            preconditions,
            proofObligations,
            nullSemantics,
            bagSemantics,
            risks,
            aggregationEvidence(candidate, blocks),
            attributes
        );
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

    private boolean requiresAggregationCheck(RelationalRewriteCandidate candidate, List<QueryBlockNode> blocks) {
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()
            || RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return true;
        }
        for (QueryBlockNode block : blocks) {
            if (!block.getGroupBy().isEmpty() || containsCountOrSum(Collections.singletonList(block))
                || containsCountDistinct(Collections.singletonList(block)) || containsAvg(Collections.singletonList(block))) {
                return true;
            }
        }
        return false;
    }

    private SemanticEquivalenceStatus constraintStatus(RelationalRewriteCandidate candidate, List<QueryBlockNode> blocks) {
        if (blocks.isEmpty()) {
            return SemanticEquivalenceStatus.UNSUPPORTED;
        }
        if (RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()
            && structurallyEquivalent(blocks)) {
            return SemanticEquivalenceStatus.CONDITIONALLY_PROVED;
        }
        if (predicateContradictionDetected(blocks) && allOutputColumnsStaticallyNotNull(blocks)) {
            return SemanticEquivalenceStatus.PROVED;
        }
        return SemanticEquivalenceStatus.NEEDS_CONSTRAINTS;
    }

    private SemanticEquivalenceStatus aggregationStatus(RelationalRewriteCandidate candidate, List<QueryBlockNode> blocks) {
        if (blocks.isEmpty()) {
            return SemanticEquivalenceStatus.UNSUPPORTED;
        }
        if (containsAvg(blocks)) {
            return SemanticEquivalenceStatus.NEEDS_CONSTRAINTS;
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType() && containsCountDistinct(blocks)) {
            return SemanticEquivalenceStatus.CONDITIONALLY_PROVED;
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return SemanticEquivalenceStatus.NEEDS_CONSTRAINTS;
        }
        return containsCountOrSum(blocks)
            ? SemanticEquivalenceStatus.CONDITIONALLY_PROVED
            : SemanticEquivalenceStatus.NEEDS_CONSTRAINTS;
    }

    private boolean structurallyEquivalent(List<QueryBlockNode> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        String structuralHash = blocks.get(0).getStructuralHash();
        if (SemanticVerificationCollections.text(structuralHash).isEmpty()) {
            return false;
        }
        int outputSize = blocks.get(0).getOutputColumns().size();
        for (QueryBlockNode block : blocks) {
            if (!structuralHash.equals(block.getStructuralHash()) || block.getOutputColumns().size() != outputSize) {
                return false;
            }
        }
        return true;
    }

    private boolean allOutputColumnsStaticallyNotNull(List<QueryBlockNode> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        for (QueryBlockNode block : blocks) {
            for (String outputColumn : block.getOutputColumns()) {
                if (!isColumnStaticallyNotNull(block, outputColumn)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isColumnStaticallyNotNull(QueryBlockNode block, String outputColumn) {
        String column = normalizeColumn(outputColumn);
        if (column.isEmpty()) {
            return false;
        }
        for (String item : block.getSelectList()) {
            String normalized = item.toUpperCase(Locale.ROOT);
            if (normalized.contains("COUNT(") && normalizeColumn(item).contains(column)) {
                return true;
            }
        }
        String where = block.getWhereClause() == null ? "" : block.getWhereClause().toUpperCase(Locale.ROOT);
        return where.contains(column.toUpperCase(Locale.ROOT) + " IS NOT NULL");
    }

    private boolean predicateContradictionDetected(List<QueryBlockNode> blocks) {
        for (QueryBlockNode block : blocks) {
            String predicate = SemanticVerificationCollections.text(block.getWhereClause());
            if (FALSE_PREDICATE_PATTERN.matcher(predicate).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean possibleNullInteraction(List<QueryBlockNode> blocks, RelationalRewriteCandidate candidate) {
        if (!allOutputColumnsStaticallyNotNull(blocks)) {
            return true;
        }
        if (NULL_TEXT_PATTERN.matcher(candidate.getReplacementForm()).find()) {
            return true;
        }
        for (QueryBlockNode block : blocks) {
            if (NULL_TEXT_PATTERN.matcher(block.getWhereClause() == null ? "" : block.getWhereClause()).find()) {
                return true;
            }
            for (String item : block.getSelectList()) {
                if (NULL_TEXT_PATTERN.matcher(item).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> nullSemantics(List<QueryBlockNode> blocks, RelationalRewriteCandidate candidate) {
        List<String> result = new ArrayList<String>();
        result.add("THREE_VALUED_LOGIC_PRESERVED");
        result.add("USE_IS_NOT_DISTINCT_FROM_FOR_NULLABLE_COLUMN_COMPARISON");
        if (possibleNullInteraction(blocks, candidate)) {
            result.add("NULL_OUTPUT_COLUMNS_REQUIRE_SCHEMA_CONSTRAINTS_OR_COMPENSATION");
        } else {
            result.add("STATIC_NOT_NULL_OUTPUT_COLUMNS");
        }
        return result;
    }

    private List<String> bagSemantics(RelationalRewriteCandidate candidate) {
        List<String> result = new ArrayList<String>();
        result.add("BAG_EQUIVALENCE_REQUIRED_BY_DEFAULT");
        if (RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()) {
            result.add("REFERENCE_REPLACEMENT_MUST_NOT_CHANGE_MULTIPLICITY");
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            result.add("LEFT_JOIN_ROW_MULTIPLICITY_MUST_BE_PRESERVED");
        }
        return result;
    }

    private String deltaPredicateStatus(List<QueryBlockNode> blocks) {
        if (predicateContradictionDetected(blocks)) {
            return "CONTRADICTION_DETECTED";
        }
        if (allOutputColumnsStaticallyNotNull(blocks)) {
            return "NEEDS_EMPTYNESS_CHECK_WITH_NOT_NULL_OUTPUTS";
        }
        return "NEEDS_NULL_AWARE_EMPTYNESS_CHECK";
    }

    private String originalExpression(List<QueryBlockNode> blocks, RelationalRewriteCandidate candidate) {
        if (blocks.isEmpty()) {
            return "ORIGINAL_BLOCKS_UNAVAILABLE(" + candidate.getSourceBlockIds() + ")";
        }
        List<String> expressions = new ArrayList<String>();
        for (QueryBlockNode block : blocks) {
            expressions.add(block.getBlockId() + "=" + block.getNormalizedRelationalForm());
        }
        return expressions.toString();
    }

    private String aggregateOriginalExpression(List<QueryBlockNode> blocks) {
        List<String> expressions = new ArrayList<String>();
        for (QueryBlockNode block : blocks) {
            expressions.add(block.getBlockId() + "{groupBy=" + block.getGroupBy()
                + ", select=" + block.getSelectList() + ", where=" + block.getWhereClause() + "}");
        }
        return expressions.toString();
    }

    private String aggregationLaw(RelationalRewriteCandidate candidate, List<QueryBlockNode> blocks) {
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType() && containsCountDistinct(blocks)) {
            return "COUNT(DISTINCT CASE WHEN p THEN x END) == COUNT(DISTINCT x) FILTER (WHERE p)";
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return "LEFT_JOIN(grouped_agg_i ON key) == GROUP_BY_EXTENDED_AGG(key, agg_i)";
        }
        return "AGGREGATE_EQUIVALENCE_REQUIRES_RULE_SPECIFIC_PROOF";
    }

    private List<Map<String, Object>> constraintEvidence(RelationalRewriteCandidate candidate,
                                                         List<QueryBlockNode> blocks,
                                                         String differenceExpression,
                                                         String reverseDifferenceExpression) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Object> diff = new LinkedHashMap<String, Object>();
        diff.put("type", "DIFFERENCE_EXPRESSIONS");
        diff.put("candidateId", candidate.getCandidateId());
        diff.put("differenceExpression", differenceExpression);
        diff.put("reverseDifferenceExpression", reverseDifferenceExpression);
        diff.put("compensationPredicates", candidate.getCompensationPredicates());
        result.add(diff);
        result.addAll(blockEvidence(blocks));
        return result;
    }

    private List<Map<String, Object>> aggregationEvidence(RelationalRewriteCandidate candidate,
                                                         List<QueryBlockNode> blocks) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Object> law = new LinkedHashMap<String, Object>();
        law.put("type", "AGGREGATION_EQUIVALENCE_LAW");
        law.put("candidateId", candidate.getCandidateId());
        law.put("ruleType", candidate.getRuleType().name());
        law.put("law", aggregationLaw(candidate, blocks));
        law.put("replacementForm", candidate.getReplacementForm());
        result.add(law);
        result.addAll(blockEvidence(blocks));
        return result;
    }

    private List<Map<String, Object>> blockEvidence(List<QueryBlockNode> blocks) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (QueryBlockNode block : blocks) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("type", "QUERY_BLOCK");
            item.put("blockId", block.getBlockId());
            item.put("structuralHash", block.getStructuralHash());
            item.put("selectList", block.getSelectList());
            item.put("fromClause", block.getFromClause());
            item.put("whereClause", block.getWhereClause());
            item.put("groupBy", block.getGroupBy());
            item.put("outputColumns", block.getOutputColumns());
            item.put("normalizedRelationalForm", block.getNormalizedRelationalForm());
            result.add(item);
        }
        return result;
    }

    private boolean containsCountDistinct(List<QueryBlockNode> blocks) {
        return containsPattern(blocks, COUNT_DISTINCT_PATTERN);
    }

    private boolean containsAvg(List<QueryBlockNode> blocks) {
        return containsPattern(blocks, AVG_PATTERN);
    }

    private boolean containsCountOrSum(List<QueryBlockNode> blocks) {
        return containsPattern(blocks, COUNT_OR_SUM_PATTERN);
    }

    private boolean containsPattern(List<QueryBlockNode> blocks, Pattern pattern) {
        for (QueryBlockNode block : blocks) {
            for (String item : block.getSelectList()) {
                if (pattern.matcher(item).find()) {
                    return true;
                }
            }
            if (pattern.matcher(block.getNormalizedRelationalForm()).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalizeColumn(String value) {
        String clean = SemanticVerificationCollections.text(value)
            .replace("\"", "")
            .replace("`", "")
            .toUpperCase(Locale.ROOT);
        int dot = clean.lastIndexOf('.');
        return dot >= 0 ? clean.substring(dot + 1) : clean;
    }

    private List<String> unverifiedCandidateIds(List<SemanticEquivalenceCheck> checks) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (SemanticEquivalenceCheck check : checks) {
            if (SemanticEquivalenceStatus.PROVED != check.getStatus()) {
                result.add(check.getCandidateId());
            }
        }
        return new ArrayList<String>(result);
    }

    private SemanticEquivalenceStatus overallStatus(List<SemanticEquivalenceCheck> checks) {
        if (checks.isEmpty()) {
            return SemanticEquivalenceStatus.NO_CANDIDATE;
        }
        boolean hasNeedsConstraints = false;
        boolean hasConditional = false;
        for (SemanticEquivalenceCheck check : checks) {
            if (SemanticEquivalenceStatus.UNSUPPORTED == check.getStatus()) {
                return SemanticEquivalenceStatus.UNSUPPORTED;
            }
            if (SemanticEquivalenceStatus.NEEDS_CONSTRAINTS == check.getStatus()) {
                hasNeedsConstraints = true;
            }
            if (SemanticEquivalenceStatus.CONDITIONALLY_PROVED == check.getStatus()) {
                hasConditional = true;
            }
        }
        if (hasNeedsConstraints) {
            return SemanticEquivalenceStatus.NEEDS_CONSTRAINTS;
        }
        return hasConditional ? SemanticEquivalenceStatus.CONDITIONALLY_PROVED : SemanticEquivalenceStatus.PROVED;
    }

    private LinkedHashMap<String, Object> baseAttributes() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "RELATIONAL_REWRITE_PLAN");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("solver", "STATIC_RULE_ENGINE");
        attributes.put("smtSolverStatus", "NOT_INTEGRATED");
        attributes.put("supportedMethods", Arrays.asList(
            SemanticEquivalenceCheckType.CONSTRAINT_BASED_EQUIVALENCE.name(),
            SemanticEquivalenceCheckType.STATISTICAL_AGGREGATION_EQUIVALENCE.name()
        ));
        return attributes;
    }

    private String checkId(int[] sequence) {
        String value = "SEV-" + sequence[0];
        sequence[0]++;
        return value;
    }
}
