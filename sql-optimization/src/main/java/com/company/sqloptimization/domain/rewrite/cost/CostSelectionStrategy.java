package com.company.sqloptimization.domain.rewrite.cost;

public enum CostSelectionStrategy {

    DEFAULT_WEIGHTED("默认加权"),
    TIMEOUT_SENSITIVE("超时敏感"),
    MEMORY_CONSTRAINED("内存紧张");

    private final String titleZh;

    CostSelectionStrategy(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleZh() {
        return titleZh;
    }
}
