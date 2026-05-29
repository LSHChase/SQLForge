package com.company.governance.domain.metadata.entity;

import java.time.Instant;
import java.util.List;

public class MetadataSnapshot extends MetadataSnapshotBase {

    public MetadataSnapshot() {
    }

    public MetadataSnapshot(String snapshotId,
                            String tenantId,
                            String datasourceCode,
                            String objectType,
                            String objectKey,
                            String objectName,
                            String catalogName,
                            String schemaName,
                            String freshnessStatus,
                            String slaStatus,
                            String queryabilityStatus,
                            String evidenceStatus,
                            Instant latestRefreshTime,
                            Instant expectedSlaTime,
                            Instant snapshotTime,
                            String evidenceSource,
                            Integer columnCount,
                            Integer partitionCount,
                            Long rowCount,
                            Long storageBytes,
                            String requestId,
                            String traceId,
                            String executionId,
                            String historyId,
                            String parseTaskId,
                            String reportCode,
                            String sqlFingerprint,
                            List<MetadataLineageRef> upstreamRefs,
                            List<MetadataLineageRef> downstreamRefs) {
        super(snapshotId, tenantId, datasourceCode, objectType, objectKey, objectName, catalogName, schemaName,
            freshnessStatus, slaStatus, queryabilityStatus, evidenceStatus, latestRefreshTime, expectedSlaTime,
            snapshotTime, evidenceSource, columnCount, partitionCount, rowCount, storageBytes, requestId, traceId,
            executionId, historyId, parseTaskId, reportCode, sqlFingerprint, upstreamRefs, downstreamRefs);
    }
}
