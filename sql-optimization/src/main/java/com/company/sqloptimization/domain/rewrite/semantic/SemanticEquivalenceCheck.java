package com.company.sqloptimization.domain.rewrite.semantic;

import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.List;
import java.util.Map;

public class SemanticEquivalenceCheck {

    private final String checkId;
    private final String candidateId;
    private final RelationalRewriteRuleType ruleType;
    private final SemanticEquivalenceCheckType checkType;
    private final SemanticEquivalenceStatus status;
    private final String originalExpression;
    private final String rewrittenExpression;
    private final String differenceExpression;
    private final String reverseDifferenceExpression;
    private final List<String> preconditions;
    private final List<String> proofObligations;
    private final List<String> nullSemantics;
    private final List<String> bagSemantics;
    private final List<String> risks;
    private final List<Map<String, Object>> evidence;
    private final Map<String, Object> attributes;

    public SemanticEquivalenceCheck(String checkId,
                                    String candidateId,
                                    RelationalRewriteRuleType ruleType,
                                    SemanticEquivalenceCheckType checkType,
                                    SemanticEquivalenceStatus status,
                                    String originalExpression,
                                    String rewrittenExpression,
                                    String differenceExpression,
                                    String reverseDifferenceExpression,
                                    List<String> preconditions,
                                    List<String> proofObligations,
                                    List<String> nullSemantics,
                                    List<String> bagSemantics,
                                    List<String> risks,
                                    List<Map<String, Object>> evidence,
                                    Map<String, Object> attributes) {
        this.checkId = checkId;
        this.candidateId = candidateId;
        this.ruleType = ruleType;
        this.checkType = checkType;
        this.status = status;
        this.originalExpression = originalExpression;
        this.rewrittenExpression = rewrittenExpression;
        this.differenceExpression = differenceExpression;
        this.reverseDifferenceExpression = reverseDifferenceExpression;
        this.preconditions = SemanticVerificationCollections.immutableStrings(preconditions);
        this.proofObligations = SemanticVerificationCollections.immutableStrings(proofObligations);
        this.nullSemantics = SemanticVerificationCollections.immutableStrings(nullSemantics);
        this.bagSemantics = SemanticVerificationCollections.immutableStrings(bagSemantics);
        this.risks = SemanticVerificationCollections.immutableStrings(risks);
        this.evidence = SemanticVerificationCollections.immutableMaps(evidence);
        this.attributes = SemanticVerificationCollections.immutableMap(attributes);
    }

    public String getCheckId() {
        return checkId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public RelationalRewriteRuleType getRuleType() {
        return ruleType;
    }

    public SemanticEquivalenceCheckType getCheckType() {
        return checkType;
    }

    public SemanticEquivalenceStatus getStatus() {
        return status;
    }

    public String getOriginalExpression() {
        return originalExpression;
    }

    public String getRewrittenExpression() {
        return rewrittenExpression;
    }

    public String getDifferenceExpression() {
        return differenceExpression;
    }

    public String getReverseDifferenceExpression() {
        return reverseDifferenceExpression;
    }

    public List<String> getPreconditions() {
        return preconditions;
    }

    public List<String> getProofObligations() {
        return proofObligations;
    }

    public List<String> getNullSemantics() {
        return nullSemantics;
    }

    public List<String> getBagSemantics() {
        return bagSemantics;
    }

    public List<String> getRisks() {
        return risks;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
