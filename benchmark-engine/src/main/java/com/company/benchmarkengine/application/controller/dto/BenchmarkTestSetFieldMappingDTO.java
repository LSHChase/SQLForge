package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTestSetFieldMappingDTO {

    @NotNull(message = "field 为必填项")
    private BenchmarkTestSetField field;

    @NotBlank(message = "columnName 为必填项")
    @Size(max = 128, message = "columnName 超过 128 个字符")
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
