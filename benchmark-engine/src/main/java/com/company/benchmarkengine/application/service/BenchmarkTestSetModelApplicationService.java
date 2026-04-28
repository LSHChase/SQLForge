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
    private static final String TEST_SET_IMPLEMENTATION_STAGE = "BATCH_IMPORT_BASELINE";

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
        return new BenchmarkTestSet(
            testSetId,
            request.getTenantId(),
            request.getTestSetName(),
            trimToNull(request.getTemplateId()),
            request.getTemplateType(),
            trimToNull(request.getTemplateVersion()),
            request.getTestSetSource(),
            status,
            Integer.valueOf(cases.size()),
            Integer.valueOf(accepted),
            Integer.valueOf(rejected),
            request.getImportRequest().getFileType(),
            request.getImportRequest().getFileName(),
            importBatchId,
            toFieldMappings(request.getImportRequest().getFieldMappings()),
            toTestSetLabels(request.getTestSetLabels()),
            refs,
            cases,
            createdBy,
            now,
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
        response.setImplementationStage(TEST_SET_IMPLEMENTATION_STAGE);
        return response;
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
