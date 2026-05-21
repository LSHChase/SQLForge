package com.company.sqloptimization.domain.rewrite.ra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RelationalRewritePlan {

    public static final String SCHEMA_VERSION = "relational-rewrite-plan/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final List<RelationalRewriteCandidate> candidates;
    private final List<String> unappliedRules;
    private final Map<String, Object> attributes;

    public RelationalRewritePlan(String schemaVersion,
                                 String sourceSchemaVersion,
                                 List<RelationalRewriteCandidate> candidates,
                                 List<String> unappliedRules,
                                 Map<String, Object> attributes) {
        this.schemaVersion = schemaVersion;
        this.sourceSchemaVersion = sourceSchemaVersion;
        this.candidates = RaRewriteCollections.immutableList(candidates);
        this.unappliedRules = RaRewriteCollections.immutableStrings(unappliedRules);
        this.attributes = RaRewriteCollections.immutableMap(attributes);
    }

    public boolean hasCandidate(RelationalRewriteRuleType ruleType) {
        for (RelationalRewriteCandidate candidate : candidates) {
            if (ruleType == candidate.getRuleType()) {
                return true;
            }
        }
        return false;
    }

    public List<RelationalRewriteCandidate> candidatesOf(RelationalRewriteRuleType ruleType) {
        if (ruleType == null) {
            return Collections.emptyList();
        }
        List<RelationalRewriteCandidate> result = new ArrayList<RelationalRewriteCandidate>();
        for (RelationalRewriteCandidate candidate : candidates) {
            if (ruleType == candidate.getRuleType()) {
                result.add(candidate);
            }
        }
        return result;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSourceSchemaVersion() {
        return sourceSchemaVersion;
    }

    public List<RelationalRewriteCandidate> getCandidates() {
        return candidates;
    }

    public List<String> getUnappliedRules() {
        return unappliedRules;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
