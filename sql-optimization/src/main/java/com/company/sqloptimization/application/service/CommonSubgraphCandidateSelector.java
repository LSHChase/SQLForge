package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.CommonSubgraphFingerprint.subgraphFingerprint;
import static com.company.sqloptimization.application.service.CommonSubgraphOutputColumnAnalyzer.outputColumns;
import static com.company.sqloptimization.application.service.CommonSubgraphOutputColumnAnalyzer.withRequiredQualifiedColumns;
import static com.company.sqloptimization.application.service.CommonSubgraphRelationReferences.accessesOriginalSources;
import static com.company.sqloptimization.application.service.CommonSubgraphRelationReferences.originalBaseSources;
import static com.company.sqloptimization.application.service.CommonSubgraphRelationReferences.relationReferenced;
import static com.company.sqloptimization.application.service.CommonSubgraphReplacementMatcher.replacementCount;
import static com.company.sqloptimization.application.service.CommonSubgraphRequiredColumnAnalyzer.requiredColumns;
import static com.company.sqloptimization.application.service.CommonSubgraphSqlRewriter.rewriteSql;
import static com.company.sqloptimization.application.service.CommonSubgraphSqlText.startsWithWord;
import static com.company.sqloptimization.application.service.CommonSubgraphSqlText.trimTrailingSemicolon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class CommonSubgraphCandidateSelector {

    private CommonSubgraphCandidateSelector() {
    }

    static CommonSubgraphCandidate chooseCandidate(String sourceSql,
                                                  List<CommonSubgraphCandidate> candidates,
                                                  Map<String, Object> advancedStructureProfile) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String mainQuery = extractMainQueryAfterWith(sourceSql);
        Map<String, Integer> fingerprintCounts = fingerprintCounts(candidates);
        Set<String> originalSources = originalBaseSources(advancedStructureProfile);
        CommonSubgraphCandidate originalAvoidingRepeatedCandidate = bestOriginalAvoidingCandidate(
            sourceSql,
            candidates,
            fingerprintCounts,
            originalSources,
            true
        );
        if (originalAvoidingRepeatedCandidate != null) {
            return originalAvoidingRepeatedCandidate;
        }
        CommonSubgraphCandidate originalAvoidingCandidate = bestOriginalAvoidingCandidate(
            sourceSql,
            candidates,
            fingerprintCounts,
            originalSources,
            false
        );
        if (originalAvoidingCandidate != null) {
            return originalAvoidingCandidate;
        }
        CommonSubgraphCandidate repeatedCandidate = firstCoveredCandidate(
            sourceSql,
            candidates,
            fingerprintCounts,
            true
        );
        if (repeatedCandidate != null) {
            return repeatedCandidate;
        }
        CommonSubgraphCandidate coveredCandidate = firstCoveredCandidate(
            sourceSql,
            candidates,
            fingerprintCounts,
            false
        );
        if (coveredCandidate != null) {
            return coveredCandidate;
        }
        for (CommonSubgraphCandidate candidate : candidates) {
            if (!"CTE".equals(candidate.sourceKind) || relationReferenced(mainQuery, candidate.sourceName)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private static CommonSubgraphCandidate bestOriginalAvoidingCandidate(
        String sourceSql,
        List<CommonSubgraphCandidate> candidates,
        Map<String, Integer> fingerprintCounts,
        Set<String> originalSources,
        boolean repeatedOnly
    ) {
        if (originalSources == null || originalSources.isEmpty()) {
            return null;
        }
        CommonSubgraphCandidate bestCandidate = null;
        int bestReplacementCount = 0;
        int bestComplexityScore = -1;
        for (CommonSubgraphCandidate candidate : candidates) {
            if ("CTE".equals(candidate.sourceKind) && !relationReferenced(sourceSql, candidate.sourceName)) {
                continue;
            }
            int replacementCount = replacementCount(sourceSql, candidate);
            int repeatCount = Math.max(
                intValue(fingerprintCounts.get(subgraphFingerprint(candidate.subgraphSql))),
                replacementCount
            );
            if (repeatedOnly && repeatCount < 2) {
                continue;
            }
            if (!coveredByCandidate(sourceSql, candidate)) {
                continue;
            }
            String rewriteSql = rewriteSql(sourceSql, candidate, "__mv_common_subgraph__");
            if (!StringUtils.hasText(rewriteSql)
                || rewriteSql.equals(trimTrailingSemicolon(sourceSql))
                || accessesOriginalSources(rewriteSql, originalSources)) {
                continue;
            }
            int complexityScore = candidateComplexityScore(candidate);
            if (bestCandidate == null
                || replacementCount > bestReplacementCount
                || (replacementCount == bestReplacementCount && complexityScore > bestComplexityScore)) {
                bestCandidate = candidate;
                bestReplacementCount = replacementCount;
                bestComplexityScore = complexityScore;
            }
        }
        return bestCandidate;
    }

    private static CommonSubgraphCandidate firstCoveredCandidate(String sourceSql,
                                                                List<CommonSubgraphCandidate> candidates,
                                                                Map<String, Integer> fingerprintCounts,
                                                                boolean repeatedOnly) {
        CommonSubgraphCandidate bestCandidate = null;
        int bestReplacementCount = 0;
        for (CommonSubgraphCandidate candidate : candidates) {
            if ("CTE".equals(candidate.sourceKind) && !relationReferenced(sourceSql, candidate.sourceName)) {
                continue;
            }
            int replacementCount = replacementCount(sourceSql, candidate);
            if (repeatedOnly
                && intValue(fingerprintCounts.get(subgraphFingerprint(candidate.subgraphSql))) < 2
                && replacementCount < 2) {
                continue;
            }
            if (coveredByCandidate(sourceSql, candidate)) {
                if (!repeatedOnly) {
                    return candidate;
                }
                if (replacementCount > bestReplacementCount) {
                    bestCandidate = candidate;
                    bestReplacementCount = replacementCount;
                }
            }
        }
        return bestCandidate;
    }

    private static boolean coveredByCandidate(String sourceSql, CommonSubgraphCandidate candidate) {
        CommonSubgraphOutputColumns outputColumns = outputColumns(candidate.subgraphSql);
        if (!outputColumns.blockingReasons.isEmpty()) {
            return false;
        }
        Set<String> requiredColumns = requiredColumns(sourceSql, candidate);
        CommonSubgraphOutputColumns coverageColumns = "DERIVED_TABLE".equals(candidate.sourceKind)
            ? withRequiredQualifiedColumns(outputColumns, requiredColumns)
            : outputColumns;
        return !requiredColumns.isEmpty() && coverageColumns.normalizedColumns.containsAll(requiredColumns);
    }

    private static Map<String, Integer> fingerprintCounts(List<CommonSubgraphCandidate> candidates) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (CommonSubgraphCandidate candidate
            : candidates == null ? Collections.<CommonSubgraphCandidate>emptyList() : candidates) {
            String fingerprint = subgraphFingerprint(candidate.subgraphSql);
            result.put(fingerprint, Integer.valueOf(intValue(result.get(fingerprint)) + 1));
        }
        return result;
    }

    private static int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private static int candidateComplexityScore(CommonSubgraphCandidate candidate) {
        return candidate == null || candidate.subgraphSql == null ? 0 : candidate.subgraphSql.length();
    }

    private static String extractMainQueryAfterWith(String sourceSql) {
        String sql = trimTrailingSemicolon(sourceSql);
        if (!StringUtils.hasText(sql) || !sql.trim().toUpperCase(Locale.ROOT).startsWith("WITH ")) {
            return sql;
        }
        int depth = 0;
        boolean seenCteBody = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
                seenCteBody = true;
            } else if (current == ')') {
                depth--;
                if (seenCteBody && depth == 0) {
                    int cursor = i + 1;
                    while (cursor < sql.length()
                        && (Character.isWhitespace(sql.charAt(cursor)) || sql.charAt(cursor) == ',')) {
                        cursor++;
                    }
                    if (startsWithWord(sql, cursor, "SELECT")) {
                        return sql.substring(cursor).trim();
                    }
                }
            }
        }
        return sql;
    }
}
