package com.company.sqloptimization.domain.rewrite.rule;

import java.util.Map;

public class RewriteRuleCostImpact {

    private final String scanReduction;
    private final String memoryIncrease;
    private final String risk;
    private final Map<String, Object> attributes;

    public RewriteRuleCostImpact(String scanReduction,
                                 String memoryIncrease,
                                 String risk,
                                 Map<String, Object> attributes) {
        this.scanReduction = RuleEngineCollections.text(scanReduction);
        this.memoryIncrease = RuleEngineCollections.text(memoryIncrease);
        this.risk = RuleEngineCollections.text(risk);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public String getScanReduction() {
        return scanReduction;
    }

    public String getMemoryIncrease() {
        return memoryIncrease;
    }

    public String getRisk() {
        return risk;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
