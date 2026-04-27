package com.company.sqloptimization.application.controller.vo;

public class ParseBatchIssueStatisticVO {

    private String issueScene;
    private Integer affectedRecords;
    private Double ratio;

    public String getIssueScene() { return issueScene; }
    public void setIssueScene(String issueScene) { this.issueScene = issueScene; }
    public Integer getAffectedRecords() { return affectedRecords; }
    public void setAffectedRecords(Integer affectedRecords) { this.affectedRecords = affectedRecords; }
    public Double getRatio() { return ratio; }
    public void setRatio(Double ratio) { this.ratio = ratio; }
}
