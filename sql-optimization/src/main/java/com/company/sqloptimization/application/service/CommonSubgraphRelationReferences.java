package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.springframework.util.StringUtils;

final class CommonSubgraphRelationReferences {

    private CommonSubgraphRelationReferences() {
    }

    static boolean relationReferenced(String sql, String relationName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return false;
        }
        try {
            return relationReferenced(CommonSubgraphSqlParser.parseStatement(sql), relationName);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    static Set<String> originalBaseSources(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> sources = new LinkedHashSet<String>();
        if (advancedStructureProfile == null) {
            return sources;
        }
        for (Map<String, Object> table : mapList(advancedStructureProfile.get("tables"))) {
            String sourceType = CommonSubgraphSqlText.text(table.get("sourceType"));
            if (StringUtils.hasText(sourceType) && !"BASE_TABLE".equalsIgnoreCase(sourceType)) {
                continue;
            }
            CommonSubgraphRelationName.addRelationKey(sources, CommonSubgraphSqlText.text(table.get("tableName")));
        }
        return sources;
    }

    static boolean accessesOriginalSources(String sql, Set<String> originalSources) {
        if (!StringUtils.hasText(sql) || originalSources == null || originalSources.isEmpty()) {
            return false;
        }
        for (String relation : fromJoinRelations(sql)) {
            if ("__UNPARSEABLE_SQL__".equals(relation)) {
                return true;
            }
            String normalized = CommonSubgraphRelationName.relationKey(relation);
            String unqualified = CommonSubgraphRelationName.relationKey(
                CommonSubgraphRelationName.unqualifiedName(relation)
            );
            if (originalSources.contains(normalized) || originalSources.contains(unqualified)) {
                return true;
            }
        }
        return false;
    }

    static void collectRelationReferences(SqlNode node, Set<String> relations) {
        if (node == null || relations == null) {
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectRelationReferences(((SqlOrderBy) node).query, relations);
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        collectRelationReferences(((SqlWithItem) item).query, relations);
                    }
                }
            }
            collectRelationReferences(with.body, relations);
            return;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            collectFromRelationReferences(select.getFrom(), relations);
            collectRelationReferences(select.getWhere(), relations);
            collectRelationReferences(select.getHaving(), relations);
            collectRelationReferences(select.getSelectList(), relations);
            collectRelationReferences(select.getOrderList(), relations);
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRelationReferences(item, relations);
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRelationReferences(operand, relations);
            }
        }
    }

    private static boolean relationReferenced(SqlNode node, String relationName) {
        LinkedHashSet<String> relations = new LinkedHashSet<String>();
        collectRelationReferences(node, relations);
        String expected = CommonSubgraphRelationName.relationKey(relationName);
        String unqualified = CommonSubgraphRelationName.relationKey(
            CommonSubgraphRelationName.unqualifiedName(relationName)
        );
        return relations.contains(expected) || relations.contains(unqualified);
    }

    private static List<String> fromJoinRelations(String sql) {
        if (!StringUtils.hasText(sql)) {
            return Collections.emptyList();
        }
        try {
            LinkedHashSet<String> relations = new LinkedHashSet<String>();
            collectRelationReferences(CommonSubgraphSqlParser.parseStatement(sql), relations);
            return new ArrayList<String>(relations);
        } catch (RuntimeException ex) {
            return Collections.singletonList("__UNPARSEABLE_SQL__");
        }
    }

    private static void collectFromRelationReferences(SqlNode from, Set<String> relations) {
        if (from == null || relations == null) {
            return;
        }
        if (from instanceof SqlIdentifier) {
            addRelationReference(relations, from.toString());
            return;
        }
        if (from instanceof SqlJoin) {
            collectFromRelationReferences(((SqlJoin) from).getLeft(), relations);
            collectFromRelationReferences(((SqlJoin) from).getRight(), relations);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (!operands.isEmpty()) {
                SqlNode relation = operands.get(0);
                if (relation instanceof SqlIdentifier) {
                    addRelationReference(relations, relation.toString());
                } else {
                    collectRelationReferences(relation, relations);
                }
            }
            return;
        }
        if (from instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                collectFromRelationReferences(operand, relations);
            }
        }
    }

    private static void addRelationReference(Set<String> relations, String relation) {
        if (!StringUtils.hasText(relation)) {
            return;
        }
        CommonSubgraphRelationName.addRelationKey(relations, relation);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }
}
