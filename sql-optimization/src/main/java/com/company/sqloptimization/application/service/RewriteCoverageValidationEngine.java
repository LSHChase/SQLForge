package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageFields.coverageFields;
import static com.company.sqloptimization.application.service.RewriteCoveragePredicates.coversPredicates;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.mapList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.reason;
import static com.company.sqloptimization.application.service.RewriteCoverageProjection.coversCommonSubgraphProjection;
import static com.company.sqloptimization.application.service.RewriteCoverageProjection.coversGrouping;
import static com.company.sqloptimization.application.service.RewriteCoverageProjection.coversProjection;
import static com.company.sqloptimization.application.service.RewriteCoverageProjection.rootCountProjectionPreservedByMvRewrite;
import static com.company.sqloptimization.application.service.RewriteCoverageRelations.accessedOriginalSources;
import static com.company.sqloptimization.application.service.RewriteCoverageRelations.originalBaseSources;
import static com.company.sqloptimization.application.service.RewriteCoverageRelations.referencesMv;
import static com.company.sqloptimization.application.service.RewriteCoverageSelect.selectedOutputs;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlSafety.isReadonly;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.normalizeSingleStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RewriteCoverageValidationEngine {

    private RewriteCoverageValidationEngine() {
    }

    static L2MaterializedViewRewriteCoverageValidator.ValidationResult validate(
        L2MaterializedViewRewriteCoverageValidator.ValidationInput input) {
        L2MaterializedViewRewriteCoverageValidator.ValidationInput safeInput =
            input == null ? L2MaterializedViewRewriteCoverageValidator.ValidationInput.empty() : input;
        String normalizedRewrite = normalizeSingleStatement(safeInput.rewriteSql);
        Set<String> mvFields = coverageFields(safeInput.mvFieldNames, Collections.<String>emptyList());
        Set<String> coverageFields = coverageFields(safeInput);
        Set<String> selectedOutputs = selectedOutputs(normalizedRewrite);

        boolean readonly = isReadonly(normalizedRewrite);
        boolean referencesMv = referencesMv(normalizedRewrite, safeInput.mvName);
        Set<String> originalSources = originalBaseSources(safeInput.advancedStructureProfile);
        List<String> accessedOriginalSources = accessedOriginalSources(
            normalizedRewrite, safeInput.mvName, originalSources
        );
        boolean avoidsOriginalSources = accessedOriginalSources.isEmpty();

        boolean commonSubgraphMv = L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(safeInput.mvType);
        boolean coversProjection = projectionCovered(
            safeInput, commonSubgraphMv, mvFields, coverageFields, selectedOutputs, normalizedRewrite
        );
        boolean coversFilters = filtersCovered(safeInput, coverageFields);
        boolean coversGrouping = commonSubgraphMv
            ? coversProjection
            : coversGrouping(mapList(safeInput.advancedStructureProfile.get("groupBy")), coverageFields);
        boolean coversMeasures = commonSubgraphMv
            ? coversProjection
            : RewriteCoverageMeasures.coversMeasures(
                mapList(safeInput.advancedStructureProfile.get("aggregations")),
                safeInput.measures,
                coverageFields
            );
        if (rootCountProjectionPreservedByMvRewrite(safeInput.advancedStructureProfile, normalizedRewrite)) {
            coversProjection = true;
            coversMeasures = true;
        }
        boolean coversSecurity = coversPredicates(securityPredicates(safeInput), coverageFields);

        LinkedHashMap<String, Object> coverage = coverage(
            coversProjection, coversFilters, coversGrouping, coversMeasures, coversSecurity,
            readonly, referencesMv, avoidsOriginalSources
        );
        List<Map<String, Object>> reasons = blockingReasons(
            coversProjection, coversFilters, coversGrouping, coversMeasures, coversSecurity,
            readonly, referencesMv, avoidsOriginalSources, accessedOriginalSources
        );
        return new L2MaterializedViewRewriteCoverageValidator.ValidationResult(
            reasons.isEmpty() && StringUtils.hasText(normalizedRewrite) ? normalizedRewrite + ";" : null,
            coverage,
            reasons
        );
    }

    private static boolean projectionCovered(
        L2MaterializedViewRewriteCoverageValidator.ValidationInput input,
        boolean commonSubgraphMv,
        Set<String> mvFields,
        Set<String> coverageFields,
        Set<String> selectedOutputs,
        String normalizedRewrite) {
        return commonSubgraphMv
            ? coversCommonSubgraphProjection(input.additionalCoverageReferences, mvFields, normalizedRewrite)
            : coversProjection(
                mapList(input.advancedStructureProfile.get("projections")),
                input.measures,
                coverageFields,
                selectedOutputs,
                normalizedRewrite
            );
    }

    private static boolean filtersCovered(L2MaterializedViewRewriteCoverageValidator.ValidationInput input,
                                          Set<String> coverageFields) {
        return coversPredicates(externalizedPredicates(input), coverageFields)
            && (input.predicateClassification == null || !input.predicateClassification.hasBlockedPredicates());
    }

    private static List<Map<String, Object>> externalizedPredicates(
        L2MaterializedViewRewriteCoverageValidator.ValidationInput input) {
        return input.predicateClassification == null
            ? Collections.<Map<String, Object>>emptyList()
            : input.predicateClassification.getExternalizedPredicates();
    }

    private static List<Map<String, Object>> securityPredicates(
        L2MaterializedViewRewriteCoverageValidator.ValidationInput input) {
        return input.predicateClassification == null
            ? Collections.<Map<String, Object>>emptyList()
            : input.predicateClassification.getSecurityPredicates();
    }

    private static LinkedHashMap<String, Object> coverage(boolean coversProjection,
                                                          boolean coversFilters,
                                                          boolean coversGrouping,
                                                          boolean coversMeasures,
                                                          boolean coversSecurity,
                                                          boolean readonly,
                                                          boolean referencesMv,
                                                          boolean avoidsOriginalSources) {
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("coversProjection", Boolean.valueOf(coversProjection));
        coverage.put("coversFilters", Boolean.valueOf(coversFilters));
        coverage.put("coversGrouping", Boolean.valueOf(coversGrouping));
        coverage.put("coversMeasures", Boolean.valueOf(coversMeasures));
        coverage.put("coversSecurity", Boolean.valueOf(coversSecurity));
        coverage.put("rewriteSqlReadonly", Boolean.valueOf(readonly));
        coverage.put("rewriteSqlReferencesMv", Boolean.valueOf(referencesMv));
        coverage.put("rewriteSqlAvoidsOriginalSources", Boolean.valueOf(avoidsOriginalSources));
        return coverage;
    }

    private static List<Map<String, Object>> blockingReasons(boolean coversProjection,
                                                             boolean coversFilters,
                                                             boolean coversGrouping,
                                                             boolean coversMeasures,
                                                             boolean coversSecurity,
                                                             boolean readonly,
                                                             boolean referencesMv,
                                                             boolean avoidsOriginalSources,
                                                             List<String> accessedOriginalSources) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        addSqlReasons(reasons, readonly, referencesMv, avoidsOriginalSources, accessedOriginalSources);
        addCoverageReasons(reasons, coversProjection, coversFilters, coversGrouping, coversMeasures, coversSecurity);
        return reasons;
    }

    private static void addSqlReasons(List<Map<String, Object>> reasons,
                                      boolean readonly,
                                      boolean referencesMv,
                                      boolean avoidsOriginalSources,
                                      List<String> accessedOriginalSources) {
        if (!readonly) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_NOT_READONLY,
                "rewriteSql 必须是单条只读 SELECT/WITH 查询，不能包含 DDL、DML、CALL 或多语句。"));
        }
        if (!referencesMv) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_MV_REFERENCE_REQUIRED,
                "rewriteSql 必须在 FROM/JOIN 中引用生成的物化视图名称。"));
        }
        if (!avoidsOriginalSources) {
            Map<String, Object> reason = reason(
                L2MaterializedViewRewriteCoverageValidator.REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE,
                "rewriteSql 不能继续从原始基表读取数据。"
            );
            reason.put("originalSources", accessedOriginalSources);
            reasons.add(reason);
        }
    }

    private static void addCoverageReasons(List<Map<String, Object>> reasons,
                                           boolean coversProjection,
                                           boolean coversFilters,
                                           boolean coversGrouping,
                                           boolean coversMeasures,
                                           boolean coversSecurity) {
        if (!coversProjection) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_PROJECTION_NOT_COVERED,
                "rewriteSql 的投影无法由 MV 字段或可重算指标完整覆盖。"));
        }
        if (!coversFilters) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_FILTER_NOT_COVERED,
                "rewriteSql 的参数化过滤字段未被 MV 字段完整覆盖。"));
        }
        if (!coversGrouping) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_GROUPING_NOT_COVERED,
                "rewriteSql 的 GROUP BY 粒度无法由 MV 字段或上卷表达式完整覆盖。"));
        }
        if (!coversMeasures) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_MEASURE_NOT_COVERED,
                "rewriteSql 的指标无法由 MV 指标列或公共子图输出字段安全重算。"));
        }
        if (!coversSecurity) {
            reasons.add(reason(L2MaterializedViewRewriteCoverageValidator.REWRITE_SECURITY_PREDICATE_NOT_COVERED,
                "rewriteSql 的安全谓词字段未被 MV 字段完整覆盖。"));
        }
    }
}
