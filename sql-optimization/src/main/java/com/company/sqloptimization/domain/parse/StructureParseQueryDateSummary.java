package com.company.sqloptimization.domain.parse;

import java.util.List;

public class StructureParseQueryDateSummary {

    private String queryDateStart;
    private String queryDateEnd;
    private List<String> queryDateFields;
    private StructureParseQueryDateStatus queryDateStatus;

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

    public StructureParseQueryDateStatus getQueryDateStatus() {
        return queryDateStatus;
    }

    public void setQueryDateStatus(StructureParseQueryDateStatus queryDateStatus) {
        this.queryDateStatus = queryDateStatus;
    }
}
