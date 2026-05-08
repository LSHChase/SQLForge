package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationParseDepth;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.Size;

public class OptimizationTaskContextDTO {

    private OptimizationParseDepth parseDepth = OptimizationParseDepth.DEEP;
    private OptimizationTaskPriority priority = OptimizationTaskPriority.NORMAL;

    @Size(max = 2048, message = "callbackUrl exceeds 2048 characters")
    private String callbackUrl;

    private List<AccelerationSuggestionType> requestedSuggestionTypes = Collections.emptyList();
    private String sourceType;
    private String sourceId;
    private String batchId;
    private String reportCode;
    private String historyId;
    private String parseTaskId;
    private String datasourceCode;
    private List<String> issueScenes = Collections.emptyList();

    public OptimizationParseDepth getParseDepth() {
        return parseDepth;
    }

    public void setParseDepth(OptimizationParseDepth parseDepth) {
        this.parseDepth = parseDepth;
    }

    public OptimizationTaskPriority getPriority() {
        return priority;
    }

    public void setPriority(OptimizationTaskPriority priority) {
        this.priority = priority;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public List<AccelerationSuggestionType> getRequestedSuggestionTypes() {
        return requestedSuggestionTypes;
    }

    public void setRequestedSuggestionTypes(List<AccelerationSuggestionType> requestedSuggestionTypes) {
        if (requestedSuggestionTypes == null) {
            this.requestedSuggestionTypes = Collections.emptyList();
            return;
        }
        this.requestedSuggestionTypes = Collections.unmodifiableList(
            new ArrayList<AccelerationSuggestionType>(requestedSuggestionTypes)
        );
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public List<String> getIssueScenes() {
        return issueScenes;
    }

    public void setIssueScenes(List<String> issueScenes) {
        if (issueScenes == null) {
            this.issueScenes = Collections.emptyList();
            return;
        }
        this.issueScenes = Collections.unmodifiableList(new ArrayList<String>(issueScenes));
    }
}
