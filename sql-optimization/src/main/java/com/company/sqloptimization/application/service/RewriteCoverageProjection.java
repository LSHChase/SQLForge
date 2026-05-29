package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageFields.allFieldsCovered;
import static com.company.sqloptimization.application.service.RewriteCoverageFields.combinedCoverage;
import static com.company.sqloptimization.application.service.RewriteCoverageFields.containsCoverageField;
import static com.company.sqloptimization.application.service.RewriteCoverageMeasures.aggregationCovered;
import static com.company.sqloptimization.application.service.RewriteCoverageMeasures.isAggregationProjection;
import static com.company.sqloptimization.application.service.RewriteCoverageMeasures.isCountAny;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.mapList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.stringList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.text;
import static com.company.sqloptimization.application.service.RewriteCoverageSelect.containsSelectStar;
import static com.company.sqloptimization.application.service.RewriteCoverageSelect.selectList;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.normalizeExpression;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripAlias;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RewriteCoverageProjection {

    private RewriteCoverageProjection() {
    }

    static boolean coversProjection(List<Map<String, Object>> projections,
                                    List<Map<String, Object>> measures,
                                    Set<String> coverageFields,
                                    Set<String> selectedOutputs,
                                    String rewriteSql) {
        if (projections.isEmpty()) {
            return StringUtils.hasText(rewriteSql) && !containsSelectStar(rewriteSql);
        }
        if (containsSelectStar(rewriteSql)) {
            return false;
        }
        Set<String> combined = combinedCoverage(coverageFields, selectedOutputs);
        for (Map<String, Object> projection : projections) {
            if (!projectionCovered(projection, measures, combined, rewriteSql)) {
                return false;
            }
        }
        return true;
    }

    static boolean coversCommonSubgraphProjection(List<String> requiredColumns,
                                                  Set<String> mvFields,
                                                  String rewriteSql) {
        if (containsSelectStar(rewriteSql)) {
            return false;
        }
        if (requiredColumns == null || requiredColumns.isEmpty()) {
            return StringUtils.hasText(rewriteSql);
        }
        return allFieldsCovered(requiredColumns, mvFields);
    }

    static boolean coversGrouping(List<Map<String, Object>> groupBy, Set<String> coverageFields) {
        if (groupBy.isEmpty()) {
            return true;
        }
        for (Map<String, Object> item : groupBy) {
            String expression = text(item.get("expression"));
            if (coveredByAliasOrExpression(expression, "", stringList(item.get("sourceColumns")), coverageFields)) {
                continue;
            }
            return false;
        }
        return true;
    }

    static boolean rootCountProjectionPreservedByMvRewrite(Map<String, Object> advancedStructureProfile,
                                                           String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql) || advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return false;
        }
        List<Map<String, Object>> projections = mapList(advancedStructureProfile.get("projections"));
        if (projections.size() != 1) {
            return false;
        }
        String expression = stripAlias(text(projections.get(0).get("expression")), text(projections.get(0).get("alias")));
        if (!isCountAny(expression)) {
            return false;
        }
        String normalizedRewrite = normalizeExpression(selectList(rewriteSql));
        return normalizedRewrite.startsWith("COUNT(*)")
            || normalizedRewrite.startsWith("COUNT(1)");
    }

    private static boolean projectionCovered(Map<String, Object> projection,
                                             List<Map<String, Object>> measures,
                                             Set<String> combined,
                                             String rewriteSql) {
        String expression = stripAlias(text(projection.get("expression")), text(projection.get("alias")));
        String alias = text(projection.get("alias"));
        if (isAggregationProjection(projection, expression)) {
            return aggregationCovered(expression, alias, stringList(projection.get("sourceColumns")), measures, combined);
        }
        return coveredByAliasOrExpression(expression, alias, stringList(projection.get("sourceColumns")), combined);
    }

    private static boolean coveredByAliasOrExpression(String expression,
                                                      String alias,
                                                      List<String> sourceColumns,
                                                      Set<String> coverageFields) {
        if (StringUtils.hasText(alias) && containsCoverageField(coverageFields, alias)) {
            return true;
        }
        if (StringUtils.hasText(expression) && containsCoverageField(coverageFields, expression)) {
            return true;
        }
        return allFieldsCovered(sourceColumns, coverageFields);
    }
}
