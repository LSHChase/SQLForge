package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.reason;
import static com.company.sqloptimization.application.service.StarAggProfileValues.stringList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.containsSameExpression;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.dimensionNameCounts;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.outputName;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.sameExpression;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.unqualifiedName;
import static com.company.sqloptimization.application.service.StarAggSecurityCoverage.securityPredicatesCovered;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.leftPredicateField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class StarAggDimensionPlanner {

    private static final Set<String> TIME_TOKENS =
        new LinkedHashSet<String>(Arrays.asList("DT", "DATE", "TIME", "DAY", "MONTH", "YEAR"));
    private static final Set<String> TIME_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("DATE_TRUNC", "TRUNC", "DATE_FORMAT", "YEAR", "MONTH", "DAY"));

    private StarAggDimensionPlanner() {
    }

    static StarAggDimensionPlan dimensionPlan(Map<String, Object> advancedStructureProfile,
                                              L2PredicateClassifier.PredicateClassificationResult classification,
                                              L2GrainMeasureDeriver.DerivationResult derivation,
                                              StarAggRelationCatalog relationCatalog,
                                              StarAggFactPlan factPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        List<StarAggDimensionSpec> dimensions = dimensionSpecs(
            derivation.getDimensions(),
            StarAggProfileValues.mapList(advancedStructureProfile.get("groupBy")),
            classification,
            relationCatalog,
            factPlan,
            reasons
        );
        if (dimensions.isEmpty()) {
            reasons.add(reason("STAR_AGG_GRAIN_NOT_COVERED", "缺少可物化的时间字段或维表属性粒度，不能生成 STAR_AGG_MV。"));
        }
        if (!securityPredicatesCovered(classification, dimensions)) {
            reasons.add(reason("STAR_AGG_SECURITY_PREDICATE_NOT_COVERED", "安全谓词字段未保留为 MV 维度，不能生成可激活 rewrite。"));
        }
        return reasons.isEmpty()
            ? new StarAggDimensionPlan(Collections.<Map<String, Object>>emptyList(), dimensions)
            : StarAggDimensionPlan.blocked(reasons);
    }

    private static List<StarAggDimensionSpec> dimensionSpecs(
        List<String> dimensions,
        List<Map<String, Object>> groupBy,
        L2PredicateClassifier.PredicateClassificationResult classification,
        StarAggRelationCatalog relationCatalog,
        StarAggFactPlan factPlan,
        List<Map<String, Object>> reasons
    ) {
        List<StarAggDimensionSpec> result = new ArrayList<StarAggDimensionSpec>();
        LinkedHashSet<String> usedNames = new LinkedHashSet<String>();
        LinkedHashMap<String, Integer> unqualifiedCounts = dimensionNameCounts(dimensions);
        for (String dimension : dimensions) {
            if (!StringUtils.hasText(dimension)) {
                continue;
            }
            StarAggDimensionContext context = dimensionContext(dimension, groupBy, classification);
            StarAggRelationSpec relation = relationForDimension(context.sourceExpression, context.references, relationCatalog);
            boolean timeDimension = isTimeDimension(context.sourceExpression, context.references);
            if (!dimensionAllowed(context, relation, factPlan, timeDimension, reasons)) {
                continue;
            }
            result.add(new StarAggDimensionSpec(
                context.sourceExpression,
                outputName(context.sourceExpression, relation, unqualifiedCounts, usedNames),
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

    private static boolean dimensionAllowed(StarAggDimensionContext context,
                                            StarAggRelationSpec relation,
                                            StarAggFactPlan factPlan,
                                            boolean timeDimension,
                                            List<Map<String, Object>> reasons) {
        if (relation == null) {
            reasons.add(reason(
                context.security ? "STAR_AGG_SECURITY_PREDICATE_NOT_COVERED" : "STAR_AGG_DIMENSION_SOURCE_UNRESOLVED",
                "维度字段缺少表限定或无法解析来源，不能证明 STAR_AGG_MV 粒度覆盖。"
            ));
            return false;
        }
        if (context.groupByDimension && factPlan.factKey.equals(relation.key) && !timeDimension) {
            reasons.add(reason("STAR_AGG_GRAIN_NOT_COVERED", "STAR_AGG_MV 的 GROUP BY 只允许事实表时间字段或维表属性。"));
            return false;
        }
        if (context.groupByDimension
            && !factPlan.factKey.equals(relation.key)
            && !factPlan.dimensionKeys().contains(relation.key)) {
            reasons.add(reason("STAR_AGG_GRAIN_NOT_COVERED", "GROUP BY 字段来源不属于事实表时间字段或维表属性。"));
            return false;
        }
        return true;
    }

    private static StarAggDimensionContext dimensionContext(String dimension,
                                                           List<Map<String, Object>> groupBy,
                                                           L2PredicateClassifier.PredicateClassificationResult classification) {
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
        StarAggPredicateFlags flags = predicateFlags(dimension, classification, references);
        return new StarAggDimensionContext(sourceExpression, references, groupByDimension, flags.externalized, flags.security);
    }

    private static StarAggPredicateFlags predicateFlags(String dimension,
                                                       L2PredicateClassifier.PredicateClassificationResult classification,
                                                       Set<String> references) {
        StarAggPredicateFlags flags = new StarAggPredicateFlags();
        if (classification != null) {
            applyPredicateFlags(flags, dimension, classification.getExternalizedPredicates(), references, true);
            applyPredicateFlags(flags, dimension, classification.getSecurityPredicates(), references, false);
        }
        return flags;
    }

    private static void applyPredicateFlags(StarAggPredicateFlags flags,
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
                flags.externalized = flags.externalized || externalized;
                flags.security = flags.security || !externalized;
            }
        }
    }

    private static StarAggRelationSpec relationForDimension(String sourceExpression,
                                                           Set<String> references,
                                                           StarAggRelationCatalog relationCatalog) {
        if (isIdentifierReference(sourceExpression)) {
            return relationCatalog.relationForColumn(sourceExpression);
        }
        for (String reference : references) {
            if (isIdentifierReference(reference)) {
                StarAggRelationSpec relation = relationCatalog.relationForColumn(reference);
                if (relation != null) {
                    return relation;
                }
            }
        }
        return null;
    }

    private static String dimensionRole(StarAggRelationSpec relation,
                                        StarAggFactPlan factPlan,
                                        boolean timeDimension,
                                        StarAggDimensionContext context) {
        if (factPlan.factKey.equals(relation.key)) {
            return timeDimension ? "FACT_TIME_FIELD" : (context.security ? "FACT_SECURITY_FIELD" : "FACT_PARAMETER_FIELD");
        }
        return "DIMENSION_ATTRIBUTE";
    }

    private static boolean isTimeDimension(String expression, Set<String> references) {
        String upper = StarAggProfileValues.upperText(expression);
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
}
