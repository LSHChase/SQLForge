package com.company.sqloptimization.domain.rewrite.rule;

import java.util.List;
import java.util.Map;

public class RewriteRulePrecondition {

    private final String check;
    private final List<String> params;
    private final Map<String, Object> attributes;

    public RewriteRulePrecondition(String check, List<String> params, Map<String, Object> attributes) {
        this.check = RuleEngineCollections.text(check);
        this.params = RuleEngineCollections.immutableStrings(params);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public String getCheck() {
        return check;
    }

    public List<String> getParams() {
        return params;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
