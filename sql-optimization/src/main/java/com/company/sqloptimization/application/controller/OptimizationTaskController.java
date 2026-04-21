package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.application.service.OptimizationTaskApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization")
public class OptimizationTaskController {

    private final OptimizationTaskApplicationService optimizationTaskApplicationService;

    public OptimizationTaskController(OptimizationTaskApplicationService optimizationTaskApplicationService) {
        this.optimizationTaskApplicationService = optimizationTaskApplicationService;
    }

    @PostMapping("/tasks")
    public OptimizationTaskSubmitResponse submitTask(@Valid @RequestBody OptimizationTaskSubmitRequest request) {
        return optimizationTaskApplicationService.submitTask(request);
    }

    @GetMapping("/tasks/{taskId}")
    public OptimizationTaskStatusResponse getTaskStatus(@PathVariable("taskId") String taskId) {
        return optimizationTaskApplicationService.getTaskStatus(taskId);
    }
}
