package com.company.benchmarkengine.domain.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkTestSet {

    private final String testSetId;
    private final String tenantId;
    private final String testSetName;
    private final String templateId;
    private final BenchmarkTemplateType templateType;
    private final String templateVersion;
    private final BenchmarkTestSetSource testSetSource;
    private final BenchmarkTestSetStatus status;
    private final Integer totalCases;
    private final Integer acceptedCases;
    private final Integer rejectedCases;
    private final BenchmarkTestSetFileType fileType;
    private final String fileName;
    private final String importBatchId;
    private final List<BenchmarkTestSetFieldMapping> fieldMappings;
    private final List<BenchmarkTestSetLabel> testSetLabels;
    private final List<BenchmarkSourceReference> testSetSourceRefs;
    private final List<BenchmarkTestSetCase> cases;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    public BenchmarkTestSet(String testSetId,
                            String tenantId,
                            String testSetName,
                            String templateId,
                            BenchmarkTemplateType templateType,
                            String templateVersion,
                            BenchmarkTestSetSource testSetSource,
                            BenchmarkTestSetStatus status,
                            Integer totalCases,
                            Integer acceptedCases,
                            Integer rejectedCases,
                            BenchmarkTestSetFileType fileType,
                            String fileName,
                            String importBatchId,
                            List<BenchmarkTestSetFieldMapping> fieldMappings,
                            List<BenchmarkTestSetLabel> testSetLabels,
                            List<BenchmarkSourceReference> testSetSourceRefs,
                            List<BenchmarkTestSetCase> cases,
                            String createdBy,
                            Instant createdAt,
                            Instant updatedAt) {
        this.testSetId = testSetId;
        this.tenantId = tenantId;
        this.testSetName = testSetName;
        this.templateId = templateId;
        this.templateType = templateType;
        this.templateVersion = templateVersion;
        this.testSetSource = testSetSource;
        this.status = status;
        this.totalCases = totalCases;
        this.acceptedCases = acceptedCases;
        this.rejectedCases = rejectedCases;
        this.fileType = fileType;
        this.fileName = fileName;
        this.importBatchId = importBatchId;
        this.fieldMappings = immutableList(fieldMappings);
        this.testSetLabels = immutableList(testSetLabels);
        this.testSetSourceRefs = immutableList(testSetSourceRefs);
        this.cases = immutableList(cases);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private <T> List<T> immutableList(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(items));
    }

    public String getTestSetId() {
        return testSetId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTestSetName() {
        return testSetName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public BenchmarkTemplateType getTemplateType() {
        return templateType;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public BenchmarkTestSetSource getTestSetSource() {
        return testSetSource;
    }

    public BenchmarkTestSetStatus getStatus() {
        return status;
    }

    public Integer getTotalCases() {
        return totalCases;
    }

    public Integer getAcceptedCases() {
        return acceptedCases;
    }

    public Integer getRejectedCases() {
        return rejectedCases;
    }

    public BenchmarkTestSetFileType getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getImportBatchId() {
        return importBatchId;
    }

    public List<BenchmarkTestSetFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public List<BenchmarkTestSetLabel> getTestSetLabels() {
        return testSetLabels;
    }

    public List<BenchmarkSourceReference> getTestSetSourceRefs() {
        return testSetSourceRefs;
    }

    public List<BenchmarkTestSetCase> getCases() {
        return cases;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
