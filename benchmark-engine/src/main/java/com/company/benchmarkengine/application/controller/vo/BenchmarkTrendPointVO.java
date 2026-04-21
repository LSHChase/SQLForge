package com.company.benchmarkengine.application.controller.vo;

import java.math.BigDecimal;

public class BenchmarkTrendPointVO {

    private final String label;
    private final BigDecimal value;

    public BenchmarkTrendPointVO(String label, BigDecimal value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getValue() {
        return value;
    }
}
