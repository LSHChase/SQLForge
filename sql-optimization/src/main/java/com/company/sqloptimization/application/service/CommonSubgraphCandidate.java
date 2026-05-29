package com.company.sqloptimization.application.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class CommonSubgraphCandidate {

    final String sourceKind;
    final String sourceName;
    final String alias;
    final String subgraphSql;
    final boolean recursive;
    final Set<String> materializedCteNames;

    CommonSubgraphCandidate(String sourceKind,
                            String sourceName,
                            String alias,
                            String subgraphSql,
                            boolean recursive) {
        this(sourceKind, sourceName, alias, subgraphSql, recursive, Collections.<String>emptySet());
    }

    CommonSubgraphCandidate(String sourceKind,
                            String sourceName,
                            String alias,
                            String subgraphSql,
                            boolean recursive,
                            Set<String> materializedCteNames) {
        this.sourceKind = sourceKind;
        this.sourceName = sourceName;
        this.alias = alias;
        this.subgraphSql = subgraphSql;
        this.recursive = recursive;
        this.materializedCteNames = materializedCteNames == null
            ? Collections.<String>emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<String>(materializedCteNames));
    }
}
