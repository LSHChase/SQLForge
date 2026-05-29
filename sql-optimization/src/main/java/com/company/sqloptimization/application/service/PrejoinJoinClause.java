package com.company.sqloptimization.application.service;

import java.util.Map;

final class PrejoinJoinClause {

    final Map<String, Object> join;
    final String condition;
    final Map<String, Object> rightTable;

    PrejoinJoinClause(Map<String, Object> join, String condition, Map<String, Object> rightTable) {
        this.join = join;
        this.condition = condition;
        this.rightTable = rightTable;
    }
}
