package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.application.service.GovernanceHistoryApplicationService;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/history")
public class GovernanceHistoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceHistoryController.class);

    private final GovernanceHistoryApplicationService governanceHistoryApplicationService;

    public GovernanceHistoryController(GovernanceHistoryApplicationService governanceHistoryApplicationService) {
        this.governanceHistoryApplicationService = governanceHistoryApplicationService;
    }

    @GetMapping("/traces")
    public List<GovernanceTraceSummaryVO> getRecentTraces(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "limit", required = false) Integer limit) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info("Handling governance trace summary query, tenantId={}, traceId={}",
            effectiveTenantId,
            RequestContext.getTraceId());
        return governanceHistoryApplicationService.findRecentTraces(effectiveTenantId, limit);
    }

    @GetMapping("/traces/{traceId}")
    public GovernanceTraceDetailVO getTraceDetail(@PathVariable("traceId") String traceId,
                                                  @RequestParam(value = "tenantId", required = false) String tenantId,
                                                  @RequestParam(value = "limit", required = false) Integer limit) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info("Handling governance trace detail query, tenantId={}, traceId={}, requestTraceId={}",
            effectiveTenantId,
            traceId,
            RequestContext.getTraceId());
        return governanceHistoryApplicationService.findTraceDetail(effectiveTenantId, traceId, limit);
    }
}
