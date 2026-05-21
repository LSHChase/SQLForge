package com.company.sqloptimization.domain.rewrite.ra;

public enum RelationalRewriteRuleType {

    CSE_ELIMINATION("公共子表达式消除"),
    VERTICAL_FOLDING("纵向折叠"),
    HORIZONTAL_UNNESTING("横向展开消除");

    private final String titleZh;

    RelationalRewriteRuleType(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleZh() {
        return titleZh;
    }
}
