package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupProfileValues.text;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.splitArguments;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.stripOuterParentheses;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class RollupTimeExpressionParser {

    static final String MV_FINEST_GRAIN = "DAY";
    static final String CALENDAR_POLICY = "NATURAL_CALENDAR_DAY_TO_MONTH_QUARTER_YEAR";

    private static final Pattern FUNCTION_PATTERN =
        Pattern.compile("(?is)^([A-Z_][A-Z0-9_]*)\\s*\\((.*)\\)$");

    private RollupTimeExpressionParser() {
    }

    static RollupParsedTimeExpression parseTimeExpression(String expression) {
        Matcher matcher = FUNCTION_PATTERN.matcher(stripOuterParentheses(expression));
        if (!matcher.matches()) {
            return null;
        }
        String functionName = matcher.group(1).toUpperCase(Locale.ROOT);
        List<String> arguments = splitArguments(matcher.group(2));
        if ("DATE_TRUNC".equals(functionName)) {
            if (arguments.size() < 2) {
                return null;
            }
            return new RollupParsedTimeExpression(
                normalizeGrain(arguments.get(0)),
                stripOuterParentheses(arguments.get(1)),
                "DATE_TRUNC"
            );
        }
        if ("TRUNC".equals(functionName)) {
            if (arguments.size() < 2) {
                return null;
            }
            String firstGrain = normalizeGrain(arguments.get(0));
            if (StringUtils.hasText(firstGrain)) {
                return new RollupParsedTimeExpression(firstGrain, stripOuterParentheses(arguments.get(1)), "DATE_TRUNC");
            }
            return new RollupParsedTimeExpression(
                normalizeGrain(arguments.get(1)),
                stripOuterParentheses(arguments.get(0)),
                "DATE_TRUNC"
            );
        }
        if ("YEAR".equals(functionName) && arguments.size() == 1) {
            return new RollupParsedTimeExpression("YEAR", stripOuterParentheses(arguments.get(0)), "YEAR_FUNCTION");
        }
        if ("MONTH".equals(functionName) && arguments.size() == 1) {
            return new RollupParsedTimeExpression("MONTH_OF_YEAR", stripOuterParentheses(arguments.get(0)), "MONTH_FUNCTION");
        }
        if ("DAY".equals(functionName) && arguments.size() == 1) {
            return new RollupParsedTimeExpression(MV_FINEST_GRAIN, stripOuterParentheses(arguments.get(0)), "DAY_FUNCTION");
        }
        if ("DATE_FORMAT".equals(functionName)) {
            return null;
        }
        return null;
    }

    static String targetExpression(String targetGrain, String mvTimeColumn, String expressionKind) {
        if ("YEAR_FUNCTION".equals(expressionKind)) {
            return "YEAR(" + mvTimeColumn + ")";
        }
        return "DATE_TRUNC('" + targetGrain.toLowerCase(Locale.ROOT) + "', " + mvTimeColumn + ")";
    }

    static boolean isSupportedTargetGrain(String grain) {
        return "MONTH".equals(grain) || "QUARTER".equals(grain) || "YEAR".equals(grain);
    }

    private static String normalizeGrain(String value) {
        String cleaned = text(value)
            .replace("'", "")
            .replace("\"", "")
            .trim()
            .toUpperCase(Locale.ROOT);
        if ("MONTH".equals(cleaned) || "MON".equals(cleaned) || "MM".equals(cleaned)) {
            return "MONTH";
        }
        if ("QUARTER".equals(cleaned) || "Q".equals(cleaned) || "QTR".equals(cleaned)) {
            return "QUARTER";
        }
        if ("YEAR".equals(cleaned) || "YYYY".equals(cleaned) || "YY".equals(cleaned)) {
            return "YEAR";
        }
        if ("WEEK".equals(cleaned) || "WW".equals(cleaned) || "IW".equals(cleaned)) {
            return "WEEK";
        }
        if ("DAY".equals(cleaned) || "DD".equals(cleaned) || "D".equals(cleaned)) {
            return MV_FINEST_GRAIN;
        }
        return "";
    }
}
