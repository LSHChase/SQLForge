package com.company.sqloptimization.application.controller.vo;

import java.util.List;
import java.util.Map;

public class ReportBatchParseStatisticsVO {

    private ParseStatisticsOverviewVO overview;
    private List<ParseIssueSceneStatisticVO> issueSceneStatistics;
    private Map<String, Integer> severityDistribution;
    private List<ReportBatchImportanceStatisticVO> importanceStatistics;
    private List<ParseReportStatisticVO> reportStatistics;
    private List<ReportBatchSqlStatisticVO> sqlStatistics;
    private List<ParsePriorityMatrixCellVO> priorityMatrix;
    private List<ReportBatchLogicalObjectStatisticVO> logicalObjectStatistics;

    public ParseStatisticsOverviewVO getOverview() { return overview; }
    public void setOverview(ParseStatisticsOverviewVO overview) { this.overview = overview; }
    public List<ParseIssueSceneStatisticVO> getIssueSceneStatistics() { return issueSceneStatistics; }
    public void setIssueSceneStatistics(List<ParseIssueSceneStatisticVO> issueSceneStatistics) {
        this.issueSceneStatistics = issueSceneStatistics;
    }
    public Map<String, Integer> getSeverityDistribution() { return severityDistribution; }
    public void setSeverityDistribution(Map<String, Integer> severityDistribution) { this.severityDistribution = severityDistribution; }
    public List<ReportBatchImportanceStatisticVO> getImportanceStatistics() { return importanceStatistics; }
    public void setImportanceStatistics(List<ReportBatchImportanceStatisticVO> importanceStatistics) {
        this.importanceStatistics = importanceStatistics;
    }
    public List<ParseReportStatisticVO> getReportStatistics() { return reportStatistics; }
    public void setReportStatistics(List<ParseReportStatisticVO> reportStatistics) { this.reportStatistics = reportStatistics; }
    public List<ReportBatchSqlStatisticVO> getSqlStatistics() { return sqlStatistics; }
    public void setSqlStatistics(List<ReportBatchSqlStatisticVO> sqlStatistics) { this.sqlStatistics = sqlStatistics; }
    public List<ParsePriorityMatrixCellVO> getPriorityMatrix() { return priorityMatrix; }
    public void setPriorityMatrix(List<ParsePriorityMatrixCellVO> priorityMatrix) { this.priorityMatrix = priorityMatrix; }
    public List<ReportBatchLogicalObjectStatisticVO> getLogicalObjectStatistics() { return logicalObjectStatistics; }
    public void setLogicalObjectStatistics(List<ReportBatchLogicalObjectStatisticVO> logicalObjectStatistics) {
        this.logicalObjectStatistics = logicalObjectStatistics;
    }
}
