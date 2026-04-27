package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.ReportBatchImportRequest;
import com.company.sqloptimization.application.controller.vo.ReportBatchStatusResponse;
import com.company.sqloptimization.application.service.ReportBatchApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/{batchId}/resolve-sqls")
    public ReportBatchStatusResponse resolveSqls(@PathVariable("batchId") String batchId) {
        return reportBatchApplicationService.resolveSqls(batchId);
    }

    @GetMapping("/{batchId}")
    public ReportBatchStatusResponse getBatch(@PathVariable("batchId") String batchId) {
        return reportBatchApplicationService.getBatch(batchId);
    }
}
