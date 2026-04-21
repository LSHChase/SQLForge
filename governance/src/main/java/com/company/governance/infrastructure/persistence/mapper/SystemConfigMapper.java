package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.system.entity.SystemConfigRecord;

public interface SystemConfigMapper {

    SystemConfigRecord selectByKey(String configKey);

    int insertOrUpdate(SystemConfigRecord systemConfigRecord);
}
