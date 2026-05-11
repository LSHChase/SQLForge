package com.company.sqloptimization.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RewriteValidationSchedulerJob {

    private final RewriteValidationSchedulerService rewriteValidationSchedulerService;

    public RewriteValidationSchedulerJob(RewriteValidationSchedulerService rewriteValidationSchedulerService) {
        this.rewriteValidationSchedulerService = rewriteValidationSchedulerService;
    }

    @Scheduled(fixedDelayString = "${sql-optimization.rewrite-validation-scheduler.fixed-delay-ms:60000}")
    public void runScheduledValidationCycle() {
        rewriteValidationSchedulerService.runScheduledValidationCycle();
    }
}
