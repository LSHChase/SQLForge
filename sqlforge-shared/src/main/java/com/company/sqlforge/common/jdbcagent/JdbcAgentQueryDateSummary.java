package com.company.sqlforge.common.jdbcagent;

import java.util.ArrayList;
import java.util.List;

public class JdbcAgentQueryDateSummary {

    private String queryDateStart;
    private String queryDateEnd;
    private List<String> queryDateFields = new ArrayList<String>();
    private String queryDateStatus = "UNRESOLVED";

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
        this.queryDateFields = queryDateFields == null ? new ArrayList<String>() : new ArrayList<String>(queryDateFields);
    }

    public String getQueryDateStatus() {
        return queryDateStatus;
    }

    public void setQueryDateStatus(String queryDateStatus) {
        this.queryDateStatus = queryDateStatus;
    }
}
