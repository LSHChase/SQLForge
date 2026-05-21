package com.company.sqloptimization.domain.rewrite.semantic;

public enum SemanticEquivalenceCheckType {

    CONSTRAINT_BASED_EQUIVALENCE("基于约束的等价性检验"),
    STATISTICAL_AGGREGATION_EQUIVALENCE("聚合统计等价性检验");

    private final String titleZh;

    SemanticEquivalenceCheckType(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleZh() {
        return titleZh;
    }
}
