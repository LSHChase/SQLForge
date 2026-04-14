package com.sqlforge.backend.web;

import com.sqlforge.backend.service.SqlIntentAnalysisService;
import com.sqlforge.backend.service.SqlIntentBatchAnalysisService;
import com.sqlforge.backend.service.SqlPressurePlanService;
import com.sqlforge.backend.service.SqlScenarioBlueprintService;
import com.sqlforge.backend.service.SqlExecutionManifestService;
import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import com.sqlforge.backend.web.dto.SqlIntentBatchRequest;
import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sql")
public class SqlAnalysisController {

    private final SqlIntentAnalysisService sqlIntentAnalysisService;
    private final SqlIntentBatchAnalysisService sqlIntentBatchAnalysisService;
    private final SqlPressurePlanService sqlPressurePlanService;
    private final SqlScenarioBlueprintService sqlScenarioBlueprintService;
    private final SqlExecutionManifestService sqlExecutionManifestService;

    public SqlAnalysisController(
        SqlIntentAnalysisService sqlIntentAnalysisService,
        SqlIntentBatchAnalysisService sqlIntentBatchAnalysisService,
        SqlPressurePlanService sqlPressurePlanService,
        SqlScenarioBlueprintService sqlScenarioBlueprintService,
        SqlExecutionManifestService sqlExecutionManifestService
    ) {
        this.sqlIntentAnalysisService = sqlIntentAnalysisService;
        this.sqlIntentBatchAnalysisService = sqlIntentBatchAnalysisService;
        this.sqlPressurePlanService = sqlPressurePlanService;
        this.sqlScenarioBlueprintService = sqlScenarioBlueprintService;
        this.sqlExecutionManifestService = sqlExecutionManifestService;
    }

    @PostMapping("/intent-analysis")
    public Map<String, Object> intentAnalysis(@Valid @RequestBody SqlIntentAnalysisRequest request) {
        return sqlIntentAnalysisService.analyze(request);
    }

    @PostMapping("/intent-analysis/daily-batch")
    public Map<String, Object> dailyBatch(@Valid @RequestBody SqlIntentBatchRequest request) {
        return sqlIntentBatchAnalysisService.analyzeBatch(request);
    }

    @PostMapping("/intent-analysis/pressure-plan")
    public Map<String, Object> pressurePlan(@Valid @RequestBody SqlPressurePlanRequest request) {
        return sqlPressurePlanService.buildPlan(request);
    }

    @PostMapping("/intent-analysis/scenario-blueprint")
    public Map<String, Object> scenarioBlueprint(@Valid @RequestBody SqlPressurePlanRequest request) {
        return sqlScenarioBlueprintService.buildBlueprint(request);
    }

    @PostMapping("/intent-analysis/execution-manifest")
    public Map<String, Object> executionManifest(@Valid @RequestBody SqlPressurePlanRequest request) {
        return sqlExecutionManifestService.buildManifest(request);
    }
}
