package com.sqlforge.backend.service;

import com.sqlforge.backend.model.SqlJoinCondition;
import com.sqlforge.backend.model.SqlStructureAst;
import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlIntentAnalysisService {

    private final SqlAssessmentService sqlAssessmentService;
    private final SqlParserAdapter sqlParserAdapter;

    public SqlIntentAnalysisService(SqlAssessmentService sqlAssessmentService, SqlParserAdapter sqlParserAdapter) {
        this.sqlAssessmentService = sqlAssessmentService;
        this.sqlParserAdapter = sqlParserAdapter;
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
        List<String> tables = castStringList(assessment.get("tables"));
        SqlStructureAst structureAst = sqlParserAdapter.parse(normalizedSql);
        List<String> intentTags = buildIntentTags(
            structureAst.getStatementType(),
            labels,
            structureAst.getCteCount(),
            structureAst.getSubqueryCount(),
            structureAst.getAggregateFunctionCount(),
            structureAst.isHasWindow(),
            structureAst.isHasLimit(),
            structureAst.isHasOrderBy()
        );
        List<String> pressureSignals = buildPressureSignals(
            labels,
            risks,
            validations,
            structureAst.getCteCount(),
            structureAst.getSubqueryCount(),
            structureAst.getSetOperationCount(),
            structureAst.isPredicateFunctionWrapped(),
            structureAst.isHasWindow(),
            structureAst.isHasOrderBy(),
            structureAst.isHasLimit()
        );
        int complexityScore = buildComplexityScore(
            tables.size(),
            structureAst.getJoinConditions().size(),
            structureAst.getCteCount(),
            structureAst.getSubqueryCount(),
            structureAst.getAggregateFunctionCount(),
            structureAst.getWindowFunctionCount(),
            structureAst.getSetOperationCount(),
            risks.size(),
            structureAst.getExpressionCount()
        );

        Map<String, Object> structure = mapOf(
            "normalizedSql",
            structureAst.getNormalizedSql(),
            "statementType",
            structureAst.getStatementType(),
            "tableCount",
            Integer.valueOf(tables.size()),
            "tables",
            tables,
            "tableReferences",
            structureAst.getTableReferences(),
            "joinCount",
            Integer.valueOf(structureAst.getJoinConditions().size()),
            "joinType",
            labels.get("joinType"),
            "joinConditions",
            toJoinConditionMaps(structureAst.getJoinConditions()),
            "cteCount",
            Integer.valueOf(structureAst.getCteCount()),
            "subqueryCount",
            Integer.valueOf(structureAst.getSubqueryCount()),
            "setOperationCount",
            Integer.valueOf(structureAst.getSetOperationCount()),
            "hasWhere",
            Boolean.valueOf(structureAst.isHasWhere()),
            "hasGroupBy",
            Boolean.valueOf(structureAst.isHasGroupBy()),
            "hasHaving",
            Boolean.valueOf(structureAst.isHasHaving()),
            "hasOrderBy",
            Boolean.valueOf(structureAst.isHasOrderBy()),
            "hasLimit",
            Boolean.valueOf(structureAst.isHasLimit()),
            "hasDistinct",
            Boolean.valueOf(structureAst.isHasDistinct()),
            "hasWindow",
            Boolean.valueOf(structureAst.isHasWindow())
        );

        Map<String, Object> projection = mapOf(
            "expressionCount",
            Integer.valueOf(structureAst.getExpressionCount()),
            "aggregateFunctionCount",
            Integer.valueOf(structureAst.getAggregateFunctionCount()),
            "windowFunctionCount",
            Integer.valueOf(structureAst.getWindowFunctionCount()),
            "hasSelectStar",
            Boolean.valueOf(structureAst.isHasSelectStar()),
            "repeatedExpressions",
            buildRepeatedExpressionSummary(validations)
        );

        Map<String, Object> predicates = mapOf(
            "scanPattern",
            labels.get("scanPattern"),
            "predicateFunctionWrapped",
            Boolean.valueOf(structureAst.isPredicateFunctionWrapped()),
            "timeFilterColumns",
            structureAst.getTimeFilterColumns(),
            "resultSetRisk",
            labels.get("resultSetRisk")
        );

        Map<String, Object> pressureProfile = mapOf(
            "parserContract",
            "typed-structure-ast",
            "loadClass",
            resolveLoadClass(
                structureAst.getStatementType(),
                labels,
                structureAst.isHasWindow(),
                structureAst.isHasOrderBy(),
                structureAst.isHasLimit(),
                structureAst.getAggregateFunctionCount(),
                structureAst.getJoinConditions().size()
            ),
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

    private List<Map<String, Object>> toJoinConditionMaps(List<SqlJoinCondition> joinConditions) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SqlJoinCondition joinCondition : joinConditions) {
            result.add(mapOf(
                "leftAlias",
                joinCondition.getLeftAlias(),
                "leftKey",
                joinCondition.getLeftKey(),
                "rightAlias",
                joinCondition.getRightAlias(),
                "rightKey",
                joinCondition.getRightKey()
            ));
        }
        return result;
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
