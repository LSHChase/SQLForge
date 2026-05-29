package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinColumnPlanner.columnPlan;
import static com.company.sqloptimization.application.service.PrejoinJoinPlanner.joinPlan;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.baseTables;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.mapList;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.reason;
import static com.company.sqloptimization.application.service.PrejoinRewriteBuilder.rewriteSql;
import static com.company.sqloptimization.application.service.PrejoinSqlBuilder.fromClause;
import static com.company.sqloptimization.application.service.PrejoinSqlBuilder.retainedWherePredicates;
import static com.company.sqloptimization.application.service.PrejoinSqlBuilder.selectSql;
import static com.company.sqloptimization.application.service.PrejoinStructuralPolicy.structuralBlockingReasons;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        PrejoinJoinPlan joinPlan = joinPlan(
            mapList(advancedStructureProfile == null ? null : advancedStructureProfile.get("joinGraph")),
            baseTables,
            sourceSql
        );
        blockingReasons.addAll(joinPlan.blockingReasons);

        PrejoinColumnPlan columnPlan = PrejoinColumnPlan.blocked(Collections.<Map<String, Object>>emptyList());
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

    static final class CandidateSql extends PrejoinCandidateSql {
        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             PrejoinJoinPlan joinPlan,
                             PrejoinColumnPlan columnPlan) {
            super(blockingReasons, ddlSql, refreshSql, validationSql, rollbackSql, rewriteSql, joinPlan, columnPlan);
        }

        private static CandidateSql blocked(List<Map<String, Object>> blockingReasons,
                                            PrejoinJoinPlan joinPlan,
                                            PrejoinColumnPlan columnPlan) {
            return new CandidateSql(blockingReasons, null, null, null, null, null, joinPlan, columnPlan);
        }

        private static CandidateSql generated(String ddlSql,
                                              String refreshSql,
                                              String validationSql,
                                              String rollbackSql,
                                              String rewriteSql,
                                              PrejoinJoinPlan joinPlan,
                                              PrejoinColumnPlan columnPlan) {
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
    }
}
