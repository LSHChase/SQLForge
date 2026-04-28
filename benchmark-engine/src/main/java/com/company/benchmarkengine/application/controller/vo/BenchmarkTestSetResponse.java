package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFieldMapping;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetStatus;
import java.time.Instant;
import java.util.List;

public class BenchmarkTestSetResponse {

    private String testSetId;
    private String tenantId;
    private String testSetName;
    private String templateId;
    private BenchmarkTemplateType templateType;
    private String templateVersion;
    private BenchmarkTestSetSource testSetSource;
    private BenchmarkTestSetStatus status;
    private Integer totalCases;
    private Integer acceptedCases;
    private Integer rejectedCases;
    private BenchmarkTestSetFileType fileType;
    private String fileName;
    private String importBatchId;
    private List<BenchmarkTestSetFieldMapping> fieldMappings;
    private List<BenchmarkTestSetLabel> testSetLabels;
    private List<BenchmarkSourceReference> testSetSourceRefs;
    private List<BenchmarkTestSetCaseVO> cases;
    private Instant createdAt;
    private Instant updatedAt;
    private String contractStage;
    private String implementationStage;

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

    public BenchmarkTestSetSource getTestSetSource() {
        return testSetSource;
    }

    public void setTestSetSource(BenchmarkTestSetSource testSetSource) {
        this.testSetSource = testSetSource;
    }

    public BenchmarkTestSetStatus getStatus() {
        return status;
    }

    public void setStatus(BenchmarkTestSetStatus status) {
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

    public BenchmarkTestSetFileType getFileType() {
        return fileType;
    }

    public void setFileType(BenchmarkTestSetFileType fileType) {
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

    public List<BenchmarkTestSetFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<BenchmarkTestSetFieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public List<BenchmarkTestSetLabel> getTestSetLabels() {
        return testSetLabels;
    }

    public void setTestSetLabels(List<BenchmarkTestSetLabel> testSetLabels) {
        this.testSetLabels = testSetLabels;
    }

    public List<BenchmarkSourceReference> getTestSetSourceRefs() {
        return testSetSourceRefs;
    }

    public void setTestSetSourceRefs(List<BenchmarkSourceReference> testSetSourceRefs) {
        this.testSetSourceRefs = testSetSourceRefs;
    }

    public List<BenchmarkTestSetCaseVO> getCases() {
        return cases;
    }

    public void setCases(List<BenchmarkTestSetCaseVO> cases) {
        this.cases = cases;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
