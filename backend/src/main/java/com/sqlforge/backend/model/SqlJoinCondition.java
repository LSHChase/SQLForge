package com.sqlforge.backend.model;

public class SqlJoinCondition {

    private final String leftAlias;
    private final String leftKey;
    private final String rightAlias;
    private final String rightKey;

    public SqlJoinCondition(String leftAlias, String leftKey, String rightAlias, String rightKey) {
        this.leftAlias = leftAlias;
        this.leftKey = leftKey;
        this.rightAlias = rightAlias;
        this.rightKey = rightKey;
    }

    public String getLeftAlias() {
        return leftAlias;
    }

    public String getLeftKey() {
        return leftKey;
    }

    public String getRightAlias() {
        return rightAlias;
    }

    public String getRightKey() {
        return rightKey;
    }
}
