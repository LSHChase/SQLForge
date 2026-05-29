package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageFields.allFieldsCovered;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.stringList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.text;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class RewriteCoveragePredicates {

    private RewriteCoveragePredicates() {
    }

    static boolean coversPredicates(List<Map<String, Object>> predicates, java.util.Set<String> coverageFields) {
        for (Map<String, Object> predicate : predicates) {
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                sourceColumns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            if (!allFieldsCovered(sourceColumns, coverageFields)) {
                return false;
            }
        }
        return true;
    }

    static String leftPredicateField(String expression) {
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
        return index < 0 ? "" : expression.substring(0, index).trim();
    }
}
