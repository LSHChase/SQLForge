package com.sqlforge.backend.service;

import com.sqlforge.backend.model.SqlJoinCondition;
import com.sqlforge.backend.model.SqlStructureAst;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class HeuristicSqlParserAdapter implements SqlParserAdapter {

    private static final Pattern TABLE_REFERENCE_PATTERN = Pattern.compile("\\b(?:from|join)\\s+([a-zA-Z0-9_.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_CONDITION_PATTERN =
        Pattern.compile("\\bon\\s+([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\s*=\\s*([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT_CLAUSE_PATTERN = Pattern.compile("\\bselect\\s+(.+?)\\s+from\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CTE_PATTERN = Pattern.compile("(?:\\bwith\\b|,)\\s*([a-zA-Z0-9_]+)\\s+as\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile("\\(\\s*select\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGGREGATE_PATTERN =
        Pattern.compile("\\b(sum|avg|max|min|count|approx_distinct|collect_set|collect_list)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOW_PATTERN = Pattern.compile("\\bover\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_FILTER_PATTERN =
        Pattern.compile("\\b(ds|dt|date|day|event_date|biz_date|partition_date|stat_date)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUNCTION_WRAPPED_PREDICATE_PATTERN =
        Pattern.compile("\\bwhere\\b.+\\b(?:lower|upper|date|cast|substr|substring|coalesce)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISTINCT_PATTERN = Pattern.compile("\\bselect\\s+distinct\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public SqlStructureAst parse(String normalizedSql) {
        String lowerSql = normalizedSql.toLowerCase(Locale.ROOT);

        List<String> tableReferences = detectTableReferences(normalizedSql);
        List<SqlJoinCondition> joinConditions = detectJoinConditions(normalizedSql);
        int cteCount = countMatches(CTE_PATTERN, normalizedSql);
        int subqueryCount = countMatches(SUBQUERY_PATTERN, normalizedSql);
        int aggregateFunctionCount = countMatches(AGGREGATE_PATTERN, normalizedSql);
        int windowFunctionCount = countMatches(WINDOW_PATTERN, normalizedSql);
        int expressionCount = countSelectExpressions(normalizedSql);
        int setOperationCount = countSetOperations(lowerSql);
        boolean hasWhere = lowerSql.contains(" where ");
        boolean hasGroupBy = lowerSql.contains(" group by ");
        boolean hasHaving = lowerSql.contains(" having ");
        boolean hasOrderBy = lowerSql.contains(" order by ");
        boolean hasLimit = lowerSql.contains(" limit ");
        boolean hasDistinct = DISTINCT_PATTERN.matcher(normalizedSql).find();
        boolean hasWindow = windowFunctionCount > 0;
        boolean hasSelectStar = normalizedSql.contains("*");
        boolean predicateFunctionWrapped = FUNCTION_WRAPPED_PREDICATE_PATTERN.matcher(normalizedSql).find();
        List<String> timeFilterColumns = detectMatches(TIME_FILTER_PATTERN, normalizedSql);

        return new SqlStructureAst(
            normalizedSql,
            detectStatementType(lowerSql),
            tableReferences,
            joinConditions,
            cteCount,
            subqueryCount,
            setOperationCount,
            expressionCount,
            aggregateFunctionCount,
            windowFunctionCount,
            hasWhere,
            hasGroupBy,
            hasHaving,
            hasOrderBy,
            hasLimit,
            hasDistinct,
            hasWindow,
            hasSelectStar,
            predicateFunctionWrapped,
            timeFilterColumns
        );
    }

    private List<String> detectTableReferences(String sql) {
        List<String> references = new ArrayList<String>();
        Matcher matcher = TABLE_REFERENCE_PATTERN.matcher(sql);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
    }

    private List<SqlJoinCondition> detectJoinConditions(String sql) {
        List<SqlJoinCondition> conditions = new ArrayList<SqlJoinCondition>();
        Matcher matcher = JOIN_CONDITION_PATTERN.matcher(sql);
        while (matcher.find()) {
            conditions.add(new SqlJoinCondition(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
        }
        return conditions;
    }

    private int countSelectExpressions(String sql) {
        Matcher matcher = SELECT_CLAUSE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return 0;
        }

        String[] expressions = matcher.group(1).split(",");
        int count = 0;
        for (String expression : expressions) {
            if (!expression.trim().isEmpty()) {
                count += 1;
            }
        }
        return count;
    }

    private int countSetOperations(String sql) {
        return countToken(sql, " union ") + countToken(sql, " intersect ") + countToken(sql, " except ");
    }

    private String detectStatementType(String lowerSql) {
        for (String keyword : Arrays.asList("select", "insert", "update", "delete", "create", "alter", "drop", "with")) {
            if (lowerSql.startsWith(keyword + " ")) {
                return "with".equals(keyword) ? "select" : keyword;
            }
        }
        return "unknown";
    }

    private List<String> detectMatches(Pattern pattern, String sql) {
        List<String> matches = new ArrayList<String>();
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String value = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!matches.contains(value)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private int countMatches(Pattern pattern, String sql) {
        int count = 0;
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            count += 1;
        }
        return count;
    }

    private int countToken(String sql, String token) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = sql.indexOf(token, fromIndex)) >= 0) {
            count += 1;
            fromIndex += token.length();
        }
        return count;
    }
}
