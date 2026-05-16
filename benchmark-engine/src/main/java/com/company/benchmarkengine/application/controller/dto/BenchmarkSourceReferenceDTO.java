package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkSourceReferenceDTO {

    @NotNull(message = "source 引用类型为必填项")
    private BenchmarkSourceReferenceType type;

    @NotBlank(message = "source referenceId 为必填项")
    @Size(max = 128, message = "source referenceId 超过 128 个字符")
    private String referenceId;

    public BenchmarkSourceReferenceType getType() {
        return type;
    }

    public void setType(BenchmarkSourceReferenceType type) {
        this.type = type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
