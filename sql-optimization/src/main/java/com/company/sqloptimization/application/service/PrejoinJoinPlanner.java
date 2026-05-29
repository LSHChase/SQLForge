package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.qualifier;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.relationKey;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.firstText;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.reason;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.text;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.upperText;
import static com.company.sqloptimization.application.service.PrejoinSqlJoinTableScanner.joinRightTables;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.splitAndConditions;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.stripOuterParentheses;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.topLevelEqualsIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class PrejoinJoinPlanner {

    private PrejoinJoinPlanner() {
    }

    static PrejoinJoinPlan joinPlan(List<Map<String, Object>> joinGraph,
                                    List<Map<String, Object>> baseTables,
                                    String sourceSql) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> joinKeys = new ArrayList<Map<String, Object>>();
        List<PrejoinJoinClause> joinClauses = new ArrayList<PrejoinJoinClause>();
        Map<String, PrejoinRelationSpec> relationByName = relationByName(relationSpecs(baseTables));
        List<Map<String, Object>> sqlJoinTables = joinRightTables(sourceSql);

        int joinIndex = 0;
        for (Map<String, Object> join : joinGraph) {
            joinIndex++;
            Map<String, Object> sequentialRightTable = sequentialRightTable(baseTables, sqlJoinTables, joinIndex);
            String joinType = upperText(join.get("joinType"));
            if (!isSupportedJoinType(joinType, blockingReasons)) {
                continue;
            }
            String condition = text(join.get("condition"));
            if (!StringUtils.hasText(condition)) {
                blockingReasons.add(reason("JOIN_CONDITION_REQUIRED", "缺少 ON 等值条件，不能生成 PREJOIN_MV。"));
                continue;
            }
            PrejoinRelationSpec rightRelation = rightRelation(join, sequentialRightTable, relationByName);
            List<String> conditions = splitAndConditions(condition);
            if (conditions.isEmpty()) {
                blockingReasons.add(reason("JOIN_CONDITION_REQUIRED", "缺少可解析的 ON 等值条件，不能生成 PREJOIN_MV。"));
                continue;
            }
            collectJoinKeys(joinKeys, blockingReasons, conditions, join, joinIndex, baseTables, sequentialRightTable, rightRelation);
            joinClauses.add(new PrejoinJoinClause(join, condition, sequentialRightTable));
        }
        return new PrejoinJoinPlan(blockingReasons, joinKeys, joinClauses);
    }

    private static Map<String, Object> sequentialRightTable(List<Map<String, Object>> baseTables,
                                                           List<Map<String, Object>> sqlJoinTables,
                                                           int joinIndex) {
        Map<String, Object> sequentialRightTable = joinIndex < baseTables.size()
            ? baseTables.get(joinIndex)
            : Collections.<String, Object>emptyMap();
        if (!hasTableName(sequentialRightTable) && joinIndex - 1 < sqlJoinTables.size()) {
            sequentialRightTable = sqlJoinTables.get(joinIndex - 1);
        }
        return sequentialRightTable;
    }

    private static boolean isSupportedJoinType(String joinType, List<Map<String, Object>> blockingReasons) {
        if ("CROSS".equals(joinType)) {
            blockingReasons.add(reason("CROSS_JOIN_PREJOIN_UNSUPPORTED", "CROSS JOIN 可能产生笛卡尔积，AMV-006 不生成可激活 PREJOIN_MV。"));
            return false;
        }
        if (isOuterJoin(joinType)) {
            blockingReasons.add(reason("OUTER_JOIN_PREJOIN_UNSUPPORTED", "OUTER JOIN 的空值补齐语义需要人工复核，AMV-006 默认阻断。"));
            return false;
        }
        if (!isInnerLikeJoin(joinType)) {
            blockingReasons.add(reason("UNSUPPORTED_JOIN_TYPE_PREJOIN", "PREJOIN_MV 只允许 INNER/SIMPLE 等值 Join。"));
            return false;
        }
        return true;
    }

    private static PrejoinRelationSpec rightRelation(Map<String, Object> join,
                                                     Map<String, Object> sequentialRightTable,
                                                     Map<String, PrejoinRelationSpec> relationByName) {
        PrejoinRelationSpec rightRelation = relationByName.get(relationKey(firstText(
            text(join.get("rightAlias")),
            text(join.get("right"))
        )));
        if (rightRelation == null) {
            rightRelation = relationByName.get(relationKey(text(join.get("right"))));
        }
        if (rightRelation == null && !sequentialRightTable.isEmpty()) {
            rightRelation = new PrejoinRelationSpec(
                text(sequentialRightTable.get("tableName")),
                text(sequentialRightTable.get("alias"))
            );
        }
        return rightRelation;
    }

    private static void collectJoinKeys(List<Map<String, Object>> joinKeys,
                                        List<Map<String, Object>> blockingReasons,
                                        List<String> conditions,
                                        Map<String, Object> join,
                                        int joinIndex,
                                        List<Map<String, Object>> baseTables,
                                        Map<String, Object> sequentialRightTable,
                                        PrejoinRelationSpec rightRelation) {
        for (String conditionPart : conditions) {
            PrejoinJoinKey joinKey = parseJoinKey(conditionPart, rightRelation);
            if (joinKey.blockingReason != null) {
                blockingReasons.add(joinKey.blockingReason);
                continue;
            }
            LinkedHashMap<String, Object> keyEvidence = new LinkedHashMap<String, Object>();
            keyEvidence.put("joinIndex", Integer.valueOf(joinIndex));
            keyEvidence.put("joinType", "INNER");
            keyEvidence.put("leftRelation", firstText(text(join.get("left")), firstTableName(baseTables)));
            keyEvidence.put("rightRelation", firstText(text(join.get("right")), text(sequentialRightTable.get("tableName"))));
            keyEvidence.put("condition", conditionPart);
            keyEvidence.put("leftKey", joinKey.leftKey);
            keyEvidence.put("rightKey", joinKey.rightKey);
            joinKeys.add(keyEvidence);
        }
    }

    private static PrejoinJoinKey parseJoinKey(String condition, PrejoinRelationSpec rightRelation) {
        int equalsIndex = topLevelEqualsIndex(condition);
        if (equalsIndex <= 0 || equalsIndex >= condition.length() - 1) {
            return PrejoinJoinKey.blocked(reason("NON_EQUI_JOIN_PREJOIN_UNSUPPORTED", "Join 条件不是简单等值表达式，AMV-006 默认阻断。"));
        }
        String left = stripOuterParentheses(condition.substring(0, equalsIndex).trim());
        String right = stripOuterParentheses(condition.substring(equalsIndex + 1).trim());
        if (!isIdentifierReference(left) || !isIdentifierReference(right)) {
            return PrejoinJoinKey.blocked(reason("COMPLEX_JOIN_KEY_PREJOIN_UNSUPPORTED", "Join key 包含函数、CAST 或复杂表达式，AMV-006 默认阻断。"));
        }
        if (!StringUtils.hasText(qualifier(left)) || !StringUtils.hasText(qualifier(right))) {
            return PrejoinJoinKey.blocked(reason("JOIN_KEY_SIDE_UNRESOLVED", "Join key 缺少表别名或限定名，无法在多表 Join 中证明字段归属。"));
        }
        boolean leftIsRightRelation = rightRelation != null && rightRelation.owns(left);
        boolean rightIsRightRelation = rightRelation != null && rightRelation.owns(right);
        if (leftIsRightRelation == rightIsRightRelation) {
            return PrejoinJoinKey.blocked(reason("JOIN_KEY_SIDE_UNRESOLVED", "Join key 两侧无法按 Join 右表消歧，不能生成 PREJOIN_MV。"));
        }
        return leftIsRightRelation ? PrejoinJoinKey.generated(right, left) : PrejoinJoinKey.generated(left, right);
    }

    private static boolean hasTableName(Map<String, Object> table) {
        return table != null && StringUtils.hasText(text(table.get("tableName")));
    }

    private static String firstTableName(List<Map<String, Object>> tables) {
        return tables == null || tables.isEmpty() ? "" : text(tables.get(0).get("tableName"));
    }

    private static List<PrejoinRelationSpec> relationSpecs(List<Map<String, Object>> tables) {
        List<PrejoinRelationSpec> result = new ArrayList<PrejoinRelationSpec>();
        for (Map<String, Object> table : tables) {
            result.add(new PrejoinRelationSpec(text(table.get("tableName")), text(table.get("alias"))));
        }
        return result;
    }

    private static Map<String, PrejoinRelationSpec> relationByName(List<PrejoinRelationSpec> relations) {
        LinkedHashMap<String, PrejoinRelationSpec> result = new LinkedHashMap<String, PrejoinRelationSpec>();
        for (PrejoinRelationSpec relation : relations) {
            for (String reference : relation.references) {
                result.put(relationKey(reference), relation);
            }
        }
        return result;
    }

    private static boolean isOuterJoin(String joinType) {
        return joinType.contains("LEFT") || joinType.contains("RIGHT") || joinType.contains("FULL") || joinType.contains("OUTER");
    }

    private static boolean isInnerLikeJoin(String joinType) {
        return "INNER".equals(joinType) || "SIMPLE".equals(joinType) || "JOIN".equals(joinType);
    }
}
