package com.company.sqloptimization.application.service;

import java.util.List;
import java.util.Map;

final class PrejoinJoinPlan {

    final List<Map<String, Object>> blockingReasons;
    final List<Map<String, Object>> joinKeys;
    final List<PrejoinJoinClause> joinClauses;

    PrejoinJoinPlan(List<Map<String, Object>> blockingReasons,
                    List<Map<String, Object>> joinKeys,
                    List<PrejoinJoinClause> joinClauses) {
        this.blockingReasons = blockingReasons;
        this.joinKeys = joinKeys;
        this.joinClauses = joinClauses;
    }
}
