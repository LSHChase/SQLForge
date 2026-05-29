package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggDimensionPlanner.dimensionPlan;
import static com.company.sqloptimization.application.service.StarAggFactPlanner.factPlan;
import static com.company.sqloptimization.application.service.StarAggJoinPlanner.joinPlan;
import static com.company.sqloptimization.application.service.StarAggMeasurePlanner.measurePlan;
import static com.company.sqloptimization.application.service.StarAggProfileValues.baseTables;
import static com.company.sqloptimization.application.service.StarAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.reason;
import static com.company.sqloptimization.application.service.StarAggRewriteBuilder.rewriteSql;
import static com.company.sqloptimization.application.service.StarAggSqlBuilder.fromClause;
import static com.company.sqloptimization.application.service.StarAggSqlBuilder.retainedWherePredicates;
import static com.company.sqloptimization.application.service.StarAggSqlBuilder.selectSql;
import static com.company.sqloptimization.application.service.StarAggStructuralPolicy.structuralBlockingReasons;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class L2StarAggMvCandidateGenerator {

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
        StarAggRelationCatalog relationCatalog = StarAggRelationCatalog.from(baseTables);
        StarAggJoinPlan joinPlan = joinPlan(
            mapList(advancedStructureProfile == null ? null : advancedStructureProfile.get("joinGraph")),
            relationCatalog
        );
        blockingReasons.addAll(joinPlan.blockingReasons);

        StarAggFactPlan factPlan = StarAggFactPlan.blocked();
        StarAggDimensionPlan dimensionPlan = StarAggDimensionPlan.blocked();
        StarAggMeasurePlan measurePlan = StarAggMeasurePlan.blocked();
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
        return renderCandidate(
            sourceSql,
            mvName,
            targetEngine,
            advancedStructureProfile,
            predicateClassification,
            grainMeasureDerivation,
            baseTables,
            factPlan,
            joinPlan,
            dimensionPlan,
            measurePlan
        );
    }

    private static CandidateSql renderCandidate(String sourceSql,
                                                String mvName,
                                                String targetEngine,
                                                Map<String, Object> advancedStructureProfile,
                                                L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                                L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
                                                List<Map<String, Object>> baseTables,
                                                StarAggFactPlan factPlan,
                                                StarAggJoinPlan joinPlan,
                                                StarAggDimensionPlan dimensionPlan,
                                                StarAggMeasurePlan measurePlan) {
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
        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult validationSql =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_STAR_AGG,
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
            return CandidateSql.blocked(validationSql.getBlockingReasons(), factPlan, joinPlan, dimensionPlan, measurePlan);
        }
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
            validationSql.getValidationSql(),
            renderedSql.getRollbackSql(),
            rewriteSql,
            factPlan,
            joinPlan,
            dimensionPlan,
            measurePlan
        );
    }

    static final class CandidateSql extends StarAggCandidateSql {
        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             StarAggFactPlan factPlan,
                             StarAggJoinPlan joinPlan,
                             StarAggDimensionPlan dimensionPlan,
                             StarAggMeasurePlan measurePlan) {
            super(blockingReasons, ddlSql, refreshSql, validationSql, rollbackSql, rewriteSql,
                factPlan, joinPlan, dimensionPlan, measurePlan);
        }

        private static CandidateSql blocked(List<Map<String, Object>> blockingReasons,
                                            StarAggFactPlan factPlan,
                                            StarAggJoinPlan joinPlan,
                                            StarAggDimensionPlan dimensionPlan,
                                            StarAggMeasurePlan measurePlan) {
            return new CandidateSql(blockingReasons, null, null, null, null, null, factPlan, joinPlan, dimensionPlan, measurePlan);
        }

        private static CandidateSql generated(String ddlSql,
                                              String refreshSql,
                                              String validationSql,
                                              String rollbackSql,
                                              String rewriteSql,
                                              StarAggFactPlan factPlan,
                                              StarAggJoinPlan joinPlan,
                                              StarAggDimensionPlan dimensionPlan,
                                              StarAggMeasurePlan measurePlan) {
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
    }
}
