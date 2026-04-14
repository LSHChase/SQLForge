package com.sqlforge.backend.web.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class QueryPreviewRequest extends ConnectionRequest {

    @NotBlank
    private String sql;

    @Min(1)
    @Max(100)
    private int maxRows = 20;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }
}
