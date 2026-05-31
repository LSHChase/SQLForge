package com.company.sqloptimization.application.service;

import org.apache.calcite.sql.SqlNode;
import org.springframework.util.StringUtils;

final class CommonSubgraphReplacementMatcher {

    private CommonSubgraphReplacementMatcher() {
    }

    static int replacementCount(String sourceSql, CommonSubgraphCandidate candidate) {
        if ("CTE".equals(candidate.sourceKind)) {
            return CommonSubgraphRelationReferenceCounter.relationReferenceCount(sourceSql, candidate.sourceName);
        }
        String alias = CommonSubgraphSqlText.firstText(candidate.alias, candidate.sourceName);
        if (!StringUtils.hasText(sourceSql) || !StringUtils.hasText(alias)) {
            return 0;
        }
        String candidateFingerprint = CommonSubgraphFingerprint.subgraphFingerprint(candidate.subgraphSql);
        int count = 0;
        for (int index = 0; index < sourceSql.length(); index++) {
            if (sourceSql.charAt(index) != '(') {
                continue;
            }
            int close = CommonSubgraphAliasScanner.matchingParen(sourceSql, index);
            if (close < 0) {
                continue;
            }
            CommonSubgraphAliasMatch anyAliasMatch = CommonSubgraphAliasScanner.aliasAfter(sourceSql, close + 1);
            if (!anyAliasMatch.matched) {
                continue;
            }
            String innerSql = sourceSql.substring(index + 1, close);
            if (candidateFingerprint.equals(CommonSubgraphFingerprint.subgraphFingerprint(innerSql))) {
                count++;
                index = anyAliasMatch.endIndex - 1;
                continue;
            }
            CommonSubgraphAliasMatch expectedAliasMatch =
                CommonSubgraphAliasScanner.aliasAfter(sourceSql, close + 1, alias);
            if (expectedAliasMatch.matched && canReplaceByAliasCoverage(innerSql, candidate)) {
                count++;
                index = expectedAliasMatch.endIndex - 1;
            }
        }
        return count;
    }

    static boolean canReplaceByAliasCoverage(String innerSql, CommonSubgraphCandidate candidate) {
        if (!"DERIVED_TABLE".equals(candidate.sourceKind) || !StringUtils.hasText(innerSql)) {
            return false;
        }
        String normalizedInner = CommonSubgraphSqlText.stripOuterParentheses(
            CommonSubgraphSqlText.trimTrailingSemicolon(innerSql)
        );
        if (!isReplacementEligibleSubquery(normalizedInner)
            || CommonSubgraphSqlText.hasOrderOrLimit(normalizedInner)) {
            return false;
        }
        CommonSubgraphOutputColumns innerColumns = CommonSubgraphOutputColumnAnalyzer.outputColumns(normalizedInner);
        CommonSubgraphOutputColumns candidateColumns =
            CommonSubgraphOutputColumnAnalyzer.outputColumns(candidate.subgraphSql);
        if (!innerColumns.blockingReasons.isEmpty()
            || !candidateColumns.blockingReasons.isEmpty()
            || innerColumns.normalizedColumns.isEmpty()
            || candidateColumns.normalizedColumns.isEmpty()) {
            return false;
        }
        return innerColumns.normalizedColumns.containsAll(candidateColumns.normalizedColumns)
            || candidateColumns.normalizedColumns.containsAll(innerColumns.normalizedColumns);
    }

    private static boolean isReplacementEligibleSubquery(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        try {
            SqlNode node = CommonSubgraphSqlParser.parseStatement(sql);
            return CommonSubgraphSqlParser.isSelectLike(node);
        } catch (RuntimeException ex) {
            return CommonSubgraphSqlText.startsWithWord(sql, 0, "SELECT");
        }
    }
}
