package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggMeasurePlanner.measureSourceColumns;
import static com.company.sqloptimization.application.service.StarAggProfileValues.reason;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.qualifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class StarAggFactPlanner {

    private StarAggFactPlanner() {
    }

    static StarAggFactPlan factPlan(Map<String, Object> advancedStructureProfile,
                                    StarAggRelationCatalog relationCatalog,
                                    StarAggJoinPlan joinPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> measureRelationKeys = new LinkedHashSet<String>();
        for (String sourceColumn : measureSourceColumns(advancedStructureProfile)) {
            if (!isIdentifierReference(sourceColumn) || !StringUtils.hasText(qualifier(sourceColumn))) {
                reasons.add(reason("FACT_TABLE_UNRESOLVED", "指标字段缺少表限定，无法在多表 Join 中证明事实表来源。"));
                continue;
            }
            StarAggRelationSpec relation = relationCatalog.relationForColumn(sourceColumn);
            if (relation == null) {
                reasons.add(reason("FACT_TABLE_UNRESOLVED", "指标字段来源不属于可解析基表，无法证明事实表。"));
            } else {
                measureRelationKeys.add(relation.key);
            }
        }
        if (measureRelationKeys.size() > 1) {
            reasons.add(reason("FACT_TABLE_UNRESOLVED", "指标字段来自多个表，当前静态规则不能保守识别唯一事实表。"));
        }
        if (!reasons.isEmpty()) {
            return StarAggFactPlan.blocked(reasons);
        }
        return resolveFactPlan(measureRelationKeys, relationCatalog, joinPlan);
    }

    private static StarAggFactPlan resolveFactPlan(Set<String> measureRelationKeys,
                                                   StarAggRelationCatalog relationCatalog,
                                                   StarAggJoinPlan joinPlan) {
        String factKey = measureRelationKeys.isEmpty() ? "" : measureRelationKeys.iterator().next();
        String inference = "MEASURE_SOURCE_AND_JOIN_TOPOLOGY";
        if (!StringUtils.hasText(factKey)) {
            factKey = topologyFactKey(joinPlan);
            inference = "JOIN_TOPOLOGY_ONLY";
        }
        if (!StringUtils.hasText(factKey)) {
            return StarAggFactPlan.blocked(Collections.singletonList(reason("FACT_TABLE_UNRESOLVED", "Join 拓扑不存在唯一中心表，无法保守识别事实表。")));
        }
        if (!isJoinCenter(factKey, joinPlan)) {
            return StarAggFactPlan.blocked(Collections.singletonList(reason("FACT_TABLE_UNRESOLVED", "指标来源表不是所有 Join 的中心表，当前静态规则不能证明星型拓扑。")));
        }
        LinkedHashSet<String> dimensionKeys = new LinkedHashSet<String>();
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        for (StarAggJoinEdge edge : joinPlan.edges) {
            if (!edge.involves(factKey)) {
                reasons.add(reason("STAR_AGG_JOIN_TO_FACT_REQUIRED", "STAR_AGG_MV 只支持事实表直接连接每个维表的星型拓扑。"));
                continue;
            }
            dimensionKeys.add(edge.other(factKey));
        }
        if (dimensionKeys.size() < 2) {
            reasons.add(reason("STAR_AGG_DIMENSION_COUNT_REQUIRED", "STAR_AGG_MV 至少需要两个可区分维表。"));
        }
        return reasons.isEmpty()
            ? StarAggFactPlan.generated(factKey, inference, relationCatalog.factEvidence(factKey, inference), relationCatalog.dimensionEvidence(dimensionKeys))
            : StarAggFactPlan.blocked(reasons);
    }

    private static String topologyFactKey(StarAggJoinPlan joinPlan) {
        LinkedHashMap<String, Set<Integer>> relationJoinIndexes = new LinkedHashMap<String, Set<Integer>>();
        for (StarAggJoinEdge edge : joinPlan.edges) {
            addJoinIndex(relationJoinIndexes, edge.leftRelation.key, edge.joinIndex);
            addJoinIndex(relationJoinIndexes, edge.rightRelation.key, edge.joinIndex);
        }
        String candidate = "";
        for (Map.Entry<String, Set<Integer>> entry : relationJoinIndexes.entrySet()) {
            if (entry.getValue().size() == joinPlan.joinCount) {
                if (StringUtils.hasText(candidate)) {
                    return "";
                }
                candidate = entry.getKey();
            }
        }
        return candidate;
    }

    private static boolean isJoinCenter(String relationKey, StarAggJoinPlan joinPlan) {
        LinkedHashSet<Integer> joinIndexes = new LinkedHashSet<Integer>();
        for (StarAggJoinEdge edge : joinPlan.edges) {
            if (edge.involves(relationKey)) {
                joinIndexes.add(Integer.valueOf(edge.joinIndex));
            }
        }
        return StringUtils.hasText(relationKey) && joinIndexes.size() == joinPlan.joinCount;
    }

    private static void addJoinIndex(Map<String, Set<Integer>> relationJoinIndexes, String relationKey, int joinIndex) {
        Set<Integer> indexes = relationJoinIndexes.get(relationKey);
        if (indexes == null) {
            indexes = new LinkedHashSet<Integer>();
            relationJoinIndexes.put(relationKey, indexes);
        }
        indexes.add(Integer.valueOf(joinIndex));
    }
}
