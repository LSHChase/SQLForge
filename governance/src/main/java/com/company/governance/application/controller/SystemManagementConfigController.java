package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.DispatchPolicyUpsertRequest;
import com.company.governance.application.controller.dto.RedisRuleSourceUpsertRequest;
import com.company.governance.application.controller.vo.DispatchPolicyVO;
import com.company.governance.application.controller.vo.RedisRuleSourceVO;
import com.company.governance.application.service.DispatchPolicyApplicationService;
import com.company.governance.application.service.RedisRuleSourceApplicationService;
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
@RequestMapping("/api/governance")
public class SystemManagementConfigController {

    private final RedisRuleSourceApplicationService redisRuleSourceApplicationService;
    private final DispatchPolicyApplicationService dispatchPolicyApplicationService;

    public SystemManagementConfigController(RedisRuleSourceApplicationService redisRuleSourceApplicationService,
                                            DispatchPolicyApplicationService dispatchPolicyApplicationService) {
        this.redisRuleSourceApplicationService = redisRuleSourceApplicationService;
        this.dispatchPolicyApplicationService = dispatchPolicyApplicationService;
    }

    @GetMapping("/redis-rule-sources")
    public List<RedisRuleSourceVO> listRedisRuleSources(@RequestParam("tenantId") String tenantId) {
        return redisRuleSourceApplicationService.list(tenantId);
    }

    @PostMapping("/redis-rule-sources")
    public RedisRuleSourceVO createRedisRuleSource(@RequestBody RedisRuleSourceUpsertRequest request) {
        return redisRuleSourceApplicationService.create(request);
    }

    @PutMapping("/redis-rule-sources/{sourceId}")
    public RedisRuleSourceVO updateRedisRuleSource(@PathVariable("sourceId") String sourceId,
                                                   @RequestBody RedisRuleSourceUpsertRequest request) {
        return redisRuleSourceApplicationService.update(sourceId, request);
    }

    @GetMapping("/dispatch-policies")
    public List<DispatchPolicyVO> listDispatchPolicies(@RequestParam("tenantId") String tenantId) {
        return dispatchPolicyApplicationService.list(tenantId);
    }

    @PostMapping("/dispatch-policies")
    public DispatchPolicyVO createDispatchPolicy(@RequestBody DispatchPolicyUpsertRequest request) {
        return dispatchPolicyApplicationService.create(request);
    }
}
