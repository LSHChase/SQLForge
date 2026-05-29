package com.company.sqloptimization.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class CommonSubgraphCandidateSql {

    private final List<Map<String, Object>> blockingReasons;
    private final String ddlSql;
    private final String refreshSql;
    private final String validationSql;
    private final String rollbackSql;
    private final String rewriteSql;
    private final Map<String, Object> commonSubgraphEvidence;

    private CommonSubgraphCandidateSql(List<Map<String, Object>> blockingReasons,
                                       String ddlSql,
                                       String refreshSql,
                                       String validationSql,
                                       String rollbackSql,
                                       String rewriteSql,
                                       Map<String, Object> commonSubgraphEvidence) {
        this.blockingReasons = blockingReasons;
        this.ddlSql = ddlSql;
        this.refreshSql = refreshSql;
        this.validationSql = validationSql;
        this.rollbackSql = rollbackSql;
        this.rewriteSql = rewriteSql;
        this.commonSubgraphEvidence = commonSubgraphEvidence;
    }

    static CommonSubgraphCandidateSql blocked(List<Map<String, Object>> blockingReasons) {
        return new CommonSubgraphCandidateSql(
            blockingReasons == null ? Collections.<Map<String, Object>>emptyList() : blockingReasons,
            null,
            null,
            null,
            null,
            null,
            Collections.<String, Object>emptyMap()
        );
    }

    static CommonSubgraphCandidateSql generated(String ddlSql,
                                                String refreshSql,
                                                String validationSql,
                                                String rollbackSql,
                                                String rewriteSql,
                                                Map<String, Object> commonSubgraphEvidence) {
        return new CommonSubgraphCandidateSql(
            Collections.<Map<String, Object>>emptyList(),
            ddlSql,
            refreshSql,
            validationSql,
            rollbackSql,
            rewriteSql,
            commonSubgraphEvidence
        );
    }

    List<Map<String, Object>> getBlockingReasons() { return blockingReasons; }
    String getDdlSql() { return ddlSql; }
    String getRefreshSql() { return refreshSql; }
    String getValidationSql() { return validationSql; }
    String getRollbackSql() { return rollbackSql; }
    String getRewriteSql() { return rewriteSql; }
    Map<String, Object> getCommonSubgraphEvidence() { return commonSubgraphEvidence; }
}
