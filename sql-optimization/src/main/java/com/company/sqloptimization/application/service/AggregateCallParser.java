package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.BLOCKED_COMPLEX_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.BLOCKED_PERCENTILE_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.DIRECT_MERGEABLE_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.upperText;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.stripOuterParentheses;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class AggregateCallParser {

    private static final Pattern FUNCTION_PATTERN =
        Pattern.compile("(?is)^([A-Z_][A-Z0-9_]*)\\s*\\((.*)\\)$");

    private AggregateCallParser() {
    }

    static RatioParts splitTopLevelDivision(String expression) {
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
        return new RatioParts(candidate.substring(0, divisionIndex).trim(), candidate.substring(divisionIndex + 1).trim());
    }

    static AggregateCallInfo parseAggregateCall(String expression, String functionName, boolean distinct) {
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
        return new AggregateCallInfo(normalizedFunction, argument, cleanedExpression, distinctArgument);
    }

    static boolean containsMeasureFunction(String expression) {
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

    private static boolean isMeasureFunction(String functionName) {
        return DIRECT_MERGEABLE_FUNCTIONS.contains(functionName)
            || "AVG".equals(functionName)
            || "MEDIAN".equals(functionName)
            || BLOCKED_PERCENTILE_FUNCTIONS.contains(functionName)
            || BLOCKED_COMPLEX_FUNCTIONS.contains(functionName);
    }
}
