package com.company.sqloptimization.domain.rewrite.ir;

import java.util.Map;

public class AstNodeReference {

    private final String nodeId;
    private final String parserEngine;
    private final String dialectNodeKind;
    private final String rootKind;
    private final String normalizedSql;
    private final Map<String, Object> nodeSummary;

    public AstNodeReference(String nodeId,
                            String parserEngine,
                            String dialectNodeKind,
                            String rootKind,
                            String normalizedSql,
                            Map<String, Object> nodeSummary) {
        this.nodeId = nodeId;
        this.parserEngine = parserEngine;
        this.dialectNodeKind = dialectNodeKind;
        this.rootKind = rootKind;
        this.normalizedSql = normalizedSql;
        this.nodeSummary = IrCollections.immutableMap(nodeSummary);
    }

    public RewriteIrLayer getLayer() {
        return RewriteIrLayer.L1_AST;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getParserEngine() {
        return parserEngine;
    }

    public String getDialectNodeKind() {
        return dialectNodeKind;
    }

    public String getRootKind() {
        return rootKind;
    }

    public String getNormalizedSql() {
        return normalizedSql;
    }

    public Map<String, Object> getNodeSummary() {
        return nodeSummary;
    }
}
