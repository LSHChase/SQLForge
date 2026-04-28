package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class BenchmarkParseResultTestSetCreateRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "testSetName is required")
    @Size(max = 128, message = "testSetName exceeds 128 characters")
    private String testSetName;

    @Size(max = 64, message = "templateId exceeds 64 characters")
    private String templateId;

    private BenchmarkTemplateType templateType;

    @Size(max = 32, message = "templateVersion exceeds 32 characters")
    private String templateVersion;

    @Size(max = 64, message = "parseTaskId exceeds 64 characters")
    private String parseTaskId;

    @Size(max = 64, message = "parseBatchId exceeds 64 characters")
    private String parseBatchId;

    @Size(max = 10485760, message = "sqlText exceeds 10MB limit")
    private String sqlText;

    private List<String> includeIssueScenes;

    private List<String> includeReportCodes;

    @Valid
    private List<BenchmarkTestSetLabelDTO> testSetLabels;

    @Valid
    private List<BenchmarkSourceReferenceDTO> testSetSourceRefs;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTestSetName() {
        return testSetName;
    }

    public void setTestSetName(String testSetName) {
        this.testSetName = testSetName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public BenchmarkTemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(BenchmarkTemplateType templateType) {
        this.templateType = templateType;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getParseBatchId() {
        return parseBatchId;
    }

    public void setParseBatchId(String parseBatchId) {
        this.parseBatchId = parseBatchId;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public List<String> getIncludeIssueScenes() {
        return includeIssueScenes;
    }

    public void setIncludeIssueScenes(List<String> includeIssueScenes) {
        this.includeIssueScenes = includeIssueScenes;
    }

    public List<String> getIncludeReportCodes() {
        return includeReportCodes;
    }

    public void setIncludeReportCodes(List<String> includeReportCodes) {
        this.includeReportCodes = includeReportCodes;
    }

    public List<BenchmarkTestSetLabelDTO> getTestSetLabels() {
        return testSetLabels;
    }

    public void setTestSetLabels(List<BenchmarkTestSetLabelDTO> testSetLabels) {
        this.testSetLabels = testSetLabels;
    }

    public List<BenchmarkSourceReferenceDTO> getTestSetSourceRefs() {
        return testSetSourceRefs;
    }

    public void setTestSetSourceRefs(List<BenchmarkSourceReferenceDTO> testSetSourceRefs) {
        this.testSetSourceRefs = testSetSourceRefs;
    }

    @AssertTrue(message = "Exactly one of parseTaskId or parseBatchId must be provided")
    public boolean isSourceSelectorValid() {
        boolean hasTask = hasText(parseTaskId);
        boolean hasBatch = hasText(parseBatchId);
        return hasTask ^ hasBatch;
    }

    @AssertTrue(message = "sqlText is required when generating from a single parseTaskId")
    public boolean isSqlTextPresentForParseTask() {
        return !hasText(parseTaskId) || hasText(sqlText);
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
