package com.sqlforge.backend.web;

import com.sqlforge.backend.service.SqlIntentAnalysisService;
import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
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

    public SqlAnalysisController(SqlIntentAnalysisService sqlIntentAnalysisService) {
        this.sqlIntentAnalysisService = sqlIntentAnalysisService;
    }

    @PostMapping("/intent-analysis")
    public Map<String, Object> intentAnalysis(@Valid @RequestBody SqlIntentAnalysisRequest request) {
        return sqlIntentAnalysisService.analyze(request);
    }
}
