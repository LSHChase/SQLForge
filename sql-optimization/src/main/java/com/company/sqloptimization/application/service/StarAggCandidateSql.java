package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggCandidateEvidence.starSchemaEvidence;
import static com.company.sqloptimization.application.service.StarAggProfileValues.immutableMapList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class StarAggCandidateSql {

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

    StarAggCandidateSql(List<Map<String, Object>> blockingReasons,
                        String ddlSql,
                        String refreshSql,
                        String validationSql,
                        String rollbackSql,
                        String rewriteSql,
                        StarAggFactPlan factPlan,
                        StarAggJoinPlan joinPlan,
                        StarAggDimensionPlan dimensionPlan,
                        StarAggMeasurePlan measurePlan) {
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
