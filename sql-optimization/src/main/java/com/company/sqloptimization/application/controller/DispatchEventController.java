package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.DispatchEventActionRequest;
import com.company.sqloptimization.application.controller.dto.DispatchRecommendationRequest;
import com.company.sqloptimization.application.controller.vo.DispatchCollaborationContractVO;
import com.company.sqloptimization.application.controller.vo.DispatchEventVO;
import com.company.sqloptimization.application.service.DispatchEventApplicationService;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization")
public class DispatchEventController {

    private final DispatchEventApplicationService dispatchEventApplicationService;

    public DispatchEventController(DispatchEventApplicationService dispatchEventApplicationService) {
        this.dispatchEventApplicationService = dispatchEventApplicationService;
    }

    @PostMapping("/recommendations/{recommendationId}/dispatch")
    public DispatchEventVO dispatchRecommendation(@PathVariable("recommendationId") String recommendationId,
                                                  @RequestBody(required = false) DispatchRecommendationRequest request) {
        return dispatchEventApplicationService.dispatchRecommendation(recommendationId, request);
    }

    @GetMapping("/dispatch-events")
    public List<DispatchEventVO> listDispatchEvents(@RequestParam(value = "status", required = false) DispatchEventStatus status) {
        return dispatchEventApplicationService.listEvents(status);
    }

    @GetMapping("/dispatch-contract")
    public DispatchCollaborationContractVO getDispatchContract() {
        return dispatchEventApplicationService.getCollaborationContract();
    }

    @GetMapping("/dispatch-events/{dispatchEventId}")
    public DispatchEventVO getDispatchEvent(@PathVariable("dispatchEventId") String dispatchEventId) {
        return dispatchEventApplicationService.getEvent(dispatchEventId);
    }

    @PostMapping("/dispatch-events/{dispatchEventId}/pull")
    public DispatchEventVO markPulled(@PathVariable("dispatchEventId") String dispatchEventId) {
        return dispatchEventApplicationService.markPulled(dispatchEventId);
    }

    @PostMapping("/dispatch-events/{dispatchEventId}/ack")
    public DispatchEventVO ack(@PathVariable("dispatchEventId") String dispatchEventId,
                               @RequestBody(required = false) DispatchEventActionRequest request) {
        return dispatchEventApplicationService.ack(dispatchEventId, request);
    }

    @PostMapping("/dispatch-events/{dispatchEventId}/fail")
    public DispatchEventVO fail(@PathVariable("dispatchEventId") String dispatchEventId,
                                @RequestBody(required = false) DispatchEventActionRequest request) {
        return dispatchEventApplicationService.fail(dispatchEventId, request);
    }
}
