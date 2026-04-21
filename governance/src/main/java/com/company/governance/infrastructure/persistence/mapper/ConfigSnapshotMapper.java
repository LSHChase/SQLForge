package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;

public interface ConfigSnapshotMapper {

    ConfigSnapshotRecord selectById(String configSnapshotId);

    int insert(ConfigSnapshotRecord configSnapshotRecord);
}
