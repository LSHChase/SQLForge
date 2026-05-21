package com.company.sqloptimization.domain.rewrite.rule;

import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class RewriteRuleCatalog {

    public List<RewriteRuleDefinition> defaultRules() {
        return Arrays.asList(
            cseDedupRule(),
            verticalAggregationFoldingRule(),
            horizontalUnnestingRule()
        );
    }

    public RewriteRuleDefinition cseDedupRule() {
        return new RewriteRuleDefinition(
            "CSE-DEDUP-001",
            "CommonSubexpressionElimination",
            RewriteRuleCategory.STRUCTURAL_REWRITE,
            RewriteRuleSeverity.HIGH,
            RelationalRewriteRuleType.CSE_ELIMINATION,
            new RewriteRulePattern(
                "MULTIPLE_QUERY_BLOCKS",
                "EQUIVALENT_HASH",
                2,
                "SAME_PARENT_BLOCK",
                Collections.<String, Object>emptyMap()
            ),
            Arrays.asList(
                precondition("output_columns_compatible", "IGNORE_ALIAS"),
                precondition("predicates_composable", "DISJOINT_OR_OVERLAPPING"),
                precondition("aggregation_decomposable", "COUNT", "SUM", "MIN", "MAX")
            ),
            Arrays.asList(
                action("MERGE_BLOCKS", "WIDEN_PREDICATE", "", "", ""),
                action("PUSH_DOWN", "", "CASE_EXPRESSION", "SELECT_LIST", ""),
                action("DEDUPLICATE", "", "", "", "WITHIN_PARENT")
            ),
            new RewriteRuleVerification(
                "STRUCTURAL_HASH",
                "SMT_SOLVER",
                "IS_NOT_DISTINCT_FROM",
                verificationBoundary()
            ),
            new RewriteRuleCostImpact(
                "MULTIPLICATIVE",
                "ADDITIVE",
                "LOW",
                Collections.<String, Object>emptyMap()
            ),
            Collections.<String>emptyList(),
            ruleAttributes("L3_L4_RULE_DSL", "NO_SQL_EXECUTION")
        );
    }

    private RewriteRuleDefinition verticalAggregationFoldingRule() {
        return new RewriteRuleDefinition(
            "AGG-VFOLD-001",
            "VerticalAggregationFolding",
            RewriteRuleCategory.AGGREGATION_REWRITE,
            RewriteRuleSeverity.HIGH,
            RelationalRewriteRuleType.VERTICAL_FOLDING,
            new RewriteRulePattern(
                "MULTIPLE_AGGREGATE_BLOCKS",
                "EQUIVALENT_GROUP_BY_AND_SOURCE",
                2,
                "SAME_PARENT_BLOCK",
                Collections.<String, Object>emptyMap()
            ),
            Arrays.asList(
                precondition("aggregation_decomposable", "COUNT", "SUM", "MIN", "MAX"),
                precondition("predicates_composable", "DISJOINT_OR_OVERLAPPING"),
                precondition("output_columns_compatible", "IGNORE_ALIAS")
            ),
            Arrays.asList(
                action("MERGE_BLOCKS", "CASE_AGGREGATION", "", "", ""),
                action("PUSH_DOWN", "", "CASE_EXPRESSION", "SELECT_LIST", ""),
                action("DEDUPLICATE", "", "", "", "WITHIN_PARENT")
            ),
            new RewriteRuleVerification(
                "STRUCTURAL_HASH",
                "SMT_SOLVER",
                "IS_NOT_DISTINCT_FROM",
                verificationBoundary()
            ),
            new RewriteRuleCostImpact(
                "MULTIPLICATIVE",
                "ADDITIVE",
                "MEDIUM",
                Collections.<String, Object>emptyMap()
            ),
            Collections.singletonList("CSE-DEDUP-001"),
            ruleAttributes("L4_AGGREGATION_RULE_DSL", "NO_SQL_EXECUTION")
        );
    }

    private RewriteRuleDefinition horizontalUnnestingRule() {
        return new RewriteRuleDefinition(
            "JOIN-HUNNEST-001",
            "HorizontalUnnestingElimination",
            RewriteRuleCategory.JOIN_REWRITE,
            RewriteRuleSeverity.HIGH,
            RelationalRewriteRuleType.HORIZONTAL_UNNESTING,
            new RewriteRulePattern(
                "MULTIPLE_LEFT_JOIN_AGGREGATE_CHILDREN",
                "EQUIVALENT_JOIN_KEY_AND_GROUP_BY",
                2,
                "SAME_PARENT_BLOCK",
                Collections.<String, Object>emptyMap()
            ),
            Arrays.asList(
                precondition("join_keys_compatible", "NULL_SAFE_EQUALITY"),
                precondition("aggregation_decomposable", "COUNT", "SUM", "MIN", "MAX"),
                precondition("children_independent", "NO_EXTERNAL_CROSS_REFERENCE")
            ),
            Arrays.asList(
                action("MERGE_BLOCKS", "GROUP_BY_EXTENSION", "", "", ""),
                action("PUSH_DOWN", "", "AGGREGATION", "JOIN_CHILD", ""),
                action("DEDUPLICATE", "", "", "", "WITHIN_PARENT_JOIN_GRAPH")
            ),
            new RewriteRuleVerification(
                "STRUCTURAL_HASH",
                "SMT_SOLVER",
                "IS_NOT_DISTINCT_FROM",
                verificationBoundary()
            ),
            new RewriteRuleCostImpact(
                "MULTIPLICATIVE",
                "ADDITIVE",
                "MEDIUM",
                Collections.<String, Object>emptyMap()
            ),
            Collections.<String>emptyList(),
            ruleAttributes("L4_JOIN_RULE_DSL", "NO_SQL_EXECUTION")
        );
    }

    private RewriteRulePrecondition precondition(String check, String... params) {
        return new RewriteRulePrecondition(
            check,
            Arrays.asList(params),
            Collections.<String, Object>emptyMap()
        );
    }

    private RewriteRuleAction action(String type, String strategy, String target, String location, String scope) {
        return new RewriteRuleAction(
            type,
            strategy,
            target,
            location,
            scope,
            Collections.<String, Object>emptyMap()
        );
    }

    private LinkedHashMap<String, Object> verificationBoundary() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("smtSolverStatus", "FALLBACK_DECLARED_NOT_INTEGRATED");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        return attributes;
    }

    private LinkedHashMap<String, Object> ruleAttributes(String sourceLayer, String runtimeBoundary) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("sourceLayer", sourceLayer);
        attributes.put("runtimeBoundary", runtimeBoundary);
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        return attributes;
    }
}
