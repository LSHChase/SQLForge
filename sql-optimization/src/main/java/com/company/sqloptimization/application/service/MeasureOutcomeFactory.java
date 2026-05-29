package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AggregateCallParser.parseAggregateCall;
import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.BLOCKED_COMPLEX_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.BLOCKED_PERCENTILE_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.DIRECT_MERGEABLE_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.cleanName;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class MeasureOutcomeFactory {

    private MeasureOutcomeFactory() {
    }

    static MeasureDeriveOutcome deriveMeasure(AggregateCallInfo call, String alias, int sequence) {
        if (call.distinct && "COUNT".equals(call.functionName)) {
            return MeasureDeriveOutcome.single(exactCountDistinctMeasure(alias, call, sequence));
        }
        if (DIRECT_MERGEABLE_FUNCTIONS.contains(call.functionName)) {
            String name = measureName(alias, call.functionName, call.argument, sequence);
            LinkedHashMap<String, Object> measure = baseMeasure(name, call.functionName, call.expression, true);
            measure.put("rewriteExpression", rewriteExpression(call.functionName, name));
            return MeasureDeriveOutcome.single(measure);
        }
        if ("AVG".equals(call.functionName)) {
            return MeasureDeriveOutcome.single(avgMeasure(alias, call, sequence));
        }
        if (BLOCKED_PERCENTILE_FUNCTIONS.contains(call.functionName)) {
            return nonMergeableOutcome(alias, call, "PERCENTILE_MEASURE_NOT_MERGEABLE", "百分位指标不能在缺少精度和合并策略时安全重聚合。");
        }
        if ("MEDIAN".equals(call.functionName)) {
            return nonMergeableOutcome(alias, call, "MEDIAN_MEASURE_NOT_MERGEABLE", "中位数指标不能在缺少有序状态聚合策略时安全重聚合。");
        }
        if (BLOCKED_COMPLEX_FUNCTIONS.contains(call.functionName)) {
            return nonMergeableOutcome(alias, call, "COMPLEX_UDAF_MEASURE_NOT_MERGEABLE", "复杂聚合函数不能在 AMV-004 中证明可安全重聚合。");
        }
        return nonMergeableOutcome(alias, call, "NON_MERGEABLE_MEASURE", "指标函数不在 AMV-004 支持的可重聚合白名单中。");
    }

    static MeasureDeriveOutcome deriveRatioMeasure(String sourceExpression,
                                                   String alias,
                                                   RatioParts ratioParts,
                                                   int sequence) {
        AggregateCallInfo numerator = parseAggregateCall(ratioParts.numerator, null, false);
        AggregateCallInfo denominator = parseAggregateCall(ratioParts.denominator, null, false);
        if (!isRatioComponent(numerator) || !isRatioComponent(denominator)) {
            Map<String, Object> measure = nonMergeableMeasure(alias, "RATIO", sourceExpression, "RATIO_MEASURE_NOT_MERGEABLE");
            return new MeasureDeriveOutcome(
                measure,
                Collections.singletonList(blockingReason(
                    "RATIO_MEASURE_NOT_MERGEABLE",
                    "比例指标仅支持由 SUM/COUNT/MIN/MAX 组成的顶层除法。",
                    sourceExpression
                ))
            );
        }

        String measureName = measureName(alias, "ratio", "value", sequence);
        String numeratorName = cleanName(measureName + "_numerator");
        String denominatorName = cleanName(measureName + "_denominator");
        LinkedHashMap<String, Object> measure = baseMeasure(measureName, "RATIO", sourceExpression, true);
        measure.put("rewriteExpression",
            rewriteExpression(numerator.functionName, numeratorName)
                + " / NULLIF(" + rewriteExpression(denominator.functionName, denominatorName) + ", 0)");
        measure.put("components", Arrays.asList(
            component(numeratorName, "NUMERATOR", numerator.functionName, numerator.expression),
            component(denominatorName, "DENOMINATOR", denominator.functionName, denominator.expression)
        ));
        return MeasureDeriveOutcome.single(measure);
    }

    static Map<String, Object> nonMergeableMeasure(String alias,
                                                   String measureType,
                                                   String sourceExpression,
                                                   String blockingReasonCode) {
        LinkedHashMap<String, Object> measure = baseMeasure(measureName(alias, measureType, "blocked", 1), measureType, sourceExpression, false);
        measure.put("blockingReasonCode", blockingReasonCode);
        return measure;
    }

    static Map<String, Object> blockingReason(String code, String description, String sourceExpression) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        reason.put("sourceExpression", sourceExpression);
        return reason;
    }

    private static boolean isRatioComponent(AggregateCallInfo call) {
        return call != null && !call.distinct && DIRECT_MERGEABLE_FUNCTIONS.contains(call.functionName);
    }

    private static LinkedHashMap<String, Object> avgMeasure(String alias, AggregateCallInfo call, int sequence) {
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

    private static LinkedHashMap<String, Object> exactCountDistinctMeasure(String alias, AggregateCallInfo call, int sequence) {
        String name = measureName(alias, "count_distinct", call.argument, sequence);
        LinkedHashMap<String, Object> measure = baseMeasure(name, "COUNT_DISTINCT", call.expression, true);
        measure.put("rewriteExpression", "COUNT(DISTINCT " + distinctKeyOutputName(call.argument) + ")");
        measure.put("distinctArgument", call.argument);
        measure.put("safeReaggregateStrategy", "EXACT_DISTINCT_KEY_IN_GRAIN");
        measure.put("reviewRequired", Boolean.FALSE);
        return measure;
    }

    private static Map<String, Object> component(String name, String role, String measureType, String sourceExpression) {
        LinkedHashMap<String, Object> component = baseMeasure(name, measureType, sourceExpression, true);
        component.put("componentRole", role);
        component.put("rewriteExpression", rewriteExpression(measureType, name));
        return component;
    }

    private static MeasureDeriveOutcome nonMergeableOutcome(String alias, AggregateCallInfo call, String code, String description) {
        return new MeasureDeriveOutcome(
            nonMergeableMeasure(alias, call.functionName, call.expression, code),
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

    private static String measureName(String alias, String functionName, String argument, int sequence) {
        if (StringUtils.hasText(alias)) {
            return cleanName(alias);
        }
        String prefix = functionName == null ? "measure" : functionName.toLowerCase(Locale.ROOT);
        String suffix = "*".equals(argument) ? "rows" : argument;
        String candidate = prefix + "_" + cleanName(suffix);
        return !StringUtils.hasText(candidate) || "measure".equals(candidate) ? "measure_" + sequence : cleanName(candidate);
    }

    private static String distinctKeyOutputName(String argument) {
        String cleaned = StringUtils.hasText(argument) ? argument.replace("`", "").replace("\"", "").trim() : "";
        int dot = cleaned.lastIndexOf('.');
        if (dot >= 0) {
            cleaned = cleaned.substring(dot + 1);
        }
        cleaned = cleaned.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+", "")
            .replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "distinct_key";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "d_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }
}
