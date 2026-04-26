package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.AccelerationPlanActionRequest;
import com.company.sqloptimization.application.controller.dto.AccelerationPlanApprovalRequest;
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

    @PostMapping("/{planId}/approval")
    public AccelerationPlanStatusResponse reviewPlan(@PathVariable("planId") String planId,
                                                     @Valid @RequestBody AccelerationPlanApprovalRequest request) {
        return accelerationPlanApplicationService.reviewPlan(planId, request);
    }

    @PostMapping("/{planId}/apply")
    public AccelerationPlanStatusResponse applyPlan(@PathVariable("planId") String planId,
                                                    @RequestBody(required = false) AccelerationPlanActionRequest request) {
        return accelerationPlanApplicationService.applyPlan(planId, request);
    }

    @PostMapping("/{planId}/verify")
    public AccelerationPlanStatusResponse verifyPlan(@PathVariable("planId") String planId,
                                                     @RequestBody(required = false) AccelerationPlanActionRequest request) {
        return accelerationPlanApplicationService.verifyPlan(planId, request);
    }

    @PostMapping("/{planId}/rollback")
    public AccelerationPlanStatusResponse rollbackPlan(@PathVariable("planId") String planId,
                                                       @RequestBody(required = false) AccelerationPlanActionRequest request) {
        return accelerationPlanApplicationService.rollbackPlan(planId, request);
    }
}
