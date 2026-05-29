package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupDimensionPlanner.dimensionSpecs;
import static com.company.sqloptimization.application.service.RollupEvidenceBuilder.timeRollupEvidence;
import static com.company.sqloptimization.application.service.RollupMeasureColumnPlanner.measureColumns;
import static com.company.sqloptimization.application.service.RollupPlanBuilder.rollupPlan;
import static com.company.sqloptimization.application.service.RollupProfileValues.mapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.reason;
import static com.company.sqloptimization.application.service.RollupRewriteBuilder.rewriteSql;
import static com.company.sqloptimization.application.service.RollupSqlBuilder.baseFromClause;
import static com.company.sqloptimization.application.service.RollupSqlBuilder.retainedWherePredicates;
import static com.company.sqloptimization.application.service.RollupSqlBuilder.selectSql;
import static com.company.sqloptimization.application.service.RollupStructuralPolicy.structuralBlockingReasons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class L2RollupMvCandidateGenerator {

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

        List<RollupDimensionSpec> dimensions = dimensionSpecs(
            mapList(advancedStructureProfile.get("groupBy")),
            predicateClassification,
            rollupPlan
        );
        String fromClause = baseFromClause(mapList(advancedStructureProfile.get("tables")));
        List<RollupMeasureColumn> measureColumns = measureColumns(grainMeasureDerivation.getMeasures());
        List<String> ddlSelectItems = new ArrayList<String>();
        ddlSelectItems.add(rollupPlan.mvTimeExpression + " AS " + rollupPlan.mvTimeColumn);
        for (RollupDimensionSpec dimension : dimensions) {
            ddlSelectItems.add(dimension.ddlSelectItem());
        }
        for (RollupMeasureColumn measureColumn : measureColumns) {
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

    static final class CandidateSql extends RollupCandidateSql {
        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             Map<String, Object> timeRollupEvidence) {
            super(blockingReasons, ddlSql, refreshSql, validationSql, rollbackSql, rewriteSql, timeRollupEvidence);
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
    }
}
