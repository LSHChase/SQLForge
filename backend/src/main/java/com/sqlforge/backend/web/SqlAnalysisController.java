package com.sqlforge.backend.web;

import com.sqlforge.backend.service.SqlIntentAnalysisService;
import com.sqlforge.backend.service.SqlIntentBatchAnalysisService;
import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import com.sqlforge.backend.web.dto.SqlIntentBatchRequest;
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

    public SqlAnalysisController(
        SqlIntentAnalysisService sqlIntentAnalysisService,
        SqlIntentBatchAnalysisService sqlIntentBatchAnalysisService
    ) {
        this.sqlIntentAnalysisService = sqlIntentAnalysisService;
        this.sqlIntentBatchAnalysisService = sqlIntentBatchAnalysisService;
    }

    @PostMapping("/intent-analysis")
    public Map<String, Object> intentAnalysis(@Valid @RequestBody SqlIntentAnalysisRequest request) {
        return sqlIntentAnalysisService.analyze(request);
    }

    @PostMapping("/intent-analysis/daily-batch")
    public Map<String, Object> dailyBatch(@Valid @RequestBody SqlIntentBatchRequest request) {
        return sqlIntentBatchAnalysisService.analyzeBatch(request);
    }
}
