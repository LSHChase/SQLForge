package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.application.controller.vo.RecommendationPageVO;
import com.company.sqloptimization.application.service.AccelerationRecommendationApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/page")
    public RecommendationPageVO listRecommendationPage(
        @RequestParam(value = "pageNo", required = false) Integer pageNo,
        @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortOrder", required = false) String sortOrder,
        @RequestParam(value = "recommendationType", required = false) String recommendationType,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "benefitLevel", required = false) String benefitLevel,
        @RequestParam(value = "riskLevel", required = false) String riskLevel,
        @RequestParam(value = "validationStatus", required = false) String validationStatus,
        @RequestParam(value = "requiresDispatch", required = false) Boolean requiresDispatch,
        @RequestParam(value = "manualReviewRequired", required = false) Boolean manualReviewRequired) {
        return recommendationApplicationService.listRecommendationPage(
            recommendationType,
            status,
            benefitLevel,
            riskLevel,
            validationStatus,
            requiresDispatch,
            manualReviewRequired,
            sortBy,
            sortOrder,
            pageNo,
            pageSize
        );
    }

    @GetMapping("/{recommendationId}")
    public AccelerationRecommendationVO getRecommendation(@PathVariable("recommendationId") String recommendationId) {
        return recommendationApplicationService.getRecommendation(recommendationId);
    }

    @GetMapping("/{recommendationId}/diff")
    public RecommendationDiffVO getRecommendationDiff(@PathVariable("recommendationId") String recommendationId) {
        return recommendationApplicationService.getRecommendationDiff(recommendationId);
    }
}
