package com.company.sqloptimization.application.service;

import java.util.Map;

final class StarAggJoinEdge {

    final int joinIndex;
    final String joinType;
    final String condition;
    final String leftKey;
    final String rightKey;
    final StarAggRelationSpec leftRelation;
    final StarAggRelationSpec rightRelation;
    final Map<String, Object> blockingReason;

    private StarAggJoinEdge(int joinIndex,
                            String joinType,
                            String condition,
                            String leftKey,
                            String rightKey,
                            StarAggRelationSpec leftRelation,
                            StarAggRelationSpec rightRelation,
                            Map<String, Object> blockingReason) {
        this.joinIndex = joinIndex;
        this.joinType = joinType;
        this.condition = condition;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.leftRelation = leftRelation;
        this.rightRelation = rightRelation;
        this.blockingReason = blockingReason;
    }

    static StarAggJoinEdge generated(int joinIndex,
                                     String joinType,
                                     String condition,
                                     String leftKey,
                                     String rightKey,
                                     StarAggRelationSpec leftRelation,
                                     StarAggRelationSpec rightRelation) {
        return new StarAggJoinEdge(joinIndex, joinType, condition, leftKey, rightKey, leftRelation, rightRelation, null);
    }

    static StarAggJoinEdge blocked(Map<String, Object> blockingReason) {
        return new StarAggJoinEdge(0, "", "", "", "", null, null, blockingReason);
    }

    boolean involves(String relationKey) {
        return leftRelation.key.equals(relationKey) || rightRelation.key.equals(relationKey);
    }

    String other(String relationKey) {
        return leftRelation.key.equals(relationKey) ? rightRelation.key : leftRelation.key;
    }
}
