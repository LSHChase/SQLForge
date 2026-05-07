package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.CombinedParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseConclusionVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteResponse;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CombinedParseApplicationService {

    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CombinedParseStatusVO> parseStates = new ConcurrentHashMap<String, CombinedParseStatusVO>();

    public CombinedParseApplicationService(StructureParseApplicationService structureParseApplicationService,
                                           AccessParseApplicationService accessParseApplicationService,
                                           GovernanceCapabilityClient governanceCapabilityClient) {
        this.structureParseApplicationService = structureParseApplicationService;
        this.accessParseApplicationService = accessParseApplicationService;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public CombinedParseStatusVO submit(CombinedParseRequest request) {
        request.setHistoryWriteEnabled(Boolean.FALSE);
        StructureParseResponseVO structureParse = structureParseApplicationService.parse(request);
        CombinedParseStatusVO status = new CombinedParseStatusVO();
        status.setParseTaskId(structureParse.getParseTaskId());
        status.setStructureParse(structureParse);
        status.setStatusHistory(new ArrayList<CombinedParseStatusHistoryVO>());
        status.setStatus("STRUCTURE_SUCCEEDED");
        appendHistory(status, "STRUCTURE_SUCCEEDED", "Structure parse returned immediately.");
        status.setConclusion(buildConclusion(status));
        parseStates.put(status.getParseTaskId(), status);

        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            status.setStatus("FAILED");
            status.setDegradeReason("STRUCTURE_PARSE_INVALID");
            appendHistory(status, "FAILED", "Structure parse returned INVALID and access parse was not started.");
            status.setConclusion(buildConclusion(status));
            writeParseHistory(status, request);
            return status;
        }

        status.setStatus("ACCESS_PARSING");
        appendHistory(status, "ACCESS_PARSING", "Access parse follow-up was scheduled after structure success.");
        status.setConclusion(buildConclusion(status));
        writeParseHistory(status, request);
        scheduleAccessParse(status.getParseTaskId(), request, RequestContext.snapshot());
        return status;
    }

    public CombinedParseStatusVO getStatus(String parseTaskId) {
        return parseStates.get(parseTaskId);
    }

    private void scheduleAccessParse(String parseTaskId,
                                     CombinedParseRequest request,
                                     RequestContext.ContextValue requestContext) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestContext.restore(requestContext);
                    delayBeforeAccessParse();
                    AccessParseRequest accessRequest = new AccessParseRequest();
                    accessRequest.setSqlText(request.getSqlText());
                    accessRequest.setSqlTemplateText(request.getSqlTemplateText());
                    accessRequest.setBindParameters(request.getBindParameters());
                    accessRequest.setBindingMode(request.getBindingMode());
                    accessRequest.setDatasourceCode(request.getDatasourceCode());
                    accessRequest.setCommentContext(request.getCommentContext());
                    accessRequest.setConnectionRequired(request.getConnectionRequired());

                    AccessParseResponseVO accessParse = accessParseApplicationService.parseAccess(accessRequest, parseTaskId);
                    CombinedParseStatusVO current = parseStates.get(parseTaskId);
                    if (current == null) {
                        return;
                    }
                    current.setAccessParse(accessParse);
                    if ("AVAILABLE".equals(accessParse.getServiceStatus()) && "CONNECTED".equals(accessParse.getConnectionStatus())) {
                        current.setStatus("ACCESS_SUCCEEDED");
                        current.setDegradeReason(null);
                        appendHistory(current, "ACCESS_SUCCEEDED", "Access parse completed with provider reachability evidence.");
                    } else if ("SKIPPED".equals(accessParse.getServiceStatus())
                        || "UNAVAILABLE".equals(accessParse.getServiceStatus())
                        || "FAILED".equals(accessParse.getConnectionStatus())
                        || "UNAVAILABLE".equals(accessParse.getConnectionStatus())
                        || "SKIPPED".equals(accessParse.getConnectionStatus())) {
                        current.setStatus("PARTIAL_SUCCEEDED");
                        current.setDegradeReason(accessParse.getDegradeReason());
                        appendHistory(
                            current,
                            "PARTIAL_SUCCEEDED",
                            "Structure parse succeeded but access parse ended with degraded status: " + accessParse.getDegradeReason()
                        );
                    } else {
                        current.setStatus("PARTIAL_SUCCEEDED");
                        current.setDegradeReason(accessParse.getDegradeReason());
                        appendHistory(current, "PARTIAL_SUCCEEDED", "Access parse completed with a non-terminal degraded state.");
                    }
                    current.setConclusion(buildConclusion(current));
                    writeParseHistory(current, request);
                } finally {
                    RequestContext.clear();
                }
            }
        });
    }

    private void writeParseHistory(CombinedParseStatusVO status, CombinedParseRequest request) {
        try {
            GovernanceParseHistoryWriteRequest historyRequest = new GovernanceParseHistoryWriteRequest();
            historyRequest.setParseTaskId(status.getParseTaskId());
            historyRequest.setSqlFingerprint(
                status.getStructureParse() == null ? null : status.getStructureParse().getSqlFingerprint()
            );
            historyRequest.setDatasourceCode(request.getDatasourceCode());
            historyRequest.setDatasourceType("AUTO");
            historyRequest.setSqlText(request.getSqlText());
            historyRequest.setSqlTemplateText(request.getSqlTemplateText());
            historyRequest.setBindingMode(request.getBindingMode());
            historyRequest.setResultStatus(normalizeHistoryResultStatus(status.getStatus()));
            historyRequest.setResultSummaryJson(toJson(status.getConclusion()));
            historyRequest.setResultPayloadJson(toJson(status));
            historyRequest.setQueryContextJson(toJson(request.getCommentContext()));
            historyRequest.setLogicalObjectHitsJson(toJson(
                status.getStructureParse() == null ? null : status.getStructureParse().getLogicalObjectHits()
            ));
            historyRequest.setSubmittedAt(Instant.now().toString());
            GovernanceParseHistoryWriteResponse response = governanceCapabilityClient.writeParseHistory(historyRequest);
            if (response != null) {
                status.setHistoryId(response.getHistoryId());
                status.setHistoryPersisted(Boolean.TRUE);
                status.setHistoryPersistenceStatus("SAVED");
                return;
            }
            status.setHistoryPersisted(Boolean.FALSE);
            status.setHistoryPersistenceStatus("NO_RESPONSE");
        } catch (RuntimeException ex) {
            status.setHistoryPersisted(Boolean.FALSE);
            status.setHistoryPersistenceStatus("WRITE_FAILED");
        }
    }

    private String normalizeHistoryResultStatus(String status) {
        if ("ACCESS_SUCCEEDED".equals(status) || "STRUCTURE_SUCCEEDED".equals(status)) {
            return "SUCCESS";
        }
        if ("PARTIAL_SUCCEEDED".equals(status) || "ACCESS_PARSING".equals(status)) {
            return "PARTIAL";
        }
        if ("FAILED".equals(status)) {
            return "FAILED";
        }
        return "PARTIAL";
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private CombinedParseConclusionVO buildConclusion(CombinedParseStatusVO status) {
        CombinedParseConclusionVO conclusion = new CombinedParseConclusionVO();
        boolean structureAvailable = status.getStructureParse() != null;
        boolean accessAvailable = status.getAccessParse() != null
            && "AVAILABLE".equals(status.getAccessParse().getServiceStatus())
            && "CONNECTED".equals(status.getAccessParse().getConnectionStatus());
        conclusion.setStructureAvailable(Boolean.valueOf(structureAvailable));
        conclusion.setAccessAvailable(Boolean.valueOf(accessAvailable));
        conclusion.setDegradeReason(status.getDegradeReason());
        if ("FAILED".equals(status.getStatus())) {
            conclusion.setOverallStatus("FAILED");
            conclusion.setSummary("Structure parse returned a failure-grade result, so access parse evidence is unavailable.");
            conclusion.setRecommendedAction("Fix SQL syntax or unsupported structure issues before retrying parse.");
            return conclusion;
        }
        if ("PARTIAL_SUCCEEDED".equals(status.getStatus())) {
            conclusion.setOverallStatus("PARTIAL_SUCCESS");
            conclusion.setSummary("Structure parse succeeded, but access parse evidence is degraded or unavailable.");
            conclusion.setRecommendedAction("Use structure evidence now and review datasource availability before rerunning access parse.");
            return conclusion;
        }
        if ("ACCESS_SUCCEEDED".equals(status.getStatus())) {
            conclusion.setOverallStatus("SUCCESS");
            conclusion.setSummary("Structure and access parse evidence are both available.");
            conclusion.setRecommendedAction("Use the combined parse result as the baseline for route, optimization, and history drill-through.");
            return conclusion;
        }
        conclusion.setOverallStatus("WAITING");
        conclusion.setSummary("Structure parse is ready and access parse follow-up is still running.");
        conclusion.setRecommendedAction("Poll the combined parse status until access parse reaches a terminal state.");
        return conclusion;
    }

    private void appendHistory(CombinedParseStatusVO status, String state, String note) {
        if (status.getStatusHistory() == null) {
            status.setStatusHistory(new ArrayList<CombinedParseStatusHistoryVO>());
        }
        CombinedParseStatusHistoryVO item = new CombinedParseStatusHistoryVO();
        item.setStatus(state);
        item.setNote(note);
        item.setOccurredAtEpochMs(Long.valueOf(System.currentTimeMillis()));
        status.getStatusHistory().add(item);
    }

    private void delayBeforeAccessParse() {
        try {
            Thread.sleep(40L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Combined parse access follow-up interrupted", ex);
        }
    }
}
