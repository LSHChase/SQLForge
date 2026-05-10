package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.application.service.SqlRewriteRecordApplicationService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/rewrite-records")
public class SqlRewriteRecordController {

    private final SqlRewriteRecordApplicationService sqlRewriteRecordApplicationService;

    public SqlRewriteRecordController(SqlRewriteRecordApplicationService sqlRewriteRecordApplicationService) {
        this.sqlRewriteRecordApplicationService = sqlRewriteRecordApplicationService;
    }

    @PostMapping
    public SqlRewriteRecordVO createRewriteRecord(@Valid @RequestBody SqlRewriteRecordCreateRequest request) {
        return sqlRewriteRecordApplicationService.createRewriteRecord(request);
    }

    @GetMapping
    public List<SqlRewriteRecordVO> listRewriteRecords(
        @RequestParam(value = "historyId", required = false) String historyId,
        @RequestParam(value = "recommendationId", required = false) String recommendationId,
        @RequestParam(value = "validationStatus", required = false) String validationStatus,
        @RequestParam(value = "sourceType", required = false) String sourceType) {
        return sqlRewriteRecordApplicationService.listRewriteRecords(
            historyId,
            recommendationId,
            validationStatus,
            sourceType
        );
    }

    @GetMapping("/{rewriteRecordId}")
    public SqlRewriteRecordVO getRewriteRecord(@PathVariable("rewriteRecordId") String rewriteRecordId) {
        return sqlRewriteRecordApplicationService.getRewriteRecord(rewriteRecordId);
    }

    @PostMapping("/{rewriteRecordId}/validation-runs")
    public RewriteValidationRunVO createValidationRun(@PathVariable("rewriteRecordId") String rewriteRecordId,
                                                      @RequestBody(required = false) RewriteValidationRunCreateRequest request) {
        return sqlRewriteRecordApplicationService.createValidationRun(rewriteRecordId, request);
    }

    @GetMapping("/{rewriteRecordId}/validation-runs")
    public List<RewriteValidationRunVO> listValidationRuns(@PathVariable("rewriteRecordId") String rewriteRecordId) {
        return sqlRewriteRecordApplicationService.listValidationRuns(rewriteRecordId);
    }
}
