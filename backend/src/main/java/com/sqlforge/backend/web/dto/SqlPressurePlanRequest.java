package com.sqlforge.backend.web.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class SqlPressurePlanRequest {

    private String batchId;
    private String source;

    @Min(1)
    private Integer targetConcurrency;

    @NotBlank
    private String rawSqlText;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getTargetConcurrency() {
        return targetConcurrency;
    }

    public void setTargetConcurrency(Integer targetConcurrency) {
        this.targetConcurrency = targetConcurrency;
    }

    public String getRawSqlText() {
        return rawSqlText;
    }

    public void setRawSqlText(String rawSqlText) {
        this.rawSqlText = rawSqlText;
    }
}
