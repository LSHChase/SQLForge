package com.company.governance.domain.tenant.logic.impl;

import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantAccessLogicImpl implements TenantAccessLogic {

    @Override
    public boolean validateDataSourceAccess(String tenantId, String dataSourceId) {
        // TODO: 阶段1完善：对接权限中心，校验租户-数据源绑定关系（R-112）
        return StringUtils.hasText(tenantId);
    }
}
