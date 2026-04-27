package com.company.sqlforge.common.jdbcagent;

public class JdbcAgentDirectResult<T> {

    private final T payload;
    private final Integer rowCount;
    private final String resultStatus;

    public JdbcAgentDirectResult(T payload, Integer rowCount, String resultStatus) {
        this.payload = payload;
        this.rowCount = rowCount;
        this.resultStatus = resultStatus;
    }

    public static <T> JdbcAgentDirectResult<T> success(T payload, Integer rowCount) {
        return new JdbcAgentDirectResult<T>(payload, rowCount, "SUCCESS");
    }

    public T getPayload() {
        return payload;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public String getResultStatus() {
        return resultStatus;
    }
}
