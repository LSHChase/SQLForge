package com.sqlforge.backend.web.dto;

import javax.validation.constraints.NotBlank;

public class SqlIntentBatchRequest {

    private String batchId;
    private String source;

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

    public String getRawSqlText() {
        return rawSqlText;
    }

    public void setRawSqlText(String rawSqlText) {
        this.rawSqlText = rawSqlText;
    }
}
