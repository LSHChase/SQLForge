package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupProfileValues.immutableMap;
import static com.company.sqloptimization.application.service.RollupProfileValues.immutableMapList;

import java.util.List;
import java.util.Map;

class RollupCandidateSql {

    private final List<Map<String, Object>> blockingReasons;
    private final String ddlSql;
    private final String refreshSql;
    private final String validationSql;
    private final String rollbackSql;
    private final String rewriteSql;
    private final Map<String, Object> timeRollupEvidence;

    RollupCandidateSql(List<Map<String, Object>> blockingReasons,
                       String ddlSql,
                       String refreshSql,
                       String validationSql,
                       String rollbackSql,
                       String rewriteSql,
                       Map<String, Object> timeRollupEvidence) {
        this.blockingReasons = immutableMapList(blockingReasons);
        this.ddlSql = ddlSql;
        this.refreshSql = refreshSql;
        this.validationSql = validationSql;
        this.rollbackSql = rollbackSql;
        this.rewriteSql = rewriteSql;
        this.timeRollupEvidence = immutableMap(timeRollupEvidence);
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

    Map<String, Object> getTimeRollupEvidence() {
        return timeRollupEvidence;
    }
}
