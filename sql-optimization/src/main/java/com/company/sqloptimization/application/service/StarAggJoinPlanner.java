package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.reason;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggProfileValues.upperText;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.splitAndConditions;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.stripOuterParentheses;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.topLevelEqualsIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class StarAggJoinPlanner {

    private StarAggJoinPlanner() {
    }

    static StarAggJoinPlan joinPlan(List<Map<String, Object>> joinGraph, StarAggRelationCatalog relationCatalog) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        List<StarAggJoinEdge> edges = new ArrayList<StarAggJoinEdge>();
        int joinIndex = 0;
        for (Map<String, Object> join : joinGraph) {
            joinIndex++;
            String joinType = upperText(join.get("joinType"));
            if (blockedJoinType(joinType, blockingReasons)) {
                continue;
            }
            List<String> conditions = splitAndConditions(text(join.get("condition")));
            if (conditions.isEmpty()) {
                blockingReasons.add(reason("JOIN_CONDITION_REQUIRED", "缺少 ON 等值条件，不能生成 STAR_AGG_MV。"));
                continue;
            }
            for (String condition : conditions) {
                StarAggJoinEdge edge = parseJoinEdge(joinIndex, joinType, condition, relationCatalog);
                if (edge.blockingReason != null) {
                    blockingReasons.add(edge.blockingReason);
                } else {
                    edges.add(edge);
                }
            }
        }
        return new StarAggJoinPlan(blockingReasons, edges, joinGraph.size());
    }

    private static StarAggJoinEdge parseJoinEdge(int joinIndex,
                                                 String joinType,
                                                 String condition,
                                                 StarAggRelationCatalog relationCatalog) {
        int equalsIndex = topLevelEqualsIndex(condition);
        if (equalsIndex <= 0 || equalsIndex >= condition.length() - 1) {
            return StarAggJoinEdge.blocked(reason("NON_EQUI_JOIN_STAR_AGG_UNSUPPORTED", "Join 条件不是简单等值表达式，AMV-007 默认阻断。"));
        }
        String leftKey = stripOuterParentheses(condition.substring(0, equalsIndex).trim());
        String rightKey = stripOuterParentheses(condition.substring(equalsIndex + 1).trim());
        if (!isIdentifierReference(leftKey) || !isIdentifierReference(rightKey)) {
            return StarAggJoinEdge.blocked(reason("COMPLEX_JOIN_KEY_STAR_AGG_UNSUPPORTED", "Join key 包含函数、CAST 或复杂表达式，AMV-007 默认阻断。"));
        }
        StarAggRelationSpec leftRelation = relationCatalog.relationForColumn(leftKey);
        StarAggRelationSpec rightRelation = relationCatalog.relationForColumn(rightKey);
        if (leftRelation == null || rightRelation == null || leftRelation.key.equals(rightRelation.key)) {
            return StarAggJoinEdge.blocked(reason("JOIN_KEY_SIDE_UNRESOLVED", "Join key 两侧无法按表别名或限定名消歧，不能生成 STAR_AGG_MV。"));
        }
        return StarAggJoinEdge.generated(joinIndex, joinType, condition, leftKey, rightKey, leftRelation, rightRelation);
    }

    private static boolean blockedJoinType(String joinType, List<Map<String, Object>> blockingReasons) {
        if ("CROSS".equals(joinType)) {
            blockingReasons.add(reason("CROSS_JOIN_STAR_AGG_UNSUPPORTED", "CROSS JOIN 可能产生笛卡尔积，AMV-007 不生成 STAR_AGG_MV。"));
            return true;
        }
        if (isOuterJoin(joinType)) {
            blockingReasons.add(reason("OUTER_JOIN_STAR_AGG_UNSUPPORTED", "OUTER JOIN 的空值补齐语义需要人工复核，AMV-007 默认阻断。"));
            return true;
        }
        if (!isInnerLikeJoin(joinType)) {
            blockingReasons.add(reason("UNSUPPORTED_JOIN_TYPE_STAR_AGG", "STAR_AGG_MV 只允许 INNER/SIMPLE 等值 Join。"));
            return true;
        }
        return false;
    }

    private static boolean isOuterJoin(String joinType) {
        return joinType.contains("LEFT")
            || joinType.contains("RIGHT")
            || joinType.contains("FULL")
            || joinType.contains("OUTER");
    }

    private static boolean isInnerLikeJoin(String joinType) {
        return "INNER".equals(joinType) || "SIMPLE".equals(joinType) || "JOIN".equals(joinType);
    }
}
