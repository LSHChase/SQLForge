package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.cleanName;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.columnName;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.containsEquivalentSource;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.RollupProfileValues.mapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.reason;
import static com.company.sqloptimization.application.service.RollupProfileValues.stringList;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.containsFiscalCalendar;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.containsTimezoneDependency;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.isTimeFunctionExpression;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.stripAlias;
import static com.company.sqloptimization.application.service.RollupTimeExpressionParser.MV_FINEST_GRAIN;
import static com.company.sqloptimization.application.service.RollupTimeExpressionParser.isSupportedTargetGrain;
import static com.company.sqloptimization.application.service.RollupTimeExpressionParser.parseTimeExpression;
import static com.company.sqloptimization.application.service.RollupTimeExpressionParser.targetExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class RollupPlanBuilder {

    private RollupPlanBuilder() {
    }

    static RollupPlan rollupPlan(Map<String, Object> advancedStructureProfile) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return RollupPlan.blocked(
                "",
                Collections.singletonList(reason(
                    "TIME_ROLLUP_EXPRESSION_REQUIRED",
                    "缺少高级结构画像，不能识别时间上卷表达式。"
                ))
            );
        }
        List<RollupPlan> candidates = new ArrayList<RollupPlan>();
        for (Map<String, Object> groupByItem : mapList(advancedStructureProfile.get("groupBy"))) {
            String expression = text(groupByItem.get("expression"));
            if (isTimeFunctionExpression(expression)) {
                candidates.add(rollupPlanForExpression(expression, stringList(groupByItem.get("sourceColumns"))));
            }
        }
        if (candidates.isEmpty()) {
            return RollupPlan.blocked(
                "",
                Collections.singletonList(reason(
                    "TIME_ROLLUP_GROUP_BY_REQUIRED",
                    "ROLLUP_MV 需要在 GROUP BY 中出现可归一化的时间粒度表达式。"
                ))
            );
        }
        if (candidates.size() > 1) {
            List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
            reasons.add(reason(
                "MULTIPLE_TIME_ROLLUP_EXPRESSIONS_UNSUPPORTED",
                "多个时间上卷表达式需要证明同源同日历策略，AMV-008 暂不自动生成。"
            ));
            return RollupPlan.blocked(candidates.get(0).queryTimeExpression, reasons);
        }
        return candidates.get(0);
    }

    private static RollupPlan rollupPlanForExpression(String expression, List<String> sourceColumns) {
        String cleanedExpression = stripAlias(expression, "");
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (containsTimezoneDependency(cleanedExpression)) {
            reasons.add(reason(
                "TIMEZONE_DEPENDENT_ROLLUP_REQUIRES_REVIEW",
                "时间表达式包含显式时区转换或时区语义，缺少时区策略时不能自动生成 Rollup MV。"
            ));
        }
        if (containsFiscalCalendar(cleanedExpression) || containsFiscalCalendar(sourceColumns)) {
            reasons.add(reason(
                "FISCAL_CALENDAR_ROLLUP_REQUIRES_REVIEW",
                "财务日历字段或表达式需要业务日历映射，AMV-008 默认不按自然日历自动生成。"
            ));
        }

        RollupParsedTimeExpression parsed = parseTimeExpression(cleanedExpression);
        if (parsed == null) {
            reasons.add(reason(
                "TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE",
                "时间表达式无法归一为自然日历日到月、季度或年的上卷关系。"
            ));
            return RollupPlan.blocked(cleanedExpression, reasons);
        }
        if ("WEEK".equals(parsed.targetGrain)) {
            reasons.add(reason(
                "WEEK_ROLLUP_CALENDAR_POLICY_REQUIRED",
                "周粒度存在周起始日和 ISO 周策略差异，缺少明确日历策略时不能自动生成。"
            ));
        } else if (MV_FINEST_GRAIN.equals(parsed.targetGrain)) {
            reasons.add(reason(
                "ROLLUP_TARGET_GRAIN_NOT_COARSER_THAN_DAY",
                "查询目标粒度已经是日粒度，不能证明需要日到粗粒度的二次聚合复用。"
            ));
        } else if (!isSupportedTargetGrain(parsed.targetGrain)) {
            reasons.add(reason(
                "TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE",
                "仅自动支持自然日历日粒度上卷到月、季度或年。"
            ));
        }
        if (!isIdentifierReference(parsed.sourceExpression)) {
            reasons.add(reason(
                "TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE",
                "时间上卷源必须是单一时间列，包含 CAST、运算或嵌套函数时需要人工归一策略。"
            ));
        }

        String sourceColumn = parsed.sourceExpression;
        if (StringUtils.hasText(sourceColumn) && !sourceColumns.isEmpty()
            && !containsEquivalentSource(sourceColumns, sourceColumn)) {
            reasons.add(reason(
                "TIME_ROLLUP_SOURCE_COLUMN_UNRESOLVED",
                "解析画像中的时间源列与时间表达式不一致，不能证明 Rollup 覆盖关系。"
            ));
        }
        String mvTimeColumn = cleanName(columnName(sourceColumn) + "_day");
        return new RollupPlan(
            cleanedExpression,
            sourceColumn,
            parsed.targetGrain,
            "DATE_TRUNC('day', " + sourceColumn + ")",
            mvTimeColumn,
            targetExpression(parsed.targetGrain, mvTimeColumn, parsed.expressionKind),
            reasons
        );
    }
}
