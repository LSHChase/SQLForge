package com.company.benchmarkengine.application.controller;

import com.company.benchmarkengine.application.controller.dto.BenchmarkRecommendationComparisonCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationComparisonResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.application.service.BenchmarkRecommendationComparisonApplicationService;
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
    private final BenchmarkRecommendationComparisonApplicationService benchmarkRecommendationComparisonApplicationService;

    public BenchmarkTaskController(BenchmarkTaskApplicationService benchmarkTaskApplicationService,
                                   BenchmarkRecommendationComparisonApplicationService benchmarkRecommendationComparisonApplicationService) {
        this.benchmarkTaskApplicationService = benchmarkTaskApplicationService;
        this.benchmarkRecommendationComparisonApplicationService = benchmarkRecommendationComparisonApplicationService;
    }

    @PostMapping("/tasks")
    public BenchmarkTaskSubmitResponse submitTask(@Valid @RequestBody BenchmarkTaskSubmitRequest request) {
        return benchmarkTaskApplicationService.submitTask(request);
    }

    @PostMapping("/tasks/recommendations/{recommendationId}/comparison")
    public BenchmarkRecommendationComparisonResponse submitRecommendationComparison(
        @PathVariable("recommendationId") String recommendationId,
        @Valid @RequestBody BenchmarkRecommendationComparisonCreateRequest request
    ) {
        return benchmarkRecommendationComparisonApplicationService.submitComparisonBenchmark(recommendationId, request);
    }

    @GetMapping("/tasks/{taskId}")
    public BenchmarkTaskStatusResponse getTaskStatus(@PathVariable("taskId") String taskId) {
        return benchmarkTaskApplicationService.getTaskStatus(taskId);
    }
}
