package com.company.sqloptimization.domain.rewrite.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RewriteActivationEligibility {

    private final String rewriteRecordId;
    private final String tenantId;
    private final String policyId;
    private final boolean eligible;
    private final String reviewStatus;
    private final String validationStatus;
    private final String activationStatus;
    private final String alertStatus;
    private final Boolean autoApplyAllowed;
    private final String lastValidationRunId;
    private final List<RewriteActivationEligibilityReason> refusalReasons;

    public RewriteActivationEligibility(String rewriteRecordId,
                                     String tenantId,
                                     String policyId,
                                     boolean eligible,
                                     String reviewStatus,
                                     String validationStatus,
                                     String activationStatus,
                                     String alertStatus,
                                     Boolean autoApplyAllowed,
                                     String lastValidationRunId,
                                     List<RewriteActivationEligibilityReason> refusalReasons) {
        this.rewriteRecordId = rewriteRecordId;
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.eligible = eligible;
        this.reviewStatus = reviewStatus;
        this.validationStatus = validationStatus;
        this.activationStatus = activationStatus;
        this.alertStatus = alertStatus;
        this.autoApplyAllowed = autoApplyAllowed;
        this.lastValidationRunId = lastValidationRunId;
        this.refusalReasons = immutableReasons(refusalReasons);
    }

    private List<RewriteActivationEligibilityReason> immutableReasons(List<RewriteActivationEligibilityReason> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<RewriteActivationEligibilityReason>(reasons));
    }

    public String getRewriteRecordId() { return rewriteRecordId; }
    public String getTenantId() { return tenantId; }
    public String getPolicyId() { return policyId; }
    public boolean isEligible() { return eligible; }
    public String getReviewStatus() { return reviewStatus; }
    public String getValidationStatus() { return validationStatus; }
    public String getActivationStatus() { return activationStatus; }
    public String getAlertStatus() { return alertStatus; }
    public Boolean getAutoApplyAllowed() { return autoApplyAllowed; }
    public String getLastValidationRunId() { return lastValidationRunId; }
    public List<RewriteActivationEligibilityReason> getRefusalReasons() { return refusalReasons; }
}
