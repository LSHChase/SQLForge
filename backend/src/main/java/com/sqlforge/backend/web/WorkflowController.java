package com.sqlforge.backend.web;

import com.sqlforge.backend.service.BiReleaseWorkflowService;
import com.sqlforge.backend.service.CapacityPlanWorkflowService;
import com.sqlforge.backend.service.PlanStabilityWorkflowService;
import com.sqlforge.backend.web.dto.BiReleaseRequest;
import com.sqlforge.backend.web.dto.CapacityPlanRequest;
import com.sqlforge.backend.web.dto.PlanStabilityRequest;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final BiReleaseWorkflowService biReleaseWorkflowService;
    private final CapacityPlanWorkflowService capacityPlanWorkflowService;
    private final PlanStabilityWorkflowService planStabilityWorkflowService;

    public WorkflowController(
        BiReleaseWorkflowService biReleaseWorkflowService,
        CapacityPlanWorkflowService capacityPlanWorkflowService,
        PlanStabilityWorkflowService planStabilityWorkflowService
    ) {
        this.biReleaseWorkflowService = biReleaseWorkflowService;
        this.capacityPlanWorkflowService = capacityPlanWorkflowService;
        this.planStabilityWorkflowService = planStabilityWorkflowService;
    }

    @PostMapping("/bi-release")
    public Map<String, Object> biRelease(@Valid @RequestBody BiReleaseRequest request) {
        return biReleaseWorkflowService.execute(request);
    }

    @PostMapping("/capacity-plan")
    public Map<String, Object> capacityPlan(@Valid @RequestBody CapacityPlanRequest request) {
        return capacityPlanWorkflowService.execute(request);
    }

    @PostMapping("/plan-stability")
    public Map<String, Object> planStability(@Valid @RequestBody PlanStabilityRequest request) {
        return planStabilityWorkflowService.execute(request);
    }
}
