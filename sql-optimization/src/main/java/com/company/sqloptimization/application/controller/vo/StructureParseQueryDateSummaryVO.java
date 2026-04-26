package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class StructureParseQueryDateSummaryVO {

    private String queryDateStart;
    private String queryDateEnd;
    private List<String> queryDateFields;
    private String queryDateStatus;

    public String getQueryDateStart() {
        return queryDateStart;
    }

    public void setQueryDateStart(String queryDateStart) {
        this.queryDateStart = queryDateStart;
    }

    public String getQueryDateEnd() {
        return queryDateEnd;
    }

    public void setQueryDateEnd(String queryDateEnd) {
        this.queryDateEnd = queryDateEnd;
    }

    public List<String> getQueryDateFields() {
        return queryDateFields;
    }

    public void setQueryDateFields(List<String> queryDateFields) {
        this.queryDateFields = queryDateFields;
    }

    public String getQueryDateStatus() {
        return queryDateStatus;
    }

    public void setQueryDateStatus(String queryDateStatus) {
        this.queryDateStatus = queryDateStatus;
    }
}
