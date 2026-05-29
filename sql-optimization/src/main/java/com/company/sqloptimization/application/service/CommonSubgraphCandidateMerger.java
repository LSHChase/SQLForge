package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CommonSubgraphCandidateMerger {

    private CommonSubgraphCandidateMerger() {
    }

    static List<CommonSubgraphCandidate> mergeCandidates(List<CommonSubgraphCandidate> primary,
                                                         List<CommonSubgraphCandidate> secondary) {
        List<CommonSubgraphCandidate> result = new ArrayList<CommonSubgraphCandidate>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        addCandidates(result, seen, primary);
        addCandidates(result, seen, secondary);
        return result;
    }

    private static void addCandidates(List<CommonSubgraphCandidate> result,
                                      Set<String> seen,
                                      List<CommonSubgraphCandidate> candidates) {
        for (CommonSubgraphCandidate candidate : candidates == null
            ? Collections.<CommonSubgraphCandidate>emptyList()
            : candidates) {
            String key = candidate.sourceKind + "|"
                + CommonSubgraphSqlText.normalizeIdentifier(candidate.alias)
                + "|"
                + CommonSubgraphFingerprint.subgraphFingerprint(candidate.subgraphSql);
            if (seen.add(key)) {
                result.add(candidate);
            }
        }
    }
}
