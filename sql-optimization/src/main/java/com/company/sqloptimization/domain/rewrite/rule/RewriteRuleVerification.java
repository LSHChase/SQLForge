package com.company.sqloptimization.domain.rewrite.rule;

import java.util.Map;

public class RewriteRuleVerification {

    private final String method;
    private final String fallback;
    private final String nullSemantics;
    private final Map<String, Object> attributes;

    public RewriteRuleVerification(String method,
                                   String fallback,
                                   String nullSemantics,
                                   Map<String, Object> attributes) {
        this.method = RuleEngineCollections.text(method);
        this.fallback = RuleEngineCollections.text(fallback);
        this.nullSemantics = RuleEngineCollections.text(nullSemantics);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public String getMethod() {
        return method;
    }

    public String getFallback() {
        return fallback;
    }

    public String getNullSemantics() {
        return nullSemantics;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
