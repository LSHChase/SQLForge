package com.company.queryexecution.application.controller;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.service.QueryExecutionApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query-execution")
public class QueryExecutionController {

    private final QueryExecutionApplicationService queryExecutionApplicationService;

    public QueryExecutionController(
        QueryExecutionApplicationService queryExecutionApplicationService) {
        this.queryExecutionApplicationService = queryExecutionApplicationService;
    }

    @PostMapping("/queries/execute")
    public QueryExecuteResponse execute(@Valid @RequestBody QueryExecuteRequest request) {
        return queryExecutionApplicationService.executeSynchronously(request);
    }
}
