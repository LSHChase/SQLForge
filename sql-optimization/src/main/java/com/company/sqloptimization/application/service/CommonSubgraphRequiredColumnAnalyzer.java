package com.company.sqloptimization.application.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;

final class CommonSubgraphRequiredColumnAnalyzer {

    private CommonSubgraphRequiredColumnAnalyzer() {
    }

    static Set<String> requiredColumns(String sourceSql, CommonSubgraphCandidate candidate) {
        LinkedHashSet<String> relationNames = new LinkedHashSet<String>();
        CommonSubgraphSqlText.addIfText(relationNames, CommonSubgraphSqlText.normalizeIdentifier(candidate.sourceName));
        CommonSubgraphSqlText.addIfText(relationNames, CommonSubgraphSqlText.normalizeIdentifier(candidate.alias));
        if (relationNames.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            LinkedHashSet<String> result = new LinkedHashSet<String>();
            collectRequiredColumns(CommonSubgraphSqlParser.parseStatement(sourceSql), candidate, relationNames, result);
            return result;
        } catch (RuntimeException ex) {
            return Collections.emptySet();
        }
    }

    private static void collectRequiredColumns(SqlNode node,
                                               CommonSubgraphCandidate candidate,
                                               Set<String> relationNames,
                                               Set<String> result) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRequiredColumns(item, candidate, relationNames, result);
            }
            return;
        }
        if (node instanceof SqlWith) {
            collectWithRequiredColumns((SqlWith) node, candidate, relationNames, result);
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectOrderByRequiredColumns((SqlOrderBy) node, candidate, relationNames, result);
            return;
        }
        if (node instanceof SqlSelect) {
            collectSelectRequiredColumns((SqlSelect) node, candidate, relationNames, result);
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == org.apache.calcite.sql.SqlKind.AS) {
            java.util.List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
            if (!operands.isEmpty()) {
                collectRequiredColumns(operands.get(0), candidate, relationNames, result);
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRequiredColumns(operand, candidate, relationNames, result);
            }
        }
    }

    private static void collectWithRequiredColumns(SqlWith with,
                                                   CommonSubgraphCandidate candidate,
                                                   Set<String> relationNames,
                                                   Set<String> result) {
        if (with.withList != null) {
            for (SqlNode item : with.withList.getList()) {
                if (item instanceof SqlWithItem) {
                    collectRequiredColumns(((SqlWithItem) item).query, candidate, relationNames, result);
                }
            }
        }
        collectRequiredColumns(with.body, candidate, relationNames, result);
    }

    private static void collectOrderByRequiredColumns(SqlOrderBy orderBy,
                                                      CommonSubgraphCandidate candidate,
                                                      Set<String> relationNames,
                                                      Set<String> result) {
        collectRequiredColumns(orderBy.query, candidate, relationNames, result);
        SqlSelect select = CommonSubgraphSqlParser.unwrapSelect(orderBy.query);
        if (select == null) {
            return;
        }
        CommonSubgraphRelationUsageScope scope =
            CommonSubgraphRelationUsageAnalyzer.relationUsageScope(select.getFrom(), candidate, relationNames);
        if (!scope.referencesCandidate()) {
            return;
        }
        CommonSubgraphColumnExpressionCollector.collectColumnsFromExpression(
            orderBy.orderList,
            scope.candidateAliases,
            scope.allowUnqualifiedColumns(),
            CommonSubgraphColumnExpressionCollector.selectAliases(select.getSelectList()),
            result
        );
    }

    private static void collectSelectRequiredColumns(SqlSelect select,
                                                     CommonSubgraphCandidate candidate,
                                                     Set<String> relationNames,
                                                     Set<String> result) {
        CommonSubgraphRelationUsageScope scope =
            CommonSubgraphRelationUsageAnalyzer.relationUsageScope(select.getFrom(), candidate, relationNames);
        if (scope.referencesCandidate()) {
            collectSelectExpressions(select, scope, result);
        }
        collectRequiredColumns(select.getSelectList(), candidate, relationNames, result);
        collectRequiredColumns(select.getFrom(), candidate, relationNames, result);
        collectRequiredColumns(select.getWhere(), candidate, relationNames, result);
        collectRequiredColumns(select.getGroup(), candidate, relationNames, result);
        collectRequiredColumns(select.getHaving(), candidate, relationNames, result);
        collectRequiredColumns(select.getOrderList(), candidate, relationNames, result);
    }

    private static void collectSelectExpressions(SqlSelect select,
                                                 CommonSubgraphRelationUsageScope scope,
                                                 Set<String> result) {
        collectScopedExpression(select.getSelectList(), scope, Collections.<String>emptySet(), result);
        collectScopedExpression(select.getWhere(), scope, Collections.<String>emptySet(), result);
        collectScopedExpression(select.getGroup(), scope, Collections.<String>emptySet(), result);
        collectScopedExpression(select.getHaving(), scope, Collections.<String>emptySet(), result);
        collectScopedExpression(
            select.getOrderList(),
            scope,
            CommonSubgraphColumnExpressionCollector.selectAliases(select.getSelectList()),
            result
        );
        CommonSubgraphColumnExpressionCollector.collectJoinConditionColumns(select.getFrom(), scope.candidateAliases, result);
    }

    private static void collectScopedExpression(SqlNode node,
                                                CommonSubgraphRelationUsageScope scope,
                                                Set<String> ignore,
                                                Set<String> result) {
        CommonSubgraphColumnExpressionCollector.collectColumnsFromExpression(
            node,
            scope.candidateAliases,
            scope.allowUnqualifiedColumns(),
            ignore,
            result
        );
    }
}
