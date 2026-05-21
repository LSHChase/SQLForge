package com.company.sqloptimization.domain.rewrite.rule;

import java.util.Map;

public class RewriteRulePattern {

    private final String type;
    private final String relation;
    private final int minCount;
    private final String context;
    private final Map<String, Object> attributes;

    public RewriteRulePattern(String type,
                              String relation,
                              int minCount,
                              String context,
                              Map<String, Object> attributes) {
        this.type = RuleEngineCollections.text(type);
        this.relation = RuleEngineCollections.text(relation);
        this.minCount = minCount;
        this.context = RuleEngineCollections.text(context);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public String getType() {
        return type;
    }

    public String getRelation() {
        return relation;
    }

    public int getMinCount() {
        return minCount;
    }

    public String getContext() {
        return context;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
