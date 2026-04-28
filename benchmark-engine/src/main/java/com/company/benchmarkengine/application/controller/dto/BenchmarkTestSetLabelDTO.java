package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTestSetLabelDTO {

    @NotNull(message = "testSet label type is required")
    private BenchmarkTestSetLabelType type;

    @NotBlank(message = "testSet label value is required")
    @Size(max = 128, message = "testSet label value exceeds 128 characters")
    private String value;

    public BenchmarkTestSetLabelType getType() {
        return type;
    }

    public void setType(BenchmarkTestSetLabelType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
