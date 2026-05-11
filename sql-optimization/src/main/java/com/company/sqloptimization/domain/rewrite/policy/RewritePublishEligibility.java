package com.company.sqloptimization.domain.rewrite.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RewritePublishEligibility {

    private final String rewriteRecordId;
    private final String tenantId;
    private final String policyId;
    private final boolean eligible;
    private final String reviewStatus;
    private final String validationStatus;
    private final String publishStatus;
    private final String alertStatus;
    private final Boolean autoApplyAllowed;
    private final String lastValidationRunId;
    private final List<RewritePublishEligibilityReason> refusalReasons;

    public RewritePublishEligibility(String rewriteRecordId,
                                     String tenantId,
                                     String policyId,
                                     boolean eligible,
                                     String reviewStatus,
                                     String validationStatus,
                                     String publishStatus,
                                     String alertStatus,
                                     Boolean autoApplyAllowed,
                                     String lastValidationRunId,
                                     List<RewritePublishEligibilityReason> refusalReasons) {
        this.rewriteRecordId = rewriteRecordId;
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.eligible = eligible;
        this.reviewStatus = reviewStatus;
        this.validationStatus = validationStatus;
        this.publishStatus = publishStatus;
        this.alertStatus = alertStatus;
        this.autoApplyAllowed = autoApplyAllowed;
        this.lastValidationRunId = lastValidationRunId;
        this.refusalReasons = immutableReasons(refusalReasons);
    }

    private List<RewritePublishEligibilityReason> immutableReasons(List<RewritePublishEligibilityReason> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<RewritePublishEligibilityReason>(reasons));
    }

    public String getRewriteRecordId() { return rewriteRecordId; }
    public String getTenantId() { return tenantId; }
    public String getPolicyId() { return policyId; }
    public boolean isEligible() { return eligible; }
    public String getReviewStatus() { return reviewStatus; }
    public String getValidationStatus() { return validationStatus; }
    public String getPublishStatus() { return publishStatus; }
    public String getAlertStatus() { return alertStatus; }
    public Boolean getAutoApplyAllowed() { return autoApplyAllowed; }
    public String getLastValidationRunId() { return lastValidationRunId; }
    public List<RewritePublishEligibilityReason> getRefusalReasons() { return refusalReasons; }
}
