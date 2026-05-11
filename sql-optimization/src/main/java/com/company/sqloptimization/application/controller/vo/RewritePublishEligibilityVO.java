package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class RewritePublishEligibilityVO {

    private String rewriteRecordId;
    private String tenantId;
    private String policyId;
    private Boolean eligible;
    private String reviewStatus;
    private String validationStatus;
    private String publishStatus;
    private String alertStatus;
    private Boolean autoApplyAllowed;
    private String lastValidationRunId;
    private List<RewritePublishEligibilityReasonVO> refusalReasons;
    private String contractStage;
    private String implementationStage;

    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public Boolean getEligible() { return eligible; }
    public void setEligible(Boolean eligible) { this.eligible = eligible; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }
    public Boolean getAutoApplyAllowed() { return autoApplyAllowed; }
    public void setAutoApplyAllowed(Boolean autoApplyAllowed) { this.autoApplyAllowed = autoApplyAllowed; }
    public String getLastValidationRunId() { return lastValidationRunId; }
    public void setLastValidationRunId(String lastValidationRunId) { this.lastValidationRunId = lastValidationRunId; }
    public List<RewritePublishEligibilityReasonVO> getRefusalReasons() { return refusalReasons; }
    public void setRefusalReasons(List<RewritePublishEligibilityReasonVO> refusalReasons) { this.refusalReasons = refusalReasons; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
