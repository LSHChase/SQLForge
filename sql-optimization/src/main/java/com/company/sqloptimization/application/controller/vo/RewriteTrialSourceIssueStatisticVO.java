package com.company.sqloptimization.application.controller.vo;

public class RewriteTrialSourceIssueStatisticVO {

    private String sourceIssueScene;
    private Integer eligibleSqlCount;
    private Integer trialedSqlCount;
    private Double candidateGeneratedRate;
    private Double noSafeRewriteRate;
    private Double manualReviewRate;
    private Double validationPassedRate;

    public String getSourceIssueScene() { return sourceIssueScene; }
    public void setSourceIssueScene(String sourceIssueScene) { this.sourceIssueScene = sourceIssueScene; }
    public Integer getEligibleSqlCount() { return eligibleSqlCount; }
    public void setEligibleSqlCount(Integer eligibleSqlCount) { this.eligibleSqlCount = eligibleSqlCount; }
    public Integer getTrialedSqlCount() { return trialedSqlCount; }
    public void setTrialedSqlCount(Integer trialedSqlCount) { this.trialedSqlCount = trialedSqlCount; }
    public Double getCandidateGeneratedRate() { return candidateGeneratedRate; }
    public void setCandidateGeneratedRate(Double candidateGeneratedRate) { this.candidateGeneratedRate = candidateGeneratedRate; }
    public Double getNoSafeRewriteRate() { return noSafeRewriteRate; }
    public void setNoSafeRewriteRate(Double noSafeRewriteRate) { this.noSafeRewriteRate = noSafeRewriteRate; }
    public Double getManualReviewRate() { return manualReviewRate; }
    public void setManualReviewRate(Double manualReviewRate) { this.manualReviewRate = manualReviewRate; }
    public Double getValidationPassedRate() { return validationPassedRate; }
    public void setValidationPassedRate(Double validationPassedRate) { this.validationPassedRate = validationPassedRate; }
}
