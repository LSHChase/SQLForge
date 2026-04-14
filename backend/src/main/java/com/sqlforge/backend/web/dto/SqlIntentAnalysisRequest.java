package com.sqlforge.backend.web.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

public class SqlIntentAnalysisRequest {

    @NotEmpty
    @Valid
    private List<StatementInput> statements;

    public List<StatementInput> getStatements() {
        return statements;
    }

    public void setStatements(List<StatementInput> statements) {
        this.statements = statements;
    }

    public static class StatementInput {
        private String id;
        private String source;

        @NotBlank
        private String sql;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }
    }
}
