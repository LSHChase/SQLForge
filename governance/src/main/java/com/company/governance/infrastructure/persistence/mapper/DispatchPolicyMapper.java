package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.DispatchPolicyRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DispatchPolicyMapper {

    int upsert(DispatchPolicyRecord record);

    List<DispatchPolicyRecord> selectByTenantId(@Param("tenantId") String tenantId);
}
