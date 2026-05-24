package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.TenantEngineConfigUpdateRequest;
import com.company.governance.application.controller.vo.TenantConfigOptionVO;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.TenantConfigApplicationService;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/tenant-config")
public class TenantConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigController.class);

    private final TenantConfigApplicationService tenantConfigApplicationService;

    public TenantConfigController(TenantConfigApplicationService tenantConfigApplicationService) {
        this.tenantConfigApplicationService = tenantConfigApplicationService;
    }

    @GetMapping
    public TenantConfigVO getTenantConfig(@RequestParam(value = "tenantId", required = false) String tenantId) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info("处理租户配置查询，tenantId={}, traceId={}",
            effectiveTenantId,
            RequestContext.getTraceId());
        return tenantConfigApplicationService.findByTenantId(effectiveTenantId);
    }

    @GetMapping("/options")
    public List<TenantConfigOptionVO> listTenantOptions() {
        LOGGER.info("处理租户配置候选查询，currentTenantId={}, traceId={}",
            TenantContext.get(),
            RequestContext.getTraceId());
        return tenantConfigApplicationService.listTenantOptions();
    }

    @PutMapping
    public TenantConfigVO updateTenantEngines(@RequestBody TenantEngineConfigUpdateRequest request) {
        LOGGER.info("处理租户默认/备用引擎更新，tenantId={}, traceId={}",
            request == null ? null : request.getTenantId(),
            RequestContext.getTraceId());
        return tenantConfigApplicationService.updateTenantEngines(request);
    }
}
