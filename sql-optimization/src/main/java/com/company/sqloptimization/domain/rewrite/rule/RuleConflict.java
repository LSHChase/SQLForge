package com.company.sqloptimization.domain.rewrite.rule;

import java.util.List;
import java.util.Map;

public class RuleConflict {

    private final String conflictId;
    private final String conflictType;
    private final List<String> ruleIds;
    private final String signal;
    private final String resolutionPolicy;
    private final Map<String, Object> evidence;

    public RuleConflict(String conflictId,
                        String conflictType,
                        List<String> ruleIds,
                        String signal,
                        String resolutionPolicy,
                        Map<String, Object> evidence) {
        this.conflictId = RuleEngineCollections.text(conflictId);
        this.conflictType = RuleEngineCollections.text(conflictType);
        this.ruleIds = RuleEngineCollections.immutableStrings(ruleIds);
        this.signal = RuleEngineCollections.text(signal);
        this.resolutionPolicy = RuleEngineCollections.text(resolutionPolicy);
        this.evidence = RuleEngineCollections.immutableMap(evidence);
    }

    public String getConflictId() {
        return conflictId;
    }

    public String getConflictType() {
        return conflictType;
    }

    public List<String> getRuleIds() {
        return ruleIds;
    }

    public String getSignal() {
        return signal;
    }

    public String getResolutionPolicy() {
        return resolutionPolicy;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }
}
