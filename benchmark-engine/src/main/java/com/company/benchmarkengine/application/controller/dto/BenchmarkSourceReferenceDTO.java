package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkSourceReferenceDTO {

    @NotNull(message = "source reference type is required")
    private BenchmarkSourceReferenceType type;

    @NotBlank(message = "source referenceId is required")
    @Size(max = 128, message = "source referenceId exceeds 128 characters")
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
