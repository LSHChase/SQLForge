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
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2PrejoinMvCandidateGenerator {

    private L2PrejoinMvCandidateGenerator() {
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
        List<Map<String, Object>> baseTables = baseTables(
            advancedStructureProfile == null ? null : mapList(advancedStructureProfile.get("tables"))
        );
        JoinPlan joinPlan = joinPlan(
            mapList(advancedStructureProfile == null ? null : advancedStructureProfile.get("joinGraph")),
            baseTables,
            sourceSql
        );
        blockingReasons.addAll(joinPlan.blockingReasons);

        ColumnPlan columnPlan = ColumnPlan.blocked(Collections.<Map<String, Object>>emptyList());
        if (blockingReasons.isEmpty()) {
            columnPlan = columnPlan(advancedStructureProfile, predicateClassification, joinPlan, baseTables);
            blockingReasons.addAll(columnPlan.blockingReasons);
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons, joinPlan, columnPlan);
        }

        String selectSql = selectSql(
            columnPlan.ddlSelectItems(),
            fromClause(sourceSql, baseTables, joinPlan),
            retainedWherePredicates(predicateClassification)
        );
        String rewriteSql = rewriteSql(mvName, advancedStructureProfile, predicateClassification, columnPlan);
        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult validationSql =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_PREJOIN,
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
            return CandidateSql.blocked(validationSql.getBlockingReasons(), joinPlan, columnPlan);
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, selectSql);
        if (renderedSql == null) {
            return CandidateSql.blocked(Collections.singletonList(reason(
                "UNSUPPORTED_TARGET_ENGINE",
                "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"
            )), joinPlan, columnPlan);
        }
        return CandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql.getValidationSql(),
            renderedSql.getRollbackSql(),
            rewriteSql,
            joinPlan,
            columnPlan
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
                "缺少高级结构画像，不能生成 PREJOIN_MV。"
            ));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可发布的 PREJOIN_MV SQL。"
            ));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason(
                "PREJOIN_MV_ONLY",
                "AMV-006 只生成 Join + 聚合/GROUP BY 的 PREJOIN_MV。"
            ));
        }
        if (mapList(advancedStructureProfile.get("joinGraph")).isEmpty()) {
            reasons.add(reason(
                "JOIN_GRAPH_REQUIRED",
                "缺少 Join 图，不能生成 PREJOIN_MV。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_MV_DEFERRED",
                "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-006 中生成。"
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
                "OR 谓词需要保持原逻辑分组，AMV-006 暂不生成可发布 PREJOIN_MV rewrite。"
            ));
        }
        List<Map<String, Object>> baseTables = baseTables(mapList(advancedStructureProfile.get("tables")));
        if (baseTables.size() < 2) {
            reasons.add(reason(
                "JOIN_BASE_TABLES_REQUIRED",
                "PREJOIN_MV 至少需要两个可解析的基表来源。"
            ));
        }
        return reasons;
    }

    private static JoinPlan joinPlan(List<Map<String, Object>> joinGraph,
                                     List<Map<String, Object>> baseTables,
                                     String sourceSql) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> joinKeys = new ArrayList<Map<String, Object>>();
        List<JoinClause> joinClauses = new ArrayList<JoinClause>();
        List<RelationSpec> relations = relationSpecs(baseTables);
        Map<String, RelationSpec> relationByName = relationByName(relations);
        List<Map<String, Object>> sqlJoinTables = joinRightTables(sourceSql);

        int joinIndex = 0;
        for (Map<String, Object> join : joinGraph) {
            joinIndex++;
            Map<String, Object> sequentialRightTable = joinIndex < baseTables.size()
                ? baseTables.get(joinIndex)
                : Collections.<String, Object>emptyMap();
            if (!hasTableName(sequentialRightTable) && joinIndex - 1 < sqlJoinTables.size()) {
                sequentialRightTable = sqlJoinTables.get(joinIndex - 1);
            }
            String joinType = upperText(join.get("joinType"));
            if ("CROSS".equals(joinType)) {
                blockingReasons.add(reason(
                    "CROSS_JOIN_PREJOIN_UNSUPPORTED",
                    "CROSS JOIN 可能产生笛卡尔积，AMV-006 不生成可发布 PREJOIN_MV。"
                ));
                continue;
            }
            if (isOuterJoin(joinType)) {
                blockingReasons.add(reason(
                    "OUTER_JOIN_PREJOIN_UNSUPPORTED",
                    "OUTER JOIN 的空值补齐语义需要人工复核，AMV-006 默认阻断。"
                ));
                continue;
            }
            if (!isInnerLikeJoin(joinType)) {
                blockingReasons.add(reason(
                    "UNSUPPORTED_JOIN_TYPE_PREJOIN",
                    "PREJOIN_MV 只允许 INNER/SIMPLE 等值 Join。"
                ));
                continue;
            }
            String condition = text(join.get("condition"));
            if (!StringUtils.hasText(condition)) {
                blockingReasons.add(reason(
                    "JOIN_CONDITION_REQUIRED",
                    "缺少 ON 等值条件，不能生成 PREJOIN_MV。"
                ));
                continue;
            }
            RelationSpec rightRelation = relationByName.get(relationKey(firstText(
                text(join.get("rightAlias")),
                text(join.get("right"))
            )));
            if (rightRelation == null) {
                rightRelation = relationByName.get(relationKey(text(join.get("right"))));
            }
            if (rightRelation == null && !sequentialRightTable.isEmpty()) {
                rightRelation = new RelationSpec(
                    text(sequentialRightTable.get("tableName")),
                    text(sequentialRightTable.get("alias"))
                );
            }
            List<String> conditions = splitAndConditions(condition);
            if (conditions.isEmpty()) {
                blockingReasons.add(reason(
                    "JOIN_CONDITION_REQUIRED",
                    "缺少可解析的 ON 等值条件，不能生成 PREJOIN_MV。"
                ));
                continue;
            }
            for (String conditionPart : conditions) {
                JoinKey joinKey = parseJoinKey(conditionPart, rightRelation);
                if (joinKey.blockingReason != null) {
                    blockingReasons.add(joinKey.blockingReason);
                    continue;
                }
                LinkedHashMap<String, Object> keyEvidence = new LinkedHashMap<String, Object>();
                keyEvidence.put("joinIndex", Integer.valueOf(joinIndex));
                keyEvidence.put("joinType", "INNER");
                keyEvidence.put("leftRelation", firstText(text(join.get("left")), firstTableName(baseTables)));
                keyEvidence.put("rightRelation", firstText(
                    text(join.get("right")),
                    text(sequentialRightTable.get("tableName"))
                ));
                keyEvidence.put("condition", conditionPart);
                keyEvidence.put("leftKey", joinKey.leftKey);
                keyEvidence.put("rightKey", joinKey.rightKey);
                joinKeys.add(keyEvidence);
            }
            joinClauses.add(new JoinClause(join, condition, sequentialRightTable));
        }
        return new JoinPlan(blockingReasons, joinKeys, joinClauses);
    }

    private static JoinKey parseJoinKey(String condition, RelationSpec rightRelation) {
        int equalsIndex = topLevelEqualsIndex(condition);
        if (equalsIndex <= 0 || equalsIndex >= condition.length() - 1) {
            return JoinKey.blocked(reason(
                "NON_EQUI_JOIN_PREJOIN_UNSUPPORTED",
                "Join 条件不是简单等值表达式，AMV-006 默认阻断。"
            ));
        }
        String left = stripOuterParentheses(condition.substring(0, equalsIndex).trim());
        String right = stripOuterParentheses(condition.substring(equalsIndex + 1).trim());
        if (!isIdentifierReference(left) || !isIdentifierReference(right)) {
            return JoinKey.blocked(reason(
                "COMPLEX_JOIN_KEY_PREJOIN_UNSUPPORTED",
                "Join key 包含函数、CAST 或复杂表达式，AMV-006 默认阻断。"
            ));
        }
        if (!StringUtils.hasText(qualifier(left)) || !StringUtils.hasText(qualifier(right))) {
            return JoinKey.blocked(reason(
                "JOIN_KEY_SIDE_UNRESOLVED",
                "Join key 缺少表别名或限定名，无法在多表 Join 中证明字段归属。"
            ));
        }
        boolean leftIsRightRelation = rightRelation != null && rightRelation.owns(left);
        boolean rightIsRightRelation = rightRelation != null && rightRelation.owns(right);
        if (leftIsRightRelation == rightIsRightRelation) {
            return JoinKey.blocked(reason(
                "JOIN_KEY_SIDE_UNRESOLVED",
                "Join key 两侧无法按 Join 右表消歧，不能生成 PREJOIN_MV。"
            ));
        }
        return leftIsRightRelation
            ? JoinKey.generated(right, left)
            : JoinKey.generated(left, right);
    }

    private static int topLevelEqualsIndex(String expression) {
        int depth = 0;
        int result = -1;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            } else if (ch == '=' && depth == 0) {
                char before = i == 0 ? '\0' : expression.charAt(i - 1);
                char after = i == expression.length() - 1 ? '\0' : expression.charAt(i + 1);
                if (before == '<' || before == '>' || before == '!' || after == '=') {
                    return -1;
                }
                if (result >= 0) {
                    return -1;
                }
                result = i;
            } else if (depth == 0 && (ch == '<' || ch == '>')) {
                return -1;
            }
        }
        return result;
    }

    private static ColumnPlan columnPlan(Map<String, Object> advancedStructureProfile,
                                         L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                         JoinPlan joinPlan,
                                         List<Map<String, Object>> baseTables) {
        LinkedHashSet<String> requiredColumns = new LinkedHashSet<String>();
        addSourceColumns(requiredColumns, mapList(advancedStructureProfile.get("projections")));
        addSourceColumns(requiredColumns, mapList(advancedStructureProfile.get("groupBy")));
        addSourceColumns(requiredColumns, mapList(advancedStructureProfile.get("aggregations")));
        if (predicateClassification != null) {
            addSourceColumns(requiredColumns, predicateClassification.getExternalizedPredicates());
            addSourceColumns(requiredColumns, predicateClassification.getSecurityPredicates());
            addSourceColumns(requiredColumns, predicateClassification.getRetainedPredicates());
        }
        for (Map<String, Object> joinKey : joinPlan.joinKeys) {
            addText(requiredColumns, text(joinKey.get("leftKey")));
            addText(requiredColumns, text(joinKey.get("rightKey")));
        }

        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Integer> unqualifiedCounts = unqualifiedCounts(requiredColumns);
        List<ColumnMapping> mappings = new ArrayList<ColumnMapping>();
        Set<String> usedOutputNames = new LinkedHashSet<String>();
        for (String sourceColumn : requiredColumns) {
            if (!isIdentifierReference(sourceColumn)) {
                blockingReasons.add(reason(
                    "COMPLEX_FIELD_MAPPING_UNSUPPORTED",
                    "PREJOIN_MV 只能映射可解析的字段引用。"
                ));
                continue;
            }
            String qualifier = qualifier(sourceColumn);
            if (!StringUtils.hasText(qualifier) && baseTables.size() > 1) {
                blockingReasons.add(reason(
                    "FIELD_AMBIGUITY_UNRESOLVED",
                    "多表 Join 中存在未限定字段，缺少元数据时无法证明字段归属。"
                ));
                continue;
            }
            String baseName = unqualifiedName(sourceColumn);
            boolean ambiguousName = intValue(unqualifiedCounts.get(normalizeName(baseName))) > 1;
            String outputName = ambiguousName
                ? cleanName(qualifier + "_" + baseName)
                : cleanName(baseName);
            outputName = uniqueName(outputName, usedOutputNames);
            mappings.add(new ColumnMapping(sourceColumn, outputName, qualifier, ambiguousName));
        }
        if (!blockingReasons.isEmpty()) {
            return ColumnPlan.blocked(blockingReasons);
        }
        return new ColumnPlan(blockingReasons, mappings);
    }

    private static String selectSql(List<String> selectItems,
                                    String fromClause,
                                    List<String> retainedWherePredicates) {
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
        builder.append(';');
        return builder.toString();
    }

    private static String rewriteSql(String mvName,
                                     Map<String, Object> advancedStructureProfile,
                                     L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                     ColumnPlan columnPlan) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, columnPlan);
        List<String> wherePredicates = rewriteWherePredicates(predicateClassification, columnPlan);
        List<String> groupByItems = rewriteGroupByItems(advancedStructureProfile, columnPlan);
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, columnPlan);

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
                                                   ColumnPlan columnPlan) {
        List<String> selectItems = new ArrayList<String>();
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            String alias = text(projection.get("alias"));
            String expression = stripAlias(text(projection.get("expression")), alias);
            if (!StringUtils.hasText(expression)) {
                continue;
            }
            selectItems.add(withAlias(rewriteColumns(expression, columnPlan), alias));
        }
        if (selectItems.isEmpty()) {
            selectItems.add("COUNT(*) AS row_count");
        }
        return selectItems;
    }

    private static List<String> rewriteWherePredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        ColumnPlan columnPlan) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteWherePredicates(predicates, predicateClassification.getExternalizedPredicates(), columnPlan);
        addRewriteWherePredicates(predicates, predicateClassification.getSecurityPredicates(), columnPlan);
        return predicates;
    }

    private static void addRewriteWherePredicates(List<String> target,
                                                  List<Map<String, Object>> predicates,
                                                  ColumnPlan columnPlan) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(rewriteColumns(text(predicate.get("expression")), columnPlan));
            }
        }
    }

    private static List<String> rewriteGroupByItems(Map<String, Object> advancedStructureProfile,
                                                    ColumnPlan columnPlan) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupBy : mapList(advancedStructureProfile.get("groupBy"))) {
            String rewritten = rewriteColumns(text(groupBy.get("expression")), columnPlan);
            if (StringUtils.hasText(rewritten) && !seen.contains(normalizeName(rewritten))) {
                items.add(rewritten);
                seen.add(normalizeName(rewritten));
            }
        }
        return items;
    }

    private static List<String> rewriteHavingPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        ColumnPlan columnPlan) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteHavingPredicates(predicates, predicateClassification.getExternalizedPredicates(), columnPlan);
        addRewriteHavingPredicates(predicates, predicateClassification.getSecurityPredicates(), columnPlan);
        addRewriteHavingPredicates(predicates, predicateClassification.getRetainedPredicates(), columnPlan);
        return predicates;
    }

    private static void addRewriteHavingPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   ColumnPlan columnPlan) {
        for (Map<String, Object> predicate : predicates) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(rewriteColumns(text(predicate.get("expression")), columnPlan));
            }
        }
    }

    private static String fromClause(String sourceSql, List<Map<String, Object>> baseTables, JoinPlan joinPlan) {
        String extractedFromClause = extractFromClause(sourceSql);
        if (StringUtils.hasText(extractedFromClause)) {
            return extractedFromClause;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(tableWithAlias(baseTables.get(0)));
        for (JoinClause joinClause : joinPlan.joinClauses) {
            builder.append('\n');
            builder.append("JOIN ");
            builder.append(tableWithAlias(joinClause.join));
            builder.append(" ON ");
            builder.append(joinClause.condition);
        }
        return builder.toString();
    }

    private static String extractFromClause(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return "";
        }
        String normalized = trimTrailingSemicolon(sourceSql).replaceAll("\\s+", " ").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        int fromIndex = upper.indexOf(" FROM ");
        if (fromIndex < 0) {
            return "";
        }
        int start = fromIndex + " FROM ".length();
        int end = normalized.length();
        for (String marker : Arrays.asList(" WHERE ", " GROUP BY ", " HAVING ", " ORDER BY ", " LIMIT ")) {
            int markerIndex = upper.indexOf(marker, start);
            if (markerIndex >= 0 && markerIndex < end) {
                end = markerIndex;
            }
        }
        return normalized.substring(start, end).trim();
    }

    private static String tableWithAlias(JoinClause joinClause) {
        if (!joinClause.rightTable.isEmpty()) {
            return tableWithAlias(joinClause.rightTable);
        }
        String right = text(joinClause.join.get("right"));
        String alias = text(joinClause.join.get("rightAlias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(right)) {
            return right + " " + alias;
        }
        return right;
    }

    private static String tableWithAlias(Map<String, Object> table) {
        String tableName = text(table.get("tableName"));
        String alias = text(table.get("alias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
            return tableName + " " + alias;
        }
        return tableName;
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

    private static String rewriteColumns(String expression, ColumnPlan columnPlan) {
        String result = expression;
        List<ColumnReplacement> replacements = new ArrayList<ColumnReplacement>();
        for (ColumnMapping mapping : columnPlan.mappings) {
            replacements.add(new ColumnReplacement(mapping.sourceColumn, mapping.outputColumn));
            if (!mapping.ambiguousName) {
                replacements.add(new ColumnReplacement(unqualifiedName(mapping.sourceColumn), mapping.outputColumn));
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
        String normalizedSource = cleanReference(source);
        Pattern pattern = Pattern.compile(
            "(?i)(^|[^A-Z0-9_$.])" + Pattern.quote(normalizedSource) + "([^A-Z0-9_]|$)"
        );
        return pattern.matcher(expression).replaceAll("$1" + target + "$2");
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

    private static void addSourceColumns(Set<String> target, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            boolean added = false;
            for (String sourceColumn : stringList(item.get("sourceColumns"))) {
                addText(target, sourceColumn);
                added = true;
            }
            if (!added) {
                addText(target, leftPredicateField(text(item.get("expression"))));
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

    private static void addText(Set<String> target, String value) {
        if (target != null && StringUtils.hasText(value)) {
            target.add(cleanReference(value));
        }
    }

    private static LinkedHashMap<String, Integer> unqualifiedCounts(Set<String> sourceColumns) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String sourceColumn : sourceColumns) {
            String key = normalizeName(unqualifiedName(sourceColumn));
            Integer current = counts.get(key);
            counts.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }
        return counts;
    }

    private static List<Map<String, Object>> baseTables(List<Map<String, Object>> tables) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (tables == null) {
            return result;
        }
        for (Map<String, Object> table : tables) {
            String sourceType = text(table.get("sourceType"));
            if (!StringUtils.hasText(sourceType) || "BASE_TABLE".equals(sourceType)) {
                result.add(table);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> joinRightTables(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return Collections.emptyList();
        }
        String normalized = sourceSql.replaceAll("\\s+", " ").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        int searchFrom = 0;
        while (searchFrom < normalized.length()) {
            int joinIndex = upper.indexOf(" JOIN ", searchFrom);
            if (joinIndex < 0) {
                break;
            }
            Token tableToken = readToken(normalized, joinIndex + " JOIN ".length());
            if (!StringUtils.hasText(tableToken.value)) {
                searchFrom = joinIndex + " JOIN ".length();
                continue;
            }
            Token next = readToken(normalized, tableToken.endIndex);
            String alias = "";
            if ("AS".equalsIgnoreCase(next.value)) {
                Token aliasToken = readToken(normalized, next.endIndex);
                alias = aliasToken.value;
                next = readToken(normalized, aliasToken.endIndex);
            } else if (!"ON".equalsIgnoreCase(next.value)) {
                alias = next.value;
                next = readToken(normalized, next.endIndex);
            }
            if (!"ON".equalsIgnoreCase(next.value)) {
                searchFrom = tableToken.endIndex;
                continue;
            }
            LinkedHashMap<String, Object> tableMap = new LinkedHashMap<String, Object>();
            tableMap.put("tableName", cleanTablePath(tableToken.value));
            tableMap.put("alias", cleanReference(alias));
            tableMap.put("sourceType", "BASE_TABLE");
            result.add(tableMap);
            searchFrom = next.endIndex;
        }
        return result;
    }

    private static Token readToken(String text, int startIndex) {
        int index = Math.max(0, startIndex);
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        int start = index;
        while (index < text.length() && !Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return new Token(start >= text.length() ? "" : text.substring(start, index), index);
    }

    private static String cleanTablePath(String value) {
        return cleanReference(value).replaceAll("\\s*\\.\\s*", ".");
    }

    private static boolean hasTableName(Map<String, Object> table) {
        return table != null && StringUtils.hasText(text(table.get("tableName")));
    }

    private static String firstTableName(List<Map<String, Object>> tables) {
        if (tables == null || tables.isEmpty()) {
            return "";
        }
        return text(tables.get(0).get("tableName"));
    }

    private static List<RelationSpec> relationSpecs(List<Map<String, Object>> tables) {
        List<RelationSpec> result = new ArrayList<RelationSpec>();
        for (Map<String, Object> table : tables) {
            result.add(new RelationSpec(text(table.get("tableName")), text(table.get("alias"))));
        }
        return result;
    }

    private static Map<String, RelationSpec> relationByName(List<RelationSpec> relations) {
        LinkedHashMap<String, RelationSpec> result = new LinkedHashMap<String, RelationSpec>();
        for (RelationSpec relation : relations) {
            for (String reference : relation.references) {
                result.put(relationKey(reference), relation);
            }
        }
        return result;
    }

    private static boolean isOuterJoin(String joinType) {
        return joinType.contains("LEFT")
            || joinType.contains("RIGHT")
            || joinType.contains("FULL")
            || joinType.contains("OUTER");
    }

    private static boolean isInnerLikeJoin(String joinType) {
        return "INNER".equals(joinType) || "SIMPLE".equals(joinType) || "JOIN".equals(joinType);
    }

    private static List<String> splitAndConditions(String condition) {
        if (!StringUtils.hasText(condition)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        String upper = condition.toUpperCase(Locale.ROOT);
        for (int i = 0; i < condition.length(); i++) {
            char ch = condition.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0 && upper.startsWith(" AND ", i)) {
                addConditionPart(result, condition.substring(start, i));
                start = i + 5;
                i += 4;
            }
        }
        addConditionPart(result, condition.substring(start));
        return result;
    }

    private static void addConditionPart(List<String> target, String value) {
        String cleaned = stripOuterParentheses(value);
        if (StringUtils.hasText(cleaned)) {
            target.add(cleaned);
        }
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

    private static String withAlias(String expression, String alias) {
        if (!StringUtils.hasText(alias) || alias.equalsIgnoreCase(expression)) {
            return expression;
        }
        return expression + " AS " + alias;
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

    private static boolean isIdentifierReference(String expression) {
        return StringUtils.hasText(expression)
            && cleanReference(expression).matches("[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*)*");
    }

    private static String qualifier(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? "" : cleaned.substring(0, index);
    }

    private static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    private static String cleanReference(String expression) {
        return StringUtils.hasText(expression)
            ? expression.replace("`", "").replace("\"", "").trim()
            : "";
    }

    private static String cleanName(String value) {
        String cleaned = StringUtils.hasText(value)
            ? value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
            : "";
        cleaned = cleaned.replaceAll("^_+", "").replaceAll("_+$", "");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "field";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "f_" + cleaned;
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64).replaceAll("_+$", "");
        }
        return cleaned;
    }

    private static String uniqueName(String candidate, Set<String> usedNames) {
        String base = StringUtils.hasText(candidate) ? candidate : "field";
        String result = base;
        int sequence = 2;
        while (usedNames.contains(result)) {
            result = base + "_" + sequence;
            sequence++;
        }
        usedNames.add(result);
        return result;
    }

    private static String relationKey(String value) {
        return cleanReference(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizeName(String value) {
        return StringUtils.hasText(value)
            ? cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
            : "";
    }

    private static int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
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

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
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

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String upperText(Object value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    static final class CandidateSql {

        private final List<Map<String, Object>> blockingReasons;
        private final String ddlSql;
        private final String refreshSql;
        private final String validationSql;
        private final String rollbackSql;
        private final String rewriteSql;
        private final List<Map<String, Object>> joinKeys;
        private final List<Map<String, Object>> fieldMappings;
        private final List<Map<String, Object>> aliasDisambiguation;
        private final Map<String, Object> rowAmplificationRisk;

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             JoinPlan joinPlan,
                             ColumnPlan columnPlan) {
            this.blockingReasons = immutableMapList(blockingReasons);
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.joinKeys = immutableMapList(joinPlan.joinKeys);
            this.fieldMappings = immutableMapList(columnPlan.fieldMappings());
            this.aliasDisambiguation = immutableMapList(columnPlan.aliasDisambiguation());
            this.rowAmplificationRisk = Collections.unmodifiableMap(rowAmplificationRisk());
        }

        private static CandidateSql blocked(List<Map<String, Object>> blockingReasons,
                                            JoinPlan joinPlan,
                                            ColumnPlan columnPlan) {
            return new CandidateSql(blockingReasons, null, null, null, null, null, joinPlan, columnPlan);
        }

        private static CandidateSql generated(String ddlSql,
                                              String refreshSql,
                                              String validationSql,
                                              String rollbackSql,
                                              String rewriteSql,
                                              JoinPlan joinPlan,
                                              ColumnPlan columnPlan) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                joinPlan,
                columnPlan
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

        List<Map<String, Object>> getJoinKeys() {
            return joinKeys;
        }

        List<Map<String, Object>> getFieldMappings() {
            return fieldMappings;
        }

        List<Map<String, Object>> getAliasDisambiguation() {
            return aliasDisambiguation;
        }

        Map<String, Object> getRowAmplificationRisk() {
            return rowAmplificationRisk;
        }
    }

    private static LinkedHashMap<String, Object> rowAmplificationRisk() {
        LinkedHashMap<String, Object> risk = new LinkedHashMap<String, Object>();
        risk.put("status", "REVIEW_REQUIRED");
        risk.put("generatedAllowed", Boolean.TRUE);
        risk.put("claimBoundary", "NO_ROW_AMPLIFICATION_PROOF_WITHOUT_METADATA");
        risk.put("missingEvidence", Arrays.asList(
            "JOIN_KEY_UNIQUENESS",
            "TABLE_CARDINALITY",
            "JOIN_SELECTIVITY"
        ));
        risk.put(
            "description",
            "当前仅证明 INNER 等值 Join 形态可预 Join；缺少唯一键/基数元数据时不能声明无行数放大。"
        );
        return risk;
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

    private static final class JoinPlan {
        private final List<Map<String, Object>> blockingReasons;
        private final List<Map<String, Object>> joinKeys;
        private final List<JoinClause> joinClauses;

        private JoinPlan(List<Map<String, Object>> blockingReasons,
                         List<Map<String, Object>> joinKeys,
                         List<JoinClause> joinClauses) {
            this.blockingReasons = blockingReasons;
            this.joinKeys = joinKeys;
            this.joinClauses = joinClauses;
        }
    }

    private static final class JoinClause {
        private final Map<String, Object> join;
        private final String condition;
        private final Map<String, Object> rightTable;

        private JoinClause(Map<String, Object> join, String condition, Map<String, Object> rightTable) {
            this.join = join;
            this.condition = condition;
            this.rightTable = rightTable;
        }
    }

    private static final class JoinKey {
        private final String leftKey;
        private final String rightKey;
        private final Map<String, Object> blockingReason;

        private JoinKey(String leftKey, String rightKey, Map<String, Object> blockingReason) {
            this.leftKey = leftKey;
            this.rightKey = rightKey;
            this.blockingReason = blockingReason;
        }

        private static JoinKey generated(String leftKey, String rightKey) {
            return new JoinKey(leftKey, rightKey, null);
        }

        private static JoinKey blocked(Map<String, Object> blockingReason) {
            return new JoinKey("", "", blockingReason);
        }
    }

    private static final class ColumnPlan {
        private final List<Map<String, Object>> blockingReasons;
        private final List<ColumnMapping> mappings;

        private ColumnPlan(List<Map<String, Object>> blockingReasons, List<ColumnMapping> mappings) {
            this.blockingReasons = blockingReasons;
            this.mappings = mappings;
        }

        private static ColumnPlan blocked(List<Map<String, Object>> blockingReasons) {
            return new ColumnPlan(blockingReasons, Collections.<ColumnMapping>emptyList());
        }

        private List<String> ddlSelectItems() {
            List<String> items = new ArrayList<String>();
            for (ColumnMapping mapping : mappings) {
                if (mapping.sourceColumn.equals(mapping.outputColumn)) {
                    items.add(mapping.sourceColumn);
                } else {
                    items.add(mapping.sourceColumn + " AS " + mapping.outputColumn);
                }
            }
            if (items.isEmpty()) {
                items.add("1 AS prejoin_marker");
            }
            return items;
        }

        private List<Map<String, Object>> fieldMappings() {
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (ColumnMapping mapping : mappings) {
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("sourceColumn", mapping.sourceColumn);
                item.put("mvColumn", mapping.outputColumn);
                item.put("sourceQualifier", mapping.qualifier);
                item.put("disambiguation", mapping.ambiguousName ? "QUALIFIER_PREFIXED" : "DIRECT");
                result.add(item);
            }
            return result;
        }

        private List<Map<String, Object>> aliasDisambiguation() {
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (ColumnMapping mapping : mappings) {
                if (!mapping.ambiguousName) {
                    continue;
                }
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("sourceColumn", mapping.sourceColumn);
                item.put("mvColumn", mapping.outputColumn);
                item.put("strategy", "QUALIFIER_PREFIXED");
                result.add(item);
            }
            return result;
        }
    }

    private static final class ColumnMapping {
        private final String sourceColumn;
        private final String outputColumn;
        private final String qualifier;
        private final boolean ambiguousName;

        private ColumnMapping(String sourceColumn, String outputColumn, String qualifier, boolean ambiguousName) {
            this.sourceColumn = sourceColumn;
            this.outputColumn = outputColumn;
            this.qualifier = qualifier;
            this.ambiguousName = ambiguousName;
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

    private static final class Token {
        private final String value;
        private final int endIndex;

        private Token(String value, int endIndex) {
            this.value = value;
            this.endIndex = endIndex;
        }
    }

    private static final class RelationSpec {
        private final Set<String> references = new LinkedHashSet<String>();

        private RelationSpec(String tableName, String alias) {
            addReference(alias);
            addReference(tableName);
            addReference(unqualifiedName(tableName));
        }

        private void addReference(String value) {
            if (StringUtils.hasText(value)) {
                references.add(cleanReference(value));
            }
        }

        private boolean owns(String sourceColumn) {
            String sourceQualifier = qualifier(sourceColumn);
            if (!StringUtils.hasText(sourceQualifier)) {
                return false;
            }
            return references.contains(sourceQualifier) || references.contains(unqualifiedName(sourceQualifier));
        }
    }
}
