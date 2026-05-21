package com.company.sqloptimization.domain.rewrite.rule;

import java.util.Map;

public class RewriteRuleAction {

    private final String type;
    private final String strategy;
    private final String target;
    private final String location;
    private final String scope;
    private final Map<String, Object> attributes;

    public RewriteRuleAction(String type,
                             String strategy,
                             String target,
                             String location,
                             String scope,
                             Map<String, Object> attributes) {
        this.type = RuleEngineCollections.text(type);
        this.strategy = RuleEngineCollections.text(strategy);
        this.target = RuleEngineCollections.text(target);
        this.location = RuleEngineCollections.text(location);
        this.scope = RuleEngineCollections.text(scope);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public boolean isType(String expectedType) {
        return type.equals(RuleEngineCollections.text(expectedType));
    }

    public String getType() {
        return type;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getTarget() {
        return target;
    }

    public String getLocation() {
        return location;
    }

    public String getScope() {
        return scope;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
