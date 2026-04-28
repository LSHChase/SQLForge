package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class StructureParseFeatureSummaryVO {

    private String parserEngine;
    private String scanMode;
    private String joinType;
    private String computeDensity;
    private String resourceType;
    private String slaLevel;
    private Integer tableCount;
    private Integer joinCount;
    private Integer predicateCount;
    private Integer windowFunctionCount;
    private Integer udfFunctionCount;
    private Integer repeatedExpressionCount;
    private List<String> evidence;

    public String getParserEngine() { return parserEngine; }
    public void setParserEngine(String parserEngine) { this.parserEngine = parserEngine; }
    public String getScanMode() { return scanMode; }
    public void setScanMode(String scanMode) { this.scanMode = scanMode; }
    public String getJoinType() { return joinType; }
    public void setJoinType(String joinType) { this.joinType = joinType; }
    public String getComputeDensity() { return computeDensity; }
    public void setComputeDensity(String computeDensity) { this.computeDensity = computeDensity; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getSlaLevel() { return slaLevel; }
    public void setSlaLevel(String slaLevel) { this.slaLevel = slaLevel; }
    public Integer getTableCount() { return tableCount; }
    public void setTableCount(Integer tableCount) { this.tableCount = tableCount; }
    public Integer getJoinCount() { return joinCount; }
    public void setJoinCount(Integer joinCount) { this.joinCount = joinCount; }
    public Integer getPredicateCount() { return predicateCount; }
    public void setPredicateCount(Integer predicateCount) { this.predicateCount = predicateCount; }
    public Integer getWindowFunctionCount() { return windowFunctionCount; }
    public void setWindowFunctionCount(Integer windowFunctionCount) { this.windowFunctionCount = windowFunctionCount; }
    public Integer getUdfFunctionCount() { return udfFunctionCount; }
    public void setUdfFunctionCount(Integer udfFunctionCount) { this.udfFunctionCount = udfFunctionCount; }
    public Integer getRepeatedExpressionCount() { return repeatedExpressionCount; }
    public void setRepeatedExpressionCount(Integer repeatedExpressionCount) { this.repeatedExpressionCount = repeatedExpressionCount; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
