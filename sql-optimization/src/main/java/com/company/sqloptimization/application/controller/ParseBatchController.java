package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchIngestRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchRetryAccessRequest;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.application.service.ParseBatchApplicationService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse-batches")
public class ParseBatchController {

    private final ParseBatchApplicationService parseBatchApplicationService;

    public ParseBatchController(ParseBatchApplicationService parseBatchApplicationService) {
        this.parseBatchApplicationService = parseBatchApplicationService;
    }

    @PostMapping
    public ParseBatchStatusResponse createBatch(@Valid @RequestBody ParseBatchCreateRequest request) {
        return parseBatchApplicationService.createBatch(request);
    }

    @GetMapping
    public List<ParseBatchStatusResponse> listBatches() {
        return parseBatchApplicationService.listBatches();
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

    @PostMapping("/{batchId}/retry-access")
    public ParseBatchStatusResponse retryAccess(@PathVariable("batchId") String batchId,
                                                @RequestBody(required = false) ParseBatchRetryAccessRequest request) {
        return parseBatchApplicationService.retryAccess(batchId, request);
    }
}
