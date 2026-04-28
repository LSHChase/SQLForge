package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.AlertPolicyRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AlertPolicyMapper {

    int insert(AlertPolicyRecord record);

    int update(AlertPolicyRecord record);

    AlertPolicyRecord selectByPolicyId(@Param("policyId") String policyId);

    List<AlertPolicyRecord> selectByTenantId(@Param("tenantId") String tenantId);

    List<AlertPolicyRecord> selectEnabledByTenantId(@Param("tenantId") String tenantId);
}
