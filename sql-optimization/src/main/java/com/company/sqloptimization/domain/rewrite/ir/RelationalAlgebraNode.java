package com.company.sqloptimization.domain.rewrite.ir;

import java.util.List;
import java.util.Map;

public class RelationalAlgebraNode {

    private final String nodeId;
    private final RelationalOperator operator;
    private final List<String> inputNodeIds;
    private final List<Map<String, Object>> expressions;
    private final List<String> outputSymbols;
    private final Map<String, Object> attributes;

    public RelationalAlgebraNode(String nodeId,
                                 RelationalOperator operator,
                                 List<String> inputNodeIds,
                                 List<Map<String, Object>> expressions,
                                 List<String> outputSymbols,
                                 Map<String, Object> attributes) {
        this.nodeId = nodeId;
        this.operator = operator;
        this.inputNodeIds = IrCollections.immutableStrings(inputNodeIds);
        this.expressions = IrCollections.immutableMaps(expressions);
        this.outputSymbols = IrCollections.immutableStrings(outputSymbols);
        this.attributes = IrCollections.immutableMap(attributes);
    }

    public RewriteIrLayer getLayer() {
        return RewriteIrLayer.L4_RELATIONAL_ALGEBRA;
    }

    public String getNodeId() {
        return nodeId;
    }

    public RelationalOperator getOperator() {
        return operator;
    }

    public String getOperatorSymbol() {
        return operator == null ? "" : operator.getSymbol();
    }

    public List<String> getInputNodeIds() {
        return inputNodeIds;
    }

    public List<Map<String, Object>> getExpressions() {
        return expressions;
    }

    public List<String> getOutputSymbols() {
        return outputSymbols;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
