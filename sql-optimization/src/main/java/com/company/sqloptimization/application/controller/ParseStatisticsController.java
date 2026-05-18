package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseSqlIssueStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialOverviewVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialSourceIssueStatisticVO;
import com.company.sqloptimization.application.service.ParseStatisticsApplicationService;
import com.company.sqloptimization.application.service.RewriteTrialApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse-statistics")
public class ParseStatisticsController {

    private final ParseStatisticsApplicationService parseStatisticsApplicationService;
    private final RewriteTrialApplicationService rewriteTrialApplicationService;

    public ParseStatisticsController(ParseStatisticsApplicationService parseStatisticsApplicationService,
                                     RewriteTrialApplicationService rewriteTrialApplicationService) {
        this.parseStatisticsApplicationService = parseStatisticsApplicationService;
        this.rewriteTrialApplicationService = rewriteTrialApplicationService;
    }

    @GetMapping("/overview")
    public ParseStatisticsOverviewVO overview() {
        return parseStatisticsApplicationService.overview();
    }

    @GetMapping("/by-issue-scene")
    public List<ParseIssueSceneStatisticVO> byIssueScene() {
        return parseStatisticsApplicationService.byIssueScene();
    }

    @GetMapping("/by-sql")
    public List<ParseSqlIssueStatisticVO> bySql() {
        return parseStatisticsApplicationService.bySql();
    }

    @GetMapping("/by-report")
    public List<ParseReportStatisticVO> byReport() {
        return parseStatisticsApplicationService.byReport();
    }

    @GetMapping("/priority-matrix")
    public List<ParsePriorityMatrixCellVO> priorityMatrix() {
        return parseStatisticsApplicationService.priorityMatrix();
    }

    @GetMapping("/important-urgent")
    public List<ParseSqlIssueStatisticVO> importantUrgent() {
        return parseStatisticsApplicationService.importantUrgentList();
    }

    @GetMapping("/rewrite-trials/overview")
    public RewriteTrialOverviewVO rewriteTrialOverview() {
        return rewriteTrialApplicationService.overview();
    }

    @GetMapping("/rewrite-trials/by-source-issue")
    public List<RewriteTrialSourceIssueStatisticVO> rewriteTrialsBySourceIssue() {
        return rewriteTrialApplicationService.bySourceIssue();
    }
}
