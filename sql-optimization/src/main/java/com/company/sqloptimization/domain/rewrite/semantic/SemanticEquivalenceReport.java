package com.company.sqloptimization.domain.rewrite.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SemanticEquivalenceReport {

    public static final String SCHEMA_VERSION = "semantic-equivalence-report/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final SemanticEquivalenceStatus status;
    private final List<SemanticEquivalenceCheck> checks;
    private final List<String> unverifiedCandidateIds;
    private final Map<String, Object> attributes;

    public SemanticEquivalenceReport(String schemaVersion,
                                     String sourceSchemaVersion,
                                     SemanticEquivalenceStatus status,
                                     List<SemanticEquivalenceCheck> checks,
                                     List<String> unverifiedCandidateIds,
                                     Map<String, Object> attributes) {
        this.schemaVersion = schemaVersion;
        this.sourceSchemaVersion = sourceSchemaVersion;
        this.status = status;
        this.checks = SemanticVerificationCollections.immutableList(checks);
        this.unverifiedCandidateIds = SemanticVerificationCollections.immutableStrings(unverifiedCandidateIds);
        this.attributes = SemanticVerificationCollections.immutableMap(attributes);
    }

    public boolean hasCheck(SemanticEquivalenceCheckType checkType) {
        for (SemanticEquivalenceCheck check : checks) {
            if (checkType == check.getCheckType()) {
                return true;
            }
        }
        return false;
    }

    public List<SemanticEquivalenceCheck> checksOf(SemanticEquivalenceCheckType checkType) {
        if (checkType == null) {
            return Collections.emptyList();
        }
        List<SemanticEquivalenceCheck> result = new ArrayList<SemanticEquivalenceCheck>();
        for (SemanticEquivalenceCheck check : checks) {
            if (checkType == check.getCheckType()) {
                result.add(check);
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

    public SemanticEquivalenceStatus getStatus() {
        return status;
    }

    public List<SemanticEquivalenceCheck> getChecks() {
        return checks;
    }

    public List<String> getUnverifiedCandidateIds() {
        return unverifiedCandidateIds;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
