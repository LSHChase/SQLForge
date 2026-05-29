package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.immutableMapList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class ParameterizedAggCandidateSql {

    private final List<Map<String, Object>> blockingReasons;
    private final String ddlSql;
    private final String refreshSql;
    private final String validationSql;
    private final String rollbackSql;
    private final String rewriteSql;

    ParameterizedAggCandidateSql(List<Map<String, Object>> blockingReasons,
                                 String ddlSql,
                                 String refreshSql,
                                 String validationSql,
                                 String rollbackSql,
                                 String rewriteSql) {
        this.blockingReasons = immutableMapList(blockingReasons);
        this.ddlSql = ddlSql;
        this.refreshSql = refreshSql;
        this.validationSql = validationSql;
        this.rollbackSql = rollbackSql;
        this.rewriteSql = rewriteSql;
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

    static List<Map<String, Object>> emptyReasons() {
        return Collections.<Map<String, Object>>emptyList();
    }
}
