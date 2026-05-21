package com.company.sqloptimization.domain.rewrite.ir;

public enum RelationalOperator {
    TABLE_SCAN("scan"),
    SIGMA("σ"),
    PI("π"),
    GAMMA("γ"),
    JOIN("⋈"),
    UNION("∪"),
    INTERSECT("∩"),
    DIFFERENCE("-");

    private final String symbol;

    RelationalOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
