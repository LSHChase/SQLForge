package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.CombinedParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseConclusionVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqlforge.common.context.RequestContext;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CombinedParseApplicationService {

    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;
    private final SqlParseHistoryApplicationService sqlParseHistoryApplicationService;
    private final Map<String, CombinedParseStatusVO> parseStates = new ConcurrentHashMap<String, CombinedParseStatusVO>();

    public CombinedParseApplicationService(StructureParseApplicationService structureParseApplicationService,
                                           AccessParseApplicationService accessParseApplicationService,
                                           SqlParseHistoryApplicationService sqlParseHistoryApplicationService) {
        this.structureParseApplicationService = structureParseApplicationService;
        this.accessParseApplicationService = accessParseApplicationService;
        this.sqlParseHistoryApplicationService = sqlParseHistoryApplicationService;
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
            if ("PARTIAL_SUCCESS".equals(structureParse.getAnalysisStatus())) {
                status.setStatus("PARTIAL_SUCCEEDED");
            } else {
                status.setStatus("FAILED");
            }
            status.setDegradeReason("STRUCTURE_PARSE_INVALID");
            appendHistory(status, status.getStatus(), "Structure parse returned INVALID and access parse was not started.");
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
                    boolean accessSucceeded = "AVAILABLE".equals(accessParse.getServiceStatus())
                        && "CONNECTED".equals(accessParse.getConnectionStatus());
                    boolean analysisSucceeded = current.getStructureParse() != null
                        && "SUCCESS".equals(current.getStructureParse().getAnalysisStatus());
                    if (accessSucceeded && analysisSucceeded) {
                        current.setStatus("ACCESS_SUCCEEDED");
                        current.setDegradeReason(null);
                        appendHistory(current, "ACCESS_SUCCEEDED", "Access parse completed with provider reachability evidence.");
                    } else if (accessSucceeded) {
                        current.setStatus("PARTIAL_SUCCEEDED");
                        current.setDegradeReason(resolvePlanDegradeReason(current));
                        appendHistory(
                            current,
                            "PARTIAL_SUCCEEDED",
                            "Access parse succeeded but structure/plan analysis ended with a degraded status."
                        );
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
            SqlParseHistoryWriteResult writeResult = sqlParseHistoryApplicationService.writeCombinedHistory(
                status,
                request,
                normalizeHistoryResultStatus(status.getStatus())
            );
            if (writeResult == null) {
                status.setHistoryPersisted(Boolean.FALSE);
                status.setHistoryPersistenceStatus("NO_RESPONSE");
                return;
            }
            status.setHistoryId(writeResult.getParseHistoryId());
            status.setHistoryPersisted(writeResult.getPersisted());
            status.setHistoryPersistenceStatus(writeResult.getPersistenceStatus());
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
            if (status.getStructureParse() != null && !"VALID".equals(status.getStructureParse().getSyntaxStatus())) {
                conclusion.setSummary("Structure parse failed, but another parse evidence channel is available.");
                conclusion.setRecommendedAction("Review the plan evidence and fix SQL syntax before rerunning full parse.");
            } else {
                conclusion.setSummary("Structure parse succeeded, but access parse evidence is degraded or unavailable.");
                conclusion.setRecommendedAction("Use structure evidence now and review datasource availability before rerunning access parse.");
            }
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

    private String resolvePlanDegradeReason(CombinedParseStatusVO status) {
        if (status == null
            || status.getStructureParse() == null
            || status.getStructureParse().getPlanAnalysis() == null) {
            return null;
        }
        return status.getStructureParse().getPlanAnalysis().getFailureReason();
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
