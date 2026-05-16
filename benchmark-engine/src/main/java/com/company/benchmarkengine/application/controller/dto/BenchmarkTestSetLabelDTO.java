package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTestSetLabelDTO {

    @NotNull(message = "testSet 标签类型为必填项")
    private BenchmarkTestSetLabelType type;

    @NotBlank(message = "testSet 标签值为必填项")
    @Size(max = 128, message = "testSet 标签值超过 128 个字符")
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
