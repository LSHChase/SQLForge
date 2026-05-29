package com.company.sqloptimization.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class CommonSubgraphRewriteResult {

    final String sql;
    final List<Map<String, Object>> rewriteAttempts;

    CommonSubgraphRewriteResult(String sql, List<Map<String, Object>> rewriteAttempts) {
        this.sql = sql == null ? "" : sql;
        this.rewriteAttempts = rewriteAttempts == null
            ? Collections.<Map<String, Object>>emptyList()
            : rewriteAttempts;
    }
}
