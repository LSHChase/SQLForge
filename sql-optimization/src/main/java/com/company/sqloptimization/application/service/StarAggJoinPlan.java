package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StarAggJoinPlan {

    final List<Map<String, Object>> blockingReasons;
    final List<StarAggJoinEdge> edges;
    final int joinCount;

    StarAggJoinPlan(List<Map<String, Object>> blockingReasons, List<StarAggJoinEdge> edges, int joinCount) {
        this.blockingReasons = blockingReasons;
        this.edges = edges;
        this.joinCount = joinCount;
    }

    List<Map<String, Object>> joinKeys() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (StarAggJoinEdge edge : edges) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("joinIndex", Integer.valueOf(edge.joinIndex));
            item.put("joinType", "INNER");
            item.put("condition", edge.condition);
            item.put("leftRelation", edge.leftRelation.tableName);
            item.put("leftAlias", edge.leftRelation.alias);
            item.put("rightRelation", edge.rightRelation.tableName);
            item.put("rightAlias", edge.rightRelation.alias);
            item.put("leftKey", edge.leftKey);
            item.put("rightKey", edge.rightKey);
            result.add(item);
        }
        return result;
    }
}
