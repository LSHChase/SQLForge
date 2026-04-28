package com.company.benchmarkengine.domain.benchmark;

public class BenchmarkTestSetLabel {

    private final BenchmarkTestSetLabelType type;
    private final String value;

    public BenchmarkTestSetLabel(BenchmarkTestSetLabelType type, String value) {
        this.type = type;
        this.value = value;
    }

    public BenchmarkTestSetLabelType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
