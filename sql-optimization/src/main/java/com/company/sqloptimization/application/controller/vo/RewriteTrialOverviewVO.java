package com.company.sqloptimization.application.controller.vo;

public class RewriteTrialOverviewVO {

    private Integer eligibleSqlCount;
    private Integer trialedSqlCount;
    private Integer candidateGeneratedCount;
    private Integer noSafeRewriteCount;
    private Integer manualReviewRequiredCount;
    private Integer validatedEquivalentCount;
    private Integer validatedDivergedCount;

    public Integer getEligibleSqlCount() { return eligibleSqlCount; }
    public void setEligibleSqlCount(Integer eligibleSqlCount) { this.eligibleSqlCount = eligibleSqlCount; }
    public Integer getTrialedSqlCount() { return trialedSqlCount; }
    public void setTrialedSqlCount(Integer trialedSqlCount) { this.trialedSqlCount = trialedSqlCount; }
    public Integer getCandidateGeneratedCount() { return candidateGeneratedCount; }
    public void setCandidateGeneratedCount(Integer candidateGeneratedCount) { this.candidateGeneratedCount = candidateGeneratedCount; }
    public Integer getNoSafeRewriteCount() { return noSafeRewriteCount; }
    public void setNoSafeRewriteCount(Integer noSafeRewriteCount) { this.noSafeRewriteCount = noSafeRewriteCount; }
    public Integer getManualReviewRequiredCount() { return manualReviewRequiredCount; }
    public void setManualReviewRequiredCount(Integer manualReviewRequiredCount) { this.manualReviewRequiredCount = manualReviewRequiredCount; }
    public Integer getValidatedEquivalentCount() { return validatedEquivalentCount; }
    public void setValidatedEquivalentCount(Integer validatedEquivalentCount) { this.validatedEquivalentCount = validatedEquivalentCount; }
    public Integer getValidatedDivergedCount() { return validatedDivergedCount; }
    public void setValidatedDivergedCount(Integer validatedDivergedCount) { this.validatedDivergedCount = validatedDivergedCount; }
}
