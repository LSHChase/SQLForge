package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SqlIntentAnalysisService {

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

    private final SqlAssessmentService sqlAssessmentService;

    public SqlIntentAnalysisService(SqlAssessmentService sqlAssessmentService) {
        this.sqlAssessmentService = sqlAssessmentService;
    }

    public Map<String, Object> analyze(SqlIntentAnalysisRequest request) {
        List<Map<String, Object>> analyses = new ArrayList<Map<String, Object>>();

        for (SqlIntentAnalysisRequest.StatementInput statement : request.getStatements()) {
            analyses.add(analyzeStatement(statement));
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("analysisMode", "pure-structure");
        result.put("analyses", analyses);
        result.put("summary", buildSummary(analyses));
        return result;
    }

    private Map<String, Object> analyzeStatement(SqlIntentAnalysisRequest.StatementInput statement) {
        Map<String, Object> sqlAssessment = sqlAssessmentService.assess(null, statement.getSql(), null, null);
        Map<String, Object> assessment = castMap(sqlAssessment.get("assessment"));
        Map<String, Object> labels = castMap(assessment.get("labels"));
        Map<String, Object> validations = castMap(assessment.get("validations"));
        List<Map<String, Object>> risks = castList(assessment.get("risks"));
        String normalizedSql = String.valueOf(assessment.get("sql"));
        String lowerSql = normalizedSql.toLowerCase(Locale.ROOT);
        List<String> tables = castStringList(assessment.get("tables"));
        List<String> tableReferences = detectTableReferences(normalizedSql);
        List<Map<String, Object>> joinConditions = detectJoinConditions(normalizedSql);

        String statementType = detectStatementType(lowerSql);
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
        boolean predicateFunctionWrapped = FUNCTION_WRAPPED_PREDICATE_PATTERN.matcher(normalizedSql).find();
        List<String> timeFilterColumns = detectMatches(TIME_FILTER_PATTERN, normalizedSql);
        List<String> intentTags = buildIntentTags(statementType, labels, cteCount, subqueryCount, aggregateFunctionCount, hasWindow, hasLimit, hasOrderBy);
        List<String> pressureSignals = buildPressureSignals(
            labels,
            risks,
            validations,
            cteCount,
            subqueryCount,
            setOperationCount,
            predicateFunctionWrapped,
            hasWindow,
            hasOrderBy,
            hasLimit
        );
        int complexityScore = buildComplexityScore(
            tables.size(),
            joinConditions.size(),
            cteCount,
            subqueryCount,
            aggregateFunctionCount,
            windowFunctionCount,
            setOperationCount,
            risks.size(),
            expressionCount
        );

        Map<String, Object> structure = mapOf(
            "normalizedSql",
            normalizedSql,
            "statementType",
            statementType,
            "tableCount",
            Integer.valueOf(tables.size()),
            "tables",
            tables,
            "tableReferences",
            tableReferences,
            "joinCount",
            Integer.valueOf(joinConditions.size()),
            "joinType",
            labels.get("joinType"),
            "joinConditions",
            joinConditions,
            "cteCount",
            Integer.valueOf(cteCount),
            "subqueryCount",
            Integer.valueOf(subqueryCount),
            "setOperationCount",
            Integer.valueOf(setOperationCount),
            "hasWhere",
            Boolean.valueOf(hasWhere),
            "hasGroupBy",
            Boolean.valueOf(hasGroupBy),
            "hasHaving",
            Boolean.valueOf(hasHaving),
            "hasOrderBy",
            Boolean.valueOf(hasOrderBy),
            "hasLimit",
            Boolean.valueOf(hasLimit),
            "hasDistinct",
            Boolean.valueOf(hasDistinct),
            "hasWindow",
            Boolean.valueOf(hasWindow)
        );

        Map<String, Object> projection = mapOf(
            "expressionCount",
            Integer.valueOf(expressionCount),
            "aggregateFunctionCount",
            Integer.valueOf(aggregateFunctionCount),
            "windowFunctionCount",
            Integer.valueOf(windowFunctionCount),
            "hasSelectStar",
            Boolean.valueOf(normalizedSql.contains("*")),
            "repeatedExpressions",
            buildRepeatedExpressionSummary(validations)
        );

        Map<String, Object> predicates = mapOf(
            "scanPattern",
            labels.get("scanPattern"),
            "predicateFunctionWrapped",
            Boolean.valueOf(predicateFunctionWrapped),
            "timeFilterColumns",
            timeFilterColumns,
            "resultSetRisk",
            labels.get("resultSetRisk")
        );

        Map<String, Object> pressureProfile = mapOf(
            "loadClass",
            resolveLoadClass(statementType, labels, hasWindow, hasOrderBy, hasLimit, aggregateFunctionCount, joinConditions.size()),
            "pressureSignals",
            pressureSignals,
            "complexityScore",
            Integer.valueOf(complexityScore),
            "complexityTier",
            resolveComplexityTier(complexityScore)
        );

        Map<String, Object> analysis = new LinkedHashMap<String, Object>();
        analysis.put("statementId", statement.getId() == null || statement.getId().trim().isEmpty() ? assessment.get("fingerprint") : statement.getId());
        analysis.put("source", statement.getSource());
        analysis.put("fingerprint", assessment.get("fingerprint"));
        analysis.put("intentTags", intentTags);
        analysis.put("structure", structure);
        analysis.put("projection", projection);
        analysis.put("predicates", predicates);
        analysis.put("pressureProfile", pressureProfile);
        analysis.put("structuralAlerts", buildStructuralAlerts(risks, validations, pressureSignals));
        analysis.put("assessment", assessment);
        return analysis;
    }

    private List<String> buildIntentTags(
        String statementType,
        Map<String, Object> labels,
        int cteCount,
        int subqueryCount,
        int aggregateFunctionCount,
        boolean hasWindow,
        boolean hasLimit,
        boolean hasOrderBy
    ) {
        List<String> tags = new ArrayList<String>();

        if (!"select".equals(statementType)) {
            tags.add(statementType + "-statement");
            return tags;
        }

        if ("point-lookup".equals(String.valueOf(labels.get("scanPattern")))) {
            tags.add("point-check");
        } else if ("range".equals(String.valueOf(labels.get("scanPattern")))) {
            tags.add("range-scan");
        } else {
            tags.add("broad-read");
        }

        if (aggregateFunctionCount > 0) {
            tags.add("aggregation");
        }

        if (hasWindow) {
            tags.add("window-analysis");
        }

        if (!"single-table".equals(String.valueOf(labels.get("joinType")))) {
            tags.add("join-analysis");
        }

        if (cteCount > 0 || subqueryCount > 0) {
            tags.add("composed-query");
        }

        if (hasLimit) {
            tags.add("bounded-inspection");
        }

        if (hasOrderBy && !hasLimit) {
            tags.add("ordered-export");
        }

        return tags;
    }

    private List<String> buildPressureSignals(
        Map<String, Object> labels,
        List<Map<String, Object>> risks,
        Map<String, Object> validations,
        int cteCount,
        int subqueryCount,
        int setOperationCount,
        boolean predicateFunctionWrapped,
        boolean hasWindow,
        boolean hasOrderBy,
        boolean hasLimit
    ) {
        List<String> signals = new ArrayList<String>();

        if ("full-table".equals(String.valueOf(labels.get("scanPattern")))) {
            signals.add("full-scan");
        }
        if ("chain".equals(String.valueOf(labels.get("joinType"))) || "snowflake".equals(String.valueOf(labels.get("joinType")))) {
            signals.add("multi-join");
        }
        if (hasWindow) {
            signals.add("window-compute");
        }
        if (predicateFunctionWrapped) {
            signals.add("predicate-function-wrap");
        }
        if ("wide-open".equals(String.valueOf(labels.get("resultSetRisk")))) {
            signals.add("wide-result");
        }
        if (hasOrderBy && !hasLimit) {
            signals.add("unbounded-sort");
        }
        if (cteCount > 0) {
            signals.add("cte-layering");
        }
        if (subqueryCount > 0) {
            signals.add("nested-subquery");
        }
        if (setOperationCount > 0) {
            signals.add("set-operation");
        }
        if ("warn".equals(String.valueOf(castMap(validations.get("projectionPushdown")).get("status")))) {
            signals.add("wide-projection");
        }

        for (Map<String, Object> risk : risks) {
            String code = String.valueOf(risk.get("code")).toLowerCase(Locale.ROOT).replace('_', '-');
            if (!signals.contains(code)) {
                signals.add(code);
            }
        }

        return signals;
    }

    private String resolveLoadClass(
        String statementType,
        Map<String, Object> labels,
        boolean hasWindow,
        boolean hasOrderBy,
        boolean hasLimit,
        int aggregateFunctionCount,
        int joinCount
    ) {
        if (!"select".equals(statementType)) {
            return "non-select";
        }
        if (hasWindow) {
            return "window-heavy";
        }
        if (joinCount >= 3) {
            return "multi-join-heavy";
        }
        if (aggregateFunctionCount > 0) {
            return "aggregation-heavy";
        }
        if (hasOrderBy && !hasLimit) {
            return "ordered-wide-read";
        }
        if ("point-lookup".equals(String.valueOf(labels.get("scanPattern")))) {
            return "point-read";
        }
        if ("range".equals(String.valueOf(labels.get("scanPattern")))) {
            return "range-read";
        }
        return "wide-read";
    }

    private int buildComplexityScore(
        int tableCount,
        int joinCount,
        int cteCount,
        int subqueryCount,
        int aggregateFunctionCount,
        int windowFunctionCount,
        int setOperationCount,
        int riskCount,
        int expressionCount
    ) {
        return tableCount * 4
            + joinCount * 8
            + cteCount * 6
            + subqueryCount * 5
            + aggregateFunctionCount * 3
            + windowFunctionCount * 10
            + setOperationCount * 6
            + riskCount * 4
            + Math.max(0, expressionCount - 4);
    }

    private String resolveComplexityTier(int complexityScore) {
        if (complexityScore >= 45) {
            return "high";
        }
        if (complexityScore >= 20) {
            return "medium";
        }
        return "low";
    }

    private List<Map<String, Object>> buildStructuralAlerts(
        List<Map<String, Object>> risks,
        Map<String, Object> validations,
        List<String> pressureSignals
    ) {
        List<Map<String, Object>> alerts = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> risk : risks) {
            alerts.add(mapOf(
                "type",
                "risk",
                "code",
                risk.get("code"),
                "severity",
                risk.get("severity"),
                "message",
                risk.get("message")
            ));
        }

        for (Map.Entry<String, Object> entry : validations.entrySet()) {
            Map<String, Object> detail = castMap(entry.getValue());
            String status = String.valueOf(detail.get("status"));
            if ("warn".equals(status) || "opportunity".equals(status)) {
                alerts.add(mapOf(
                    "type",
                    "validation",
                    "code",
                    entry.getKey(),
                    "severity",
                    status,
                    "message",
                    detail.get("message")
                ));
            }
        }

        if (!pressureSignals.isEmpty()) {
            alerts.add(mapOf(
                "type",
                "pressure-profile",
                "code",
                "pressure-signals",
                "severity",
                "info",
                "message",
                "结构侧识别到压测相关信号: " + String.join(", ", pressureSignals)
            ));
        }

        return alerts;
    }

    private Object buildRepeatedExpressionSummary(Map<String, Object> validations) {
        Map<String, Object> detail = castMap(validations.get("expressionReuse"));
        return mapOf("status", detail.get("status"), "message", detail.get("message"));
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> analyses) {
        Map<String, Integer> byStatementType = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byLoadClass = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byIntentTag = new LinkedHashMap<String, Integer>();

        for (Map<String, Object> analysis : analyses) {
            Map<String, Object> structure = castMap(analysis.get("structure"));
            Map<String, Object> pressureProfile = castMap(analysis.get("pressureProfile"));
            increment(byStatementType, String.valueOf(structure.get("statementType")));
            increment(byLoadClass, String.valueOf(pressureProfile.get("loadClass")));
            for (String tag : castStringList(analysis.get("intentTags"))) {
                increment(byIntentTag, tag);
            }
        }

        return mapOf(
            "statementCount",
            Integer.valueOf(analyses.size()),
            "byStatementType",
            byStatementType,
            "byLoadClass",
            byLoadClass,
            "byIntentTag",
            byIntentTag
        );
    }

    private void increment(Map<String, Integer> counter, String key) {
        Integer value = counter.get(key);
        counter.put(key, value == null ? Integer.valueOf(1) : Integer.valueOf(value.intValue() + 1));
    }

    private List<String> detectTableReferences(String sql) {
        List<String> references = new ArrayList<String>();
        Matcher matcher = TABLE_REFERENCE_PATTERN.matcher(sql);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
    }

    private List<Map<String, Object>> detectJoinConditions(String sql) {
        List<Map<String, Object>> conditions = new ArrayList<Map<String, Object>>();
        Matcher matcher = JOIN_CONDITION_PATTERN.matcher(sql);
        while (matcher.find()) {
            conditions.add(mapOf(
                "leftAlias",
                matcher.group(1),
                "leftKey",
                matcher.group(2),
                "rightAlias",
                matcher.group(3),
                "rightKey",
                matcher.group(4)
            ));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<String, Object>() : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<Map<String, Object>>() : (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return value == null ? new ArrayList<String>() : (List<String>) value;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }
}
