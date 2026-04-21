package com.company.benchmarkengine.application.controller;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.application.service.BenchmarkTaskApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark-engine")
public class BenchmarkTaskController {

    private final BenchmarkTaskApplicationService benchmarkTaskApplicationService;

    public BenchmarkTaskController(BenchmarkTaskApplicationService benchmarkTaskApplicationService) {
        this.benchmarkTaskApplicationService = benchmarkTaskApplicationService;
    }

    @PostMapping("/tasks")
    public BenchmarkTaskSubmitResponse submitTask(@Valid @RequestBody BenchmarkTaskSubmitRequest request) {
        return benchmarkTaskApplicationService.submitTask(request);
    }

    @GetMapping("/tasks/{taskId}")
    public BenchmarkTaskStatusResponse getTaskStatus(@PathVariable("taskId") String taskId) {
        return benchmarkTaskApplicationService.getTaskStatus(taskId);
    }
}
