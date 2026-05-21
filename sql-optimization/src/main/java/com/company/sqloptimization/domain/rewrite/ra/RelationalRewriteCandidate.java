package com.company.sqloptimization.domain.rewrite.ra;

import java.util.List;
import java.util.Map;

public class RelationalRewriteCandidate {

    private final String candidateId;
    private final RelationalRewriteRuleType ruleType;
    private final String titleZh;
    private final String primaryBlockId;
    private final List<String> sourceBlockIds;
    private final String replacementForm;
    private final List<String> compensationPredicates;
    private final List<String> preconditions;
    private final List<String> semanticRisks;
    private final List<Map<String, Object>> evidence;
    private final Map<String, Object> estimatedBenefit;
    private final boolean autoApplyAllowed;
    private final boolean manualReviewRequired;
    private final Map<String, Object> attributes;

    public RelationalRewriteCandidate(String candidateId,
                                      RelationalRewriteRuleType ruleType,
                                      String primaryBlockId,
                                      List<String> sourceBlockIds,
                                      String replacementForm,
                                      List<String> compensationPredicates,
                                      List<String> preconditions,
                                      List<String> semanticRisks,
                                      List<Map<String, Object>> evidence,
                                      Map<String, Object> estimatedBenefit,
                                      boolean autoApplyAllowed,
                                      boolean manualReviewRequired,
                                      Map<String, Object> attributes) {
        this.candidateId = candidateId;
        this.ruleType = ruleType;
        this.titleZh = ruleType == null ? "" : ruleType.getTitleZh();
        this.primaryBlockId = primaryBlockId;
        this.sourceBlockIds = RaRewriteCollections.immutableStrings(sourceBlockIds);
        this.replacementForm = replacementForm;
        this.compensationPredicates = RaRewriteCollections.immutableStrings(compensationPredicates);
        this.preconditions = RaRewriteCollections.immutableStrings(preconditions);
        this.semanticRisks = RaRewriteCollections.immutableStrings(semanticRisks);
        this.evidence = RaRewriteCollections.immutableMaps(evidence);
        this.estimatedBenefit = RaRewriteCollections.immutableMap(estimatedBenefit);
        this.autoApplyAllowed = autoApplyAllowed;
        this.manualReviewRequired = manualReviewRequired;
        this.attributes = RaRewriteCollections.immutableMap(attributes);
    }

    public String getCandidateId() {
        return candidateId;
    }

    public RelationalRewriteRuleType getRuleType() {
        return ruleType;
    }

    public String getTitleZh() {
        return titleZh;
    }

    public String getPrimaryBlockId() {
        return primaryBlockId;
    }

    public List<String> getSourceBlockIds() {
        return sourceBlockIds;
    }

    public String getReplacementForm() {
        return replacementForm;
    }

    public List<String> getCompensationPredicates() {
        return compensationPredicates;
    }

    public List<String> getPreconditions() {
        return preconditions;
    }

    public List<String> getSemanticRisks() {
        return semanticRisks;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getEstimatedBenefit() {
        return estimatedBenefit;
    }

    public boolean isAutoApplyAllowed() {
        return autoApplyAllowed;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
