package com.company.sqloptimization.application.service;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.parser.SqlParserPos;

final class CommonSubgraphCteSqlRewriter {

    private CommonSubgraphCteSqlRewriter() {
    }

    static SqlNode rewriteCteCandidateByAst(SqlNode statement,
                                            CommonSubgraphCandidate candidate,
                                            String mvName,
                                            CommonSubgraphRewriteState state) {
        SqlNode rewritten = CommonSubgraphRelationAstRewriter.rewriteRelationReferencesByAst(
            statement,
            candidate.sourceName,
            mvName,
            state
        );
        if (rewritten instanceof SqlOrderBy) {
            return rewriteOrderByCte((SqlOrderBy) rewritten, candidate);
        }
        if (!(rewritten instanceof SqlWith)) {
            return rewritten;
        }
        return CommonSubgraphCtePruner.pruneMaterializedAndUnusedCtes(
            (SqlWith) rewritten,
            candidate.materializedCteNames
        );
    }

    private static SqlNode rewriteOrderByCte(SqlOrderBy orderBy, CommonSubgraphCandidate candidate) {
        if (!(orderBy.query instanceof SqlWith)) {
            return orderBy;
        }
        SqlNode prunedQuery = CommonSubgraphCtePruner.pruneMaterializedAndUnusedCtes(
            (SqlWith) orderBy.query,
            candidate.materializedCteNames
        );
        return prunedQuery == orderBy.query ? orderBy : new SqlOrderBy(
            orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
            prunedQuery,
            orderBy.orderList,
            orderBy.offset,
            orderBy.fetch
        );
    }
}
