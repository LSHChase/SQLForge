package com.company.sqloptimization.domain.parse;

import java.util.HashMap;
import java.util.Map;

public enum StructureParseIssueScenario {

    PARSER_FAILURE("PARSER_FAILURE", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.HIGH, true, false, 8),
    WIDE_PROJECTION("WIDE_PROJECTION", StructureParseIssueDomain.GOVERNANCE, StructureParseIssueSeverity.MEDIUM, true, false, 3),
    MISSING_FILTER("MISSING_FILTER", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    UNBOUNDED_SORT("UNBOUNDED_SORT", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, false, false, 4),
    MULTI_JOIN_COMPLEXITY("MULTI_JOIN_COMPLEXITY", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.MEDIUM, true, false, 5),
    QUERY_DATE_UNRESOLVED("QUERY_DATE_UNRESOLVED", StructureParseIssueDomain.DATA, StructureParseIssueSeverity.MEDIUM, true, false, 5),
    ROUTE_HINT_CONFLICT("ROUTE_HINT_CONFLICT", StructureParseIssueDomain.ROUTING, StructureParseIssueSeverity.HIGH, true, true, 8),
    DIALECT_INCOMPATIBLE("DIALECT_INCOMPATIBLE", StructureParseIssueDomain.COMPATIBILITY, StructureParseIssueSeverity.HIGH, true, false, 6),
    PARAMETER_BINDING_RISK("PARAMETER_BINDING_RISK", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.MEDIUM, true, false, 4),
    SQL_SYNTAX_INVALID("SQL_SYNTAX_INVALID", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.HIGH, true, false, 8),
    SQL_TOO_LONG("SQL_TOO_LONG", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.HIGH, true, false, 8),
    SELECT_STAR("SELECT_STAR", StructureParseIssueDomain.GOVERNANCE, StructureParseIssueSeverity.MEDIUM, true, false, 3),
    FULL_SCAN_RISK("FULL_SCAN_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    NO_PREDICATE("NO_PREDICATE", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    ORDER_BY_WITHOUT_LIMIT("ORDER_BY_WITHOUT_LIMIT", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, false, false, 4),
    HEAVY_JOIN_GRAPH("HEAVY_JOIN_GRAPH", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.MEDIUM, true, false, 5),
    LARGE_JOIN_PAIR_RISK("LARGE_JOIN_PAIR_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    REPEATED_EXPRESSION_COMPUTE("REPEATED_EXPRESSION_COMPUTE", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, false, false, 1),
    LARGE_RESULT_SET_RISK("LARGE_RESULT_SET_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 3),
    SCALAR_SUBQUERY_IN_SELECT("SCALAR_SUBQUERY_IN_SELECT", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    NESTED_SUBQUERY_RISK("NESTED_SUBQUERY_RISK", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.HIGH, true, true, 8),
    CORRELATED_SUBQUERY_RISK("CORRELATED_SUBQUERY_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    FUNCTION_WRAPPED_PREDICATE("FUNCTION_WRAPPED_PREDICATE", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, true, false, 8),
    NOT_EXISTS_ANTI_JOIN_RISK("NOT_EXISTS_ANTI_JOIN_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, true, false, 5),
    LEADING_WILDCARD_LIKE_RISK("LEADING_WILDCARD_LIKE_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, true, false, 8),
    OR_PREDICATE_INDEX_RISK("OR_PREDICATE_INDEX_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.MEDIUM, true, false, 8),
    ORDER_BY_RANDOM_RISK("ORDER_BY_RANDOM_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 4),
    REPEATED_TABLE_SCAN_RISK("REPEATED_TABLE_SCAN_RISK", StructureParseIssueDomain.PERFORMANCE, StructureParseIssueSeverity.HIGH, true, true, 8),
    COMPLEX_QUERY_GRAPH_RISK("COMPLEX_QUERY_GRAPH_RISK", StructureParseIssueDomain.STRUCTURE, StructureParseIssueSeverity.HIGH, true, true, 8),
    REPORT_SQL_MERGE_CANDIDATE("REPORT_SQL_MERGE_CANDIDATE", StructureParseIssueDomain.GOVERNANCE, StructureParseIssueSeverity.MEDIUM, false, false, 0),
    GENERAL_WARNING("GENERAL_WARNING", StructureParseIssueDomain.CONVENTION, StructureParseIssueSeverity.LOW, false, false, 1);

    private static final Map<String, StructureParseIssueScenario> BY_SCENE = new HashMap<String, StructureParseIssueScenario>();

    static {
        for (StructureParseIssueScenario scenario : values()) {
            BY_SCENE.put(scenario.issueScene, scenario);
        }
    }

    private final String issueScene;
    private final StructureParseIssueDomain defaultDomain;
    private final StructureParseIssueSeverity defaultSeverity;
    private final boolean defaultImportant;
    private final boolean defaultUrgent;
    private final int sceneWeight;

    StructureParseIssueScenario(String issueScene,
                                StructureParseIssueDomain defaultDomain,
                                StructureParseIssueSeverity defaultSeverity,
                                boolean defaultImportant,
                                boolean defaultUrgent,
                                int sceneWeight) {
        this.issueScene = issueScene;
        this.defaultDomain = defaultDomain;
        this.defaultSeverity = defaultSeverity;
        this.defaultImportant = defaultImportant;
        this.defaultUrgent = defaultUrgent;
        this.sceneWeight = sceneWeight;
    }

    public static StructureParseIssueScenario resolve(String issueScene) {
        StructureParseIssueScenario scenario = BY_SCENE.get(issueScene);
        return scenario == null ? GENERAL_WARNING : scenario;
    }

    public String getIssueScene() {
        return issueScene;
    }

    public StructureParseIssueDomain getDefaultDomain() {
        return defaultDomain;
    }

    public StructureParseIssueSeverity getDefaultSeverity() {
        return defaultSeverity;
    }

    public boolean isDefaultImportant() {
        return defaultImportant;
    }

    public boolean isDefaultUrgent() {
        return defaultUrgent;
    }

    public int getSceneWeight() {
        return sceneWeight;
    }
}
