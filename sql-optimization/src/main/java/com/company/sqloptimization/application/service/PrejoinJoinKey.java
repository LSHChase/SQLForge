package com.company.sqloptimization.application.service;

import java.util.Map;

final class PrejoinJoinKey {

    final String leftKey;
    final String rightKey;
    final Map<String, Object> blockingReason;

    private PrejoinJoinKey(String leftKey, String rightKey, Map<String, Object> blockingReason) {
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.blockingReason = blockingReason;
    }

    static PrejoinJoinKey generated(String leftKey, String rightKey) {
        return new PrejoinJoinKey(leftKey, rightKey, null);
    }

    static PrejoinJoinKey blocked(Map<String, Object> blockingReason) {
        return new PrejoinJoinKey("", "", blockingReason);
    }
}
