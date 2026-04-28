package com.company.benchmarkengine.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class BenchmarkTestSetRecord {

    private String testSetId;
    private String tenantId;
    private String testSetName;
    private String templateId;
    private String templateType;
    private String templateVersion;
    private String testSetSource;
    private String status;
    private Integer totalCases;
    private Integer acceptedCases;
    private Integer rejectedCases;
    private String fileType;
    private String fileName;
    private String importBatchId;
    private String fieldMappingsJson;
    private String testSetLabelsJson;
    private String testSetSourceRefsJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(String testSetId) {
        this.testSetId = testSetId;
    }

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

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getTestSetSource() {
        return testSetSource;
    }

    public void setTestSetSource(String testSetSource) {
        this.testSetSource = testSetSource;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(Integer totalCases) {
        this.totalCases = totalCases;
    }

    public Integer getAcceptedCases() {
        return acceptedCases;
    }

    public void setAcceptedCases(Integer acceptedCases) {
        this.acceptedCases = acceptedCases;
    }

    public Integer getRejectedCases() {
        return rejectedCases;
    }

    public void setRejectedCases(Integer rejectedCases) {
        this.rejectedCases = rejectedCases;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(String importBatchId) {
        this.importBatchId = importBatchId;
    }

    public String getFieldMappingsJson() {
        return fieldMappingsJson;
    }

    public void setFieldMappingsJson(String fieldMappingsJson) {
        this.fieldMappingsJson = fieldMappingsJson;
    }

    public String getTestSetLabelsJson() {
        return testSetLabelsJson;
    }

    public void setTestSetLabelsJson(String testSetLabelsJson) {
        this.testSetLabelsJson = testSetLabelsJson;
    }

    public String getTestSetSourceRefsJson() {
        return testSetSourceRefsJson;
    }

    public void setTestSetSourceRefsJson(String testSetSourceRefsJson) {
        this.testSetSourceRefsJson = testSetSourceRefsJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
