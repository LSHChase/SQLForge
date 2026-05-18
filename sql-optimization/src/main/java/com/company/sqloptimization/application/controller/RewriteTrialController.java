package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.RewriteTrialRequest;
import com.company.sqloptimization.application.controller.vo.RewriteTrialRunVO;
import com.company.sqloptimization.application.service.RewriteTrialApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/rewrite-trials")
public class RewriteTrialController {

    private final RewriteTrialApplicationService rewriteTrialApplicationService;

    public RewriteTrialController(RewriteTrialApplicationService rewriteTrialApplicationService) {
        this.rewriteTrialApplicationService = rewriteTrialApplicationService;
    }

    @PostMapping
    public RewriteTrialRunVO createTrial(@Valid @RequestBody RewriteTrialRequest request) {
        return rewriteTrialApplicationService.createTrial(request);
    }

    @GetMapping("/{runId}")
    public RewriteTrialRunVO getTrial(@PathVariable("runId") String runId) {
        return rewriteTrialApplicationService.getTrial(runId);
    }
}
