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

final class L2StarAggMvCandidateGenerator {

    private static final Pattern QUALIFIED_REFERENCE_PATTERN =
        Pattern.compile("(?i)\\b[A-Z_][A-Z0-9_$]*\\.[A-Z_][A-Z0-9_$]*\\b");
    private static final Set<String> TIME_TOKENS =
        new LinkedHashSet<String>(Arrays.asList("DT", "DATE", "TIME", "DAY", "MONTH", "YEAR"));
    private static final Set<String> TIME_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("DATE_TRUNC", "TRUNC", "DATE_FORMAT", "YEAR", "MONTH", "DAY"));

    private L2StarAggMvCandidateGenerator() {
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
        RelationCatalog relationCatalog = RelationCatalog.from(baseTables);
        JoinPlan joinPlan = joinPlan(
            mapList(advancedStructureProfile == null ? null : advancedStructureProfile.get("joinGraph")),
            relationCatalog
        );
        blockingReasons.addAll(joinPlan.blockingReasons);

        FactPlan factPlan = FactPlan.blocked();
        DimensionPlan dimensionPlan = DimensionPlan.blocked();
        MeasurePlan measurePlan = MeasurePlan.blocked();
        if (blockingReasons.isEmpty()) {
            factPlan = factPlan(advancedStructureProfile, relationCatalog, joinPlan);
            blockingReasons.addAll(factPlan.blockingReasons);
        }
        if (blockingReasons.isEmpty()) {
            dimensionPlan = dimensionPlan(
                advancedStructureProfile,
                predicateClassification,
                grainMeasureDerivation,
                relationCatalog,
                factPlan
            );
            blockingReasons.addAll(dimensionPlan.blockingReasons);
        }
        if (blockingReasons.isEmpty()) {
            measurePlan = measurePlan(advancedStructureProfile, grainMeasureDerivation, relationCatalog, factPlan);
            blockingReasons.addAll(measurePlan.blockingReasons);
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons, factPlan, joinPlan, dimensionPlan, measurePlan);
        }

        String selectSql = selectSql(
            dimensionPlan.ddlSelectItems(),
            measurePlan.ddlSelectItems(),
            fromClause(sourceSql, baseTables),
            retainedWherePredicates(predicateClassification),
            dimensionPlan
        );
        String rewriteSql = rewriteSql(
            mvName,
            advancedStructureProfile,
            predicateClassification,
            grainMeasureDerivation.getMeasures(),
            dimensionPlan
        );
        String validationSql = validationSql(sourceSql, rewriteSql);
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, selectSql);
        if (renderedSql == null) {
            return CandidateSql.blocked(Collections.singletonList(reason(
                "UNSUPPORTED_TARGET_ENGINE",
                "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"
            )), factPlan, joinPlan, dimensionPlan, measurePlan);
        }
        return CandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql,
            renderedSql.getRollbackSql(),
            rewriteSql,
            factPlan,
            joinPlan,
            dimensionPlan,
            measurePlan
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
                "缺少高级结构画像，不能生成 STAR_AGG_MV。"
            ));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可发布的 STAR_AGG_MV SQL。"
            ));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason(
                "STAR_AGG_MV_ONLY",
                "AMV-007 只生成事实表 Join 维表后的星型聚合 MV。"
            ));
        }
        if (grainMeasureDerivation == null || grainMeasureDerivation.getMeasures().isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可重聚合指标，不能生成 STAR_AGG_MV。"));
        }
        if (!Boolean.TRUE.equals(mapValue(grainMeasureDerivation == null ? null : grainMeasureDerivation.getCoverage(),
            "coversSecurity"))) {
            reasons.add(reason(
                "STAR_AGG_SECURITY_PREDICATE_NOT_COVERED",
                "安全谓词字段未进入 MV 粒度，不能生成可发布 STAR_AGG_MV rewrite。"
            ));
        }
        if (mapList(advancedStructureProfile.get("joinGraph")).size() < 2) {
            reasons.add(reason(
                "STAR_AGG_JOIN_COUNT_REQUIRED",
                "STAR_AGG_MV 至少需要两个维表 Join。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_MV_DEFERRED",
                "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-007 中生成。"
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
                "OR 谓词需要保持原逻辑分组，AMV-007 暂不生成可发布 STAR_AGG_MV rewrite。"
            ));
        }
        if (baseTables(mapList(advancedStructureProfile.get("tables"))).size() < 3) {
            reasons.add(reason(
                "STAR_AGG_BASE_TABLES_REQUIRED",
                "STAR_AGG_MV 需要一个事实表和至少两个维表。"
            ));
        }
        return reasons;
    }

    private static JoinPlan joinPlan(List<Map<String, Object>> joinGraph, RelationCatalog relationCatalog) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        List<JoinEdge> edges = new ArrayList<JoinEdge>();
        int joinIndex = 0;
        for (Map<String, Object> join : joinGraph) {
            joinIndex++;
            String joinType = upperText(join.get("joinType"));
            if ("CROSS".equals(joinType)) {
                blockingReasons.add(reason(
                    "CROSS_JOIN_STAR_AGG_UNSUPPORTED",
                    "CROSS JOIN 可能产生笛卡尔积，AMV-007 不生成 STAR_AGG_MV。"
                ));
                continue;
            }
            if (isOuterJoin(joinType)) {
                blockingReasons.add(reason(
                    "OUTER_JOIN_STAR_AGG_UNSUPPORTED",
                    "OUTER JOIN 的空值补齐语义需要人工复核，AMV-007 默认阻断。"
                ));
                continue;
            }
            if (!isInnerLikeJoin(joinType)) {
                blockingReasons.add(reason(
                    "UNSUPPORTED_JOIN_TYPE_STAR_AGG",
                    "STAR_AGG_MV 只允许 INNER/SIMPLE 等值 Join。"
                ));
                continue;
            }
            List<String> conditions = splitAndConditions(text(join.get("condition")));
            if (conditions.isEmpty()) {
                blockingReasons.add(reason(
                    "JOIN_CONDITION_REQUIRED",
                    "缺少 ON 等值条件，不能生成 STAR_AGG_MV。"
                ));
                continue;
            }
            for (String condition : conditions) {
                JoinEdge edge = parseJoinEdge(joinIndex, joinType, condition, relationCatalog);
                if (edge.blockingReason != null) {
                    blockingReasons.add(edge.blockingReason);
                } else {
                    edges.add(edge);
                }
            }
        }
        return new JoinPlan(blockingReasons, edges, joinGraph.size());
    }

    private static JoinEdge parseJoinEdge(int joinIndex,
                                          String joinType,
                                          String condition,
                                          RelationCatalog relationCatalog) {
        int equalsIndex = topLevelEqualsIndex(condition);
        if (equalsIndex <= 0 || equalsIndex >= condition.length() - 1) {
            return JoinEdge.blocked(reason(
                "NON_EQUI_JOIN_STAR_AGG_UNSUPPORTED",
                "Join 条件不是简单等值表达式，AMV-007 默认阻断。"
            ));
        }
        String leftKey = stripOuterParentheses(condition.substring(0, equalsIndex).trim());
        String rightKey = stripOuterParentheses(condition.substring(equalsIndex + 1).trim());
        if (!isIdentifierReference(leftKey) || !isIdentifierReference(rightKey)) {
            return JoinEdge.blocked(reason(
                "COMPLEX_JOIN_KEY_STAR_AGG_UNSUPPORTED",
                "Join key 包含函数、CAST 或复杂表达式，AMV-007 默认阻断。"
            ));
        }
        RelationSpec leftRelation = relationCatalog.relationForColumn(leftKey);
        RelationSpec rightRelation = relationCatalog.relationForColumn(rightKey);
        if (leftRelation == null || rightRelation == null || leftRelation.key.equals(rightRelation.key)) {
            return JoinEdge.blocked(reason(
                "JOIN_KEY_SIDE_UNRESOLVED",
                "Join key 两侧无法按表别名或限定名消歧，不能生成 STAR_AGG_MV。"
            ));
        }
        return JoinEdge.generated(joinIndex, joinType, condition, leftKey, rightKey, leftRelation, rightRelation);
    }

    private static FactPlan factPlan(Map<String, Object> advancedStructureProfile,
                                     RelationCatalog relationCatalog,
                                     JoinPlan joinPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> measureRelationKeys = new LinkedHashSet<String>();
        for (String sourceColumn : measureSourceColumns(advancedStructureProfile)) {
            if (!isIdentifierReference(sourceColumn) || !StringUtils.hasText(qualifier(sourceColumn))) {
                reasons.add(reason(
                    "FACT_TABLE_UNRESOLVED",
                    "指标字段缺少表限定，无法在多表 Join 中证明事实表来源。"
                ));
                continue;
            }
            RelationSpec relation = relationCatalog.relationForColumn(sourceColumn);
            if (relation == null) {
                reasons.add(reason(
                    "FACT_TABLE_UNRESOLVED",
                    "指标字段来源不属于可解析基表，无法证明事实表。"
                ));
            } else {
                measureRelationKeys.add(relation.key);
            }
        }
        if (measureRelationKeys.size() > 1) {
            reasons.add(reason(
                "FACT_TABLE_UNRESOLVED",
                "指标字段来自多个表，当前静态规则不能保守识别唯一事实表。"
            ));
        }
        if (!reasons.isEmpty()) {
            return FactPlan.blocked(reasons);
        }

        String factKey = measureRelationKeys.isEmpty() ? "" : measureRelationKeys.iterator().next();
        String inference = "MEASURE_SOURCE_AND_JOIN_TOPOLOGY";
        if (!StringUtils.hasText(factKey)) {
            factKey = topologyFactKey(joinPlan);
            inference = "JOIN_TOPOLOGY_ONLY";
        }
        if (!StringUtils.hasText(factKey)) {
            return FactPlan.blocked(Collections.singletonList(reason(
                "FACT_TABLE_UNRESOLVED",
                "Join 拓扑不存在唯一中心表，无法保守识别事实表。"
            )));
        }
        if (!isJoinCenter(factKey, joinPlan)) {
            return FactPlan.blocked(Collections.singletonList(reason(
                "FACT_TABLE_UNRESOLVED",
                "指标来源表不是所有 Join 的中心表，当前静态规则不能证明星型拓扑。"
            )));
        }

        LinkedHashSet<String> dimensionKeys = new LinkedHashSet<String>();
        for (JoinEdge edge : joinPlan.edges) {
            if (!edge.involves(factKey)) {
                reasons.add(reason(
                    "STAR_AGG_JOIN_TO_FACT_REQUIRED",
                    "STAR_AGG_MV 只支持事实表直接连接每个维表的星型拓扑。"
                ));
                continue;
            }
            dimensionKeys.add(edge.other(factKey));
        }
        if (dimensionKeys.size() < 2) {
            reasons.add(reason(
                "STAR_AGG_DIMENSION_COUNT_REQUIRED",
                "STAR_AGG_MV 至少需要两个可区分维表。"
            ));
        }
        if (!reasons.isEmpty()) {
            return FactPlan.blocked(reasons);
        }
        return FactPlan.generated(
            factKey,
            inference,
            relationCatalog.factEvidence(factKey, inference),
            relationCatalog.dimensionEvidence(dimensionKeys)
        );
    }

    private static String topologyFactKey(JoinPlan joinPlan) {
        LinkedHashMap<String, Set<Integer>> relationJoinIndexes = new LinkedHashMap<String, Set<Integer>>();
        for (JoinEdge edge : joinPlan.edges) {
            addJoinIndex(relationJoinIndexes, edge.leftRelation.key, edge.joinIndex);
            addJoinIndex(relationJoinIndexes, edge.rightRelation.key, edge.joinIndex);
        }
        String candidate = "";
        for (Map.Entry<String, Set<Integer>> entry : relationJoinIndexes.entrySet()) {
            if (entry.getValue().size() == joinPlan.joinCount) {
                if (StringUtils.hasText(candidate)) {
                    return "";
                }
                candidate = entry.getKey();
            }
        }
        return candidate;
    }

    private static boolean isJoinCenter(String relationKey, JoinPlan joinPlan) {
        if (!StringUtils.hasText(relationKey) || joinPlan.joinCount <= 0) {
            return false;
        }
        LinkedHashSet<Integer> joinIndexes = new LinkedHashSet<Integer>();
        for (JoinEdge edge : joinPlan.edges) {
            if (edge.involves(relationKey)) {
                joinIndexes.add(Integer.valueOf(edge.joinIndex));
            }
        }
        return joinIndexes.size() == joinPlan.joinCount;
    }

    private static void addJoinIndex(Map<String, Set<Integer>> relationJoinIndexes, String relationKey, int joinIndex) {
        Set<Integer> indexes = relationJoinIndexes.get(relationKey);
        if (indexes == null) {
            indexes = new LinkedHashSet<Integer>();
            relationJoinIndexes.put(relationKey, indexes);
        }
        indexes.add(Integer.valueOf(joinIndex));
    }

    private static DimensionPlan dimensionPlan(Map<String, Object> advancedStructureProfile,
                                               L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                               L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
                                               RelationCatalog relationCatalog,
                                               FactPlan factPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        List<DimensionSpec> dimensions = dimensionSpecs(
            grainMeasureDerivation.getDimensions(),
            mapList(advancedStructureProfile.get("groupBy")),
            predicateClassification,
            relationCatalog,
            factPlan,
            reasons
        );
        if (dimensions.isEmpty()) {
            reasons.add(reason(
                "STAR_AGG_GRAIN_NOT_COVERED",
                "缺少可物化的时间字段或维表属性粒度，不能生成 STAR_AGG_MV。"
            ));
        }
        if (!securityPredicatesCovered(predicateClassification, dimensions)) {
            reasons.add(reason(
                "STAR_AGG_SECURITY_PREDICATE_NOT_COVERED",
                "安全谓词字段未保留为 MV 维度，不能生成可发布 rewrite。"
            ));
        }
        if (!reasons.isEmpty()) {
            return DimensionPlan.blocked(reasons);
        }
        return new DimensionPlan(Collections.<Map<String, Object>>emptyList(), dimensions);
    }

    private static List<DimensionSpec> dimensionSpecs(List<String> dimensions,
                                                      List<Map<String, Object>> groupBy,
                                                      L2PredicateClassifier.PredicateClassificationResult
                                                          predicateClassification,
                                                      RelationCatalog relationCatalog,
                                                      FactPlan factPlan,
                                                      List<Map<String, Object>> reasons) {
        List<DimensionSpec> result = new ArrayList<DimensionSpec>();
        LinkedHashSet<String> usedNames = new LinkedHashSet<String>();
        LinkedHashMap<String, Integer> unqualifiedCounts = dimensionNameCounts(dimensions);
        for (String dimension : dimensions) {
            if (!StringUtils.hasText(dimension)) {
                continue;
            }
            DimensionContext context = dimensionContext(dimension, groupBy, predicateClassification);
            String sourceExpression = context.sourceExpression;
            RelationSpec relation = relationForDimension(sourceExpression, context.references, relationCatalog);
            boolean timeDimension = isTimeDimension(sourceExpression, context.references);
            if (relation == null) {
                reasons.add(reason(
                    context.security ? "STAR_AGG_SECURITY_PREDICATE_NOT_COVERED" : "STAR_AGG_DIMENSION_SOURCE_UNRESOLVED",
                    "维度字段缺少表限定或无法解析来源，不能证明 STAR_AGG_MV 粒度覆盖。"
                ));
                continue;
            }
            if (context.groupByDimension && factPlan.factKey.equals(relation.key) && !timeDimension) {
                reasons.add(reason(
                    "STAR_AGG_GRAIN_NOT_COVERED",
                    "STAR_AGG_MV 的 GROUP BY 只允许事实表时间字段或维表属性。"
                ));
                continue;
            }
            if (context.groupByDimension
                && !factPlan.factKey.equals(relation.key)
                && !factPlan.dimensionKeys().contains(relation.key)) {
                reasons.add(reason(
                    "STAR_AGG_GRAIN_NOT_COVERED",
                    "GROUP BY 字段来源不属于事实表时间字段或维表属性。"
                ));
                continue;
            }
            String outputName = outputName(sourceExpression, relation, unqualifiedCounts, usedNames);
            result.add(new DimensionSpec(
                sourceExpression,
                outputName,
                context.references,
                relation,
                context.groupByDimension,
                context.externalized,
                context.security,
                dimensionRole(relation, factPlan, timeDimension, context)
            ));
        }
        return result;
    }

    private static DimensionContext dimensionContext(String dimension,
                                                     List<Map<String, Object>> groupBy,
                                                     L2PredicateClassifier.PredicateClassificationResult
                                                         predicateClassification) {
        LinkedHashSet<String> references = new LinkedHashSet<String>();
        references.add(dimension.trim());
        String sourceExpression = dimension.trim();
        boolean groupByDimension = false;
        for (Map<String, Object> groupByItem : groupBy) {
            if (sameExpression(dimension, text(groupByItem.get("expression")))
                || containsSameExpression(stringList(groupByItem.get("sourceColumns")), dimension)) {
                sourceExpression = text(groupByItem.get("expression"));
                references.add(sourceExpression);
                references.addAll(stringList(groupByItem.get("sourceColumns")));
                groupByDimension = true;
            }
        }
        PredicateFlags flags = predicateFlags(dimension, predicateClassification, references);
        return new DimensionContext(
            sourceExpression,
            references,
            groupByDimension,
            flags.externalized,
            flags.security
        );
    }

    private static PredicateFlags predicateFlags(String dimension,
                                                 L2PredicateClassifier.PredicateClassificationResult classification,
                                                 Set<String> references) {
        PredicateFlags flags = new PredicateFlags();
        if (classification == null) {
            return flags;
        }
        applyPredicateFlags(flags, dimension, classification.getExternalizedPredicates(), references, true);
        applyPredicateFlags(flags, dimension, classification.getSecurityPredicates(), references, false);
        return flags;
    }

    private static void applyPredicateFlags(PredicateFlags flags,
                                            String dimension,
                                            List<Map<String, Object>> predicates,
                                            Set<String> references,
                                            boolean externalized) {
        for (Map<String, Object> predicate : predicates) {
            if (sameExpression(dimension, leftPredicateField(text(predicate.get("expression"))))
                || containsSameExpression(stringList(predicate.get("sourceColumns")), dimension)) {
                references.add(text(predicate.get("expression")));
                references.add(leftPredicateField(text(predicate.get("expression"))));
                references.addAll(stringList(predicate.get("sourceColumns")));
                if (externalized) {
                    flags.externalized = true;
                } else {
                    flags.security = true;
                }
            }
        }
    }

    private static RelationSpec relationForDimension(String sourceExpression,
                                                     Set<String> references,
                                                     RelationCatalog relationCatalog) {
        if (isIdentifierReference(sourceExpression)) {
            return relationCatalog.relationForColumn(sourceExpression);
        }
        for (String reference : references) {
            if (isIdentifierReference(reference)) {
                RelationSpec relation = relationCatalog.relationForColumn(reference);
                if (relation != null) {
                    return relation;
                }
            }
        }
        return null;
    }

    private static String dimensionRole(RelationSpec relation,
                                        FactPlan factPlan,
                                        boolean timeDimension,
                                        DimensionContext context) {
        if (factPlan.factKey.equals(relation.key)) {
            if (timeDimension) {
                return "FACT_TIME_FIELD";
            }
            if (context.security) {
                return "FACT_SECURITY_FIELD";
            }
            return "FACT_PARAMETER_FIELD";
        }
        return "DIMENSION_ATTRIBUTE";
    }

    private static MeasurePlan measurePlan(Map<String, Object> advancedStructureProfile,
                                           L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
                                           RelationCatalog relationCatalog,
                                           FactPlan factPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        List<MeasureColumn> columns = new ArrayList<MeasureColumn>();
        List<Map<String, Object>> sources = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> measure : grainMeasureDerivation.getMeasures()) {
            List<Map<String, Object>> components = mapList(measure.get("components"));
            if (components.isEmpty()) {
                addMeasureColumn(columns, sources, reasons, measure, advancedStructureProfile, relationCatalog, factPlan);
            } else {
                for (Map<String, Object> component : components) {
                    addMeasureColumn(columns, sources, reasons, component, advancedStructureProfile, relationCatalog, factPlan);
                }
                sources.add(measureSourceEvidence(measure, sourceColumnsForExpression(
                    text(measure.get("sourceExpression")),
                    advancedStructureProfile
                ), factPlan));
            }
        }
        if (columns.isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可写入 STAR_AGG_MV 的指标列。"));
        }
        if (!reasons.isEmpty()) {
            return MeasurePlan.blocked(reasons);
        }
        return new MeasurePlan(Collections.<Map<String, Object>>emptyList(), columns, sources);
    }

    private static void addMeasureColumn(List<MeasureColumn> columns,
                                         List<Map<String, Object>> sources,
                                         List<Map<String, Object>> reasons,
                                         Map<String, Object> measure,
                                         Map<String, Object> advancedStructureProfile,
                                         RelationCatalog relationCatalog,
                                         FactPlan factPlan) {
        String sourceExpression = text(measure.get("sourceExpression"));
        String name = text(measure.get("name"));
        List<String> sourceColumns = sourceColumnsForExpression(sourceExpression, advancedStructureProfile);
        for (String sourceColumn : sourceColumns) {
            if (!isIdentifierReference(sourceColumn) || !StringUtils.hasText(qualifier(sourceColumn))) {
                reasons.add(reason(
                    "STAR_AGG_MEASURE_SOURCE_NOT_FACT",
                    "指标字段缺少事实表限定，不能生成 STAR_AGG_MV。"
                ));
                continue;
            }
            RelationSpec relation = relationCatalog.relationForColumn(sourceColumn);
            if (relation == null || !factPlan.factKey.equals(relation.key)) {
                reasons.add(reason(
                    "STAR_AGG_MEASURE_SOURCE_NOT_FACT",
                    "STAR_AGG_MV 指标必须来自事实表字段。"
                ));
            }
        }
        columns.add(new MeasureColumn(name, sourceExpression));
        sources.add(measureSourceEvidence(measure, sourceColumns, factPlan));
    }

    private static List<String> sourceColumnsForExpression(String expression, Map<String, Object> advancedStructureProfile) {
        String normalized = normalizeExpression(expression);
        for (Map<String, Object> aggregation : mapList(advancedStructureProfile.get("aggregations"))) {
            if (normalized.equals(normalizeExpression(text(aggregation.get("expression"))))) {
                return stringList(aggregation.get("sourceColumns"));
            }
        }
        return qualifiedReferences(expression);
    }

    private static List<String> measureSourceColumns(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Map<String, Object> aggregation : mapList(advancedStructureProfile.get("aggregations"))) {
            result.addAll(stringList(aggregation.get("sourceColumns")));
        }
        return new ArrayList<String>(result);
    }

    private static List<String> qualifiedReferences(String expression) {
        if (!StringUtils.hasText(expression)) {
            return Collections.emptyList();
        }
        Matcher matcher = QUALIFIED_REFERENCE_PATTERN.matcher(expression);
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return new ArrayList<String>(result);
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

    private static String selectSql(List<String> dimensionSelectItems,
                                    List<String> measureSelectItems,
                                    String fromClause,
                                    List<String> retainedWherePredicates,
                                    DimensionPlan dimensionPlan) {
        List<String> selectItems = new ArrayList<String>();
        selectItems.addAll(dimensionSelectItems);
        selectItems.addAll(measureSelectItems);
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
        builder.append("GROUP BY ");
        builder.append(String.join(", ", dimensionPlan.ddlGroupByItems()));
        builder.append('\n');
        builder.append(';');
        return builder.toString();
    }

    private static String rewriteSql(String mvName,
                                     Map<String, Object> advancedStructureProfile,
                                     L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                     List<Map<String, Object>> measures,
                                     DimensionPlan dimensionPlan) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, measures, dimensionPlan);
        List<String> wherePredicates = rewriteFilterPredicates(predicateClassification, dimensionPlan);
        List<String> groupByItems = rewriteGroupByItems(mapList(advancedStructureProfile.get("groupBy")), dimensionPlan);
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, measures, dimensionPlan);

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
                                                   DimensionPlan dimensionPlan) {
        List<String> selectItems = new ArrayList<String>();
        Set<String> selectedDimensions = new LinkedHashSet<String>();
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            if (isMeasureProjection(projection, measures)) {
                continue;
            }
            DimensionSpec dimension = dimensionPlan.find(text(projection.get("expression")));
            if (dimension == null) {
                dimension = dimensionPlan.findBySources(stringList(projection.get("sourceColumns")));
            }
            if (dimension == null || selectedDimensions.contains(dimension.outputName)) {
                continue;
            }
            selectItems.add(withAlias(dimension.outputName, text(projection.get("alias"))));
            selectedDimensions.add(dimension.outputName);
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
        if ("AGGREGATION".equals(text(projection.get("expressionType")))) {
            return true;
        }
        for (Map<String, Object> measure : measures) {
            if (expression.equals(normalizeExpression(text(measure.get("sourceExpression"))))) {
                return true;
            }
        }
        return containsAggregateToken(expression);
    }

    private static List<String> rewriteFilterPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        DimensionPlan dimensionPlan) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteFilterPredicates(predicates, predicateClassification.getExternalizedPredicates(), dimensionPlan);
        addRewriteFilterPredicates(predicates, predicateClassification.getSecurityPredicates(), dimensionPlan);
        return predicates;
    }

    private static void addRewriteFilterPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   DimensionPlan dimensionPlan) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(rewriteColumns(text(predicate.get("expression")), dimensionPlan));
            }
        }
    }

    private static List<String> rewriteGroupByItems(List<Map<String, Object>> groupBy,
                                                    DimensionPlan dimensionPlan) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupByItem : groupBy) {
            DimensionSpec dimension = dimensionPlan.find(text(groupByItem.get("expression")));
            if (dimension == null) {
                dimension = dimensionPlan.findBySources(stringList(groupByItem.get("sourceColumns")));
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
        DimensionPlan dimensionPlan) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteMeasureExpressions(text(predicate.get("expression")), measures);
                predicates.add(rewriteColumns(expression, dimensionPlan));
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

    private static String rewriteColumns(String expression, DimensionPlan dimensionPlan) {
        String result = expression;
        List<ColumnReplacement> replacements = new ArrayList<ColumnReplacement>();
        for (DimensionSpec dimension : dimensionPlan.dimensions) {
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
        Pattern pattern = Pattern.compile(
            "(?i)(^|[^A-Z0-9_$.])" + Pattern.quote(cleanReference(source)) + "([^A-Z0-9_]|$)"
        );
        return pattern.matcher(expression).replaceAll("$1" + target + "$2");
    }

    private static boolean securityPredicatesCovered(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        List<DimensionSpec> dimensions) {
        if (predicateClassification == null || predicateClassification.getSecurityPredicates().isEmpty()) {
            return true;
        }
        for (Map<String, Object> predicate : predicateClassification.getSecurityPredicates()) {
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                sourceColumns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            boolean covered = false;
            for (String sourceColumn : sourceColumns) {
                for (DimensionSpec dimension : dimensions) {
                    if (dimension.matches(sourceColumn)) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String fromClause(String sourceSql, List<Map<String, Object>> baseTables) {
        String extracted = extractFromClause(sourceSql);
        if (StringUtils.hasText(extracted)) {
            return extracted;
        }
        if (baseTables.isEmpty()) {
            return "";
        }
        return tableWithAlias(baseTables.get(0));
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

    private static String validationSql(String sourceSql, String rewriteSql) {
        return "WITH original_result AS (\n"
            + trimTrailingSemicolon(sourceSql)
            + "\n),\nrewrite_result AS (\n"
            + trimTrailingSemicolon(rewriteSql)
            + "\n)\nSELECT 'original' AS source_name, COUNT(*) AS row_count FROM original_result\n"
            + "UNION ALL\n"
            + "SELECT 'rewrite' AS source_name, COUNT(*) AS row_count FROM rewrite_result;";
    }

    private static LinkedHashMap<String, Object> measureSourceEvidence(Map<String, Object> measure,
                                                                        List<String> sourceColumns,
                                                                        FactPlan factPlan) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("name", text(measure.get("name")));
        source.put("measureType", text(measure.get("measureType")));
        source.put("sourceExpression", text(measure.get("sourceExpression")));
        source.put("sourceColumns", new ArrayList<String>(sourceColumns));
        source.put("factTable", mapValue(factPlan.factTable, "tableName"));
        source.put("factAlias", mapValue(factPlan.factTable, "alias"));
        return source;
    }

    private static boolean isTimeDimension(String expression, Set<String> references) {
        String upper = upperText(expression);
        for (String functionName : TIME_FUNCTIONS) {
            if (upper.startsWith(functionName + "(") || upper.contains(functionName + "(")) {
                return true;
            }
        }
        for (String reference : references) {
            String name = unqualifiedName(reference).toUpperCase(Locale.ROOT);
            for (String token : TIME_TOKENS) {
                if (name.equals(token) || name.endsWith("_" + token) || name.contains(token + "_")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static LinkedHashMap<String, Integer> dimensionNameCounts(List<String> dimensions) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String dimension : dimensions) {
            String key = normalizeName(unqualifiedName(dimension));
            Integer current = counts.get(key);
            counts.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }
        return counts;
    }

    private static String outputName(String sourceExpression,
                                     RelationSpec relation,
                                     Map<String, Integer> unqualifiedCounts,
                                     Set<String> usedNames) {
        String base = columnName(sourceExpression);
        if (isIdentifierReference(sourceExpression)
            && intValue(unqualifiedCounts.get(normalizeName(unqualifiedName(sourceExpression)))) > 1) {
            base = cleanName(firstText(relation.alias, relation.tableName) + "_" + unqualifiedName(sourceExpression));
        }
        return uniqueName(base, usedNames);
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

    private static boolean containsSameExpression(List<String> expressions, String expected) {
        for (String expression : expressions) {
            if (sameExpression(expression, expected)) {
                return true;
            }
        }
        return false;
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
        return index < 0 ? "" : expression.substring(0, index).trim();
    }

    private static String tableWithAlias(Map<String, Object> table) {
        String tableName = text(table.get("tableName"));
        String alias = text(table.get("alias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
            return tableName + " " + alias;
        }
        return tableName;
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

    private static String columnName(String expression) {
        String candidate = isIdentifierReference(expression) ? unqualifiedName(expression) : expression;
        return cleanName(candidate);
    }

    private static String normalizeName(String value) {
        return StringUtils.hasText(value)
            ? cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
            : "";
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
        return "";
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
        private final Map<String, Object> factTable;
        private final List<Map<String, Object>> dimensionTables;
        private final List<Map<String, Object>> joinKeys;
        private final List<Map<String, Object>> dimensionSources;
        private final List<Map<String, Object>> measureSources;
        private final Map<String, Object> starSchemaEvidence;

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             FactPlan factPlan,
                             JoinPlan joinPlan,
                             DimensionPlan dimensionPlan,
                             MeasurePlan measurePlan) {
            this.blockingReasons = immutableMapList(blockingReasons);
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.factTable = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(factPlan.factTable));
            this.dimensionTables = immutableMapList(factPlan.dimensionTables);
            this.joinKeys = immutableMapList(joinPlan.joinKeys());
            this.dimensionSources = immutableMapList(dimensionPlan.dimensionSources());
            this.measureSources = immutableMapList(measurePlan.measureSources);
            this.starSchemaEvidence = Collections.unmodifiableMap(starSchemaEvidence(factPlan, joinPlan, dimensionPlan, measurePlan));
        }

        private static CandidateSql blocked(List<Map<String, Object>> blockingReasons,
                                            FactPlan factPlan,
                                            JoinPlan joinPlan,
                                            DimensionPlan dimensionPlan,
                                            MeasurePlan measurePlan) {
            return new CandidateSql(blockingReasons, null, null, null, null, null, factPlan, joinPlan, dimensionPlan, measurePlan);
        }

        private static CandidateSql generated(String ddlSql,
                                              String refreshSql,
                                              String validationSql,
                                              String rollbackSql,
                                              String rewriteSql,
                                              FactPlan factPlan,
                                              JoinPlan joinPlan,
                                              DimensionPlan dimensionPlan,
                                              MeasurePlan measurePlan) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                factPlan,
                joinPlan,
                dimensionPlan,
                measurePlan
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

        Map<String, Object> getFactTable() {
            return factTable;
        }

        List<Map<String, Object>> getDimensionTables() {
            return dimensionTables;
        }

        List<Map<String, Object>> getJoinKeys() {
            return joinKeys;
        }

        List<Map<String, Object>> getDimensionSources() {
            return dimensionSources;
        }

        List<Map<String, Object>> getMeasureSources() {
            return measureSources;
        }

        Map<String, Object> getStarSchemaEvidence() {
            return starSchemaEvidence;
        }
    }

    private static Map<String, Object> starSchemaEvidence(FactPlan factPlan,
                                                          JoinPlan joinPlan,
                                                          DimensionPlan dimensionPlan,
                                                          MeasurePlan measurePlan) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("factInference", factPlan.inference);
        evidence.put("joinCount", Integer.valueOf(joinPlan.joinCount));
        evidence.put("dimensionTableCount", Integer.valueOf(factPlan.dimensionTables.size()));
        evidence.put("dimensionColumnCount", Integer.valueOf(dimensionPlan.dimensions.size()));
        evidence.put("measureColumnCount", Integer.valueOf(measurePlan.columns.size()));
        evidence.put("claimBoundary", "STATIC_STAR_SCHEMA_INFERENCE_WITHOUT_CARDINALITY_METADATA");
        evidence.put("missingRuntimeEvidence", Arrays.asList(
            "FACT_TABLE_ROW_COUNT",
            "DIMENSION_KEY_UNIQUENESS",
            "JOIN_SELECTIVITY"
        ));
        return evidence;
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

    private static final class RelationCatalog {
        private final Map<String, RelationSpec> relationByKey;
        private final Map<String, RelationSpec> relationByReference;

        private RelationCatalog(Map<String, RelationSpec> relationByKey,
                                Map<String, RelationSpec> relationByReference) {
            this.relationByKey = relationByKey;
            this.relationByReference = relationByReference;
        }

        private static RelationCatalog from(List<Map<String, Object>> tables) {
            LinkedHashMap<String, RelationSpec> byKey = new LinkedHashMap<String, RelationSpec>();
            LinkedHashMap<String, RelationSpec> byReference = new LinkedHashMap<String, RelationSpec>();
            for (Map<String, Object> table : tables) {
                RelationSpec relation = new RelationSpec(text(table.get("tableName")), text(table.get("alias")));
                byKey.put(relation.key, relation);
                for (String reference : relation.references) {
                    byReference.put(relationKey(reference), relation);
                }
            }
            return new RelationCatalog(byKey, byReference);
        }

        private RelationSpec relationForColumn(String sourceColumn) {
            String sourceQualifier = qualifier(sourceColumn);
            if (!StringUtils.hasText(sourceQualifier)) {
                return null;
            }
            return relationByReference.get(relationKey(sourceQualifier));
        }

        private Map<String, Object> factEvidence(String factKey, String inference) {
            RelationSpec fact = relationByKey.get(factKey);
            Map<String, Object> item = fact == null ? new LinkedHashMap<String, Object>() : fact.toMap();
            item.put("inference", inference);
            return item;
        }

        private List<Map<String, Object>> dimensionEvidence(Set<String> dimensionKeys) {
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (String dimensionKey : dimensionKeys) {
                RelationSpec relation = relationByKey.get(dimensionKey);
                if (relation != null) {
                    result.add(relation.toMap());
                }
            }
            return result;
        }
    }

    private static String relationKey(String value) {
        return cleanReference(value).toUpperCase(Locale.ROOT);
    }

    private static final class RelationSpec {
        private final String key;
        private final String tableName;
        private final String alias;
        private final Set<String> references = new LinkedHashSet<String>();

        private RelationSpec(String tableName, String alias) {
            this.tableName = tableName;
            this.alias = alias;
            this.key = relationKey(firstText(alias, tableName));
            addReference(alias);
            addReference(tableName);
            addReference(unqualifiedName(tableName));
        }

        private void addReference(String value) {
            if (StringUtils.hasText(value)) {
                references.add(cleanReference(value));
            }
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("tableName", tableName);
            item.put("alias", alias);
            return item;
        }
    }

    private static final class JoinPlan {
        private final List<Map<String, Object>> blockingReasons;
        private final List<JoinEdge> edges;
        private final int joinCount;

        private JoinPlan(List<Map<String, Object>> blockingReasons, List<JoinEdge> edges, int joinCount) {
            this.blockingReasons = blockingReasons;
            this.edges = edges;
            this.joinCount = joinCount;
        }

        private List<Map<String, Object>> joinKeys() {
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (JoinEdge edge : edges) {
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("joinIndex", Integer.valueOf(edge.joinIndex));
                item.put("joinType", "INNER");
                item.put("condition", edge.condition);
                item.put("leftRelation", edge.leftRelation.tableName);
                item.put("leftAlias", edge.leftRelation.alias);
                item.put("rightRelation", edge.rightRelation.tableName);
                item.put("rightAlias", edge.rightRelation.alias);
                item.put("leftKey", edge.leftKey);
                item.put("rightKey", edge.rightKey);
                result.add(item);
            }
            return result;
        }
    }

    private static final class JoinEdge {
        private final int joinIndex;
        private final String joinType;
        private final String condition;
        private final String leftKey;
        private final String rightKey;
        private final RelationSpec leftRelation;
        private final RelationSpec rightRelation;
        private final Map<String, Object> blockingReason;

        private JoinEdge(int joinIndex,
                         String joinType,
                         String condition,
                         String leftKey,
                         String rightKey,
                         RelationSpec leftRelation,
                         RelationSpec rightRelation,
                         Map<String, Object> blockingReason) {
            this.joinIndex = joinIndex;
            this.joinType = joinType;
            this.condition = condition;
            this.leftKey = leftKey;
            this.rightKey = rightKey;
            this.leftRelation = leftRelation;
            this.rightRelation = rightRelation;
            this.blockingReason = blockingReason;
        }

        private static JoinEdge generated(int joinIndex,
                                          String joinType,
                                          String condition,
                                          String leftKey,
                                          String rightKey,
                                          RelationSpec leftRelation,
                                          RelationSpec rightRelation) {
            return new JoinEdge(joinIndex, joinType, condition, leftKey, rightKey, leftRelation, rightRelation, null);
        }

        private static JoinEdge blocked(Map<String, Object> blockingReason) {
            return new JoinEdge(0, "", "", "", "", null, null, blockingReason);
        }

        private boolean involves(String relationKey) {
            return leftRelation.key.equals(relationKey) || rightRelation.key.equals(relationKey);
        }

        private String other(String relationKey) {
            return leftRelation.key.equals(relationKey) ? rightRelation.key : leftRelation.key;
        }
    }

    private static final class FactPlan {
        private final List<Map<String, Object>> blockingReasons;
        private final String factKey;
        private final String inference;
        private final Map<String, Object> factTable;
        private final List<Map<String, Object>> dimensionTables;

        private FactPlan(List<Map<String, Object>> blockingReasons,
                         String factKey,
                         String inference,
                         Map<String, Object> factTable,
                         List<Map<String, Object>> dimensionTables) {
            this.blockingReasons = blockingReasons;
            this.factKey = factKey;
            this.inference = inference;
            this.factTable = factTable;
            this.dimensionTables = dimensionTables;
        }

        private static FactPlan blocked() {
            return blocked(Collections.<Map<String, Object>>emptyList());
        }

        private static FactPlan blocked(List<Map<String, Object>> blockingReasons) {
            return new FactPlan(
                blockingReasons,
                "",
                "UNRESOLVED",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList()
            );
        }

        private static FactPlan generated(String factKey,
                                          String inference,
                                          Map<String, Object> factTable,
                                          List<Map<String, Object>> dimensionTables) {
            return new FactPlan(Collections.<Map<String, Object>>emptyList(), factKey, inference, factTable, dimensionTables);
        }

        private Set<String> dimensionKeys() {
            LinkedHashSet<String> keys = new LinkedHashSet<String>();
            for (Map<String, Object> dimensionTable : dimensionTables) {
                keys.add(relationKey(firstText(text(dimensionTable.get("alias")), text(dimensionTable.get("tableName")))));
            }
            return keys;
        }
    }

    private static final class DimensionPlan {
        private final List<Map<String, Object>> blockingReasons;
        private final List<DimensionSpec> dimensions;

        private DimensionPlan(List<Map<String, Object>> blockingReasons, List<DimensionSpec> dimensions) {
            this.blockingReasons = blockingReasons;
            this.dimensions = dimensions;
        }

        private static DimensionPlan blocked() {
            return blocked(Collections.<Map<String, Object>>emptyList());
        }

        private static DimensionPlan blocked(List<Map<String, Object>> blockingReasons) {
            return new DimensionPlan(blockingReasons, Collections.<DimensionSpec>emptyList());
        }

        private List<String> ddlSelectItems() {
            List<String> items = new ArrayList<String>();
            for (DimensionSpec dimension : dimensions) {
                if (dimension.sourceExpression.equals(dimension.outputName)) {
                    items.add(dimension.sourceExpression);
                } else {
                    items.add(dimension.sourceExpression + " AS " + dimension.outputName);
                }
            }
            return items;
        }

        private List<String> ddlGroupByItems() {
            List<String> items = new ArrayList<String>();
            for (DimensionSpec dimension : dimensions) {
                items.add(dimension.sourceExpression);
            }
            return items;
        }

        private DimensionSpec find(String expression) {
            String normalized = normalizeExpression(expression);
            for (DimensionSpec dimension : dimensions) {
                if (dimension.matches(normalized)) {
                    return dimension;
                }
            }
            return null;
        }

        private DimensionSpec findBySources(List<String> sourceColumns) {
            for (String sourceColumn : sourceColumns) {
                DimensionSpec dimension = find(sourceColumn);
                if (dimension != null) {
                    return dimension;
                }
            }
            return null;
        }

        private List<Map<String, Object>> dimensionSources() {
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (DimensionSpec dimension : dimensions) {
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("sourceExpression", dimension.sourceExpression);
                item.put("mvColumn", dimension.outputName);
                item.put("sourceColumns", new ArrayList<String>(dimension.references));
                item.put("sourceTable", dimension.relation.tableName);
                item.put("sourceAlias", dimension.relation.alias);
                item.put("sourceRole", dimension.role);
                item.put("groupByDimension", Boolean.valueOf(dimension.groupByDimension));
                item.put("externalizedPredicateDimension", Boolean.valueOf(dimension.externalized));
                item.put("securityPredicateDimension", Boolean.valueOf(dimension.security));
                result.add(item);
            }
            return result;
        }
    }

    private static final class DimensionSpec {
        private final String sourceExpression;
        private final String outputName;
        private final Set<String> references;
        private final RelationSpec relation;
        private final boolean groupByDimension;
        private final boolean externalized;
        private final boolean security;
        private final String role;

        private DimensionSpec(String sourceExpression,
                              String outputName,
                              Set<String> references,
                              RelationSpec relation,
                              boolean groupByDimension,
                              boolean externalized,
                              boolean security,
                              String role) {
            this.sourceExpression = sourceExpression;
            this.outputName = outputName;
            this.references = Collections.unmodifiableSet(new LinkedHashSet<String>(references));
            this.relation = relation;
            this.groupByDimension = groupByDimension;
            this.externalized = externalized;
            this.security = security;
            this.role = role;
        }

        private boolean matches(String expression) {
            String normalized = normalizeExpression(expression);
            if (normalizeExpression(sourceExpression).equals(normalized)
                || normalizeExpression(outputName).equals(normalized)) {
                return true;
            }
            for (String reference : references) {
                if (normalizeExpression(reference).equals(normalized)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class DimensionContext {
        private final String sourceExpression;
        private final Set<String> references;
        private final boolean groupByDimension;
        private final boolean externalized;
        private final boolean security;

        private DimensionContext(String sourceExpression,
                                 Set<String> references,
                                 boolean groupByDimension,
                                 boolean externalized,
                                 boolean security) {
            this.sourceExpression = sourceExpression;
            this.references = references;
            this.groupByDimension = groupByDimension;
            this.externalized = externalized;
            this.security = security;
        }
    }

    private static final class PredicateFlags {
        private boolean externalized;
        private boolean security;
    }

    private static final class MeasurePlan {
        private final List<Map<String, Object>> blockingReasons;
        private final List<MeasureColumn> columns;
        private final List<Map<String, Object>> measureSources;

        private MeasurePlan(List<Map<String, Object>> blockingReasons,
                            List<MeasureColumn> columns,
                            List<Map<String, Object>> measureSources) {
            this.blockingReasons = blockingReasons;
            this.columns = columns;
            this.measureSources = measureSources;
        }

        private static MeasurePlan blocked() {
            return blocked(Collections.<Map<String, Object>>emptyList());
        }

        private static MeasurePlan blocked(List<Map<String, Object>> blockingReasons) {
            return new MeasurePlan(
                blockingReasons,
                Collections.<MeasureColumn>emptyList(),
                Collections.<Map<String, Object>>emptyList()
            );
        }

        private List<String> ddlSelectItems() {
            List<String> items = new ArrayList<String>();
            for (MeasureColumn column : columns) {
                items.add(column.sourceExpression + " AS " + column.name);
            }
            return items;
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
