package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.service.AccelerationRecommendationApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/recommendations")
public class AccelerationRecommendationController {

    private final AccelerationRecommendationApplicationService recommendationApplicationService;

    public AccelerationRecommendationController(AccelerationRecommendationApplicationService recommendationApplicationService) {
        this.recommendationApplicationService = recommendationApplicationService;
    }

    @GetMapping
    public List<AccelerationRecommendationVO> listRecommendations() {
        return recommendationApplicationService.listRecommendations();
    }

    @GetMapping("/{recommendationId}")
    public AccelerationRecommendationVO getRecommendation(@PathVariable("recommendationId") String recommendationId) {
        return recommendationApplicationService.getRecommendation(recommendationId);
    }
}
