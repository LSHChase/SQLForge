package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggDimensionPlanner.dimensionSpecs;
import static com.company.sqloptimization.application.service.ParameterizedAggMeasurePlanner.measureColumns;
import static com.company.sqloptimization.application.service.ParameterizedAggPolicy.structuralBlockingReasons;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.reason;
import static com.company.sqloptimization.application.service.ParameterizedAggRewriteBuilder.rewriteSql;
import static com.company.sqloptimization.application.service.ParameterizedAggSqlBuilder.baseFromClause;
import static com.company.sqloptimization.application.service.ParameterizedAggSqlBuilder.retainedWherePredicates;
import static com.company.sqloptimization.application.service.ParameterizedAggSqlBuilder.selectSql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            grainMeasureDerivation
        );
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons);
        }
        List<ParameterizedAggDimensionSpec> dimensions = dimensionSpecs(
            grainMeasureDerivation.getDimensions(),
            mapList(advancedStructureProfile.get("groupBy"))
        );
        List<ParameterizedAggMeasureColumn> measureColumns = measureColumns(grainMeasureDerivation.getMeasures());
        String selectSql = selectSql(
            ddlSelectItems(dimensions, measureColumns),
            baseFromClause(mapList(advancedStructureProfile.get("tables"))),
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
        return renderCandidate(targetEngine, mvName, selectSql, rewriteSql, validationSql);
    }

    private static CandidateSql renderCandidate(String targetEngine,
                                                String mvName,
                                                String selectSql,
                                                String rewriteSql,
                                                L2MaterializedViewValidationSqlBuilder.ValidationSqlResult validationSql) {
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

    private static List<String> ddlSelectItems(List<ParameterizedAggDimensionSpec> dimensions,
                                               List<ParameterizedAggMeasureColumn> measureColumns) {
        List<String> items = new ArrayList<String>();
        for (ParameterizedAggDimensionSpec dimension : dimensions) {
            items.add(dimension.ddlSelectItem());
        }
        for (ParameterizedAggMeasureColumn measureColumn : measureColumns) {
            items.add(measureColumn.sourceExpression + " AS " + measureColumn.name);
        }
        return items;
    }

    static final class CandidateSql extends ParameterizedAggCandidateSql {

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql) {
            super(blockingReasons, ddlSql, refreshSql, validationSql, rollbackSql, rewriteSql);
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
                ParameterizedAggCandidateSql.emptyReasons(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql
            );
        }
    }
}
