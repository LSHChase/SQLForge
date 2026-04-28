package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTestSetFieldMappingDTO {

    @NotNull(message = "field is required")
    private BenchmarkTestSetField field;

    @NotBlank(message = "columnName is required")
    @Size(max = 128, message = "columnName exceeds 128 characters")
    private String columnName;

    public BenchmarkTestSetField getField() {
        return field;
    }

    public void setField(BenchmarkTestSetField field) {
        this.field = field;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
