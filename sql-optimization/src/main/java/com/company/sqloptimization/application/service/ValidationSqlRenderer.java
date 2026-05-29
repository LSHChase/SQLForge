package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ValidationSqlChecks.groupAggregateExpression;
import static com.company.sqloptimization.application.service.ValidationSqlText.join;

import java.util.ArrayList;
import java.util.List;

final class ValidationSqlRenderer {

    private ValidationSqlRenderer() {
    }

    static String render(List<ValidationSqlCte> ctes, List<String> checks) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH ");
        for (int i = 0; i < ctes.size(); i++) {
            if (i > 0) {
                sql.append(",\n");
            }
            ValidationSqlCte cte = ctes.get(i);
            sql.append(cte.name).append(" AS (\n").append(cte.sql).append("\n)");
        }
        sql.append("\n");
        for (int i = 0; i < checks.size(); i++) {
            if (i > 0) {
                sql.append("\nUNION ALL\n");
            }
            sql.append(checks.get(i));
        }
        sql.append(";");
        return sql.toString();
    }

    static String groupedResultSql(String sourceRelation,
                                   List<ValidationSqlColumn> groupKeys,
                                   List<ValidationSqlMeasure> measures,
                                   String markerName) {
        List<String> selectItems = new ArrayList<String>();
        List<String> groupItems = new ArrayList<String>();
        for (ValidationSqlColumn groupKey : groupKeys) {
            selectItems.add(groupKey.name);
            groupItems.add(groupKey.name);
        }
        for (ValidationSqlMeasure measure : measures) {
            selectItems.add(groupAggregateExpression(measure) + " AS " + measure.name);
        }
        selectItems.add("1 AS " + markerName);
        return "SELECT\n       " + join(selectItems, ",\n       ")
            + "\nFROM " + sourceRelation
            + "\nGROUP BY " + join(groupItems, ", ");
    }

    static String mvSubgraphResultSql(String mvName, List<String> outputColumns) {
        return "SELECT " + join(outputColumns, ", ") + "\nFROM " + mvName;
    }
}
