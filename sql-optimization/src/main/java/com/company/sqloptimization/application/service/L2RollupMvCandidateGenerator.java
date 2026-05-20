package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2RollupMvCandidateGenerator {

    private static final String MV_FINEST_GRAIN = "DAY";
    private static final String CALENDAR_POLICY = "NATURAL_CALENDAR_DAY_TO_MONTH_QUARTER_YEAR";
    private static final Pattern FUNCTION_PATTERN =
        Pattern.compile("(?is)^([A-Z_][A-Z0-9_]*)\\s*\\((.*)\\)$");

    private L2RollupMvCandidateGenerator() {
    }

    static CandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 Map<String, Object> advancedStructureProfile,
                                 L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                 L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        RollupPlan rollupPlan = rollupPlan(advancedStructureProfile);
        List<Map<String, Object>> blockingReasons = structuralBlockingReasons(
            advancedStructureProfile,
            predicateClassification,
            grainMeasureDerivation,
            rollupPlan
        );
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons, timeRollupEvidence(rollupPlan, blockingReasons));
        }

        List<DimensionSpec> dimensions = dimensionSpecs(
            mapList(advancedStructureProfile.get("groupBy")),
            predicateClassification,
            rollupPlan
        );
        String fromClause = baseFromClause(mapList(advancedStructureProfile.get("tables")));
        List<MeasureColumn> measureColumns = measureColumns(grainMeasureDerivation.getMeasures());
        List<String> ddlSelectItems = new ArrayList<String>();
        ddlSelectItems.add(rollupPlan.mvTimeExpression + " AS " + rollupPlan.mvTimeColumn);
        for (DimensionSpec dimension : dimensions) {
            ddlSelectItems.add(dimension.ddlSelectItem());
        }
        for (MeasureColumn measureColumn : measureColumns) {
            ddlSelectItems.add(measureColumn.sourceExpression + " AS " + measureColumn.name);
        }

        String selectSql = selectSql(
            ddlSelectItems,
            fromClause,
            retainedWherePredicates(predicateClassification),
            rollupPlan,
            dimensions
        );
        String rewriteSql = rewriteSql(
            mvName,
            advancedStructureProfile,
            predicateClassification,
            grainMeasureDerivation.getMeasures(),
            rollupPlan,
            dimensions
        );
        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult validationSql =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_ROLLUP,
                    sourceSql,
                    rewriteSql,
                    mvName,
                    advancedStructureProfile,
                    grainMeasureDerivation.getMeasures(),
                    null,
                    Collections.<String>emptyList()
                )
            );
        if (!validationSql.isGenerated()) {
            return CandidateSql.blocked(
                validationSql.getBlockingReasons(),
                timeRollupEvidence(rollupPlan, validationSql.getBlockingReasons())
            );
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, selectSql);
        if (renderedSql == null) {
            List<Map<String, Object>> renderingReasons = Collections.singletonList(reason(
                "UNSUPPORTED_TARGET_ENGINE",
                "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"
            ));
            return CandidateSql.blocked(renderingReasons, timeRollupEvidence(rollupPlan, renderingReasons));
        }
        return CandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql.getValidationSql(),
            renderedSql.getRollbackSql(),
            rewriteSql,
            timeRollupEvidence(rollupPlan, Collections.<Map<String, Object>>emptyList())
        );
    }

    private static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        RollupPlan rollupPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "缺少高级结构画像，不能生成 ROLLUP_MV。"
            ));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可激活的 ROLLUP_MV SQL。"
            ));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_ROLLUP.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason(
                "ROLLUP_MV_ONLY",
                "AMV-008 只生成 ROLLUP_MV，其他高级 MV 类型由对应任务处理。"
            ));
        }
        if (grainMeasureDerivation == null || grainMeasureDerivation.getMeasures().isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可重聚合指标，不能生成 Rollup MV。"));
        }
        if (!mapList(advancedStructureProfile.get("joinGraph")).isEmpty()) {
            reasons.add(reason(
                "ROLLUP_JOIN_MV_DEFERRED",
                "Join 形态的时间上卷需要与 PREJOIN_MV 或 STAR_AGG_MV 组合，AMV-008 不生成多表 Rollup SQL。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_MV_DEFERRED",
                "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-008 中生成。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("orderBy")).isEmpty()
            || booleanValue(mapValue(advancedStructureProfile.get("limit"), "present"))) {
            reasons.add(reason(
                "ORDER_LIMIT_REWRITE_UNSUPPORTED",
                "ORDER BY 或 LIMIT 的保序 Rollup rewrite 校验留给后续静态覆盖任务。"
            ));
        }
        if (hasOrPredicate(predicateClassification)) {
            reasons.add(reason(
                "OR_PREDICATE_REWRITE_UNSUPPORTED",
                "OR 谓词需要保持原逻辑分组，AMV-008 暂不生成可激活 Rollup rewrite。"
            ));
        }
        List<Map<String, Object>> baseTables = baseTables(mapList(advancedStructureProfile.get("tables")));
        if (baseTables.isEmpty()) {
            reasons.add(reason(
                "BASE_TABLE_REQUIRED",
                "缺少单表基表来源，不能生成 ROLLUP_MV。"
            ));
        } else if (baseTables.size() > 1) {
            reasons.add(reason(
                "SINGLE_BASE_TABLE_REQUIRED",
                "ROLLUP_MV 当前只支持单基表聚合，多表形态留给 Join/星型 MV 组合任务。"
            ));
        }
        if (rollupPlan == null) {
            reasons.add(reason(
                "TIME_ROLLUP_EXPRESSION_REQUIRED",
                "缺少可识别的时间上卷分组表达式，不能生成 ROLLUP_MV。"
            ));
        } else {
            reasons.addAll(rollupPlan.blockingReasons);
        }
        return reasons;
    }

    private static RollupPlan rollupPlan(Map<String, Object> advancedStructureProfile) {
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

        ParsedTimeExpression parsed = parseTimeExpression(cleanedExpression);
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

    private static ParsedTimeExpression parseTimeExpression(String expression) {
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
            return new ParsedTimeExpression(
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
                return new ParsedTimeExpression(firstGrain, stripOuterParentheses(arguments.get(1)), "DATE_TRUNC");
            }
            return new ParsedTimeExpression(
                normalizeGrain(arguments.get(1)),
                stripOuterParentheses(arguments.get(0)),
                "DATE_TRUNC"
            );
        }
        if ("YEAR".equals(functionName) && arguments.size() == 1) {
            return new ParsedTimeExpression("YEAR", stripOuterParentheses(arguments.get(0)), "YEAR_FUNCTION");
        }
        if ("MONTH".equals(functionName) && arguments.size() == 1) {
            return new ParsedTimeExpression("MONTH_OF_YEAR", stripOuterParentheses(arguments.get(0)), "MONTH_FUNCTION");
        }
        if ("DAY".equals(functionName) && arguments.size() == 1) {
            return new ParsedTimeExpression(MV_FINEST_GRAIN, stripOuterParentheses(arguments.get(0)), "DAY_FUNCTION");
        }
        if ("DATE_FORMAT".equals(functionName)) {
            return null;
        }
        return null;
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

    private static boolean isSupportedTargetGrain(String grain) {
        return "MONTH".equals(grain) || "QUARTER".equals(grain) || "YEAR".equals(grain);
    }

    private static String targetExpression(String targetGrain, String mvTimeColumn, String expressionKind) {
        if ("YEAR_FUNCTION".equals(expressionKind)) {
            return "YEAR(" + mvTimeColumn + ")";
        }
        return "DATE_TRUNC('" + targetGrain.toLowerCase(Locale.ROOT) + "', " + mvTimeColumn + ")";
    }

    private static List<DimensionSpec> dimensionSpecs(List<Map<String, Object>> groupBy,
                                                      L2PredicateClassifier.PredicateClassificationResult
                                                          predicateClassification,
                                                      RollupPlan rollupPlan) {
        List<DimensionSpec> result = new ArrayList<DimensionSpec>();
        Set<String> usedNames = new LinkedHashSet<String>();
        usedNames.add(rollupPlan.mvTimeColumn);
        Set<String> seen = new LinkedHashSet<String>();
        seen.add(normalizeExpression(rollupPlan.queryTimeExpression));
        seen.add(normalizeExpression(rollupPlan.sourceColumn));
        for (Map<String, Object> groupByItem : groupBy) {
            String expression = text(groupByItem.get("expression"));
            if (!StringUtils.hasText(expression) || rollupPlan.matchesTimeReference(expression)) {
                continue;
            }
            addDimension(result, seen, usedNames, expression, stringList(groupByItem.get("sourceColumns")), rollupPlan);
        }
        if (predicateClassification != null) {
            addPredicateDimensions(result, seen, usedNames, predicateClassification.getExternalizedPredicates(), rollupPlan);
            addPredicateDimensions(result, seen, usedNames, predicateClassification.getSecurityPredicates(), rollupPlan);
        }
        return result;
    }

    private static void addPredicateDimensions(List<DimensionSpec> result,
                                               Set<String> seen,
                                               Set<String> usedNames,
                                               List<Map<String, Object>> predicates,
                                               RollupPlan rollupPlan) {
        for (Map<String, Object> predicate : predicates) {
            if (!"WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                continue;
            }
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                sourceColumns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            for (String sourceColumn : sourceColumns) {
                addDimension(
                    result,
                    seen,
                    usedNames,
                    sourceColumn,
                    Collections.singletonList(sourceColumn),
                    rollupPlan
                );
            }
        }
    }

    private static void addDimension(List<DimensionSpec> result,
                                     Set<String> seen,
                                     Set<String> usedNames,
                                     String expression,
                                     List<String> sourceColumns,
                                     RollupPlan rollupPlan) {
        if (!StringUtils.hasText(expression) || rollupPlan.matchesTimeReference(expression)) {
            return;
        }
        String normalizedExpression = normalizeExpression(expression);
        if (!StringUtils.hasText(normalizedExpression) || seen.contains(normalizedExpression)) {
            return;
        }
        LinkedHashSet<String> references = new LinkedHashSet<String>();
        references.add(expression.trim());
        for (String sourceColumn : sourceColumns) {
            if (StringUtils.hasText(sourceColumn) && !rollupPlan.matchesTimeReference(sourceColumn)) {
                references.add(sourceColumn.trim());
            }
        }
        String outputName = uniqueName(columnName(expression), usedNames);
        result.add(new DimensionSpec(expression.trim(), outputName, references));
        seen.add(normalizedExpression);
        for (String reference : references) {
            seen.add(normalizeExpression(reference));
        }
    }

    private static String selectSql(List<String> selectItems,
                                    String fromClause,
                                    List<String> retainedWherePredicates,
                                    RollupPlan rollupPlan,
                                    List<DimensionSpec> dimensions) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT\n");
        for (int i = 0; i < selectItems.size(); i++) {
            builder.append("  ");
            builder.append(selectItems.get(i));
            builder.append(i == selectItems.size() - 1 ? "\n" : ",\n");
        }
        builder.append("FROM ").append(fromClause).append('\n');
        if (!retainedWherePredicates.isEmpty()) {
            builder.append("WHERE ");
            builder.append(String.join("\n  AND ", retainedWherePredicates));
            builder.append('\n');
        }
        List<String> groupByItems = new ArrayList<String>();
        groupByItems.add(rollupPlan.mvTimeExpression);
        groupByItems.addAll(dimensionExpressions(dimensions));
        builder.append("GROUP BY ");
        builder.append(String.join(", ", groupByItems));
        builder.append('\n');
        builder.append(';');
        return builder.toString();
    }

    private static String rewriteSql(String mvName,
                                     Map<String, Object> advancedStructureProfile,
                                     L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                     List<Map<String, Object>> measures,
                                     RollupPlan rollupPlan,
                                     List<DimensionSpec> dimensions) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, measures, rollupPlan, dimensions);
        List<String> wherePredicates = rewriteFilterPredicates(predicateClassification, rollupPlan, dimensions);
        List<String> groupByItems = rewriteGroupByItems(
            mapList(advancedStructureProfile.get("groupBy")),
            rollupPlan,
            dimensions
        );
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, measures, rollupPlan, dimensions);

        StringBuilder builder = new StringBuilder();
        builder.append("SELECT\n");
        for (int i = 0; i < selectItems.size(); i++) {
            builder.append("  ");
            builder.append(selectItems.get(i));
            builder.append(i == selectItems.size() - 1 ? "\n" : ",\n");
        }
        builder.append("FROM ").append(mvName).append('\n');
        if (!wherePredicates.isEmpty()) {
            builder.append("WHERE ");
            builder.append(String.join("\n  AND ", wherePredicates));
            builder.append('\n');
        }
        if (!groupByItems.isEmpty()) {
            builder.append("GROUP BY ");
            builder.append(String.join(", ", groupByItems));
            builder.append('\n');
        }
        if (!havingPredicates.isEmpty()) {
            builder.append("HAVING ");
            builder.append(String.join("\n  AND ", havingPredicates));
            builder.append('\n');
        }
        builder.append(';');
        return builder.toString();
    }

    private static List<String> rewriteSelectItems(Map<String, Object> advancedStructureProfile,
                                                   List<Map<String, Object>> measures,
                                                   RollupPlan rollupPlan,
                                                   List<DimensionSpec> dimensions) {
        List<String> selectItems = new ArrayList<String>();
        Set<String> selectedDimensions = new LinkedHashSet<String>();
        boolean selectedTime = false;
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            if (isMeasureProjection(projection, measures)) {
                continue;
            }
            String alias = text(projection.get("alias"));
            String expression = stripAlias(text(projection.get("expression")), alias);
            if (rollupPlan.matchesTimeReference(expression)) {
                if (!selectedTime) {
                    selectItems.add(withAlias(rollupPlan.rewriteRollupExpression, alias));
                    selectedTime = true;
                }
                continue;
            }
            DimensionSpec dimension = findDimension(expression, dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(projection.get("sourceColumns")), dimensions);
            }
            if (dimension == null || selectedDimensions.contains(dimension.outputName)) {
                continue;
            }
            selectItems.add(withAlias(dimension.outputName, alias));
            selectedDimensions.add(dimension.outputName);
        }
        if (!selectedTime) {
            selectItems.add(rollupPlan.rewriteRollupExpression);
        }
        for (Map<String, Object> measure : measures) {
            String rewriteExpression = text(measure.get("rewriteExpression"));
            String name = text(measure.get("name"));
            if (StringUtils.hasText(rewriteExpression) && StringUtils.hasText(name)) {
                selectItems.add(rewriteExpression + " AS " + name);
            }
        }
        return selectItems;
    }

    private static boolean isMeasureProjection(Map<String, Object> projection, List<Map<String, Object>> measures) {
        String expression = normalizeExpression(stripAlias(
            text(projection.get("expression")),
            text(projection.get("alias"))
        ));
        String expressionType = text(projection.get("expressionType"));
        if ("AGGREGATION".equals(expressionType)) {
            return true;
        }
        for (Map<String, Object> measure : measures) {
            if (expression.equals(normalizeExpression(text(measure.get("sourceExpression"))))) {
                return true;
            }
        }
        return containsAggregateToken(expression);
    }

    private static List<String> retainedWherePredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                predicates.add(text(predicate.get("expression")));
            }
        }
        return predicates;
    }

    private static List<String> rewriteFilterPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        RollupPlan rollupPlan,
        List<DimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteFilterPredicates(
            predicates,
            predicateClassification.getExternalizedPredicates(),
            rollupPlan,
            dimensions
        );
        addRewriteFilterPredicates(
            predicates,
            predicateClassification.getSecurityPredicates(),
            rollupPlan,
            dimensions
        );
        return predicates;
    }

    private static void addRewriteFilterPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   RollupPlan rollupPlan,
                                                   List<DimensionSpec> dimensions) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteTimeReferences(text(predicate.get("expression")), rollupPlan);
                target.add(rewriteColumns(expression, dimensions));
            }
        }
    }

    private static List<String> rewriteGroupByItems(List<Map<String, Object>> groupBy,
                                                    RollupPlan rollupPlan,
                                                    List<DimensionSpec> dimensions) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupByItem : groupBy) {
            String expression = text(groupByItem.get("expression"));
            if (rollupPlan.matchesTimeReference(expression)) {
                if (!seen.contains(rollupPlan.rewriteRollupExpression)) {
                    items.add(rollupPlan.rewriteRollupExpression);
                    seen.add(rollupPlan.rewriteRollupExpression);
                }
                continue;
            }
            DimensionSpec dimension = findDimension(expression, dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(groupByItem.get("sourceColumns")), dimensions);
            }
            if (dimension != null && !seen.contains(dimension.outputName)) {
                items.add(dimension.outputName);
                seen.add(dimension.outputName);
            }
        }
        return items;
    }

    private static List<String> rewriteHavingPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        List<Map<String, Object>> measures,
        RollupPlan rollupPlan,
        List<DimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteMeasureExpressions(text(predicate.get("expression")), measures);
                expression = rewriteTimeReferences(expression, rollupPlan);
                predicates.add(rewriteColumns(expression, dimensions));
            }
        }
        return predicates;
    }

    private static String rewriteMeasureExpressions(String expression, List<Map<String, Object>> measures) {
        String result = expression;
        List<MeasureReplacement> replacements = new ArrayList<MeasureReplacement>();
        for (Map<String, Object> measure : measures) {
            addMeasureReplacement(
                replacements,
                text(measure.get("sourceExpression")),
                text(measure.get("rewriteExpression"))
            );
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addMeasureReplacement(
                    replacements,
                    text(component.get("sourceExpression")),
                    text(component.get("rewriteExpression"))
                );
            }
        }
        Collections.sort(replacements, new Comparator<MeasureReplacement>() {
            @Override
            public int compare(MeasureReplacement left, MeasureReplacement right) {
                return Integer.compare(right.sourceExpression.length(), left.sourceExpression.length());
            }
        });
        for (MeasureReplacement replacement : replacements) {
            result = result.replace(replacement.sourceExpression, replacement.rewriteExpression);
        }
        return result;
    }

    private static void addMeasureReplacement(List<MeasureReplacement> replacements,
                                              String sourceExpression,
                                              String rewriteExpression) {
        if (StringUtils.hasText(sourceExpression) && StringUtils.hasText(rewriteExpression)) {
            replacements.add(new MeasureReplacement(sourceExpression, rewriteExpression));
        }
    }

    private static List<MeasureColumn> measureColumns(List<Map<String, Object>> measures) {
        List<MeasureColumn> columns = new ArrayList<MeasureColumn>();
        for (Map<String, Object> measure : measures) {
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                columns.add(new MeasureColumn(text(component.get("name")), text(component.get("sourceExpression"))));
            }
            if (mapList(measure.get("components")).isEmpty()) {
                columns.add(new MeasureColumn(text(measure.get("name")), text(measure.get("sourceExpression"))));
            }
        }
        return columns;
    }

    private static List<String> dimensionExpressions(List<DimensionSpec> dimensions) {
        List<String> expressions = new ArrayList<String>();
        for (DimensionSpec dimension : dimensions) {
            expressions.add(dimension.sourceExpression);
        }
        return expressions;
    }

    private static String baseFromClause(List<Map<String, Object>> tables) {
        List<Map<String, Object>> baseTables = baseTables(tables);
        if (baseTables.isEmpty()) {
            return "";
        }
        Map<String, Object> table = baseTables.get(0);
        String tableName = text(table.get("tableName"));
        String alias = text(table.get("alias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
            return tableName + " " + alias;
        }
        return tableName;
    }

    private static List<Map<String, Object>> baseTables(List<Map<String, Object>> tables) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> table : tables) {
            String sourceType = text(table.get("sourceType"));
            if (!StringUtils.hasText(sourceType) || "BASE_TABLE".equals(sourceType)) {
                result.add(table);
            }
        }
        return result;
    }

    private static Map<String, Object> timeRollupEvidence(RollupPlan rollupPlan,
                                                          List<Map<String, Object>> blockingReasons) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("status", blockingReasons == null || blockingReasons.isEmpty() ? "GENERATED" : "BLOCKED");
        evidence.put("timeSourceColumn", rollupPlan == null ? "" : rollupPlan.sourceColumn);
        evidence.put("queryTimeExpression", rollupPlan == null ? "" : rollupPlan.queryTimeExpression);
        evidence.put("mvFinestGrain", MV_FINEST_GRAIN);
        evidence.put("queryTargetGrain", rollupPlan == null ? "" : rollupPlan.queryTargetGrain);
        evidence.put("mvTimeExpression", rollupPlan == null ? "" : rollupPlan.mvTimeExpression);
        evidence.put("mvTimeColumn", rollupPlan == null ? "" : rollupPlan.mvTimeColumn);
        evidence.put("rewriteRollupExpression", rollupPlan == null ? "" : rollupPlan.rewriteRollupExpression);
        evidence.put("calendarPolicy", CALENDAR_POLICY);
        evidence.put(
            "calendarPolicyZh",
            "仅支持自然日历日粒度上卷到月、季度或年；周、财务日历和显式时区语义需要人工策略。"
        );
        evidence.put("blockingReasons", immutableMapList(blockingReasons));
        evidence.put("reviewWarnings", reviewWarnings(blockingReasons));
        return evidence;
    }

    private static List<Map<String, Object>> reviewWarnings(List<Map<String, Object>> blockingReasons) {
        if (blockingReasons != null && !blockingReasons.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", "NATURAL_CALENDAR_ASSUMPTION");
        warning.put(
            "description",
            "ROLLUP_MV 静态生成按自然日历 DATE_TRUNC 语义，不声明财务日历或时区转换等价。"
        );
        warning.put("requiredEvidence", Arrays.asList(
            "TIME_COLUMN_TYPE",
            "CALENDAR_POLICY",
            "TIMEZONE_POLICY"
        ));
        warning.put("generatedAllowed", Boolean.TRUE);
        return Collections.<Map<String, Object>>singletonList(warning);
    }

    private static boolean hasOrPredicate(L2PredicateClassifier.PredicateClassificationResult classification) {
        return hasOrPredicate(classification == null
            ? Collections.<Map<String, Object>>emptyList()
            : classification.getExternalizedPredicates())
            || hasOrPredicate(classification == null
            ? Collections.<Map<String, Object>>emptyList()
            : classification.getRetainedPredicates())
            || hasOrPredicate(classification == null
            ? Collections.<Map<String, Object>>emptyList()
            : classification.getSecurityPredicates());
    }

    private static boolean hasOrPredicate(List<Map<String, Object>> predicates) {
        for (Map<String, Object> predicate : predicates) {
            if ("OR".equalsIgnoreCase(text(predicate.get("logicalContext")))) {
                return true;
            }
        }
        return false;
    }

    private static String rewriteTimeReferences(String expression, RollupPlan rollupPlan) {
        String result = expression;
        if (!StringUtils.hasText(result) || rollupPlan == null) {
            return result;
        }
        if (StringUtils.hasText(rollupPlan.queryTimeExpression)) {
            result = result.replace(rollupPlan.queryTimeExpression, rollupPlan.rewriteRollupExpression);
        }
        result = replaceIdentifier(result, rollupPlan.sourceColumn, rollupPlan.mvTimeColumn);
        String unqualified = unqualifiedName(rollupPlan.sourceColumn);
        if (!unqualified.equals(rollupPlan.sourceColumn)) {
            result = replaceIdentifier(result, unqualified, rollupPlan.mvTimeColumn);
        }
        return result;
    }

    private static String rewriteColumns(String expression, List<DimensionSpec> dimensions) {
        String result = expression;
        List<ColumnReplacement> replacements = new ArrayList<ColumnReplacement>();
        for (DimensionSpec dimension : dimensions) {
            for (String reference : dimension.references) {
                if (StringUtils.hasText(reference) && isIdentifierReference(reference)) {
                    replacements.add(new ColumnReplacement(reference, dimension.outputName));
                    String unqualified = unqualifiedName(reference);
                    if (!unqualified.equals(reference)) {
                        replacements.add(new ColumnReplacement(unqualified, dimension.outputName));
                    }
                }
            }
        }
        Collections.sort(replacements, new Comparator<ColumnReplacement>() {
            @Override
            public int compare(ColumnReplacement left, ColumnReplacement right) {
                return Integer.compare(right.source.length(), left.source.length());
            }
        });
        Set<String> applied = new LinkedHashSet<String>();
        for (ColumnReplacement replacement : replacements) {
            String key = replacement.source + "->" + replacement.target;
            if (applied.contains(key)) {
                continue;
            }
            result = replaceIdentifier(result, replacement.source, replacement.target);
            applied.add(key);
        }
        return result;
    }

    private static String replaceIdentifier(String expression, String source, String target) {
        if (!StringUtils.hasText(expression) || !StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return expression;
        }
        String normalizedSource = source.replace("`", "").replace("\"", "").trim();
        Pattern pattern = Pattern.compile(
            "(?i)(^|[^A-Z0-9_$.])" + Pattern.quote(normalizedSource) + "([^A-Z0-9_]|$)"
        );
        return pattern.matcher(expression).replaceAll("$1" + target + "$2");
    }

    private static DimensionSpec findDimension(String expression, List<DimensionSpec> dimensions) {
        String normalized = normalizeExpression(expression);
        for (DimensionSpec dimension : dimensions) {
            if (dimension.matches(normalized)) {
                return dimension;
            }
        }
        return null;
    }

    private static DimensionSpec findDimensionBySources(List<String> sourceColumns, List<DimensionSpec> dimensions) {
        for (String sourceColumn : sourceColumns) {
            DimensionSpec dimension = findDimension(sourceColumn, dimensions);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
    }

    private static String withAlias(String expression, String alias) {
        if (!StringUtils.hasText(alias) || alias.equalsIgnoreCase(expression)) {
            return expression;
        }
        return expression + " AS " + alias;
    }

    private static boolean containsAggregateToken(String expression) {
        String upper = expression.toUpperCase(Locale.ROOT);
        return upper.contains("SUM(")
            || upper.contains("COUNT(")
            || upper.contains("MIN(")
            || upper.contains("MAX(")
            || upper.contains("AVG(");
    }

    private static boolean isTimeFunctionExpression(String expression) {
        String upper = normalizeExpression(stripAlias(expression, ""));
        return upper.startsWith("DATE_TRUNC(")
            || upper.startsWith("TRUNC(")
            || upper.startsWith("DATE_FORMAT(")
            || upper.startsWith("YEAR(")
            || upper.startsWith("MONTH(")
            || upper.startsWith("DAY(");
    }

    private static boolean containsTimezoneDependency(String expression) {
        String upper = normalizeExpression(expression);
        return upper.contains(" AT TIME ZONE ")
            || upper.contains("CONVERT_TIMEZONE(")
            || upper.contains("CONVERT_TZ(")
            || upper.contains("TIMEZONE(");
    }

    private static boolean containsFiscalCalendar(String expression) {
        return normalizeExpression(expression).contains("FISCAL");
    }

    private static boolean containsFiscalCalendar(List<String> values) {
        for (String value : values) {
            if (containsFiscalCalendar(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsEquivalentSource(List<String> sourceColumns, String sourceColumn) {
        String normalizedSource = normalizeExpression(sourceColumn);
        String unqualifiedSource = normalizeExpression(unqualifiedName(sourceColumn));
        for (String item : sourceColumns) {
            String normalizedItem = normalizeExpression(item);
            if (normalizedSource.equals(normalizedItem) || unqualifiedSource.equals(normalizedItem)) {
                return true;
            }
        }
        return false;
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

    private static List<String> splitArguments(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '(' && !inSingleQuote && !inDoubleQuote) {
                depth++;
            } else if (ch == ')' && !inSingleQuote && !inDoubleQuote) {
                depth = Math.max(0, depth - 1);
            }
            if (ch == ',' && depth == 0 && !inSingleQuote && !inDoubleQuote) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        if (StringUtils.hasText(alias)) {
            result = result.replaceAll("(?is)\\s+AS\\s+" + Pattern.quote(alias.trim()) + "\\s*$", "");
        }
        return result.replaceAll("(?is)\\s+AS\\s+[A-Z_][A-Z0-9_]*\\s*$", "").trim();
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

    private static String uniqueName(String candidate, Set<String> usedNames) {
        String base = StringUtils.hasText(candidate) ? candidate : "dimension";
        String result = base;
        int sequence = 2;
        while (usedNames.contains(result)) {
            result = base + "_" + sequence;
            sequence++;
        }
        usedNames.add(result);
        return result;
    }

    private static String columnName(String expression) {
        String candidate = expression;
        if (isIdentifierReference(candidate)) {
            candidate = unqualifiedName(candidate);
        }
        String cleaned = cleanName(candidate);
        return StringUtils.hasText(cleaned) ? cleaned : "dimension";
    }

    private static String cleanName(String value) {
        String cleaned = StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT)
                .replace("`", "")
                .replace("\"", "")
                .replaceAll("[^a-z0-9]+", "_")
            : "";
        cleaned = cleaned.replaceAll("^_+", "").replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "dimension";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "d_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }

    private static boolean isIdentifierReference(String expression) {
        return StringUtils.hasText(expression)
            && expression.replace("`", "").replace("\"", "").trim()
                .matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    }

    private static String unqualifiedName(String expression) {
        String cleaned = text(expression).replace("`", "").replace("\"", "").trim();
        int index = cleaned.lastIndexOf('.');
        return index >= 0 ? cleaned.substring(index + 1) : cleaned;
    }

    private static String normalizeExpression(String expression) {
        return StringUtils.hasText(expression)
            ? expression.replace("`", "")
                .replace("\"", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
            : "";
    }

    private static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    private static Object mapValue(Object value, String key) {
        if (!(value instanceof Map<?, ?>)) {
            return null;
        }
        return ((Map<?, ?>) value).get(key);
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

    private static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static final class CandidateSql {

        private final List<Map<String, Object>> blockingReasons;
        private final String ddlSql;
        private final String refreshSql;
        private final String validationSql;
        private final String rollbackSql;
        private final String rewriteSql;
        private final Map<String, Object> timeRollupEvidence;

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             Map<String, Object> timeRollupEvidence) {
            this.blockingReasons = immutableMapList(blockingReasons);
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.timeRollupEvidence = immutableMap(timeRollupEvidence);
        }

        private static CandidateSql blocked(List<Map<String, Object>> blockingReasons,
                                            Map<String, Object> timeRollupEvidence) {
            return new CandidateSql(blockingReasons, null, null, null, null, null, timeRollupEvidence);
        }

        private static CandidateSql generated(String ddlSql,
                                              String refreshSql,
                                              String validationSql,
                                              String rollbackSql,
                                              String rewriteSql,
                                              Map<String, Object> timeRollupEvidence) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                timeRollupEvidence
            );
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        String getDdlSql() {
            return ddlSql;
        }

        String getRefreshSql() {
            return refreshSql;
        }

        String getValidationSql() {
            return validationSql;
        }

        String getRollbackSql() {
            return rollbackSql;
        }

        String getRewriteSql() {
            return rewriteSql;
        }

        Map<String, Object> getTimeRollupEvidence() {
            return timeRollupEvidence;
        }
    }

    private static final class ParsedTimeExpression {
        private final String targetGrain;
        private final String sourceExpression;
        private final String expressionKind;

        private ParsedTimeExpression(String targetGrain, String sourceExpression, String expressionKind) {
            this.targetGrain = targetGrain;
            this.sourceExpression = sourceExpression;
            this.expressionKind = expressionKind;
        }
    }

    private static final class RollupPlan {
        private final String queryTimeExpression;
        private final String sourceColumn;
        private final String queryTargetGrain;
        private final String mvTimeExpression;
        private final String mvTimeColumn;
        private final String rewriteRollupExpression;
        private final List<Map<String, Object>> blockingReasons;

        private RollupPlan(String queryTimeExpression,
                           String sourceColumn,
                           String queryTargetGrain,
                           String mvTimeExpression,
                           String mvTimeColumn,
                           String rewriteRollupExpression,
                           List<Map<String, Object>> blockingReasons) {
            this.queryTimeExpression = text(queryTimeExpression);
            this.sourceColumn = text(sourceColumn);
            this.queryTargetGrain = text(queryTargetGrain);
            this.mvTimeExpression = text(mvTimeExpression);
            this.mvTimeColumn = text(mvTimeColumn);
            this.rewriteRollupExpression = text(rewriteRollupExpression);
            this.blockingReasons = immutableMapList(blockingReasons);
        }

        private static RollupPlan blocked(String queryTimeExpression, List<Map<String, Object>> blockingReasons) {
            return new RollupPlan(queryTimeExpression, "", "", "", "", "", blockingReasons);
        }

        private boolean matchesTimeReference(String expression) {
            String normalized = normalizeExpression(stripAlias(expression, ""));
            return StringUtils.hasText(normalized)
                && (normalized.equals(normalizeExpression(queryTimeExpression))
                || normalized.equals(normalizeExpression(sourceColumn))
                || normalized.equals(normalizeExpression(unqualifiedName(sourceColumn))));
        }
    }

    private static final class DimensionSpec {

        private final String sourceExpression;
        private final String outputName;
        private final Set<String> references;

        private DimensionSpec(String sourceExpression, String outputName, Set<String> references) {
            this.sourceExpression = sourceExpression;
            this.outputName = outputName;
            this.references = new LinkedHashSet<String>();
            for (String reference : references) {
                if (StringUtils.hasText(reference)) {
                    this.references.add(reference.trim());
                }
            }
        }

        private String ddlSelectItem() {
            if (sourceExpression.equals(outputName)) {
                return sourceExpression;
            }
            return sourceExpression + " AS " + outputName;
        }

        private boolean matches(String normalizedExpression) {
            if (normalizeExpression(sourceExpression).equals(normalizedExpression)
                || normalizeExpression(outputName).equals(normalizedExpression)) {
                return true;
            }
            for (String reference : references) {
                if (normalizeExpression(reference).equals(normalizedExpression)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class MeasureColumn {

        private final String name;
        private final String sourceExpression;

        private MeasureColumn(String name, String sourceExpression) {
            this.name = name;
            this.sourceExpression = sourceExpression;
        }
    }

    private static final class MeasureReplacement {

        private final String sourceExpression;
        private final String rewriteExpression;

        private MeasureReplacement(String sourceExpression, String rewriteExpression) {
            this.sourceExpression = sourceExpression;
            this.rewriteExpression = rewriteExpression;
        }
    }

    private static final class ColumnReplacement {

        private final String source;
        private final String target;

        private ColumnReplacement(String source, String target) {
            this.source = source;
            this.target = target;
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

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(source));
    }
}
