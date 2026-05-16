package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class BenchmarkParseResultTestSetCreateRequest {

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotBlank(message = "testSetName 为必填项")
    @Size(max = 128, message = "testSetName 超过 128 个字符")
    private String testSetName;

    @Size(max = 64, message = "templateId 超过 64 个字符")
    private String templateId;

    private BenchmarkTemplateType templateType;

    @Size(max = 32, message = "templateVersion 超过 32 个字符")
    private String templateVersion;

    @Size(max = 64, message = "parseTaskId 超过 64 个字符")
    private String parseTaskId;

    @Size(max = 64, message = "parseBatchId 超过 64 个字符")
    private String parseBatchId;

    @Size(max = 10485760, message = "sqlText 超过 10MB 限制")
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

    @AssertTrue(message = "必须且只能提供 parseTaskId 或 parseBatchId 中的一个")
    public boolean isSourceSelectorValid() {
        boolean hasTask = hasText(parseTaskId);
        boolean hasBatch = hasText(parseBatchId);
        return hasTask ^ hasBatch;
    }

    @AssertTrue(message = "使用单个 parseTaskId 生成时 sqlText 为必填项")
    public boolean isSqlTextPresentForParseTask() {
        return !hasText(parseTaskId) || hasText(sqlText);
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
