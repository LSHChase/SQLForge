package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.AccelerationCandidateCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationCandidateVO;
import com.company.sqloptimization.domain.candidate.AccelerationCandidate;
import com.company.sqloptimization.domain.candidate.repository.AccelerationCandidateRepository;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccelerationCandidateApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "ACCELERATION_REWRITE_CONTRACT_BASELINE";
    private static final EnumSet<GovernanceSourceKind> PARSE_SOURCE_KINDS = EnumSet.of(
        GovernanceSourceKind.STRUCTURE_PARSE,
        GovernanceSourceKind.COMBINED_PARSE,
        GovernanceSourceKind.PARSE_BATCH,
        GovernanceSourceKind.REPORT_BATCH,
        GovernanceSourceKind.END_OF_DAY_SLOW_SQL
    );
    private static final EnumSet<GovernanceSourceKind> QUERY_SOURCE_KINDS = EnumSet.of(
        GovernanceSourceKind.QUERY_HISTORY,
        GovernanceSourceKind.SLOW_SQL,
        GovernanceSourceKind.HIGH_P99,
        GovernanceSourceKind.HIGH_SCAN,
        GovernanceSourceKind.BENCHMARK_REGRESSION,
        GovernanceSourceKind.MANUAL
    );
    private static final EnumSet<EvidenceLevel> PARSE_EVIDENCE_LEVELS = EnumSet.of(
        EvidenceLevel.STATIC_PARSE,
        EvidenceLevel.ACCESS_PARSE,
        EvidenceLevel.MIXED
    );
    private static final EnumSet<EvidenceLevel> QUERY_EVIDENCE_LEVELS = EnumSet.of(
        EvidenceLevel.RUNTIME_HISTORY,
        EvidenceLevel.EXPLAIN_PLAN,
        EvidenceLevel.BENCHMARK,
        EvidenceLevel.MIXED
    );

    private final AccelerationCandidateRepository accelerationCandidateRepository;

    public AccelerationCandidateApplicationService(AccelerationCandidateRepository accelerationCandidateRepository) {
        this.accelerationCandidateRepository = accelerationCandidateRepository;
    }

    public AccelerationCandidateVO createCandidate(AccelerationCandidateCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null) {
            throw invalidArgument("request", "candidate request is required");
        }
        NormalizedSource normalizedSource = normalizeSource(request);
        Instant now = Instant.now();
        AccelerationCandidate candidate = AccelerationCandidate.builder()
            .candidateId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .sourceType(normalizedSource.sourceType)
            .sourceKind(normalizedSource.sourceKind)
            .sourceId(normalizedSource.sourceId)
            .historyId(trimToNull(request.getHistoryId()))
            .parseHistoryId(trimToNull(request.getParseHistoryId()))
            .parseTaskId(trimToNull(request.getParseTaskId()))
            .batchId(trimToNull(request.getBatchId()))
            .batchItemId(trimToNull(request.getBatchItemId()))
            .benchmarkTaskId(trimToNull(request.getBenchmarkTaskId()))
            .optimizationTaskId(trimToNull(request.getOptimizationTaskId()))
            .sqlFingerprint(trimToNull(request.getSqlFingerprint()))
            .datasourceCode(trimToNull(request.getDatasourceCode()))
            .stage(trimToNull(request.getStage()))
            .reportCode(trimToNull(request.getReportCode()))
            .candidateType(request.getCandidateType())
            .status(request.getStatus())
            .confidence(request.getConfidence())
            .priority(request.getPriority())
            .evidenceLevel(normalizedSource.evidenceLevel)
            .schemaVersion(trimToNull(request.getSchemaVersion()))
            .createdBy(RequestContext.getUserId())
            .createdAt(now)
            .updatedAt(now)
            .sourceEvidence(request.getSourceEvidence())
            .issueEvidence(request.getIssueEvidence())
            .runtimeEvidence(request.getRuntimeEvidence())
            .benefitEstimate(request.getBenefitEstimate())
            .costEstimate(request.getCostEstimate())
            .risk(request.getRisk())
            .build();
        return toVo(accelerationCandidateRepository.save(candidate));
    }

    private NormalizedSource normalizeSource(AccelerationCandidateCreateRequest request) {
        GovernanceSourceType sourceType = request.getSourceType();
        GovernanceSourceKind sourceKind = request.getSourceKind();
        EvidenceLevel evidenceLevel = request.getEvidenceLevel();
        if (sourceType == null) {
            throw invalidArgument("sourceType", "sourceType is required");
        }
        if (sourceKind == null) {
            throw invalidArgument("sourceKind", "sourceKind is required");
        }
        if (evidenceLevel == null) {
            throw invalidArgument("evidenceLevel", "evidenceLevel is required");
        }
        if (sourceType == GovernanceSourceType.PARSE) {
            validateParseSource(request, sourceKind, evidenceLevel);
            return new NormalizedSource(sourceType, sourceKind, resolveParseSourceId(request), evidenceLevel);
        }
        if (sourceType == GovernanceSourceType.QUERY) {
            validateQuerySource(sourceKind, evidenceLevel);
            return new NormalizedSource(sourceType, sourceKind, resolveQuerySourceId(request), evidenceLevel);
        }
        throw invalidArgument("sourceType", "Unsupported sourceType: " + sourceType);
    }

    private void validateParseSource(AccelerationCandidateCreateRequest request,
                                     GovernanceSourceKind sourceKind,
                                     EvidenceLevel evidenceLevel) {
        if (!PARSE_SOURCE_KINDS.contains(sourceKind)) {
            throw invalidArgument("sourceKind", "sourceKind " + sourceKind + " is not valid for PARSE sourceType");
        }
        if (!PARSE_EVIDENCE_LEVELS.contains(evidenceLevel)) {
            throw invalidArgument(
                "evidenceLevel",
                "evidenceLevel " + evidenceLevel + " is not valid for PARSE sourceType"
            );
        }
        if (request.getRuntimeEvidence() != null
            && !request.getRuntimeEvidence().isEmpty()
            && (evidenceLevel == EvidenceLevel.STATIC_PARSE || evidenceLevel == EvidenceLevel.ACCESS_PARSE)) {
            throw invalidArgument(
                "runtimeEvidence",
                "runtimeEvidence requires MIXED or QUERY evidence and cannot be attached to static parse evidence"
            );
        }
    }

    private void validateQuerySource(GovernanceSourceKind sourceKind, EvidenceLevel evidenceLevel) {
        if (!QUERY_SOURCE_KINDS.contains(sourceKind)) {
            throw invalidArgument("sourceKind", "sourceKind " + sourceKind + " is not valid for QUERY sourceType");
        }
        if (!QUERY_EVIDENCE_LEVELS.contains(evidenceLevel)) {
            throw invalidArgument(
                "evidenceLevel",
                "evidenceLevel " + evidenceLevel + " is not valid for QUERY sourceType"
            );
        }
    }

    private String resolveParseSourceId(AccelerationCandidateCreateRequest request) {
        String sourceId = firstNonBlank(
            request.getParseHistoryId(),
            request.getParseTaskId(),
            request.getBatchItemId(),
            request.getBatchId(),
            request.getSourceId()
        );
        if (!StringUtils.hasText(sourceId)) {
            throw invalidArgument(
                "sourceId",
                "PARSE candidate requires sourceId, parseHistoryId, parseTaskId, batchItemId or batchId"
            );
        }
        return sourceId;
    }

    private String resolveQuerySourceId(AccelerationCandidateCreateRequest request) {
        String sourceId = firstNonBlank(
            request.getHistoryId(),
            request.getBenchmarkTaskId(),
            request.getSourceId()
        );
        if (!StringUtils.hasText(sourceId)) {
            throw invalidArgument("sourceId", "QUERY candidate requires sourceId, historyId or benchmarkTaskId");
        }
        return sourceId;
    }

    public AccelerationCandidateVO getCandidate(String candidateId) {
        AccelerationCandidate candidate = accelerationCandidateRepository.findByCandidateId(candidateId);
        if (candidate == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Acceleration candidate not found: " + candidateId
            );
        }
        verifyTenantAccess(candidate.getTenantId());
        return toVo(candidate);
    }

    public List<AccelerationCandidateVO> listCandidates() {
        String tenantId = requireContextTenant();
        List<AccelerationCandidate> candidates = accelerationCandidateRepository.findByTenantId(tenantId);
        List<AccelerationCandidateVO> result = new ArrayList<AccelerationCandidateVO>(candidates.size());
        for (AccelerationCandidate candidate : candidates) {
            result.add(toVo(candidate));
        }
        return result;
    }

    private AccelerationCandidateVO toVo(AccelerationCandidate candidate) {
        AccelerationCandidateVO vo = new AccelerationCandidateVO();
        vo.setCandidateId(candidate.getCandidateId());
        vo.setTenantId(candidate.getTenantId());
        vo.setSourceType(candidate.getSourceType().name());
        vo.setSourceKind(candidate.getSourceKind().name());
        vo.setSourceId(candidate.getSourceId());
        vo.setHistoryId(candidate.getHistoryId());
        vo.setParseHistoryId(candidate.getParseHistoryId());
        vo.setParseTaskId(candidate.getParseTaskId());
        vo.setBatchId(candidate.getBatchId());
        vo.setBatchItemId(candidate.getBatchItemId());
        vo.setBenchmarkTaskId(candidate.getBenchmarkTaskId());
        vo.setOptimizationTaskId(candidate.getOptimizationTaskId());
        vo.setSqlFingerprint(candidate.getSqlFingerprint());
        vo.setDatasourceCode(candidate.getDatasourceCode());
        vo.setStage(candidate.getStage());
        vo.setReportCode(candidate.getReportCode());
        vo.setCandidateType(candidate.getCandidateType().name());
        vo.setStatus(candidate.getStatus().name());
        vo.setConfidence(candidate.getConfidence());
        vo.setPriority(candidate.getPriority());
        vo.setEvidenceLevel(candidate.getEvidenceLevel().name());
        vo.setSchemaVersion(candidate.getSchemaVersion());
        vo.setCreatedBy(candidate.getCreatedBy());
        vo.setCreatedAt(candidate.getCreatedAt());
        vo.setUpdatedAt(candidate.getUpdatedAt());
        vo.setSourceEvidence(candidate.getSourceEvidence());
        vo.setIssueEvidence(candidate.getIssueEvidence());
        vo.setRuntimeEvidence(candidate.getRuntimeEvidence());
        vo.setBenefitEstimate(candidate.getBenefitEstimate());
        vo.setCostEstimate(candidate.getCostEstimate());
        vo.setRisk(candidate.getRisk());
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(IMPLEMENTATION_STAGE);
        return vo;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = requireContextTenant();
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private String requireContextTenant() {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = requireContextTenant();
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this acceleration candidate");
        }
    }

    private BizException invalidArgument(String field, String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            field + ": " + message
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(new String[] { first, second, third });
    }

    private String firstNonBlank(String first, String second, String third, String fourth, String fifth) {
        return firstNonBlank(new String[] { first, second, third, fourth, fifth });
    }

    private String firstNonBlank(String[] values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static final class NormalizedSource {
        private final GovernanceSourceType sourceType;
        private final GovernanceSourceKind sourceKind;
        private final String sourceId;
        private final EvidenceLevel evidenceLevel;

        private NormalizedSource(GovernanceSourceType sourceType,
                                 GovernanceSourceKind sourceKind,
                                 String sourceId,
                                 EvidenceLevel evidenceLevel) {
            this.sourceType = sourceType;
            this.sourceKind = sourceKind;
            this.sourceId = sourceId;
            this.evidenceLevel = evidenceLevel;
        }
    }
}
