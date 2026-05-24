package com.company.sqloptimization.application.controller;

import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import com.company.sqloptimization.application.controller.dto.MaterializedViewCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.application.controller.vo.RecommendationPageVO;
import com.company.sqloptimization.application.service.AccelerationRecommendationApplicationService;
import com.company.sqloptimization.application.service.MaterializedViewCreateApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/recommendations")
public class AccelerationRecommendationController {

    private final AccelerationRecommendationApplicationService recommendationApplicationService;
    private final MaterializedViewCreateApplicationService materializedViewCreateApplicationService;

    public AccelerationRecommendationController(AccelerationRecommendationApplicationService recommendationApplicationService,
                                                MaterializedViewCreateApplicationService materializedViewCreateApplicationService) {
        this.recommendationApplicationService = recommendationApplicationService;
        this.materializedViewCreateApplicationService = materializedViewCreateApplicationService;
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
        @RequestParam(value = "manualReviewRequired", required = false) Boolean manualReviewRequired,
        @RequestParam(value = "sourceType", required = false) String sourceType,
        @RequestParam(value = "sourceKind", required = false) String sourceKind,
        @RequestParam(value = "sourceId", required = false) String sourceId,
        @RequestParam(value = "historyId", required = false) String historyId,
        @RequestParam(value = "parseTaskId", required = false) String parseTaskId,
        @RequestParam(value = "batchId", required = false) String batchId,
        @RequestParam(value = "reportCode", required = false) String reportCode) {
        return recommendationApplicationService.listRecommendationPage(
            recommendationType,
            status,
            benefitLevel,
            riskLevel,
            validationStatus,
            requiresDispatch,
            manualReviewRequired,
            sourceType,
            sourceKind,
            sourceId,
            historyId,
            parseTaskId,
            batchId,
            reportCode,
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

    @PostMapping("/{recommendationId}/materialized-view/create")
    public QueryExecutionMaterializedViewCreateResponse createMaterializedView(
        @PathVariable("recommendationId") String recommendationId,
        @RequestBody MaterializedViewCreateRequest request
    ) {
        return materializedViewCreateApplicationService.create(recommendationId, request);
    }
}
