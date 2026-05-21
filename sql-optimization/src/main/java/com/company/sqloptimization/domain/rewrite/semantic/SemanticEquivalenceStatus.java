package com.company.sqloptimization.domain.rewrite.semantic;

public enum SemanticEquivalenceStatus {

    PROVED("已证明"),
    CONDITIONALLY_PROVED("有条件证明"),
    NEEDS_CONSTRAINTS("需要约束证明"),
    UNSUPPORTED("暂不支持"),
    NO_CANDIDATE("无候选");

    private final String titleZh;

    SemanticEquivalenceStatus(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleZh() {
        return titleZh;
    }
}
