package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTestSetCreateRequest {

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

    @NotNull(message = "testSetSource is required")
    private BenchmarkTestSetSource testSetSource = BenchmarkTestSetSource.BATCH_IMPORT;

    @Valid
    private List<BenchmarkTestSetLabelDTO> testSetLabels;

    @Valid
    private List<BenchmarkSourceReferenceDTO> testSetSourceRefs;

    @Valid
    @NotNull(message = "importRequest is required")
    private BenchmarkTestSetImportRequest importRequest;

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

    public BenchmarkTestSetImportRequest getImportRequest() {
        return importRequest;
    }

    public void setImportRequest(BenchmarkTestSetImportRequest importRequest) {
        this.importRequest = importRequest;
    }

    @AssertTrue(message = "batch-import endpoint only supports testSetSource=BATCH_IMPORT in the current baseline")
    public boolean isBatchImportSourceCompatible() {
        return testSetSource == BenchmarkTestSetSource.BATCH_IMPORT;
    }
}
