package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseSqlIssueStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.application.service.ParseStatisticsApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse-statistics")
public class ParseStatisticsController {

    private final ParseStatisticsApplicationService parseStatisticsApplicationService;

    public ParseStatisticsController(ParseStatisticsApplicationService parseStatisticsApplicationService) {
        this.parseStatisticsApplicationService = parseStatisticsApplicationService;
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
}
