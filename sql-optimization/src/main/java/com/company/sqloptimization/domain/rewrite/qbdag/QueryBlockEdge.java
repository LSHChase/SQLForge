package com.company.sqloptimization.domain.rewrite.qbdag;

import java.util.List;
import java.util.Map;

public class QueryBlockEdge {

    public static final String CONTAINS = "CONTAINS";
    public static final String OUTPUT_REFERENCE = "OUTPUT_REFERENCE";
    public static final String EXTERNAL_REFERENCE = "EXTERNAL_REFERENCE";
    public static final String LATERAL_JOIN_PROMOTION = "LATERAL_JOIN_PROMOTION";

    private final String fromBlockId;
    private final String toBlockId;
    private final String edgeType;
    private final List<String> evidence;
    private final boolean breaksCycle;
    private final String rewriteAction;
    private final Map<String, Object> attributes;

    public QueryBlockEdge(String fromBlockId,
                          String toBlockId,
                          String edgeType,
                          List<String> evidence,
                          boolean breaksCycle,
                          String rewriteAction,
                          Map<String, Object> attributes) {
        this.fromBlockId = fromBlockId;
        this.toBlockId = toBlockId;
        this.edgeType = edgeType;
        this.evidence = QbDagCollections.immutableStrings(evidence);
        this.breaksCycle = breaksCycle;
        this.rewriteAction = rewriteAction;
        this.attributes = QbDagCollections.immutableMap(attributes);
    }

    public String getFromBlockId() {
        return fromBlockId;
    }

    public String getToBlockId() {
        return toBlockId;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public boolean isBreaksCycle() {
        return breaksCycle;
    }

    public String getRewriteAction() {
        return rewriteAction;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
