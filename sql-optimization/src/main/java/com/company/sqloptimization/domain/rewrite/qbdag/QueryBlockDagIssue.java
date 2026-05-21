package com.company.sqloptimization.domain.rewrite.qbdag;

import java.util.List;

public class QueryBlockDagIssue {

    private final String code;
    private final String severity;
    private final String message;
    private final String recommendedAction;
    private final List<String> affectedBlockIds;

    public QueryBlockDagIssue(String code,
                              String severity,
                              String message,
                              String recommendedAction,
                              List<String> affectedBlockIds) {
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.recommendedAction = recommendedAction;
        this.affectedBlockIds = QbDagCollections.immutableStrings(affectedBlockIds);
    }

    public String getCode() {
        return code;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public List<String> getAffectedBlockIds() {
        return affectedBlockIds;
    }
}
