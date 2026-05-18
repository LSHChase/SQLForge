package com.company.benchmarkengine.application.controller.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

public class BenchmarkScaleEvidenceFileDigestDTO {

    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "sha256 必须为 64 位十六进制字符串")
    private String sha256;

    @Min(value = 1, message = "sizeBytes 必须大于 0")
    private Long sizeBytes;

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
