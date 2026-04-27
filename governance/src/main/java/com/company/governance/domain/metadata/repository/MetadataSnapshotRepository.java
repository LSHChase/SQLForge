package com.company.governance.domain.metadata.repository;

import com.company.governance.domain.metadata.entity.MetadataSnapshot;
import java.util.List;
import java.util.Optional;

public interface MetadataSnapshotRepository {

    List<MetadataSnapshot> findSnapshots(String tenantId,
                                         String datasourceCode,
                                         String objectType,
                                         String objectKey,
                                         String freshnessStatus,
                                         String slaStatus,
                                         String queryabilityStatus,
                                         String evidenceStatus);

    Optional<MetadataSnapshot> findByTenantAndObjectKey(String tenantId, String objectKey);
}
