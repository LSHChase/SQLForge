package com.company.benchmarkengine.domain.benchmark;

public class BenchmarkTestSetFieldMapping {

    private final BenchmarkTestSetField field;
    private final String columnName;

    public BenchmarkTestSetFieldMapping(BenchmarkTestSetField field, String columnName) {
        this.field = field;
        this.columnName = columnName;
    }

    public BenchmarkTestSetField getField() {
        return field;
    }

    public String getColumnName() {
        return columnName;
    }
}
