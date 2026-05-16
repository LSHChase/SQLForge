package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.BusinessLogicalViewVO;
import com.company.governance.application.service.LogicalViewCatalogApplicationService;
import com.company.sqlforge.common.context.RequestContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/logical-views")
public class LogicalViewCatalogController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogicalViewCatalogController.class);

    private final LogicalViewCatalogApplicationService logicalViewCatalogApplicationService;

    public LogicalViewCatalogController(LogicalViewCatalogApplicationService logicalViewCatalogApplicationService) {
        this.logicalViewCatalogApplicationService = logicalViewCatalogApplicationService;
    }

    @GetMapping
    public List<BusinessLogicalViewVO> listLogicalViews(@RequestParam(value = "tenantId", required = false) String tenantId,
                                                        @RequestParam(value = "datasourceCode", required = false) String datasourceCode) {
        LOGGER.info("处理业务逻辑视图列表查询，tenantId={}, datasourceCode={}, traceId={}",
            tenantId,
            datasourceCode,
            RequestContext.getTraceId());
        return logicalViewCatalogApplicationService.listLogicalViews(tenantId, datasourceCode);
    }

    @GetMapping("/{viewCode}")
    public BusinessLogicalViewVO getLogicalView(@PathVariable("viewCode") String viewCode,
                                                @RequestParam(value = "tenantId", required = false) String tenantId) {
        LOGGER.info("处理业务逻辑视图详情查询，tenantId={}, viewCode={}, traceId={}",
            tenantId,
            viewCode,
            RequestContext.getTraceId());
        return logicalViewCatalogApplicationService.findLogicalView(tenantId, viewCode);
    }
}
