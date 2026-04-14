package com.sqlforge.backend.model;

import java.util.ArrayList;
import java.util.List;

public class SqlStructureAst {

    private final String normalizedSql;
    private final String statementType;
    private final List<String> tableReferences;
    private final List<SqlJoinCondition> joinConditions;
    private final int cteCount;
    private final int subqueryCount;
    private final int setOperationCount;
    private final int expressionCount;
    private final int aggregateFunctionCount;
    private final int windowFunctionCount;
    private final boolean hasWhere;
    private final boolean hasGroupBy;
    private final boolean hasHaving;
    private final boolean hasOrderBy;
    private final boolean hasLimit;
    private final boolean hasDistinct;
    private final boolean hasWindow;
    private final boolean hasSelectStar;
    private final boolean predicateFunctionWrapped;
    private final List<String> timeFilterColumns;

    public SqlStructureAst(
        String normalizedSql,
        String statementType,
        List<String> tableReferences,
        List<SqlJoinCondition> joinConditions,
        int cteCount,
        int subqueryCount,
        int setOperationCount,
        int expressionCount,
        int aggregateFunctionCount,
        int windowFunctionCount,
        boolean hasWhere,
        boolean hasGroupBy,
        boolean hasHaving,
        boolean hasOrderBy,
        boolean hasLimit,
        boolean hasDistinct,
        boolean hasWindow,
        boolean hasSelectStar,
        boolean predicateFunctionWrapped,
        List<String> timeFilterColumns
    ) {
        this.normalizedSql = normalizedSql;
        this.statementType = statementType;
        this.tableReferences = new ArrayList<String>(tableReferences);
        this.joinConditions = new ArrayList<SqlJoinCondition>(joinConditions);
        this.cteCount = cteCount;
        this.subqueryCount = subqueryCount;
        this.setOperationCount = setOperationCount;
        this.expressionCount = expressionCount;
        this.aggregateFunctionCount = aggregateFunctionCount;
        this.windowFunctionCount = windowFunctionCount;
        this.hasWhere = hasWhere;
        this.hasGroupBy = hasGroupBy;
        this.hasHaving = hasHaving;
        this.hasOrderBy = hasOrderBy;
        this.hasLimit = hasLimit;
        this.hasDistinct = hasDistinct;
        this.hasWindow = hasWindow;
        this.hasSelectStar = hasSelectStar;
        this.predicateFunctionWrapped = predicateFunctionWrapped;
        this.timeFilterColumns = new ArrayList<String>(timeFilterColumns);
    }

    public String getNormalizedSql() {
        return normalizedSql;
    }

    public String getStatementType() {
        return statementType;
    }

    public List<String> getTableReferences() {
        return new ArrayList<String>(tableReferences);
    }

    public List<SqlJoinCondition> getJoinConditions() {
        return new ArrayList<SqlJoinCondition>(joinConditions);
    }

    public int getCteCount() {
        return cteCount;
    }

    public int getSubqueryCount() {
        return subqueryCount;
    }

    public int getSetOperationCount() {
        return setOperationCount;
    }

    public int getExpressionCount() {
        return expressionCount;
    }

    public int getAggregateFunctionCount() {
        return aggregateFunctionCount;
    }

    public int getWindowFunctionCount() {
        return windowFunctionCount;
    }

    public boolean isHasWhere() {
        return hasWhere;
    }

    public boolean isHasGroupBy() {
        return hasGroupBy;
    }

    public boolean isHasHaving() {
        return hasHaving;
    }

    public boolean isHasOrderBy() {
        return hasOrderBy;
    }

    public boolean isHasLimit() {
        return hasLimit;
    }

    public boolean isHasDistinct() {
        return hasDistinct;
    }

    public boolean isHasWindow() {
        return hasWindow;
    }

    public boolean isHasSelectStar() {
        return hasSelectStar;
    }

    public boolean isPredicateFunctionWrapped() {
        return predicateFunctionWrapped;
    }

    public List<String> getTimeFilterColumns() {
        return new ArrayList<String>(timeFilterColumns);
    }
}
