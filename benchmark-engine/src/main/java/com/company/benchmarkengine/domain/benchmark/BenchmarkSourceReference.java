package com.company.benchmarkengine.domain.benchmark;

public class BenchmarkSourceReference {

    private final BenchmarkSourceReferenceType type;
    private final String referenceId;

    public BenchmarkSourceReference(BenchmarkSourceReferenceType type, String referenceId) {
        this.type = type;
        this.referenceId = referenceId;
    }

    public BenchmarkSourceReferenceType getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
