package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.CombinedParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CombinedParseApplicationService {

    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;
    private final Map<String, CombinedParseStatusVO> parseStates = new ConcurrentHashMap<String, CombinedParseStatusVO>();

    public CombinedParseApplicationService(StructureParseApplicationService structureParseApplicationService,
                                           AccessParseApplicationService accessParseApplicationService) {
        this.structureParseApplicationService = structureParseApplicationService;
        this.accessParseApplicationService = accessParseApplicationService;
    }

    public CombinedParseStatusVO submit(CombinedParseRequest request) {
        StructureParseResponseVO structureParse = structureParseApplicationService.parse(request);
        CombinedParseStatusVO status = new CombinedParseStatusVO();
        status.setParseTaskId(structureParse.getParseTaskId());
        status.setStructureParse(structureParse);
        status.setStatus("STRUCTURE_SUCCEEDED");
        parseStates.put(status.getParseTaskId(), status);

        if (!"VALID".equals(structureParse.getSyntaxStatus())) {
            status.setStatus("FAILED");
            status.setDegradeReason("STRUCTURE_PARSE_INVALID");
            return status;
        }

        status.setStatus("ACCESS_PARSING");
        scheduleAccessParse(status.getParseTaskId(), request);
        return status;
    }

    public CombinedParseStatusVO getStatus(String parseTaskId) {
        return parseStates.get(parseTaskId);
    }

    private void scheduleAccessParse(String parseTaskId, CombinedParseRequest request) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
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
                } else if ("SKIPPED".equals(accessParse.getServiceStatus())
                    || "UNAVAILABLE".equals(accessParse.getServiceStatus())
                    || "FAILED".equals(accessParse.getConnectionStatus())
                    || "UNAVAILABLE".equals(accessParse.getConnectionStatus())
                    || "SKIPPED".equals(accessParse.getConnectionStatus())) {
                    current.setStatus("PARTIAL_SUCCEEDED");
                    current.setDegradeReason(accessParse.getDegradeReason());
                } else {
                    current.setStatus("PARTIAL_SUCCEEDED");
                    current.setDegradeReason(accessParse.getDegradeReason());
                }
            }
        });
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
