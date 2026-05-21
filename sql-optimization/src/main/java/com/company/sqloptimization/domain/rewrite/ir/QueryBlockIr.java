package com.company.sqloptimization.domain.rewrite.ir;

import java.util.List;
import java.util.Map;

public class QueryBlockIr {

    private final String blockId;
    private final String blockType;
    private final List<String> inputReferenceIds;
    private final List<Map<String, Object>> outputs;
    private final List<Map<String, Object>> predicates;
    private final List<Map<String, Object>> aggregations;
    private final Map<String, Object> attributes;

    public QueryBlockIr(String blockId,
                        String blockType,
                        List<String> inputReferenceIds,
                        List<Map<String, Object>> outputs,
                        List<Map<String, Object>> predicates,
                        List<Map<String, Object>> aggregations,
                        Map<String, Object> attributes) {
        this.blockId = blockId;
        this.blockType = blockType;
        this.inputReferenceIds = IrCollections.immutableStrings(inputReferenceIds);
        this.outputs = IrCollections.immutableMaps(outputs);
        this.predicates = IrCollections.immutableMaps(predicates);
        this.aggregations = IrCollections.immutableMaps(aggregations);
        this.attributes = IrCollections.immutableMap(attributes);
    }

    public RewriteIrLayer getLayer() {
        return RewriteIrLayer.L3_QUERY_BLOCK;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getBlockType() {
        return blockType;
    }

    public List<String> getInputReferenceIds() {
        return inputReferenceIds;
    }

    public List<Map<String, Object>> getOutputs() {
        return outputs;
    }

    public List<Map<String, Object>> getPredicates() {
        return predicates;
    }

    public List<Map<String, Object>> getAggregations() {
        return aggregations;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
