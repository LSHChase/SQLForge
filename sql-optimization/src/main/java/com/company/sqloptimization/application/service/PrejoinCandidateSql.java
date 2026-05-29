package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinProfileValues.immutableMapList;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PrejoinCandidateSql {

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

    PrejoinCandidateSql(List<Map<String, Object>> blockingReasons,
                        String ddlSql,
                        String refreshSql,
                        String validationSql,
                        String rollbackSql,
                        String rewriteSql,
                        PrejoinJoinPlan joinPlan,
                        PrejoinColumnPlan columnPlan) {
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

    private static LinkedHashMap<String, Object> rowAmplificationRisk() {
        LinkedHashMap<String, Object> risk = new LinkedHashMap<String, Object>();
        risk.put("status", "REVIEW_REQUIRED");
        risk.put("generatedAllowed", Boolean.TRUE);
        risk.put("claimBoundary", "NO_ROW_AMPLIFICATION_PROOF_WITHOUT_METADATA");
        risk.put("missingEvidence", Arrays.asList("JOIN_KEY_UNIQUENESS", "TABLE_CARDINALITY", "JOIN_SELECTIVITY"));
        risk.put(
            "description",
            "当前仅证明 INNER 等值 Join 形态可预 Join；缺少唯一键/基数元数据时不能声明无行数放大。"
        );
        return risk;
    }
}
