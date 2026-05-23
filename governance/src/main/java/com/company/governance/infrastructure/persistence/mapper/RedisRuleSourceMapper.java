package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.RedisRuleSourceRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RedisRuleSourceMapper {

    int upsert(RedisRuleSourceRecord record);

    RedisRuleSourceRecord selectByTenantIdAndSourceId(@Param("tenantId") String tenantId,
                                                      @Param("sourceId") String sourceId);

    List<RedisRuleSourceRecord> selectByTenantId(@Param("tenantId") String tenantId);
}
