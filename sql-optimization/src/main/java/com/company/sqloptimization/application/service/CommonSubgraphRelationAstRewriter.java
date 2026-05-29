package com.company.sqloptimization.application.service;

import java.util.List;
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
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

final class CommonSubgraphRelationAstRewriter {

    private CommonSubgraphRelationAstRewriter() {
    }

    static SqlNode rewriteRelationReferencesByAst(SqlNode node,
                                                  String relationName,
                                                  String mvName,
                                                  CommonSubgraphRewriteState state) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            SqlNode rewrittenQuery = rewriteRelationReferencesByAst(orderBy.query, relationName, mvName, state);
            return rewrittenQuery == orderBy.query ? orderBy : new SqlOrderBy(
                orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                rewrittenQuery,
                orderBy.orderList,
                orderBy.offset,
                orderBy.fetch
            );
        }
        if (node instanceof SqlWith) {
            rewriteWithRelations((SqlWith) node, relationName, mvName, state);
            return node;
        }
        if (node instanceof SqlSelect) {
            rewriteSelectRelations((SqlSelect) node, relationName, mvName, state);
            return node;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode rewritten = rewriteRelationReferencesByAst(operands.get(index), relationName, mvName, state);
                if (rewritten != operands.get(index)) {
                    CommonSubgraphRewriteAstSupport.setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return node;
    }

    private static SqlNode rewriteRelationInFrom(SqlNode from,
                                                 String relationName,
                                                 String mvName,
                                                 CommonSubgraphRewriteState state) {
        if (from == null) {
            return null;
        }
        if (from instanceof SqlIdentifier && CommonSubgraphRelationName.relationNameMatches((SqlIdentifier) from, relationName)) {
            state.replaced = true;
            return CommonSubgraphRewriteAstSupport.aliasedMvReference(
                mvName,
                CommonSubgraphRelationName.unqualifiedName(relationName)
            );
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            join.setLeft(rewriteRelationInFrom(join.getLeft(), relationName, mvName, state));
            join.setRight(rewriteRelationInFrom(join.getRight(), relationName, mvName, state));
            return join;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            return rewriteAliasedRelationInFrom((SqlBasicCall) from, relationName, mvName, state);
        }
        if (from instanceof SqlCall) {
            rewriteRelationInFromCall((SqlCall) from, relationName, mvName, state);
        }
        return from;
    }

    private static void rewriteWithRelations(SqlWith with,
                                             String relationName,
                                             String mvName,
                                             CommonSubgraphRewriteState state) {
        if (with.withList != null) {
            for (SqlNode item : with.withList.getList()) {
                if (item instanceof SqlWithItem) {
                    ((SqlWithItem) item).query = rewriteRelationReferencesByAst(
                        ((SqlWithItem) item).query,
                        relationName,
                        mvName,
                        state
                    );
                }
            }
        }
        with.body = rewriteRelationReferencesByAst(with.body, relationName, mvName, state);
    }

    private static void rewriteSelectRelations(SqlSelect select,
                                               String relationName,
                                               String mvName,
                                               CommonSubgraphRewriteState state) {
        select.setFrom(rewriteRelationInFrom(select.getFrom(), relationName, mvName, state));
        rewriteChildQueries(select.getSelectList(), relationName, mvName, state);
        rewriteRelationReferencesByAst(select.getWhere(), relationName, mvName, state);
        rewriteRelationReferencesByAst(select.getHaving(), relationName, mvName, state);
        rewriteChildQueries(select.getOrderList(), relationName, mvName, state);
    }

    private static SqlNode rewriteAliasedRelationInFrom(SqlBasicCall from,
                                                        String relationName,
                                                        String mvName,
                                                        CommonSubgraphRewriteState state) {
        List<SqlNode> operands = from.getOperandList();
        if (operands.size() < 2) {
            return from;
        }
        SqlNode relation = operands.get(0);
        SqlNode alias = operands.get(1);
        if (relation instanceof SqlIdentifier && CommonSubgraphRelationName.relationNameMatches((SqlIdentifier) relation, relationName)) {
            state.replaced = true;
            return CommonSubgraphRewriteAstSupport.aliasedMvReference(mvName, alias);
        }
        SqlNode rewrittenRelation = rewriteRelationReferencesByAst(relation, relationName, mvName, state);
        if (rewrittenRelation == relation) {
            return from;
        }
        return SqlStdOperatorTable.AS.createCall(
            from.getParserPosition() == null ? SqlParserPos.ZERO : from.getParserPosition(),
            rewrittenRelation,
            alias
        );
    }

    private static void rewriteRelationInFromCall(SqlCall call,
                                                  String relationName,
                                                  String mvName,
                                                  CommonSubgraphRewriteState state) {
        List<SqlNode> operands = call.getOperandList();
        for (int index = 0; index < operands.size(); index++) {
            SqlNode rewritten = rewriteRelationInFrom(operands.get(index), relationName, mvName, state);
            if (rewritten != operands.get(index)) {
                CommonSubgraphRewriteAstSupport.setOperandIfPossible(call, index, rewritten);
            }
        }
    }

    private static void rewriteChildQueries(SqlNodeList nodes,
                                            String relationName,
                                            String mvName,
                                            CommonSubgraphRewriteState state) {
        if (nodes == null) {
            return;
        }
        for (SqlNode node : nodes.getList()) {
            rewriteRelationReferencesByAst(node, relationName, mvName, state);
        }
    }
}
