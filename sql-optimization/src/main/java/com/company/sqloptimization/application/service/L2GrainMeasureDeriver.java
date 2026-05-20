package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2GrainMeasureDeriver {

    static final String MV_TYPE_PARAMETERIZED_AGG = "PARAMETERIZED_AGG_MV";
    static final String MV_TYPE_PREJOIN = "PREJOIN_MV";
    static final String MV_TYPE_STAR_AGG = "STAR_AGG_MV";
    static final String MV_TYPE_ROLLUP = "ROLLUP_MV";
    static final String MV_TYPE_COMMON_SUBGRAPH = "COMMON_SUBGRAPH_MV";

    private static final Set<String> DIRECT_MERGEABLE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("SUM", "COUNT", "MIN", "MAX"));
    private static final Set<String> BLOCKED_PERCENTILE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("APPROX_PERCENTILE", "PERCENTILE_CONT", "PERCENTILE_DISC", "QUANTILE"));
    private static final Set<String> BLOCKED_COMPLEX_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("APPROX_DISTINCT", "GROUP_CONCAT", "STRING_AGG", "LISTAGG"));
    private static final Set<String> TIME_ROLLUP_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("DATE_TRUNC", "TRUNC", "DATE_FORMAT", "YEAR", "MONTH", "DAY"));
    private static final Pattern FUNCTION_PATTERN =
        Pattern.compile("(?is)^([A-Z_][A-Z0-9_]*)\\s*\\((.*)\\)$");
    private static final Pattern ALIAS_PATTERN =
        Pattern.compile("(?is)\\s+AS\\s+([A-Z_][A-Z0-9_]*)\\s*$");

    private L2GrainMeasureDeriver() {
    }

    static DerivationResult derive(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                   L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        return derive(profile == null ? null : profile.toAdvancedStructureProfile(), predicateClassification);
    }

    static DerivationResult derive(Map<String, Object> advancedStructureProfile,
                                   L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return DerivationResult.empty();
        }

        GrainDerivation grainDerivation = deriveGrain(advancedStructureProfile, predicateClassification);
        MeasureDerivation measureDerivation = deriveMeasures(
            mapList(advancedStructureProfile.get("projections")),
            mapList(advancedStructureProfile.get("aggregations"))
        );
        LinkedHashMap<String, Object> coverage = coverage(
            grainDerivation,
            measureDerivation,
            predicateClassification
        );
        List<Map<String, Object>> joinGraph = mapList(advancedStructureProfile.get("joinGraph"));
        String mvType = mvType(advancedStructureProfile, joinGraph, grainDerivation, measureDerivation);
        return new DerivationResult(
            mvType,
            grainDerivation.grain,
            grainDerivation.grain,
            measureDerivation.measures,
            joinGraph,
            coverage,
            measureDerivation.blockingReasons,
            reviewWarnings(joinGraph, mvType)
        );
    }

    private static String mvType(Map<String, Object> advancedStructureProfile,
                                 List<Map<String, Object>> joinGraph,
                                 GrainDerivation grainDerivation,
                                 MeasureDerivation measureDerivation) {
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            return MV_TYPE_COMMON_SUBGRAPH;
        }
        if (!joinGraph.isEmpty()) {
            return joinGraph.size() >= 2 && !measureDerivation.measures.isEmpty()
                ? MV_TYPE_STAR_AGG
                : MV_TYPE_PREJOIN;
        }
        return grainDerivation.hasTimeRollup ? MV_TYPE_ROLLUP : MV_TYPE_PARAMETERIZED_AGG;
    }

    private static List<Map<String, Object>> reviewWarnings(List<Map<String, Object>> joinGraph, String mvType) {
        if (MV_TYPE_COMMON_SUBGRAPH.equals(mvType) || joinGraph == null || joinGraph.isEmpty()) {
            return Collections.emptyList();
        }
        if (MV_TYPE_STAR_AGG.equals(mvType)) {
            LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
            warning.put("code", "STAR_SCHEMA_METADATA_MISSING");
            warning.put(
                "description",
                "缺少唯一键、维表基数与 Join 选择率元数据，STAR_AGG_MV 只能基于静态 Join 拓扑和指标来源保守生成。"
            );
            warning.put("requiredEvidence", Arrays.asList(
                "FACT_TABLE_ROW_COUNT",
                "DIMENSION_KEY_UNIQUENESS",
                "JOIN_SELECTIVITY"
            ));
            warning.put("generatedAllowed", Boolean.TRUE);
            return Collections.<Map<String, Object>>singletonList(warning);
        }
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", "ROW_AMPLIFICATION_METADATA_MISSING");
        warning.put(
            "description",
            "缺少唯一键、表基数与 Join 选择率元数据，PREJOIN_MV 只能说明 Join key 形态安全，不能证明无行数放大。"
        );
        warning.put("requiredEvidence", Arrays.asList(
            "JOIN_KEY_UNIQUENESS",
            "TABLE_CARDINALITY",
            "JOIN_SELECTIVITY"
        ));
        warning.put("generatedAllowed", Boolean.TRUE);
        return Collections.<Map<String, Object>>singletonList(warning);
    }

    private static GrainDerivation deriveGrain(Map<String, Object> advancedStructureProfile,
                                               L2PredicateClassifier.PredicateClassificationResult
                                                   predicateClassification) {
        List<String> grain = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        boolean hasTimeRollup = false;

        for (Map<String, Object> item : mapList(advancedStructureProfile.get("groupBy"))) {
            String expression = text(item.get("expression"));
            addText(grain, seen, expression);
            if (isStableTimeRollup(expression)) {
                hasTimeRollup = true;
                addTextList(grain, seen, stringList(item.get("sourceColumns")));
            }
        }
        if (predicateClassification != null) {
            addPredicateFields(grain, seen, predicateClassification.getExternalizedPredicates());
            addPredicateFields(grain, seen, predicateClassification.getSecurityPredicates());
        }
        for (Map<String, Object> item : mapList(advancedStructureProfile.get("timeFunctions"))) {
            String functionName = upperText(item.get("functionName"));
            if (TIME_ROLLUP_FUNCTIONS.contains(functionName)) {
                hasTimeRollup = true;
                addTextList(grain, seen, stringList(item.get("sourceColumns")));
            }
        }

        return new GrainDerivation(grain, hasTimeRollup);
    }

    private static void addPredicateFields(List<String> grain,
                                           Set<String> seen,
                                           List<Map<String, Object>> predicates) {
        for (Map<String, Object> predicate : predicates) {
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                String field = leftPredicateField(text(predicate.get("expression")));
                addText(grain, seen, field);
            } else {
                addTextList(grain, seen, sourceColumns);
            }
        }
    }

    private static String leftPredicateField(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String[] operators = {" BETWEEN ", " IN ", ">=", "<=", "<>", "!=", "=", ">", "<", " LIKE "};
        String upper = expression.toUpperCase(Locale.ROOT);
        int index = -1;
        for (String operator : operators) {
            index = upper.indexOf(operator);
            if (index >= 0) {
                break;
            }
        }
        if (index < 0) {
            return "";
        }
        return expression.substring(0, index).trim();
    }

    private static boolean isStableTimeRollup(String expression) {
        String upper = upperText(expression);
        for (String functionName : TIME_ROLLUP_FUNCTIONS) {
            if (upper.startsWith(functionName + "(") || upper.contains(functionName + "(")) {
                return true;
            }
        }
        return false;
    }

    private static MeasureDerivation deriveMeasures(List<Map<String, Object>> projections,
                                                    List<Map<String, Object>> aggregations) {
        List<Map<String, Object>> measures = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        Set<String> consumedAggregateExpressions = new LinkedHashSet<String>();
        int sequence = 1;

        for (Map<String, Object> projection : projections) {
            String alias = text(projection.get("alias"));
            String sourceExpression = stripAlias(text(projection.get("expression")), alias);
            if (!StringUtils.hasText(sourceExpression)) {
                continue;
            }
            RatioParts ratioParts = splitTopLevelDivision(sourceExpression);
            if (ratioParts != null) {
                DeriveOutcome outcome = deriveRatioMeasure(sourceExpression, alias, ratioParts, sequence);
                sequence++;
                measures.add(outcome.measure);
                blockingReasons.addAll(outcome.blockingReasons);
                consumedAggregateExpressions.add(normalizeExpression(ratioParts.numerator));
                consumedAggregateExpressions.add(normalizeExpression(ratioParts.denominator));
                continue;
            }
            AggregateCall call = parseAggregateCall(sourceExpression, null, false);
            if (call != null) {
                DeriveOutcome outcome = deriveMeasure(call, alias, sequence);
                sequence++;
                measures.add(outcome.measure);
                blockingReasons.addAll(outcome.blockingReasons);
                consumedAggregateExpressions.add(normalizeExpression(call.expression));
            } else if (containsMeasureFunction(sourceExpression)) {
                Map<String, Object> measure = nonMergeableMeasure(
                    alias,
                    "COMPLEX_EXPRESSION",
                    sourceExpression,
                    "COMPLEX_MEASURE_EXPRESSION_NOT_MERGEABLE"
                );
                measures.add(measure);
                blockingReasons.add(blockingReason(
                    "COMPLEX_MEASURE_EXPRESSION_NOT_MERGEABLE",
                    "复杂指标表达式无法在 AMV-004 中证明可重聚合。",
                    sourceExpression
                ));
            }
        }

        for (Map<String, Object> aggregation : aggregations) {
            AggregateCall call = parseAggregateCall(
                text(aggregation.get("expression")),
                text(aggregation.get("functionName")),
                booleanValue(aggregation.get("distinct"))
            );
            if (call == null || consumedAggregateExpressions.contains(normalizeExpression(call.expression))) {
                continue;
            }
            DeriveOutcome outcome = deriveMeasure(call, "", sequence);
            sequence++;
            measures.add(outcome.measure);
            blockingReasons.addAll(outcome.blockingReasons);
        }
        return new MeasureDerivation(measures, blockingReasons);
    }

    private static DeriveOutcome deriveMeasure(AggregateCall call, String alias, int sequence) {
        if (call.distinct && "COUNT".equals(call.functionName)) {
            return nonMergeableOutcome(
                alias,
                call,
                "COUNT_DISTINCT_MEASURE_NOT_MERGEABLE",
                "COUNT(DISTINCT) 不能在缺少 sketch/状态聚合策略时安全重聚合。"
            );
        }
        if (DIRECT_MERGEABLE_FUNCTIONS.contains(call.functionName)) {
            String name = measureName(alias, call.functionName, call.argument, sequence);
            LinkedHashMap<String, Object> measure = baseMeasure(name, call.functionName, call.expression, true);
            measure.put("rewriteExpression", rewriteExpression(call.functionName, name));
            return DeriveOutcome.single(measure);
        }
        if ("AVG".equals(call.functionName)) {
            return DeriveOutcome.single(avgMeasure(alias, call, sequence));
        }
        if (BLOCKED_PERCENTILE_FUNCTIONS.contains(call.functionName)) {
            return nonMergeableOutcome(
                alias,
                call,
                "PERCENTILE_MEASURE_NOT_MERGEABLE",
                "百分位指标不能在缺少精度和合并策略时安全重聚合。"
            );
        }
        if ("MEDIAN".equals(call.functionName)) {
            return nonMergeableOutcome(
                alias,
                call,
                "MEDIAN_MEASURE_NOT_MERGEABLE",
                "中位数指标不能在缺少有序状态聚合策略时安全重聚合。"
            );
        }
        if (BLOCKED_COMPLEX_FUNCTIONS.contains(call.functionName)) {
            return nonMergeableOutcome(
                alias,
                call,
                "COMPLEX_UDAF_MEASURE_NOT_MERGEABLE",
                "复杂聚合函数不能在 AMV-004 中证明可安全重聚合。"
            );
        }
        return nonMergeableOutcome(
            alias,
            call,
            "NON_MERGEABLE_MEASURE",
            "指标函数不在 AMV-004 支持的可重聚合白名单中。"
        );
    }

    private static DeriveOutcome deriveRatioMeasure(String sourceExpression,
                                                    String alias,
                                                    RatioParts ratioParts,
                                                    int sequence) {
        AggregateCall numerator = parseAggregateCall(ratioParts.numerator, null, false);
        AggregateCall denominator = parseAggregateCall(ratioParts.denominator, null, false);
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (!isRatioComponent(numerator) || !isRatioComponent(denominator)) {
            Map<String, Object> measure = nonMergeableMeasure(
                alias,
                "RATIO",
                sourceExpression,
                "RATIO_MEASURE_NOT_MERGEABLE"
            );
            reasons.add(blockingReason(
                "RATIO_MEASURE_NOT_MERGEABLE",
                "比例指标仅支持由 SUM/COUNT/MIN/MAX 组成的顶层除法。",
                sourceExpression
            ));
            return new DeriveOutcome(measure, reasons);
        }

        String measureName = measureName(alias, "ratio", "value", sequence);
        String numeratorName = cleanName(measureName + "_numerator");
        String denominatorName = cleanName(measureName + "_denominator");
        Map<String, Object> numeratorComponent = component(
            numeratorName,
            "NUMERATOR",
            numerator.functionName,
            numerator.expression
        );
        Map<String, Object> denominatorComponent = component(
            denominatorName,
            "DENOMINATOR",
            denominator.functionName,
            denominator.expression
        );
        LinkedHashMap<String, Object> measure = baseMeasure(measureName, "RATIO", sourceExpression, true);
        measure.put("rewriteExpression",
            rewriteExpression(numerator.functionName, numeratorName)
                + " / NULLIF(" + rewriteExpression(denominator.functionName, denominatorName) + ", 0)");
        measure.put("components", Arrays.asList(numeratorComponent, denominatorComponent));
        return DeriveOutcome.single(measure);
    }

    private static boolean isRatioComponent(AggregateCall call) {
        return call != null && !call.distinct && DIRECT_MERGEABLE_FUNCTIONS.contains(call.functionName);
    }

    private static LinkedHashMap<String, Object> avgMeasure(String alias, AggregateCall call, int sequence) {
        String name = measureName(alias, "avg", call.argument, sequence);
        String sumName = cleanName(name + "_sum");
        String countName = cleanName(name + "_count");
        LinkedHashMap<String, Object> measure = baseMeasure(name, "AVG", call.expression, true);
        measure.put("rewriteExpression", "SUM(" + sumName + ") / NULLIF(SUM(" + countName + "), 0)");
        measure.put("components", Arrays.asList(
            component(sumName, "SUM_COMPONENT", "SUM", "SUM(" + call.argument + ")"),
            component(countName, "COUNT_COMPONENT", "COUNT", "COUNT(" + call.argument + ")")
        ));
        return measure;
    }

    private static Map<String, Object> component(String name,
                                                 String role,
                                                 String measureType,
                                                 String sourceExpression) {
        LinkedHashMap<String, Object> component = baseMeasure(name, measureType, sourceExpression, true);
        component.put("componentRole", role);
        component.put("rewriteExpression", rewriteExpression(measureType, name));
        return component;
    }

    private static DeriveOutcome nonMergeableOutcome(String alias,
                                                     AggregateCall call,
                                                     String code,
                                                     String description) {
        Map<String, Object> measure = nonMergeableMeasure(alias, call.functionName, call.expression, code);
        return new DeriveOutcome(
            measure,
            Collections.singletonList(blockingReason(code, description, call.expression))
        );
    }

    private static LinkedHashMap<String, Object> baseMeasure(String name,
                                                            String measureType,
                                                            String sourceExpression,
                                                            boolean mergeable) {
        LinkedHashMap<String, Object> measure = new LinkedHashMap<String, Object>();
        measure.put("name", name);
        measure.put("measureType", measureType);
        measure.put("sourceExpression", sourceExpression);
        measure.put("mergeable", Boolean.valueOf(mergeable));
        return measure;
    }

    private static Map<String, Object> nonMergeableMeasure(String alias,
                                                           String measureType,
                                                           String sourceExpression,
                                                           String blockingReasonCode) {
        LinkedHashMap<String, Object> measure = baseMeasure(
            measureName(alias, measureType, "blocked", 1),
            measureType,
            sourceExpression,
            false
        );
        measure.put("blockingReasonCode", blockingReasonCode);
        return measure;
    }

    private static Map<String, Object> blockingReason(String code, String description, String sourceExpression) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        reason.put("sourceExpression", sourceExpression);
        return reason;
    }

    private static String rewriteExpression(String functionName, String measureName) {
        if ("COUNT".equals(functionName) || "SUM".equals(functionName)) {
            return "SUM(" + measureName + ")";
        }
        if ("MIN".equals(functionName)) {
            return "MIN(" + measureName + ")";
        }
        if ("MAX".equals(functionName)) {
            return "MAX(" + measureName + ")";
        }
        return functionName + "(" + measureName + ")";
    }

    private static RatioParts splitTopLevelDivision(String expression) {
        String candidate = stripOuterParentheses(expression);
        int depth = 0;
        int divisionIndex = -1;
        for (int i = 0; i < candidate.length(); i++) {
            char ch = candidate.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            } else if (ch == '/' && depth == 0) {
                if (divisionIndex >= 0) {
                    return null;
                }
                divisionIndex = i;
            }
        }
        if (divisionIndex <= 0 || divisionIndex >= candidate.length() - 1) {
            return null;
        }
        return new RatioParts(
            candidate.substring(0, divisionIndex).trim(),
            candidate.substring(divisionIndex + 1).trim()
        );
    }

    private static AggregateCall parseAggregateCall(String expression, String functionName, boolean distinct) {
        String cleanedExpression = stripOuterParentheses(expression);
        Matcher matcher = FUNCTION_PATTERN.matcher(cleanedExpression);
        if (!matcher.matches()) {
            return null;
        }
        String normalizedFunction = StringUtils.hasText(functionName)
            ? upperText(functionName)
            : matcher.group(1).toUpperCase(Locale.ROOT);
        String argument = matcher.group(2).trim();
        boolean distinctArgument = distinct;
        if (argument.toUpperCase(Locale.ROOT).startsWith("DISTINCT ")) {
            distinctArgument = true;
            argument = argument.substring("DISTINCT ".length()).trim();
        }
        if (!isMeasureFunction(normalizedFunction)) {
            return null;
        }
        return new AggregateCall(normalizedFunction, argument, cleanedExpression, distinctArgument);
    }

    private static boolean isMeasureFunction(String functionName) {
        return DIRECT_MERGEABLE_FUNCTIONS.contains(functionName)
            || "AVG".equals(functionName)
            || "MEDIAN".equals(functionName)
            || BLOCKED_PERCENTILE_FUNCTIONS.contains(functionName)
            || BLOCKED_COMPLEX_FUNCTIONS.contains(functionName);
    }

    private static boolean containsMeasureFunction(String expression) {
        String upper = upperText(expression);
        for (String functionName : DIRECT_MERGEABLE_FUNCTIONS) {
            if (upper.contains(functionName + "(")) {
                return true;
            }
        }
        if (upper.contains("AVG(") || upper.contains("MEDIAN(")) {
            return true;
        }
        for (String functionName : BLOCKED_PERCENTILE_FUNCTIONS) {
            if (upper.contains(functionName + "(")) {
                return true;
            }
        }
        for (String functionName : BLOCKED_COMPLEX_FUNCTIONS) {
            if (upper.contains(functionName + "(")) {
                return true;
            }
        }
        return false;
    }

    private static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        if (StringUtils.hasText(alias)) {
            result = result.replaceAll("(?is)\\s+AS\\s+" + Pattern.quote(alias.trim()) + "\\s*$", "");
        }
        Matcher matcher = ALIAS_PATTERN.matcher(result);
        if (matcher.find()) {
            result = result.substring(0, matcher.start()).trim();
        }
        return result;
    }

    private static String stripOuterParentheses(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        while (result.startsWith("(") && result.endsWith(")") && wrapsWholeExpression(result)) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private static boolean wrapsWholeExpression(String expression) {
        int depth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0 && i < expression.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static String measureName(String alias, String functionName, String argument, int sequence) {
        if (StringUtils.hasText(alias)) {
            return cleanName(alias);
        }
        String prefix = functionName == null ? "measure" : functionName.toLowerCase(Locale.ROOT);
        String suffix = "*".equals(argument) ? "rows" : argument;
        String candidate = prefix + "_" + cleanName(suffix);
        if (!StringUtils.hasText(candidate) || "measure".equals(candidate)) {
            candidate = "measure_" + sequence;
        }
        return cleanName(candidate);
    }

    private static String cleanName(String value) {
        String cleaned = StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
            : "";
        cleaned = cleaned.replaceAll("^_+", "").replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "measure";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "m_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }

    private static String normalizeExpression(String expression) {
        return StringUtils.hasText(expression)
            ? expression.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
            : "";
    }

    private static LinkedHashMap<String, Object> coverage(GrainDerivation grainDerivation,
                                                          MeasureDerivation measureDerivation,
                                                          L2PredicateClassifier.PredicateClassificationResult
                                                              predicateClassification) {
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("coversProjection", Boolean.valueOf(measureDerivation.blockingReasons.isEmpty()));
        coverage.put("coversFilters", Boolean.valueOf(predicateClassification == null
            || !predicateClassification.hasBlockedPredicates()));
        coverage.put("coversGrouping", Boolean.TRUE);
        coverage.put("coversMeasures", Boolean.valueOf(measureDerivation.blockingReasons.isEmpty()
            && !measureDerivation.measures.isEmpty()));
        coverage.put("coversSecurity", Boolean.valueOf(securityCovered(
            grainDerivation.grain,
            predicateClassification == null
                ? Collections.<Map<String, Object>>emptyList()
                : predicateClassification.getSecurityPredicates()
        )));
        return coverage;
    }

    private static boolean securityCovered(List<String> grain, List<Map<String, Object>> securityPredicates) {
        if (securityPredicates == null || securityPredicates.isEmpty()) {
            return true;
        }
        Set<String> normalizedGrain = new LinkedHashSet<String>();
        for (String item : grain) {
            normalizedGrain.add(normalizeName(item));
        }
        for (Map<String, Object> predicate : securityPredicates) {
            List<String> columns = stringList(predicate.get("sourceColumns"));
            if (columns.isEmpty()) {
                columns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            for (String column : columns) {
                if (!normalizedGrain.contains(normalizeName(column))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void addTextList(List<String> target, Set<String> seen, List<String> values) {
        for (String value : values) {
            addText(target, seen, value);
        }
    }

    private static void addText(List<String> target, Set<String> seen, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String cleaned = value.trim();
        String key = normalizeName(cleaned);
        if (!StringUtils.hasText(key) || seen.contains(key)) {
            return;
        }
        seen.add(key);
        target.add(cleaned);
    }

    private static String normalizeName(String value) {
        return StringUtils.hasText(value)
            ? value.replace('`', ' ')
                .replace('"', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
            : "";
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String upperText(Object value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }
        return result;
    }

    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (Iterable<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add(copyMap((Map<?, ?>) item));
            }
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    private static final class GrainDerivation {
        private final List<String> grain;
        private final boolean hasTimeRollup;

        private GrainDerivation(List<String> grain, boolean hasTimeRollup) {
            this.grain = Collections.unmodifiableList(new ArrayList<String>(grain));
            this.hasTimeRollup = hasTimeRollup;
        }
    }

    private static final class MeasureDerivation {
        private final List<Map<String, Object>> measures;
        private final List<Map<String, Object>> blockingReasons;

        private MeasureDerivation(List<Map<String, Object>> measures,
                                  List<Map<String, Object>> blockingReasons) {
            this.measures = immutableMapList(measures);
            this.blockingReasons = immutableMapList(blockingReasons);
        }
    }

    private static final class DeriveOutcome {
        private final Map<String, Object> measure;
        private final List<Map<String, Object>> blockingReasons;

        private DeriveOutcome(Map<String, Object> measure, List<Map<String, Object>> blockingReasons) {
            this.measure = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(measure));
            this.blockingReasons = immutableMapList(blockingReasons);
        }

        private static DeriveOutcome single(Map<String, Object> measure) {
            return new DeriveOutcome(measure, Collections.<Map<String, Object>>emptyList());
        }
    }

    private static final class AggregateCall {
        private final String functionName;
        private final String argument;
        private final String expression;
        private final boolean distinct;

        private AggregateCall(String functionName, String argument, String expression, boolean distinct) {
            this.functionName = functionName;
            this.argument = argument;
            this.expression = expression;
            this.distinct = distinct;
        }
    }

    private static final class RatioParts {
        private final String numerator;
        private final String denominator;

        private RatioParts(String numerator, String denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

    static final class DerivationResult {
        private final String mvType;
        private final List<String> grain;
        private final List<String> dimensions;
        private final List<Map<String, Object>> measures;
        private final List<Map<String, Object>> joinGraph;
        private final Map<String, Object> coverage;
        private final List<Map<String, Object>> blockingReasons;
        private final List<Map<String, Object>> reviewWarnings;

        private DerivationResult(String mvType,
                                 List<String> grain,
                                 List<String> dimensions,
                                 List<Map<String, Object>> measures,
                                 List<Map<String, Object>> joinGraph,
                                 Map<String, Object> coverage,
                                 List<Map<String, Object>> blockingReasons,
                                 List<Map<String, Object>> reviewWarnings) {
            this.mvType = mvType;
            this.grain = Collections.unmodifiableList(new ArrayList<String>(grain));
            this.dimensions = Collections.unmodifiableList(new ArrayList<String>(dimensions));
            this.measures = immutableMapList(measures);
            this.joinGraph = immutableMapList(joinGraph);
            this.coverage = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(coverage));
            this.blockingReasons = immutableMapList(blockingReasons);
            this.reviewWarnings = immutableMapList(reviewWarnings);
        }

        private static DerivationResult empty() {
            LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
            coverage.put("coversProjection", Boolean.FALSE);
            coverage.put("coversFilters", Boolean.FALSE);
            coverage.put("coversGrouping", Boolean.FALSE);
            coverage.put("coversMeasures", Boolean.FALSE);
            coverage.put("coversSecurity", Boolean.FALSE);
            return new DerivationResult(
                MV_TYPE_PARAMETERIZED_AGG,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                coverage,
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList()
            );
        }

        String getMvType() {
            return mvType;
        }

        List<String> getGrain() {
            return grain;
        }

        List<String> getDimensions() {
            return dimensions;
        }

        List<Map<String, Object>> getMeasures() {
            return measures;
        }

        List<Map<String, Object>> getJoinGraph() {
            return joinGraph;
        }

        Map<String, Object> getCoverage() {
            return coverage;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        List<Map<String, Object>> getReviewWarnings() {
            return reviewWarnings;
        }
    }

    private static List<Map<String, Object>> immutableMapList(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : source) {
            result.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(item)));
        }
        return Collections.unmodifiableList(result);
    }
}
