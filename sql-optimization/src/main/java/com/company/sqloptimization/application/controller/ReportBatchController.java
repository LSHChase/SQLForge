package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusResponse;
import com.company.sqloptimization.application.service.ReportBatchApplicationService;
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
@RequestMapping("/api/sql-optimization/report-batches")
public class ReportBatchController {

    private final ReportBatchApplicationService reportBatchApplicationService;

    public ReportBatchController(ReportBatchApplicationService reportBatchApplicationService) {
        this.reportBatchApplicationService = reportBatchApplicationService;
    }

    @PostMapping("/import")
    public ReportBatchStatusResponse importBatch(@Valid @RequestBody ReportBatchImportRequest request) {
        return reportBatchApplicationService.importBatch(request);
    }

    @GetMapping
    public List<ReportBatchStatusResponse> listBatches() {
        return reportBatchApplicationService.listBatches();
    }

    @PostMapping("/{batchId}/resolve-sqls")
    public ReportBatchStatusResponse resolveSqls(@PathVariable("batchId") String batchId) {
        return reportBatchApplicationService.resolveSqls(batchId);
    }

    @GetMapping("/{batchId}")
    public ReportBatchStatusResponse getBatch(@PathVariable("batchId") String batchId,
                                              @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                                              @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                              @RequestParam(value = "reportCode", required = false) String reportCode) {
        return reportBatchApplicationService.getBatch(batchId, pageNumber, pageSize, reportCode);
    }

    @GetMapping("/{batchId}/parse-statistics")
    public ReportBatchParseStatisticsVO getBatchParseStatistics(
        @PathVariable("batchId") String batchId,
        @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @RequestParam(value = "reportCode", required = false) String reportCode) {
        return reportBatchApplicationService.getBatchParseStatistics(batchId, pageNumber, pageSize, reportCode);
    }

    @GetMapping("/{batchId}/parse-statistics/issue-scenes/{issueScene}")
    public ReportBatchIssueSceneDetailVO getBatchIssueSceneDetail(
        @PathVariable("batchId") String batchId,
        @PathVariable("issueScene") String issueScene,
        @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @RequestParam(value = "reportCode", required = false) String reportCode,
        @RequestParam(value = "logicalObjectKey", required = false) String logicalObjectKey) {
        return reportBatchApplicationService.getBatchIssueSceneDetail(
            batchId,
            issueScene,
            pageNumber,
            pageSize,
            reportCode,
            logicalObjectKey
        );
    }
}
