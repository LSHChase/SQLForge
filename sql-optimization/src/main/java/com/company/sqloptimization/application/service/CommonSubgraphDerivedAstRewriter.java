package com.company.sqloptimization.application.service;

import java.util.List;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.springframework.util.StringUtils;

final class CommonSubgraphDerivedAstRewriter {

    private CommonSubgraphDerivedAstRewriter() {
    }

    static SqlNode rewriteDerivedSourcesByAst(SqlNode node,
                                              CommonSubgraphCandidate candidate,
                                              String mvName,
                                              CommonSubgraphRewriteState state) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            SqlNode rewrittenQuery = rewriteDerivedSourcesByAst(orderBy.query, candidate, mvName, state);
            return rewrittenQuery == orderBy.query ? orderBy : new SqlOrderBy(
                orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                rewrittenQuery,
                orderBy.orderList,
                orderBy.offset,
                orderBy.fetch
            );
        }
        if (node instanceof SqlWith) {
            rewriteWithDerivedSources((SqlWith) node, candidate, mvName, state);
            return node;
        }
        if (node instanceof SqlSelect) {
            rewriteSelectDerivedSources((SqlSelect) node, candidate, mvName, state);
            return node;
        }
        if (node instanceof SqlCall) {
            rewriteDerivedCall((SqlCall) node, candidate, mvName, state);
        }
        return node;
    }

    private static SqlNode rewriteDerivedInFrom(SqlNode from,
                                                CommonSubgraphCandidate candidate,
                                                String mvName,
                                                CommonSubgraphRewriteState state) {
        if (from == null) {
            return null;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            join.setLeft(rewriteDerivedInFrom(join.getLeft(), candidate, mvName, state));
            join.setRight(rewriteDerivedInFrom(join.getRight(), candidate, mvName, state));
            return join;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            return rewriteAliasedDerivedInFrom((SqlBasicCall) from, candidate, mvName, state);
        }
        if (from instanceof SqlCall) {
            rewriteDerivedInFromCall((SqlCall) from, candidate, mvName, state);
        }
        return from;
    }

    private static void rewriteWithDerivedSources(SqlWith with,
                                                  CommonSubgraphCandidate candidate,
                                                  String mvName,
                                                  CommonSubgraphRewriteState state) {
        if (with.withList != null) {
            for (SqlNode item : with.withList.getList()) {
                if (item instanceof SqlWithItem) {
                    ((SqlWithItem) item).query = rewriteDerivedSourcesByAst(
                        ((SqlWithItem) item).query,
                        candidate,
                        mvName,
                        state
                    );
                }
            }
        }
        with.body = rewriteDerivedSourcesByAst(with.body, candidate, mvName, state);
    }

    private static void rewriteSelectDerivedSources(SqlSelect select,
                                                    CommonSubgraphCandidate candidate,
                                                    String mvName,
                                                    CommonSubgraphRewriteState state) {
        select.setFrom(rewriteDerivedInFrom(select.getFrom(), candidate, mvName, state));
        rewriteDerivedChildren(select.getSelectList(), candidate, mvName, state);
        rewriteDerivedSourcesByAst(select.getWhere(), candidate, mvName, state);
        rewriteDerivedSourcesByAst(select.getHaving(), candidate, mvName, state);
        rewriteDerivedChildren(select.getOrderList(), candidate, mvName, state);
    }

    private static SqlNode rewriteAliasedDerivedInFrom(SqlBasicCall from,
                                                       CommonSubgraphCandidate candidate,
                                                       String mvName,
                                                       CommonSubgraphRewriteState state) {
        List<SqlNode> operands = from.getOperandList();
        if (operands.size() < 2) {
            return from;
        }
        SqlNode relation = operands.get(0);
        SqlNode alias = operands.get(1);
        state.recordDerivedAttempt(relation, alias);
        if (derivedCandidateMatches(relation, alias, candidate)) {
            state.replaced = true;
            return CommonSubgraphRewriteAstSupport.aliasedMvReference(mvName, alias);
        }
        SqlNode rewrittenRelation = rewriteDerivedSourcesByAst(relation, candidate, mvName, state);
        if (rewrittenRelation == relation) {
            return from;
        }
        return SqlStdOperatorTable.AS.createCall(
            from.getParserPosition() == null ? SqlParserPos.ZERO : from.getParserPosition(),
            rewrittenRelation,
            alias
        );
    }

    private static boolean derivedCandidateMatches(SqlNode relation, SqlNode alias, CommonSubgraphCandidate candidate) {
        if (candidate == null || relation == null || !CommonSubgraphSqlParser.isSelectLike(relation)) {
            return false;
        }
        String candidateAlias = CommonSubgraphSqlText.normalizeIdentifier(
            CommonSubgraphSqlText.firstText(candidate.alias, candidate.sourceName)
        );
        String actualAlias = CommonSubgraphSqlText.normalizeIdentifier(alias == null ? "" : alias.toString());
        String relationSql = CommonSubgraphSqlText.normalizeSubgraphSql(relation.toString());
        boolean structuralMatch = CommonSubgraphFingerprint.subgraphFingerprint(candidate.subgraphSql)
            .equals(CommonSubgraphFingerprint.subgraphFingerprint(relationSql));
        if (structuralMatch) {
            return true;
        }
        boolean aliasMatches = !StringUtils.hasText(candidateAlias) || candidateAlias.equals(actualAlias);
        return aliasMatches && CommonSubgraphReplacementMatcher.canReplaceByAliasCoverage(relationSql, candidate);
    }

    private static void rewriteDerivedCall(SqlCall call,
                                           CommonSubgraphCandidate candidate,
                                           String mvName,
                                           CommonSubgraphRewriteState state) {
        List<SqlNode> operands = call.getOperandList();
        for (int index = 0; index < operands.size(); index++) {
            SqlNode rewritten = rewriteDerivedSourcesByAst(operands.get(index), candidate, mvName, state);
            if (rewritten != operands.get(index)) {
                CommonSubgraphRewriteAstSupport.setOperandIfPossible(call, index, rewritten);
            }
        }
    }

    private static void rewriteDerivedInFromCall(SqlCall call,
                                                 CommonSubgraphCandidate candidate,
                                                 String mvName,
                                                 CommonSubgraphRewriteState state) {
        List<SqlNode> operands = call.getOperandList();
        for (int index = 0; index < operands.size(); index++) {
            SqlNode rewritten = rewriteDerivedInFrom(operands.get(index), candidate, mvName, state);
            if (rewritten != operands.get(index)) {
                CommonSubgraphRewriteAstSupport.setOperandIfPossible(call, index, rewritten);
            }
        }
    }

    private static void rewriteDerivedChildren(SqlNodeList nodes,
                                               CommonSubgraphCandidate candidate,
                                               String mvName,
                                               CommonSubgraphRewriteState state) {
        if (nodes == null) {
            return;
        }
        for (SqlNode node : nodes.getList()) {
            rewriteDerivedSourcesByAst(node, candidate, mvName, state);
        }
    }
}
