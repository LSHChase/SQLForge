package com.company.sqloptimization.application.service;

import java.util.Collections;
import org.apache.calcite.sql.SqlNode;
import org.springframework.util.StringUtils;

final class CommonSubgraphSqlRewriter {

    private CommonSubgraphSqlRewriter() {
    }

    static String rewriteSql(String sourceSql, CommonSubgraphCandidate candidate, String mvName) {
        String rewritten = rewriteSqlWithEvidence(sourceSql, candidate, mvName).sql;
        return StringUtils.hasText(rewritten) ? rewritten + ";" : "";
    }

    static CommonSubgraphRewriteResult rewriteSqlWithEvidence(String sourceSql,
                                                              CommonSubgraphCandidate candidate,
                                                              String mvName) {
        if (!StringUtils.hasText(sourceSql) || candidate == null || !StringUtils.hasText(mvName)) {
            return new CommonSubgraphRewriteResult("", Collections.<java.util.Map<String, Object>>emptyList());
        }
        CommonSubgraphRewriteState state = new CommonSubgraphRewriteState(candidate);
        try {
            SqlNode statement = CommonSubgraphSqlParser.parseStatement(sourceSql);
            SqlNode rewritten = "CTE".equals(candidate.sourceKind)
                ? CommonSubgraphCteSqlRewriter.rewriteCteCandidateByAst(statement, candidate, mvName, state)
                : CommonSubgraphDerivedAstRewriter.rewriteDerivedSourcesByAst(statement, candidate, mvName, state);
            if (!state.replaced || rewritten == null) {
                return new CommonSubgraphRewriteResult("", state.rewriteAttempts);
            }
            return new CommonSubgraphRewriteResult(
                normalizeCalciteRenderedRewrite(CommonSubgraphSqlText.trimTrailingSemicolon(rewritten.toString())),
                state.rewriteAttempts
            );
        } catch (RuntimeException ex) {
            state.recordError(ex);
            return new CommonSubgraphRewriteResult("", state.rewriteAttempts);
        }
    }

    private static String normalizeCalciteRenderedRewrite(String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return "";
        }
        String normalized = rewriteSql.replaceAll("`([A-Za-z_][A-Za-z0-9_$]*)`", "$1");
        normalized = normalized.replaceAll(
            "(?is)\\bFETCH\\s+NEXT\\s+(\\d+)\\s+ROWS\\s+ONLY\\b",
            "LIMIT $1"
        );
        normalized = normalized.replaceAll(
            "(?i)\\b(FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_$]*(?:\\.[A-Za-z_][A-Za-z0-9_$]*)?)\\s+AS\\s+([A-Za-z_][A-Za-z0-9_$]*)\\b",
            "$1 $2 $3"
        );
        return normalized;
    }
}
