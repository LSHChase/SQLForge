package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.AccelerationPlanActionRequest;
import com.company.sqloptimization.application.controller.dto.AccelerationPlanSubmitRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanStatusResponse;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanSubmitResponse;
import com.company.sqloptimization.application.service.AccelerationPlanApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/acceleration-plans")
public class AccelerationPlanController {

    private final AccelerationPlanApplicationService accelerationPlanApplicationService;

    public AccelerationPlanController(AccelerationPlanApplicationService accelerationPlanApplicationService) {
        this.accelerationPlanApplicationService = accelerationPlanApplicationService;
    }

    @PostMapping
    public AccelerationPlanSubmitResponse submitPlan(@Valid @RequestBody AccelerationPlanSubmitRequest request) {
        return accelerationPlanApplicationService.submitPlan(request);
    }

    @GetMapping("/{planId}")
    public AccelerationPlanStatusResponse getPlan(@PathVariable("planId") String planId) {
        return accelerationPlanApplicationService.getPlanStatus(planId);
    }

    @PostMapping("/{planId}/activate")
    public AccelerationPlanStatusResponse activatePlan(@PathVariable("planId") String planId,
                                                    @RequestBody(required = false) AccelerationPlanActionRequest request) {
        return accelerationPlanApplicationService.activatePlan(planId, request);
    }

    @PostMapping("/{planId}/pause")
    public AccelerationPlanStatusResponse pausePlan(@PathVariable("planId") String planId,
                                                       @RequestBody(required = false) AccelerationPlanActionRequest request) {
        return accelerationPlanApplicationService.pausePlan(planId, request);
    }
}
