package com.company.sqloptimization.application.service;

final class CommonSubgraphAliasMatch {

    final boolean matched;
    final int endIndex;

    CommonSubgraphAliasMatch(boolean matched, int endIndex) {
        this.matched = matched;
        this.endIndex = endIndex;
    }

    static CommonSubgraphAliasMatch none() {
        return new CommonSubgraphAliasMatch(false, -1);
    }
}
