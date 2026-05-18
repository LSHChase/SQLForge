package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchIngestRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchRetryAccessRequest;
import com.company.sqloptimization.application.controller.dto.RewriteTrialBatchRequest;
import com.company.sqloptimization.application.controller.vo.BatchPageResponse;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.application.controller.vo.RewriteTrialRunVO;
import com.company.sqloptimization.application.service.ParseBatchApplicationService;
import com.company.sqloptimization.application.service.RewriteTrialApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse-batches")
public class ParseBatchController {

    private final ParseBatchApplicationService parseBatchApplicationService;
    private final RewriteTrialApplicationService rewriteTrialApplicationService;

    public ParseBatchController(ParseBatchApplicationService parseBatchApplicationService,
                                RewriteTrialApplicationService rewriteTrialApplicationService) {
        this.parseBatchApplicationService = parseBatchApplicationService;
        this.rewriteTrialApplicationService = rewriteTrialApplicationService;
    }

    @PostMapping
    public ParseBatchStatusResponse createBatch(@Valid @RequestBody ParseBatchCreateRequest request) {
        return parseBatchApplicationService.createBatch(request);
    }

    @GetMapping
    public BatchPageResponse<ParseBatchStatusResponse> listBatches(
        @RequestParam(value = "pageNo", required = false) Integer pageNo,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return parseBatchApplicationService.listBatches(pageNo, pageSize);
    }

    @PostMapping("/{batchId}/ingest")
    public ParseBatchStatusResponse ingestBatch(@PathVariable("batchId") String batchId,
                                                @Valid @RequestBody ParseBatchIngestRequest request) {
        return parseBatchApplicationService.ingestBatch(batchId, request);
    }

    @GetMapping("/{batchId}")
    public ParseBatchStatusResponse getBatch(@PathVariable("batchId") String batchId) {
        return parseBatchApplicationService.getBatch(batchId);
    }

    @PostMapping("/{batchId}/rewrite-trials")
    public RewriteTrialRunVO createRewriteTrials(@PathVariable("batchId") String batchId,
                                                 @RequestBody(required = false) RewriteTrialBatchRequest request) {
        return rewriteTrialApplicationService.createBatchTrial(batchId, request);
    }

    @GetMapping("/{batchId}/rewrite-trials/latest")
    public RewriteTrialRunVO latestRewriteTrial(@PathVariable("batchId") String batchId) {
        return rewriteTrialApplicationService.latestBatchTrial(batchId);
    }

    @PostMapping("/{batchId}/retry-access")
    public ParseBatchStatusResponse retryAccess(@PathVariable("batchId") String batchId,
                                                @RequestBody(required = false) ParseBatchRetryAccessRequest request) {
        return parseBatchApplicationService.retryAccess(batchId, request);
    }
}
