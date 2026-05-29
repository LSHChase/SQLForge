package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.CombinedParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
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
    private final CombinedParseConclusionBuilder conclusionBuilder = new CombinedParseConclusionBuilder();
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
        appendHistory(status, "STRUCTURE_SUCCEEDED", "结构解析已即时返回。");
        status.setConclusion(conclusionBuilder.build(status));
        parseStates.put(status.getParseTaskId(), status);

        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            if ("PARTIAL_SUCCESS".equals(structureParse.getAnalysisStatus())) {
                status.setStatus("PARTIAL_SUCCEEDED");
            } else {
                status.setStatus("FAILED");
            }
            status.setDegradeReason("STRUCTURE_PARSE_INVALID");
            appendHistory(status, status.getStatus(), "结构解析返回 INVALID，未启动访问解析。");
            status.setConclusion(conclusionBuilder.build(status));
            writeParseHistory(status, request);
            return status;
        }

        status.setStatus("ACCESS_PARSING");
        appendHistory(status, "ACCESS_PARSING", "结构解析成功后已调度访问解析跟进流程。");
        status.setConclusion(conclusionBuilder.build(status));
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
                        appendHistory(current, "ACCESS_SUCCEEDED", "访问解析已完成，并返回 provider 可达性证据。");
                    } else if (accessSucceeded) {
                        current.setStatus("PARTIAL_SUCCEEDED");
                        current.setDegradeReason(resolvePlanDegradeReason(current));
                        appendHistory(
                            current,
                            "PARTIAL_SUCCEEDED",
                            "访问解析成功，但结构或计划分析以降级状态结束。"
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
                            "结构解析成功，但访问解析以降级状态结束：" + accessParse.getDegradeReason()
                        );
                    } else {
                        current.setStatus("PARTIAL_SUCCEEDED");
                        current.setDegradeReason(accessParse.getDegradeReason());
                        appendHistory(current, "PARTIAL_SUCCEEDED", "访问解析完成，但仍处于非终态降级状态。");
                    }
                    current.setConclusion(conclusionBuilder.build(current));
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
            throw new IllegalStateException("组合解析访问跟进流程被中断", ex);
        }
    }
}
