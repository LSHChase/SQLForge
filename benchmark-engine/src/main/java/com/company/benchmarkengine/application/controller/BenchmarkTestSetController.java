package com.company.benchmarkengine.application.controller;

import com.company.benchmarkengine.application.controller.dto.BenchmarkParseResultTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetResponse;
import com.company.benchmarkengine.application.service.BenchmarkParseResultTestSetApplicationService;
import com.company.benchmarkengine.application.service.BenchmarkTestSetApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark-engine/test-sets")
public class BenchmarkTestSetController {

    private final BenchmarkTestSetApplicationService benchmarkTestSetApplicationService;
    private final BenchmarkParseResultTestSetApplicationService benchmarkParseResultTestSetApplicationService;

    public BenchmarkTestSetController(BenchmarkTestSetApplicationService benchmarkTestSetApplicationService,
                                      BenchmarkParseResultTestSetApplicationService benchmarkParseResultTestSetApplicationService) {
        this.benchmarkTestSetApplicationService = benchmarkTestSetApplicationService;
        this.benchmarkParseResultTestSetApplicationService = benchmarkParseResultTestSetApplicationService;
    }

    @PostMapping
    public BenchmarkTestSetResponse createTestSet(@Valid @RequestBody BenchmarkTestSetCreateRequest request) {
        return benchmarkTestSetApplicationService.createTestSet(request);
    }

    @PostMapping("/parse-results")
    public BenchmarkTestSetResponse createTestSetFromParseResults(
        @Valid @RequestBody BenchmarkParseResultTestSetCreateRequest request
    ) {
        return benchmarkParseResultTestSetApplicationService.createFromParseResults(request);
    }

    @GetMapping("/{testSetId}")
    public BenchmarkTestSetResponse getTestSet(@PathVariable("testSetId") String testSetId) {
        return benchmarkTestSetApplicationService.getTestSet(testSetId);
    }
}
