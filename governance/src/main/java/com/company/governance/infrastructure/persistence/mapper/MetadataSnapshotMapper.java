package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.MetadataSnapshotRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MetadataSnapshotMapper {

    int upsert(MetadataSnapshotRecord record);

    MetadataSnapshotRecord selectByTenantAndObjectKey(@Param("tenantId") String tenantId,
                                                      @Param("objectKey") String objectKey);

    List<MetadataSnapshotRecord> selectSnapshots(@Param("tenantId") String tenantId,
                                                 @Param("datasourceCode") String datasourceCode,
                                                 @Param("objectType") String objectType,
                                                 @Param("objectKey") String objectKey,
                                                 @Param("freshnessStatus") String freshnessStatus,
                                                 @Param("slaStatus") String slaStatus,
                                                 @Param("queryabilityStatus") String queryabilityStatus,
                                                 @Param("evidenceStatus") String evidenceStatus);
}
