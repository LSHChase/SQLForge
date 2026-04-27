package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.vo.RecommendationTraceVO;
import com.company.sqloptimization.application.service.RecommendationTraceApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/recommendations")
public class RecommendationTraceController {

    private final RecommendationTraceApplicationService recommendationTraceApplicationService;

    public RecommendationTraceController(RecommendationTraceApplicationService recommendationTraceApplicationService) {
        this.recommendationTraceApplicationService = recommendationTraceApplicationService;
    }

    @GetMapping("/{recommendationId}/trace")
    public RecommendationTraceVO trace(@PathVariable("recommendationId") String recommendationId) {
        return recommendationTraceApplicationService.trace(recommendationId);
    }
}
