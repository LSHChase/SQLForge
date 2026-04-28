package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetFieldMappingDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetCaseVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFieldMapping;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkTestSetModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String TEST_SET_IMPORT_IMPLEMENTATION_STAGE = "BATCH_IMPORT_BASELINE";
    private static final String TEST_SET_PARSE_IMPLEMENTATION_STAGE = "PARSE_RESULT_GENERATION_BASELINE";

    public BenchmarkTestSet buildImportedTestSet(BenchmarkTestSetCreateRequest request,
                                                 String testSetId,
                                                 String importBatchId,
                                                 List<BenchmarkTestSetCase> cases,
                                                 Instant now,
                                                 String createdBy) {
        int accepted = 0;
        int rejected = 0;
        for (BenchmarkTestSetCase item : cases) {
            if (item.getStatus() == BenchmarkTestSetCaseStatus.ACCEPTED) {
                accepted++;
            } else {
                rejected++;
            }
        }
        BenchmarkTestSetStatus status = accepted <= 0
            ? BenchmarkTestSetStatus.FAILED
            : rejected > 0
            ? BenchmarkTestSetStatus.PARTIAL_READY
            : BenchmarkTestSetStatus.READY;
        List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>(toSourceReferences(request.getTestSetSourceRefs()));
        if (!containsSourceReference(refs, BenchmarkSourceReferenceType.IMPORT_BATCH, importBatchId)) {
            refs.add(new BenchmarkSourceReference(BenchmarkSourceReferenceType.IMPORT_BATCH, importBatchId));
        }
        return buildTestSet(
            testSetId,
            request.getTenantId(),
            request.getTestSetName(),
            trimToNull(request.getTemplateId()),
            request.getTemplateType(),
            trimToNull(request.getTemplateVersion()),
            request.getTestSetSource(),
            request.getImportRequest().getFileType(),
            request.getImportRequest().getFileName(),
            importBatchId,
            toFieldMappings(request.getImportRequest().getFieldMappings()),
            toTestSetLabels(request.getTestSetLabels()),
            refs,
            cases,
            createdBy,
            now
        );
    }

    public BenchmarkTestSet buildGeneratedTestSet(String tenantId,
                                                  String testSetName,
                                                  String templateId,
                                                  BenchmarkTemplateType templateType,
                                                  String templateVersion,
                                                  List<BenchmarkTestSetLabel> labels,
                                                  List<BenchmarkSourceReference> refs,
                                                  List<BenchmarkTestSetCase> cases,
                                                  Instant now,
                                                  String createdBy) {
        return buildTestSet(
            null,
            tenantId,
            testSetName,
            trimToNull(templateId),
            templateType,
            trimToNull(templateVersion),
            com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource.PARSE_RESULT_GENERATION,
            null,
            null,
            null,
            Collections.<BenchmarkTestSetFieldMapping>emptyList(),
            labels,
            refs,
            cases,
            createdBy,
            now
        );
    }

    public BenchmarkTestSetResponse buildResponse(BenchmarkTestSet testSet) {
        BenchmarkTestSetResponse response = new BenchmarkTestSetResponse();
        response.setTestSetId(testSet.getTestSetId());
        response.setTenantId(testSet.getTenantId());
        response.setTestSetName(testSet.getTestSetName());
        response.setTemplateId(testSet.getTemplateId());
        response.setTemplateType(testSet.getTemplateType());
        response.setTemplateVersion(testSet.getTemplateVersion());
        response.setTestSetSource(testSet.getTestSetSource());
        response.setStatus(testSet.getStatus());
        response.setTotalCases(testSet.getTotalCases());
        response.setAcceptedCases(testSet.getAcceptedCases());
        response.setRejectedCases(testSet.getRejectedCases());
        response.setFileType(testSet.getFileType());
        response.setFileName(testSet.getFileName());
        response.setImportBatchId(testSet.getImportBatchId());
        response.setFieldMappings(testSet.getFieldMappings());
        response.setTestSetLabels(testSet.getTestSetLabels());
        response.setTestSetSourceRefs(testSet.getTestSetSourceRefs());
        response.setCases(toCaseVos(testSet.getCases()));
        response.setCreatedAt(testSet.getCreatedAt());
        response.setUpdatedAt(testSet.getUpdatedAt());
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(resolveImplementationStage(testSet));
        return response;
    }

    private BenchmarkTestSet buildTestSet(String requestedTestSetId,
                                          String tenantId,
                                          String testSetName,
                                          String templateId,
                                          BenchmarkTemplateType templateType,
                                          String templateVersion,
                                          com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource source,
                                          com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType fileType,
                                          String fileName,
                                          String importBatchId,
                                          List<BenchmarkTestSetFieldMapping> fieldMappings,
                                          List<BenchmarkTestSetLabel> labels,
                                          List<BenchmarkSourceReference> refs,
                                          List<BenchmarkTestSetCase> cases,
                                          String createdBy,
                                          Instant now) {
        String testSetId = trimToNull(requestedTestSetId);
        if (testSetId == null) {
            testSetId = java.util.UUID.randomUUID().toString();
        }
        int accepted = 0;
        int rejected = 0;
        for (BenchmarkTestSetCase item : cases) {
            if (item.getStatus() == BenchmarkTestSetCaseStatus.ACCEPTED) {
                accepted++;
            } else {
                rejected++;
            }
        }
        BenchmarkTestSetStatus status = accepted <= 0
            ? BenchmarkTestSetStatus.FAILED
            : rejected > 0
            ? BenchmarkTestSetStatus.PARTIAL_READY
            : BenchmarkTestSetStatus.READY;
        List<BenchmarkTestSetCase> normalizedCases = normalizeCases(testSetId, cases);
        return new BenchmarkTestSet(
            testSetId,
            tenantId,
            testSetName,
            templateId,
            templateType,
            templateVersion,
            source,
            status,
            Integer.valueOf(normalizedCases.size()),
            Integer.valueOf(accepted),
            Integer.valueOf(rejected),
            fileType,
            fileName,
            importBatchId,
            fieldMappings,
            labels,
            refs,
            normalizedCases,
            createdBy,
            now,
            now
        );
    }

    private List<BenchmarkTestSetCase> normalizeCases(String testSetId, List<BenchmarkTestSetCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetCase> normalized = new ArrayList<BenchmarkTestSetCase>(cases.size());
        for (BenchmarkTestSetCase item : cases) {
            normalized.add(
                new BenchmarkTestSetCase(
                    item.getCaseId(),
                    testSetId,
                    item.getSequenceNumber(),
                    item.getSourceLineNumber(),
                    item.getCaseName(),
                    item.getSqlText(),
                    item.getSqlFingerprint(),
                    item.getDatasourceCode(),
                    item.getReportCode(),
                    item.getTags(),
                    item.getBindParametersJson(),
                    item.getStatus(),
                    item.getRejectionReason(),
                    item.getRawCaseDataJson()
                )
            );
        }
        return Collections.unmodifiableList(normalized);
    }

    private String resolveImplementationStage(BenchmarkTestSet testSet) {
        if (testSet.getTestSetSource() == com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource.PARSE_RESULT_GENERATION) {
            return TEST_SET_PARSE_IMPLEMENTATION_STAGE;
        }
        return TEST_SET_IMPORT_IMPLEMENTATION_STAGE;
    }

    private boolean containsSourceReference(List<BenchmarkSourceReference> refs,
                                            BenchmarkSourceReferenceType type,
                                            String referenceId) {
        if (refs == null || type == null || referenceId == null) {
            return false;
        }
        for (BenchmarkSourceReference ref : refs) {
            if (ref != null && type == ref.getType() && referenceId.equals(ref.getReferenceId())) {
                return true;
            }
        }
        return false;
    }

    private List<BenchmarkTestSetFieldMapping> toFieldMappings(List<BenchmarkTestSetFieldMappingDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetFieldMapping> mappings = new ArrayList<BenchmarkTestSetFieldMapping>(items.size());
        for (BenchmarkTestSetFieldMappingDTO item : items) {
            mappings.add(new BenchmarkTestSetFieldMapping(item.getField(), trimToNull(item.getColumnName())));
        }
        return Collections.unmodifiableList(mappings);
    }

    private List<BenchmarkTestSetLabel> toTestSetLabels(List<BenchmarkTestSetLabelDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetLabel> labels = new ArrayList<BenchmarkTestSetLabel>(items.size());
        for (BenchmarkTestSetLabelDTO item : items) {
            labels.add(new BenchmarkTestSetLabel(item.getType(), item.getValue()));
        }
        return Collections.unmodifiableList(labels);
    }

    private List<BenchmarkSourceReference> toSourceReferences(List<BenchmarkSourceReferenceDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>(items.size());
        for (BenchmarkSourceReferenceDTO item : items) {
            refs.add(new BenchmarkSourceReference(item.getType(), item.getReferenceId()));
        }
        return Collections.unmodifiableList(refs);
    }

    private List<BenchmarkTestSetCaseVO> toCaseVos(List<BenchmarkTestSetCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetCaseVO> items = new ArrayList<BenchmarkTestSetCaseVO>(cases.size());
        for (BenchmarkTestSetCase item : cases) {
            BenchmarkTestSetCaseVO vo = new BenchmarkTestSetCaseVO();
            vo.setCaseId(item.getCaseId());
            vo.setSequenceNumber(item.getSequenceNumber());
            vo.setSourceLineNumber(item.getSourceLineNumber());
            vo.setCaseName(item.getCaseName());
            vo.setSqlText(item.getSqlText());
            vo.setSqlFingerprint(item.getSqlFingerprint());
            vo.setDatasourceCode(item.getDatasourceCode());
            vo.setReportCode(item.getReportCode());
            vo.setTags(item.getTags());
            vo.setBindParametersJson(item.getBindParametersJson());
            vo.setStatus(item.getStatus());
            vo.setRejectionReason(item.getRejectionReason());
            vo.setRawCaseDataJson(item.getRawCaseDataJson());
            items.add(vo);
        }
        return items;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
