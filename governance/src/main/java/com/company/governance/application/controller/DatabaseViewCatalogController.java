package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.DatabaseViewRefVO;
import com.company.governance.application.service.DatabaseViewCatalogApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/db-views")
public class DatabaseViewCatalogController {

    private final DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService;

    public DatabaseViewCatalogController(DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService) {
        this.databaseViewCatalogApplicationService = databaseViewCatalogApplicationService;
    }

    @GetMapping
    public List<DatabaseViewRefVO> listDbViews(@RequestParam(value = "tenantId", required = false) String tenantId,
                                               @RequestParam(value = "datasourceCode", required = false) String datasourceCode) {
        return databaseViewCatalogApplicationService.listDbViews(tenantId, datasourceCode);
    }

    @GetMapping("/{viewName}")
    public DatabaseViewRefVO getDbView(@PathVariable("viewName") String viewName,
                                       @RequestParam(value = "tenantId", required = false) String tenantId,
                                       @RequestParam("datasourceCode") String datasourceCode) {
        return databaseViewCatalogApplicationService.findDbView(tenantId, datasourceCode, viewName);
    }
}
