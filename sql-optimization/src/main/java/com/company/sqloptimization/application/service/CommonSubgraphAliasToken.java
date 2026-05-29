package com.company.sqloptimization.application.service;

final class CommonSubgraphAliasToken {

    final boolean present;
    final String value;
    final int endIndex;

    CommonSubgraphAliasToken(boolean present, String value, int endIndex) {
        this.present = present;
        this.value = value;
        this.endIndex = endIndex;
    }

    static CommonSubgraphAliasToken none() {
        return new CommonSubgraphAliasToken(false, "", -1);
    }
}
