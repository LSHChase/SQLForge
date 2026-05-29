package com.company.sqloptimization.application.service;

import java.util.Collections;
import java.util.LinkedHashSet;
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
import org.springframework.util.StringUtils;

final class CommonSubgraphColumnExpressionCollector {

    private CommonSubgraphColumnExpressionCollector() {
    }

    static void collectJoinConditionColumns(SqlNode from, Set<String> candidateAliases, Set<String> result) {
        if (from == null) {
            return;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            collectColumnsFromExpression(
                join.getCondition(),
                candidateAliases,
                false,
                Collections.<String>emptySet(),
                result
            );
            collectJoinConditionColumns(join.getLeft(), candidateAliases, result);
            collectJoinConditionColumns(join.getRight(), candidateAliases, result);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            return;
        }
        if (from instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                collectJoinConditionColumns(operand, candidateAliases, result);
            }
        }
    }

    static void collectColumnsFromExpression(SqlNode node,
                                             Set<String> candidateAliases,
                                             boolean includeUnqualified,
                                             Set<String> unqualifiedIgnore,
                                             Set<String> result) {
        if (node == null || result == null) {
            return;
        }
        if (node instanceof SqlIdentifier) {
            collectIdentifierColumn((SqlIdentifier) node, candidateAliases, includeUnqualified, unqualifiedIgnore, result);
            return;
        }
        if (node instanceof SqlSelect || node instanceof SqlWith || node instanceof SqlOrderBy) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectColumnsFromExpression(item, candidateAliases, includeUnqualified, unqualifiedIgnore, result);
            }
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            collectAliasedExpression((SqlBasicCall) node, candidateAliases, includeUnqualified, unqualifiedIgnore, result);
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectColumnsFromExpression(operand, candidateAliases, includeUnqualified, unqualifiedIgnore, result);
            }
        }
    }

    static Set<String> selectAliases(SqlNodeList selectList) {
        if (selectList == null) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (SqlNode item : selectList.getList()) {
            CommonSubgraphSqlText.addIfText(
                result,
                CommonSubgraphSqlText.normalizeIdentifier(CommonSubgraphSqlIdentifierSupport.aliasName(item))
            );
        }
        return result;
    }

    private static void collectIdentifierColumn(SqlIdentifier identifier,
                                                Set<String> candidateAliases,
                                                boolean includeUnqualified,
                                                Set<String> unqualifiedIgnore,
                                                Set<String> result) {
        if (identifier.isStar()) {
            return;
        }
        if (identifier.names != null && identifier.names.size() >= 2) {
            String qualifier = CommonSubgraphSqlText.normalizeIdentifier(identifier.names.get(0));
            if (candidateAliases != null && candidateAliases.contains(qualifier)) {
                String column = CommonSubgraphSqlText.normalizeIdentifier(
                    identifier.names.get(identifier.names.size() - 1)
                );
                if (!"*".equals(column)) {
                    result.add(column);
                }
            }
            return;
        }
        if (!includeUnqualified) {
            return;
        }
        String column = CommonSubgraphSqlText.normalizeIdentifier(
            CommonSubgraphSqlIdentifierSupport.identifierTail(identifier)
        );
        if (!StringUtils.hasText(column)
            || (candidateAliases != null && candidateAliases.contains(column))
            || (unqualifiedIgnore != null && unqualifiedIgnore.contains(column))) {
            return;
        }
        result.add(column);
    }

    private static void collectAliasedExpression(SqlBasicCall node,
                                                 Set<String> candidateAliases,
                                                 boolean includeUnqualified,
                                                 Set<String> unqualifiedIgnore,
                                                 Set<String> result) {
        java.util.List<SqlNode> operands = node.getOperandList();
        if (!operands.isEmpty()) {
            collectColumnsFromExpression(
                operands.get(0),
                candidateAliases,
                includeUnqualified,
                unqualifiedIgnore,
                result
            );
        }
    }
}
