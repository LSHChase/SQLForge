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
    private Integer subqueryCount;
    private Integer scalarSubqueryCount;
    private Integer nestedSubqueryDepth;
    private Integer correlatedSubqueryCount;
    private Integer orPredicateCount;
    private Integer functionWrappedPredicateCount;
    private Integer leadingWildcardLikeCount;
    private Integer randomOrderCount;
    private Integer repeatedTableScanCount;
    private Integer orderByExpressionCount;
    private Integer duplicateOrderByKeyCount;
    private Integer duplicateGroupByKeyCount;
    private Boolean groupByWithoutAggregate;
    private Integer aggregateFunctionCount;
    private Integer stringProjectionCount;
    private Integer stringConcatenationCount;
    private Integer largeStringAggregateCount;
    private Integer repeatedSubqueryCount;
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
    public Integer getSubqueryCount() { return subqueryCount; }
    public void setSubqueryCount(Integer subqueryCount) { this.subqueryCount = subqueryCount; }
    public Integer getScalarSubqueryCount() { return scalarSubqueryCount; }
    public void setScalarSubqueryCount(Integer scalarSubqueryCount) { this.scalarSubqueryCount = scalarSubqueryCount; }
    public Integer getNestedSubqueryDepth() { return nestedSubqueryDepth; }
    public void setNestedSubqueryDepth(Integer nestedSubqueryDepth) { this.nestedSubqueryDepth = nestedSubqueryDepth; }
    public Integer getCorrelatedSubqueryCount() { return correlatedSubqueryCount; }
    public void setCorrelatedSubqueryCount(Integer correlatedSubqueryCount) { this.correlatedSubqueryCount = correlatedSubqueryCount; }
    public Integer getOrPredicateCount() { return orPredicateCount; }
    public void setOrPredicateCount(Integer orPredicateCount) { this.orPredicateCount = orPredicateCount; }
    public Integer getFunctionWrappedPredicateCount() { return functionWrappedPredicateCount; }
    public void setFunctionWrappedPredicateCount(Integer functionWrappedPredicateCount) { this.functionWrappedPredicateCount = functionWrappedPredicateCount; }
    public Integer getLeadingWildcardLikeCount() { return leadingWildcardLikeCount; }
    public void setLeadingWildcardLikeCount(Integer leadingWildcardLikeCount) { this.leadingWildcardLikeCount = leadingWildcardLikeCount; }
    public Integer getRandomOrderCount() { return randomOrderCount; }
    public void setRandomOrderCount(Integer randomOrderCount) { this.randomOrderCount = randomOrderCount; }
    public Integer getRepeatedTableScanCount() { return repeatedTableScanCount; }
    public void setRepeatedTableScanCount(Integer repeatedTableScanCount) { this.repeatedTableScanCount = repeatedTableScanCount; }
    public Integer getOrderByExpressionCount() { return orderByExpressionCount; }
    public void setOrderByExpressionCount(Integer orderByExpressionCount) { this.orderByExpressionCount = orderByExpressionCount; }
    public Integer getDuplicateOrderByKeyCount() { return duplicateOrderByKeyCount; }
    public void setDuplicateOrderByKeyCount(Integer duplicateOrderByKeyCount) { this.duplicateOrderByKeyCount = duplicateOrderByKeyCount; }
    public Integer getDuplicateGroupByKeyCount() { return duplicateGroupByKeyCount; }
    public void setDuplicateGroupByKeyCount(Integer duplicateGroupByKeyCount) { this.duplicateGroupByKeyCount = duplicateGroupByKeyCount; }
    public Boolean getGroupByWithoutAggregate() { return groupByWithoutAggregate; }
    public void setGroupByWithoutAggregate(Boolean groupByWithoutAggregate) { this.groupByWithoutAggregate = groupByWithoutAggregate; }
    public Integer getAggregateFunctionCount() { return aggregateFunctionCount; }
    public void setAggregateFunctionCount(Integer aggregateFunctionCount) { this.aggregateFunctionCount = aggregateFunctionCount; }
    public Integer getStringProjectionCount() { return stringProjectionCount; }
    public void setStringProjectionCount(Integer stringProjectionCount) { this.stringProjectionCount = stringProjectionCount; }
    public Integer getStringConcatenationCount() { return stringConcatenationCount; }
    public void setStringConcatenationCount(Integer stringConcatenationCount) { this.stringConcatenationCount = stringConcatenationCount; }
    public Integer getLargeStringAggregateCount() { return largeStringAggregateCount; }
    public void setLargeStringAggregateCount(Integer largeStringAggregateCount) { this.largeStringAggregateCount = largeStringAggregateCount; }
    public Integer getRepeatedSubqueryCount() { return repeatedSubqueryCount; }
    public void setRepeatedSubqueryCount(Integer repeatedSubqueryCount) { this.repeatedSubqueryCount = repeatedSubqueryCount; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
