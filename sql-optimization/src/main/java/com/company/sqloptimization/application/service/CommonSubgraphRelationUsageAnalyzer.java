package com.company.sqloptimization.application.service;

import java.util.List;
import java.util.Set;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlCall;

final class CommonSubgraphRelationUsageAnalyzer {

    private CommonSubgraphRelationUsageAnalyzer() {
    }

    static CommonSubgraphRelationUsageScope relationUsageScope(SqlNode from,
                                                               CommonSubgraphCandidate candidate,
                                                               Set<String> relationNames) {
        CommonSubgraphRelationUsageScope scope = new CommonSubgraphRelationUsageScope();
        collectRelationUsageScope(from, candidate, relationNames, scope);
        return scope;
    }

    private static void collectRelationUsageScope(SqlNode from,
                                                  CommonSubgraphCandidate candidate,
                                                  Set<String> relationNames,
                                                  CommonSubgraphRelationUsageScope scope) {
        if (from == null || scope == null) {
            return;
        }
        if (from instanceof SqlIdentifier) {
            scope.sourceCount++;
            if (identifierMatchesAnyRelation((SqlIdentifier) from, relationNames)) {
                CommonSubgraphSqlText.addIfText(
                    scope.candidateAliases,
                    CommonSubgraphSqlText.normalizeIdentifier(
                        CommonSubgraphSqlIdentifierSupport.identifierTail((SqlIdentifier) from)
                    )
                );
            }
            return;
        }
        if (from instanceof SqlJoin) {
            collectRelationUsageScope(((SqlJoin) from).getLeft(), candidate, relationNames, scope);
            collectRelationUsageScope(((SqlJoin) from).getRight(), candidate, relationNames, scope);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            collectAliasedRelationUsage((SqlBasicCall) from, candidate, relationNames, scope);
            return;
        }
        if (from instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                collectRelationUsageScope(operand, candidate, relationNames, scope);
            }
        }
    }

    private static void collectAliasedRelationUsage(SqlBasicCall from,
                                                    CommonSubgraphCandidate candidate,
                                                    Set<String> relationNames,
                                                    CommonSubgraphRelationUsageScope scope) {
        List<SqlNode> operands = from.getOperandList();
        if (operands.size() < 2) {
            return;
        }
        scope.sourceCount++;
        SqlNode relation = operands.get(0);
        SqlNode alias = operands.get(1);
        if (!relationAliasMatchesCandidate(relation, alias, candidate, relationNames)) {
            return;
        }
        CommonSubgraphSqlText.addIfText(
            scope.candidateAliases,
            CommonSubgraphSqlText.normalizeIdentifier(alias == null ? "" : alias.toString())
        );
        if (relation instanceof SqlIdentifier) {
            CommonSubgraphSqlText.addIfText(
                scope.candidateAliases,
                CommonSubgraphSqlText.normalizeIdentifier(
                    CommonSubgraphSqlIdentifierSupport.identifierTail((SqlIdentifier) relation)
                )
            );
        }
    }

    private static boolean relationAliasMatchesCandidate(SqlNode relation,
                                                         SqlNode alias,
                                                         CommonSubgraphCandidate candidate,
                                                         Set<String> relationNames) {
        String normalizedAlias = CommonSubgraphSqlText.normalizeIdentifier(alias == null ? "" : alias.toString());
        if ("DERIVED_TABLE".equals(candidate.sourceKind)
            && CommonSubgraphSqlParser.isSelectLike(relation)
            && relationNames.contains(normalizedAlias)) {
            return true;
        }
        if (relation instanceof SqlIdentifier && identifierMatchesAnyRelation((SqlIdentifier) relation, relationNames)) {
            return true;
        }
        return relationNames.contains(normalizedAlias) && CommonSubgraphSqlParser.isSelectLike(relation);
    }

    private static boolean identifierMatchesAnyRelation(SqlIdentifier identifier, Set<String> relationNames) {
        if (identifier == null || relationNames == null || relationNames.isEmpty()) {
            return false;
        }
        String actual = CommonSubgraphSqlText.normalizeIdentifier(identifier.toString());
        String tail = CommonSubgraphSqlText.normalizeIdentifier(
            CommonSubgraphSqlIdentifierSupport.identifierTail(identifier)
        );
        return relationNames.contains(actual) || relationNames.contains(tail);
    }
}
