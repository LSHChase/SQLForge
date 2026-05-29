package com.company.sqloptimization.application.service;

import java.util.LinkedHashSet;

final class CommonSubgraphRelationUsageScope {

    final LinkedHashSet<String> candidateAliases = new LinkedHashSet<String>();
    int sourceCount;

    boolean referencesCandidate() {
        return !candidateAliases.isEmpty();
    }

    boolean allowUnqualifiedColumns() {
        return referencesCandidate() && sourceCount == 1;
    }
}
