package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.calcite.sql.SqlNode;
import org.springframework.util.StringUtils;

final class CommonSubgraphRewriteState {

    final CommonSubgraphCandidate candidate;
    final List<Map<String, Object>> rewriteAttempts = new ArrayList<Map<String, Object>>();
    boolean replaced;

    CommonSubgraphRewriteState(CommonSubgraphCandidate candidate) {
        this.candidate = candidate;
    }

    void recordDerivedAttempt(SqlNode relation, SqlNode alias) {
        if (candidate == null || rewriteAttempts.size() >= 12) {
            return;
        }
        String candidateAlias = CommonSubgraphSqlText.normalizeIdentifier(
            CommonSubgraphSqlText.firstText(candidate.alias, candidate.sourceName)
        );
        String actualAlias = CommonSubgraphSqlText.normalizeIdentifier(alias == null ? "" : alias.toString());
        String relationSql = relation == null ? "" : CommonSubgraphSqlText.normalizeSubgraphSql(relation.toString());
        String relationFingerprint = CommonSubgraphFingerprint.subgraphFingerprint(relationSql);
        String candidateFingerprint = CommonSubgraphFingerprint.subgraphFingerprint(candidate.subgraphSql);
        LinkedHashMap<String, Object> attempt = new LinkedHashMap<String, Object>();
        attempt.put("actualAlias", CommonSubgraphSqlText.cleanIdentifier(alias == null ? "" : alias.toString()));
        attempt.put(
            "candidateAlias",
            CommonSubgraphSqlText.cleanIdentifier(CommonSubgraphSqlText.firstText(candidate.alias, candidate.sourceName))
        );
        attempt.put("aliasMatched", Boolean.valueOf(!StringUtils.hasText(candidateAlias) || candidateAlias.equals(actualAlias)));
        attempt.put("relationKind", relation == null ? "" : String.valueOf(relation.getKind()));
        attempt.put("relationClass", relation == null ? "" : relation.getClass().getSimpleName());
        attempt.put("selectLike", Boolean.valueOf(CommonSubgraphSqlParser.isSelectLike(relation)));
        attempt.put("relationFingerprint", relationFingerprint);
        attempt.put("candidateFingerprint", candidateFingerprint);
        attempt.put("fingerprintEqual", Boolean.valueOf(candidateFingerprint.equals(relationFingerprint)));
        attempt.put(
            "aliasCoverageReplaceable",
            Boolean.valueOf(CommonSubgraphReplacementMatcher.canReplaceByAliasCoverage(relationSql, candidate))
        );
        attempt.put("relationSqlPrefix", relationSql.length() <= 160 ? relationSql : relationSql.substring(0, 160));
        rewriteAttempts.add(attempt);
    }

    void recordError(RuntimeException ex) {
        if (rewriteAttempts.size() >= 12) {
            return;
        }
        LinkedHashMap<String, Object> attempt = new LinkedHashMap<String, Object>();
        attempt.put("error", ex == null ? "" : ex.getClass().getSimpleName());
        attempt.put("message", ex == null ? "" : ex.getMessage());
        rewriteAttempts.add(attempt);
    }
}
