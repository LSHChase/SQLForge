package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkParseResultTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTestSetRepository;
import com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationCombinedParseStatus;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseBatchItem;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseBatchStatus;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseResultClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseSqlIssueStatistic;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationStructureParseIssue;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationStructureParseStatus;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkParseResultTestSetApplicationService {

    private static final String CREATE_OPERATION = "BENCHMARK_TEST_SET_PARSE_GENERATE";
    private static final String RESOURCE_TYPE_TEST_SET = "BENCHMARK_ENGINE_TEST_SET";

    private final BenchmarkTestSetModelApplicationService modelService;
    private final BenchmarkTestSetRepository repository;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final SqlOptimizationParseResultClient sqlOptimizationParseResultClient;

    public BenchmarkParseResultTestSetApplicationService(BenchmarkTestSetModelApplicationService modelService,
                                                         BenchmarkTestSetRepository repository,
                                                         GovernanceCapabilityClient governanceCapabilityClient,
                                                         SqlOptimizationParseResultClient sqlOptimizationParseResultClient) {
        this.modelService = modelService;
        this.repository = repository;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.sqlOptimizationParseResultClient = sqlOptimizationParseResultClient;
    }

    public BenchmarkTestSetResponse createFromParseResults(BenchmarkParseResultTestSetCreateRequest request) {
        long start = System.currentTimeMillis();
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
        try {
            List<BenchmarkTestSetCase> cases;
            List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>(toSourceReferences(request.getTestSetSourceRefs()));
            List<BenchmarkTestSetLabel> labels = new ArrayList<BenchmarkTestSetLabel>(toLabels(request.getTestSetLabels()));
            if (hasText(request.getParseBatchId())) {
                cases = buildCasesFromParseBatch(request, refs, labels);
            } else {
                cases = buildCaseFromParseTask(request, refs, labels);
            }
            if (cases.isEmpty()) {
                throw invalidArgument("parseResultSelector", "No parse results matched the benchmark test-set generation filters");
            }
            BenchmarkTestSet testSet = modelService.buildGeneratedTestSet(
                request.getTenantId(),
                null,
                request.getTestSetName(),
                request.getTemplateId(),
                request.getTemplateType(),
                request.getTemplateVersion(),
                BenchmarkTestSetSource.PARSE_RESULT_GENERATION,
                labels,
                refs,
                cases,
                Instant.now(),
                RequestContext.getUserId()
            );
            assertAuthorization(testSet.getTenantId(), testSet.getTestSetId(), CREATE_OPERATION);
            repository.saveTestSet(testSet);
            BenchmarkTestSetResponse response = modelService.buildResponse(testSet);
            writeAudit(
                CREATE_OPERATION,
                testSet.getTestSetId(),
                testSet.getStatus().name(),
                System.currentTimeMillis() - start,
                buildRequestSummary(request),
                buildResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            writeAudit(
                CREATE_OPERATION,
                hasText(request.getParseBatchId()) ? request.getParseBatchId() : request.getParseTaskId(),
                "FAILED",
                System.currentTimeMillis() - start,
                buildRequestSummary(request),
                buildResponseSummary(null, ex.getMessage())
            );
            throw ex;
        }
    }

    private List<BenchmarkTestSetCase> buildCasesFromParseBatch(BenchmarkParseResultTestSetCreateRequest request,
                                                                List<BenchmarkSourceReference> refs,
                                                                List<BenchmarkTestSetLabel> labels) {
        SqlOptimizationParseBatchStatus batch = sqlOptimizationParseResultClient.getParseBatch(trimToNull(request.getParseBatchId()));
        if (batch == null) {
            throw invalidArgument("parseBatchId", "Parse batch was not found");
        }
        verifyTenantAccess(batch.getTenantId(), "Authenticated tenant cannot access this parse batch");
        Map<String, SqlOptimizationParseSqlIssueStatistic> importantUrgent = indexByItemId(
            sqlOptimizationParseResultClient.getImportantUrgentSqls(),
            batch.getBatchId()
        );
        List<BenchmarkTestSetCase> cases = new ArrayList<BenchmarkTestSetCase>();
        List<SqlOptimizationParseBatchItem> importedRecords =
            batch.getImportedRecords() == null ? Collections.<SqlOptimizationParseBatchItem>emptyList() : batch.getImportedRecords();
        int sequence = 0;
        for (SqlOptimizationParseBatchItem item : importedRecords) {
            SqlOptimizationParseSqlIssueStatistic priority = importantUrgent.get(item.getItemId());
            if (priority == null) {
                continue;
            }
            if (!matchesFilters(item.getIssueScenes(), item.getReportCode(), request)) {
                continue;
            }
            sequence++;
            appendUniqueRef(refs, BenchmarkSourceReferenceType.PARSE_TASK, item.getParseTaskId());
            appendUniqueRef(refs, BenchmarkSourceReferenceType.REPORT, item.getReportCode());
            appendUniqueLabel(labels, autoRiskLabel(priority.getImportant(), priority.getUrgent()));
            cases.add(
                buildCase(
                    Integer.valueOf(sequence),
                    item.getSequenceNumber(),
                    firstNonBlank(item.getReportCode(), "PARSE-BATCH-" + sequence),
                    item.getSqlText(),
                    item.getDatasourceCode(),
                    item.getReportCode(),
                    parseTags(priority, item.getIssueScenes()),
                    item.getStructureSyntaxStatus(),
                    JsonUtils.toJson(buildBatchRawEvidence(batch.getBatchId(), item, priority))
                )
            );
        }
        appendUniqueLabel(labels, BenchmarkTestSetLabelType.SOURCE, "PARSE_BATCH_IMPORTANT_URGENT");
        return cases;
    }

    private List<BenchmarkTestSetCase> buildCaseFromParseTask(BenchmarkParseResultTestSetCreateRequest request,
                                                              List<BenchmarkSourceReference> refs,
                                                              List<BenchmarkTestSetLabel> labels) {
        SqlOptimizationCombinedParseStatus status = sqlOptimizationParseResultClient.getCombinedParseStatus(trimToNull(request.getParseTaskId()));
        if (status == null || status.getStructureParse() == null) {
            throw invalidArgument("parseTaskId", "Combined parse task was not found");
        }
        SqlOptimizationStructureParseStatus structureParse = status.getStructureParse();
        if (!matchesFilters(extractIssueScenes(structureParse), null, request)) {
            return Collections.emptyList();
        }
        appendUniqueRef(refs, BenchmarkSourceReferenceType.PARSE_TASK, request.getParseTaskId());
        appendUniqueRef(refs, BenchmarkSourceReferenceType.SQL_FINGERPRINT, SqlFingerprintUtils.fingerprint(request.getSqlText()));
        appendUniqueLabel(labels, autoRiskLabel(structureParse.getImportant(), structureParse.getUrgent()));
        appendUniqueLabel(labels, BenchmarkTestSetLabelType.SOURCE, "COMBINED_PARSE");
        List<BenchmarkTestSetCase> cases = new ArrayList<BenchmarkTestSetCase>(1);
        cases.add(
            buildCase(
                Integer.valueOf(1),
                Integer.valueOf(1),
                "PARSE-TASK-" + trimToNull(request.getParseTaskId()),
                request.getSqlText(),
                null,
                null,
                parseTags(structureParse),
                structureParse.getSyntaxStatus(),
                JsonUtils.toJson(buildTaskRawEvidence(status))
            )
        );
        return cases;
    }

    private BenchmarkTestSetCase buildCase(Integer sequenceNumber,
                                           Integer sourceLineNumber,
                                           String caseName,
                                           String sqlText,
                                           String datasourceCode,
                                           String reportCode,
                                           List<String> tags,
                                           String syntaxStatus,
                                           String rawCaseDataJson) {
        String rejectionReason = null;
        if (!"VALID".equals(syntaxStatus)) {
            rejectionReason = "parse structure syntax status is not VALID";
        } else {
            rejectionReason = BenchmarkReadonlySqlSupport.validateReadonlySql(sqlText);
        }
        return new BenchmarkTestSetCase(
            UUID.randomUUID().toString(),
            null,
            sequenceNumber,
            sourceLineNumber,
            caseName,
            sqlText,
            hasText(sqlText) ? SqlFingerprintUtils.fingerprint(sqlText) : null,
            trimToNull(datasourceCode),
            trimToNull(reportCode),
            tags,
            null,
            rejectionReason == null ? BenchmarkTestSetCaseStatus.ACCEPTED : BenchmarkTestSetCaseStatus.REJECTED,
            rejectionReason,
            rawCaseDataJson
        );
    }

    private Map<String, SqlOptimizationParseSqlIssueStatistic> indexByItemId(List<SqlOptimizationParseSqlIssueStatistic> items,
                                                                             String batchId) {
        Map<String, SqlOptimizationParseSqlIssueStatistic> results = new LinkedHashMap<String, SqlOptimizationParseSqlIssueStatistic>();
        if (items == null) {
            return results;
        }
        for (SqlOptimizationParseSqlIssueStatistic item : items) {
            if (item == null || !batchId.equals(item.getBatchId())) {
                continue;
            }
            results.put(item.getItemId(), item);
        }
        return results;
    }

    private boolean matchesFilters(List<String> issueScenes,
                                   String reportCode,
                                   BenchmarkParseResultTestSetCreateRequest request) {
        if (request.getIncludeIssueScenes() != null && !request.getIncludeIssueScenes().isEmpty()) {
            boolean matched = false;
            for (String issueScene : issueScenes == null ? Collections.<String>emptyList() : issueScenes) {
                if (request.getIncludeIssueScenes().contains(issueScene)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        if (request.getIncludeReportCodes() != null && !request.getIncludeReportCodes().isEmpty()) {
            return request.getIncludeReportCodes().contains(reportCode);
        }
        return true;
    }

    private List<String> extractIssueScenes(SqlOptimizationStructureParseStatus structureParse) {
        if (structureParse == null || structureParse.getIssues() == null || structureParse.getIssues().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> issueScenes = new ArrayList<String>();
        for (SqlOptimizationStructureParseIssue issue : structureParse.getIssues()) {
            if (issue != null && hasText(issue.getIssueScene())) {
                issueScenes.add(issue.getIssueScene());
            }
        }
        return issueScenes;
    }

    private List<String> parseTags(SqlOptimizationParseSqlIssueStatistic priority, List<String> issueScenes) {
        List<String> tags = new ArrayList<String>();
        if (priority != null && hasText(priority.getHighestPriorityLevel())) {
            tags.add(priority.getHighestPriorityLevel());
        }
        if (priority != null && Boolean.TRUE.equals(priority.getImportant())) {
            tags.add("IMPORTANT");
        }
        if (priority != null && Boolean.TRUE.equals(priority.getUrgent())) {
            tags.add("URGENT");
        }
        if (issueScenes != null) {
            tags.addAll(issueScenes);
        }
        return deduplicate(tags);
    }

    private List<String> parseTags(SqlOptimizationStructureParseStatus structureParse) {
        List<String> tags = new ArrayList<String>();
        if (structureParse != null && hasText(structureParse.getPriorityLevel())) {
            tags.add(structureParse.getPriorityLevel());
        }
        if (structureParse != null && Boolean.TRUE.equals(structureParse.getImportant())) {
            tags.add("IMPORTANT");
        }
        if (structureParse != null && Boolean.TRUE.equals(structureParse.getUrgent())) {
            tags.add("URGENT");
        }
        if (structureParse != null && structureParse.getIssues() != null) {
            for (SqlOptimizationStructureParseIssue issue : structureParse.getIssues()) {
                if (issue != null && hasText(issue.getIssueScene())) {
                    tags.add(issue.getIssueScene());
                }
            }
        }
        return deduplicate(tags);
    }

    private List<String> deduplicate(List<String> items) {
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        for (String item : items) {
            if (hasText(item)) {
                unique.add(item);
            }
        }
        return new ArrayList<String>(unique);
    }

    private BenchmarkTestSetLabel autoRiskLabel(Boolean important, Boolean urgent) {
        if (Boolean.TRUE.equals(important) && Boolean.TRUE.equals(urgent)) {
            return new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.RISK, "IMPORTANT_URGENT");
        }
        if (Boolean.TRUE.equals(important)) {
            return new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.RISK, "IMPORTANT");
        }
        if (Boolean.TRUE.equals(urgent)) {
            return new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.RISK, "URGENT");
        }
        return new BenchmarkTestSetLabel(BenchmarkTestSetLabelType.RISK, "NORMAL");
    }

    private void appendUniqueRef(List<BenchmarkSourceReference> refs, BenchmarkSourceReferenceType type, String referenceId) {
        if (!hasText(referenceId)) {
            return;
        }
        for (BenchmarkSourceReference ref : refs) {
            if (ref.getType() == type && referenceId.equals(ref.getReferenceId())) {
                return;
            }
        }
        refs.add(new BenchmarkSourceReference(type, referenceId));
    }

    private void appendUniqueLabel(List<BenchmarkTestSetLabel> labels, BenchmarkTestSetLabelType type, String value) {
        if (!hasText(value)) {
            return;
        }
        for (BenchmarkTestSetLabel label : labels) {
            if (label.getType() == type && value.equals(label.getValue())) {
                return;
            }
        }
        labels.add(new BenchmarkTestSetLabel(type, value));
    }

    private void appendUniqueLabel(List<BenchmarkTestSetLabel> labels, BenchmarkTestSetLabel label) {
        if (label == null) {
            return;
        }
        appendUniqueLabel(labels, label.getType(), label.getValue());
    }

    private Map<String, Object> buildBatchRawEvidence(String batchId,
                                                      SqlOptimizationParseBatchItem item,
                                                      SqlOptimizationParseSqlIssueStatistic priority) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("parseBatchId", batchId);
        evidence.put("itemId", item.getItemId());
        evidence.put("parseTaskId", item.getParseTaskId());
        evidence.put("reportCode", item.getReportCode());
        evidence.put("datasourceCode", item.getDatasourceCode());
        evidence.put("issueScenes", item.getIssueScenes());
        evidence.put("highestPriorityLevel", priority == null ? null : priority.getHighestPriorityLevel());
        evidence.put("important", priority == null ? null : priority.getImportant());
        evidence.put("urgent", priority == null ? null : priority.getUrgent());
        return evidence;
    }

    private Map<String, Object> buildTaskRawEvidence(SqlOptimizationCombinedParseStatus status) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("parseTaskId", status.getParseTaskId());
        evidence.put("status", status.getStatus());
        evidence.put("structureSyntaxStatus", status.getStructureParse() == null ? null : status.getStructureParse().getSyntaxStatus());
        evidence.put("priorityLevel", status.getStructureParse() == null ? null : status.getStructureParse().getPriorityLevel());
        evidence.put("important", status.getStructureParse() == null ? null : status.getStructureParse().getImportant());
        evidence.put("urgent", status.getStructureParse() == null ? null : status.getStructureParse().getUrgent());
        return evidence;
    }

    private List<BenchmarkSourceReference> toSourceReferences(List<BenchmarkSourceReferenceDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>(items.size());
        for (BenchmarkSourceReferenceDTO item : items) {
            refs.add(new BenchmarkSourceReference(item.getType(), item.getReferenceId()));
        }
        return refs;
    }

    private List<BenchmarkTestSetLabel> toLabels(List<BenchmarkTestSetLabelDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetLabel> labels = new ArrayList<BenchmarkTestSetLabel>(items.size());
        for (BenchmarkTestSetLabelDTO item : items) {
            labels.add(new BenchmarkTestSetLabel(item.getType(), item.getValue()));
        }
        return labels;
    }

    private void assertAuthorization(String tenantId, String resourceId, String operationCode) {
        governanceCapabilityClient.assertAuthorization(
            tenantId,
            DataSourceTypeEnum.HETU,
            RESOURCE_TYPE_TEST_SET,
            resourceId,
            operationCode
        );
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId, String message) {
        String contextTenantId = RequestContext.getTenantId();
        if (!hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException(message);
        }
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(
            ErrorCodeConstants.BENCHMARK_TASK_INVALID,
            HttpStatus.BAD_REQUEST,
            "Invalid parse-to-benchmark request " + fieldName + ": " + message
        );
    }

    private void writeAudit(String operationCode,
                            String resourceId,
                            String resultStatus,
                            long elapsedMs,
                            String requestParams,
                            String responseSummary) {
        governanceCapabilityClient.writeAudit(
            new BenchmarkAuditRecord(
                operationCode,
                RESOURCE_TYPE_TEST_SET,
                resourceId,
                resultStatus,
                elapsedMs,
                null,
                null,
                null,
                null,
                null,
                requestParams,
                responseSummary
            )
        );
    }

    private String buildRequestSummary(BenchmarkParseResultTestSetCreateRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", request.getTenantId());
        payload.put("testSetName", request.getTestSetName());
        payload.put("parseTaskId", trimToNull(request.getParseTaskId()));
        payload.put("parseBatchId", trimToNull(request.getParseBatchId()));
        payload.put("templateId", trimToNull(request.getTemplateId()));
        payload.put("templateType", request.getTemplateType() == null ? null : request.getTemplateType().name());
        payload.put("includeIssueScenes", request.getIncludeIssueScenes());
        payload.put("includeReportCodes", request.getIncludeReportCodes());
        return JsonUtils.toJson(payload);
    }

    private String buildResponseSummary(BenchmarkTestSetResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getStatus().name());
        payload.put("acceptedCases", response == null ? null : response.getAcceptedCases());
        payload.put("rejectedCases", response == null ? null : response.getRejectedCases());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
