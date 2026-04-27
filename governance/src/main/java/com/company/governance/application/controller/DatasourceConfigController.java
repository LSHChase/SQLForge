package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.DatasourceConfigUpsertRequest;
import com.company.governance.application.controller.dto.DatasourceConnectionTestRequest;
import com.company.governance.application.controller.vo.DatasourceConfigVO;
import com.company.governance.application.controller.vo.DatasourceConnectionTestVO;
import com.company.governance.application.service.DatasourceConfigApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/datasources")
public class DatasourceConfigController {

    private final DatasourceConfigApplicationService datasourceConfigApplicationService;

    public DatasourceConfigController(DatasourceConfigApplicationService datasourceConfigApplicationService) {
        this.datasourceConfigApplicationService = datasourceConfigApplicationService;
    }

    @GetMapping
    public List<DatasourceConfigVO> list(@RequestParam("tenantId") String tenantId) {
        return datasourceConfigApplicationService.list(tenantId);
    }

    @GetMapping("/{datasourceId}")
    public DatasourceConfigVO find(@RequestParam("tenantId") String tenantId,
                                   @PathVariable("datasourceId") String datasourceId) {
        return datasourceConfigApplicationService.find(tenantId, datasourceId);
    }

    @PostMapping
    public DatasourceConfigVO create(@RequestBody DatasourceConfigUpsertRequest request) {
        return datasourceConfigApplicationService.create(request);
    }

    @PutMapping("/{datasourceId}")
    public DatasourceConfigVO update(@PathVariable("datasourceId") String datasourceId,
                                     @RequestBody DatasourceConfigUpsertRequest request) {
        return datasourceConfigApplicationService.update(datasourceId, request);
    }

    @PostMapping("/{datasourceId}/test-connection")
    public DatasourceConnectionTestVO testConnection(@PathVariable("datasourceId") String datasourceId,
                                                     @RequestBody(required = false) DatasourceConnectionTestRequest request) {
        return datasourceConfigApplicationService.testConnection(datasourceId, request);
    }
}
