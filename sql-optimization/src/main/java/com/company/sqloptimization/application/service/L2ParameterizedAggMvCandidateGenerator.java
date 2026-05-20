package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2ParameterizedAggMvCandidateGenerator {

    private L2ParameterizedAggMvCandidateGenerator() {
    }

    static CandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 Map<String, Object> advancedStructureProfile,
                                 L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                 L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        List<Map<String, Object>> blockingReasons = structuralBlockingReasons(
            advancedStructureProfile,
            predicateClassification,
            grainMeasureDerivation
        );
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons);
        }

        List<DimensionSpec> dimensions = dimensionSpecs(
            grainMeasureDerivation.getDimensions(),
            mapList(advancedStructureProfile.get("groupBy"))
        );
        String fromClause = baseFromClause(mapList(advancedStructureProfile.get("tables")));
        List<MeasureColumn> measureColumns = measureColumns(grainMeasureDerivation.getMeasures());
        List<String> ddlSelectItems = new ArrayList<String>();
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
            dimensions
        );
        String rewriteSql = rewriteSql(
            mvName,
            advancedStructureProfile,
            predicateClassification,
            grainMeasureDerivation.getMeasures(),
            dimensions
        );
        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult validationSql =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG,
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
            return CandidateSql.blocked(validationSql.getBlockingReasons());
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, selectSql);
        if (renderedSql == null) {
            return CandidateSql.blocked(Collections.singletonList(reason(
                "UNSUPPORTED_TARGET_ENGINE",
                "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"
            )));
        }
        return CandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql.getValidationSql(),
            renderedSql.getRollbackSql(),
            rewriteSql
        );
    }

    private static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "缺少高级结构画像，不能生成 PARAMETERIZED_AGG_MV。"
            ));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可激活的 PARAMETERIZED_AGG_MV SQL。"
            ));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason(
                "PARAMETERIZED_AGG_MV_ONLY",
                "AMV-005 只生成 PARAMETERIZED_AGG_MV，其他高级 MV 类型留给后续任务。"
            ));
        }
        if (grainMeasureDerivation == null || grainMeasureDerivation.getMeasures().isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可重聚合指标，不能生成聚合 MV。"));
        }
        if (!mapList(advancedStructureProfile.get("joinGraph")).isEmpty()) {
            reasons.add(reason(
                "JOIN_MV_TYPE_DEFERRED",
                "Join、预 Join 与星型聚合 MV 留给 AMV-006/AMV-007，不在 AMV-005 中生成。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_MV_DEFERRED",
                "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-005 中生成。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("orderBy")).isEmpty()
            || booleanValue(mapValue(advancedStructureProfile.get("limit"), "present"))) {
            reasons.add(reason(
                "ORDER_LIMIT_REWRITE_UNSUPPORTED",
                "ORDER BY 或 LIMIT 的保序 rewrite 校验留给后续静态覆盖任务。"
            ));
        }
        if (hasOrPredicate(predicateClassification)) {
            reasons.add(reason(
                "OR_PREDICATE_REWRITE_UNSUPPORTED",
                "OR 谓词需要保持原逻辑分组，AMV-005 暂不生成可激活 rewrite。"
            ));
        }
        List<Map<String, Object>> baseTables = baseTables(mapList(advancedStructureProfile.get("tables")));
        if (baseTables.isEmpty()) {
            reasons.add(reason(
                "BASE_TABLE_REQUIRED",
                "缺少单表基表来源，不能生成 PARAMETERIZED_AGG_MV。"
            ));
        } else if (baseTables.size() > 1) {
            reasons.add(reason(
                "SINGLE_BASE_TABLE_REQUIRED",
                "PARAMETERIZED_AGG_MV 当前只支持单基表聚合，多表形态留给 Join/星型 MV 任务。"
            ));
        }
        return reasons;
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

    private static String selectSql(List<String> selectItems,
                                    String fromClause,
                                    List<String> retainedWherePredicates,
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
        if (!dimensions.isEmpty()) {
            builder.append("GROUP BY ");
            builder.append(String.join(", ", dimensionExpressions(dimensions)));
            builder.append('\n');
        }
        builder.append(';');
        return builder.toString();
    }

    private static String rewriteSql(String mvName,
                                     Map<String, Object> advancedStructureProfile,
                                     L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                     List<Map<String, Object>> measures,
                                     List<DimensionSpec> dimensions) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, measures, dimensions);
        List<String> wherePredicates = rewriteFilterPredicates(predicateClassification, dimensions);
        List<String> groupByItems = rewriteGroupByItems(mapList(advancedStructureProfile.get("groupBy")), dimensions);
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, measures, dimensions);

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
                                                   List<DimensionSpec> dimensions) {
        List<String> selectItems = new ArrayList<String>();
        Set<String> selectedDimensions = new LinkedHashSet<String>();
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            if (isMeasureProjection(projection, measures)) {
                continue;
            }
            DimensionSpec dimension = findDimension(text(projection.get("expression")), dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(projection.get("sourceColumns")), dimensions);
            }
            if (dimension == null || selectedDimensions.contains(dimension.outputName)) {
                continue;
            }
            String alias = text(projection.get("alias"));
            selectItems.add(withAlias(dimension.outputName, alias));
            selectedDimensions.add(dimension.outputName);
        }
        if (selectItems.isEmpty()) {
            for (Map<String, Object> groupBy : mapList(advancedStructureProfile.get("groupBy"))) {
                DimensionSpec dimension = findDimension(text(groupBy.get("expression")), dimensions);
                if (dimension == null) {
                    dimension = findDimensionBySources(stringList(groupBy.get("sourceColumns")), dimensions);
                }
                if (dimension != null && !selectedDimensions.contains(dimension.outputName)) {
                    selectItems.add(dimension.outputName);
                    selectedDimensions.add(dimension.outputName);
                }
            }
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
        List<DimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteFilterPredicates(predicates, predicateClassification.getExternalizedPredicates(), dimensions);
        addRewriteFilterPredicates(predicates, predicateClassification.getSecurityPredicates(), dimensions);
        return predicates;
    }

    private static void addRewriteFilterPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   List<DimensionSpec> dimensions) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(rewriteColumns(text(predicate.get("expression")), dimensions));
            }
        }
    }

    private static List<String> rewriteGroupByItems(List<Map<String, Object>> groupBy,
                                                    List<DimensionSpec> dimensions) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupByItem : groupBy) {
            DimensionSpec dimension = findDimension(text(groupByItem.get("expression")), dimensions);
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
        List<DimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteMeasureExpressions(text(predicate.get("expression")), measures);
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
            addMeasureReplacement(
                replacements,
                text(measure.get("name")),
                text(measure.get("rewriteExpression"))
            );
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addMeasureReplacement(
                    replacements,
                    text(component.get("sourceExpression")),
                    text(component.get("rewriteExpression"))
                );
                addMeasureReplacement(
                    replacements,
                    text(component.get("name")),
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
        boolean exactExpressionApplied = false;
        for (MeasureReplacement replacement : replacements) {
            if (replacement.sourceExpression.indexOf('(') >= 0) {
                String next = result.replace(replacement.sourceExpression, replacement.rewriteExpression);
                exactExpressionApplied = exactExpressionApplied || !next.equals(result);
                result = next;
            }
        }
        if (!exactExpressionApplied) {
            for (MeasureReplacement replacement : replacements) {
                if (replacement.sourceExpression.indexOf('(') < 0) {
                    result = replaceIdentifier(result, replacement.sourceExpression, replacement.rewriteExpression);
                }
            }
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

    private static List<DimensionSpec> dimensionSpecs(List<String> dimensions, List<Map<String, Object>> groupBy) {
        List<DimensionSpec> result = new ArrayList<DimensionSpec>();
        Set<String> usedNames = new LinkedHashSet<String>();
        for (String dimension : dimensions) {
            if (!StringUtils.hasText(dimension)) {
                continue;
            }
            LinkedHashSet<String> references = new LinkedHashSet<String>();
            references.add(dimension.trim());
            for (Map<String, Object> groupByItem : groupBy) {
                if (sameExpression(dimension, text(groupByItem.get("expression")))
                    || stringList(groupByItem.get("sourceColumns")).contains(dimension)) {
                    references.addAll(stringList(groupByItem.get("sourceColumns")));
                    references.add(text(groupByItem.get("expression")));
                }
            }
            String outputName = uniqueName(columnName(dimension), usedNames);
            result.add(new DimensionSpec(dimension.trim(), outputName, references));
        }
        return result;
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

    private static boolean sameExpression(String left, String right) {
        return normalizeExpression(left).equals(normalizeExpression(right));
    }

    private static boolean containsAggregateToken(String expression) {
        String upper = expression.toUpperCase(Locale.ROOT);
        return upper.contains("SUM(")
            || upper.contains("COUNT(")
            || upper.contains("MIN(")
            || upper.contains("MAX(")
            || upper.contains("AVG(");
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
        String cleaned = candidate.toLowerCase(Locale.ROOT)
            .replace("`", "")
            .replace("\"", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+", "")
            .replaceAll("_+$", "");
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
        String cleaned = expression.replace("`", "").replace("\"", "").trim();
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

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql) {
            this.blockingReasons = immutableMapList(blockingReasons);
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
        }

        private static CandidateSql blocked(List<Map<String, Object>> blockingReasons) {
            return new CandidateSql(blockingReasons, null, null, null, null, null);
        }

        private static CandidateSql generated(String ddlSql,
                                              String refreshSql,
                                              String validationSql,
                                              String rollbackSql,
                                              String rewriteSql) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql
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
}
