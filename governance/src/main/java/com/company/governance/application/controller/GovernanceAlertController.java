package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.GovernanceAlertDetailVO;
import com.company.governance.application.controller.vo.GovernanceAlertPageVO;
import com.company.governance.application.service.GovernanceAlertApplicationService;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/alerts")
public class GovernanceAlertController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceAlertController.class);

    private final GovernanceAlertApplicationService governanceAlertApplicationService;

    public GovernanceAlertController(GovernanceAlertApplicationService governanceAlertApplicationService) {
        this.governanceAlertApplicationService = governanceAlertApplicationService;
    }

    @GetMapping
    public GovernanceAlertPageVO getAlerts(@RequestParam(value = "tenantId", required = false) String tenantId,
                                           @RequestParam(value = "alertStatus", required = false) String alertStatus,
                                           @RequestParam(value = "alertType", required = false) String alertType,
                                           @RequestParam(value = "notifyStatus", required = false) String notifyStatus,
                                           @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                           @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info("Handling governance alert page, tenantId={}, alertStatus={}, alertType={}, notifyStatus={}, requestTraceId={}",
            effectiveTenantId,
            alertStatus,
            alertType,
            notifyStatus,
            RequestContext.getTraceId());
        return governanceAlertApplicationService.findAlertPage(
            effectiveTenantId,
            alertStatus,
            alertType,
            notifyStatus,
            pageNo,
            pageSize
        );
    }

    @GetMapping("/{alertId}")
    public GovernanceAlertDetailVO getAlertDetail(@PathVariable("alertId") String alertId,
                                                  @RequestParam(value = "tenantId", required = false) String tenantId) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info("Handling governance alert detail, tenantId={}, alertId={}, requestTraceId={}",
            effectiveTenantId,
            alertId,
            RequestContext.getTraceId());
        return governanceAlertApplicationService.findAlertDetail(effectiveTenantId, alertId);
    }

    @PostMapping("/{alertId}/ack")
    public GovernanceAlertDetailVO ackAlert(@PathVariable("alertId") String alertId,
                                            @RequestParam(value = "tenantId", required = false) String tenantId) {
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : TenantContext.get();
        LOGGER.info("Handling governance alert ack, tenantId={}, alertId={}, operator={}, requestTraceId={}",
            effectiveTenantId,
            alertId,
            RequestContext.getUserId(),
            RequestContext.getTraceId());
        return governanceAlertApplicationService.ackAlert(
            effectiveTenantId,
            alertId,
            RequestContext.getUserId(),
            Instant.now()
        );
    }
}
