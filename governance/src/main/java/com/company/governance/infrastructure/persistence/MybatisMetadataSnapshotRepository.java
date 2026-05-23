package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.metadata.entity.MetadataLineageRef;
import com.company.governance.domain.metadata.entity.MetadataSnapshot;
import com.company.governance.domain.metadata.repository.MetadataSnapshotRepository;
import com.company.governance.infrastructure.persistence.entity.MetadataSnapshotRecord;
import com.company.governance.infrastructure.persistence.mapper.MetadataSnapshotMapper;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisMetadataSnapshotRepository implements MetadataSnapshotRepository {

    private static final TypeReference<List<MetadataLineageRef>> LINEAGE_REFS =
        new TypeReference<List<MetadataLineageRef>>() {
        };

    private final MetadataSnapshotMapper mapper;

    public MybatisMetadataSnapshotRepository(MetadataSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MetadataSnapshot save(MetadataSnapshot snapshot) {
        mapper.upsert(toRecord(snapshot));
        return snapshot;
    }

    @Override
    public List<MetadataSnapshot> findSnapshots(String tenantId,
                                                String datasourceCode,
                                                String objectType,
                                                String objectKey,
                                                String freshnessStatus,
                                                String slaStatus,
                                                String queryabilityStatus,
                                                String evidenceStatus) {
        List<MetadataSnapshotRecord> records = mapper.selectSnapshots(
            tenantId,
            datasourceCode,
            objectType,
            objectKey,
            freshnessStatus,
            slaStatus,
            queryabilityStatus,
            evidenceStatus
        );
        List<MetadataSnapshot> result = new ArrayList<MetadataSnapshot>(records == null ? 0 : records.size());
        if (records != null) {
            for (MetadataSnapshotRecord record : records) {
                result.add(toDomain(record));
            }
        }
        return result;
    }

    @Override
    public Optional<MetadataSnapshot> findByTenantAndObjectKey(String tenantId, String objectKey) {
        return Optional.ofNullable(toDomain(mapper.selectByTenantAndObjectKey(tenantId, objectKey)));
    }

    private MetadataSnapshot toDomain(MetadataSnapshotRecord record) {
        if (record == null) {
            return null;
        }
        return new MetadataSnapshot(
            record.getSnapshotId(),
            record.getTenantId(),
            record.getDatasourceCode(),
            record.getObjectType(),
            record.getObjectKey(),
            record.getObjectName(),
            record.getCatalogName(),
            record.getSchemaName(),
            record.getFreshnessStatus(),
            record.getSlaStatus(),
            record.getQueryabilityStatus(),
            record.getEvidenceStatus(),
            record.getLatestRefreshTime(),
            record.getExpectedSlaTime(),
            record.getSnapshotTime(),
            record.getEvidenceSource(),
            record.getColumnCount(),
            record.getPartitionCount(),
            record.getRowCount(),
            record.getStorageBytes(),
            record.getRequestId(),
            record.getTraceId(),
            record.getExecutionId(),
            record.getHistoryId(),
            record.getParseTaskId(),
            record.getReportCode(),
            record.getSqlFingerprint(),
            readLineageRefs(record.getUpstreamRefsJson()),
            readLineageRefs(record.getDownstreamRefsJson())
        );
    }

    private MetadataSnapshotRecord toRecord(MetadataSnapshot snapshot) {
        MetadataSnapshotRecord record = new MetadataSnapshotRecord();
        record.setSnapshotId(snapshot.getSnapshotId());
        record.setTenantId(snapshot.getTenantId());
        record.setDatasourceCode(snapshot.getDatasourceCode());
        record.setObjectType(snapshot.getObjectType());
        record.setObjectKey(snapshot.getObjectKey());
        record.setObjectName(snapshot.getObjectName());
        record.setCatalogName(snapshot.getCatalogName());
        record.setSchemaName(snapshot.getSchemaName());
        record.setFreshnessStatus(snapshot.getFreshnessStatus());
        record.setSlaStatus(snapshot.getSlaStatus());
        record.setQueryabilityStatus(snapshot.getQueryabilityStatus());
        record.setEvidenceStatus(snapshot.getEvidenceStatus());
        record.setLatestRefreshTime(snapshot.getLatestRefreshTime());
        record.setExpectedSlaTime(snapshot.getExpectedSlaTime());
        record.setSnapshotTime(snapshot.getSnapshotTime());
        record.setEvidenceSource(snapshot.getEvidenceSource());
        record.setColumnCount(snapshot.getColumnCount());
        record.setPartitionCount(snapshot.getPartitionCount());
        record.setRowCount(snapshot.getRowCount());
        record.setStorageBytes(snapshot.getStorageBytes());
        record.setRequestId(snapshot.getRequestId());
        record.setTraceId(snapshot.getTraceId());
        record.setExecutionId(snapshot.getExecutionId());
        record.setHistoryId(snapshot.getHistoryId());
        record.setParseTaskId(snapshot.getParseTaskId());
        record.setReportCode(snapshot.getReportCode());
        record.setSqlFingerprint(snapshot.getSqlFingerprint());
        record.setUpstreamRefsJson(JsonUtils.toJson(emptyIfNull(snapshot.getUpstreamRefs())));
        record.setDownstreamRefsJson(JsonUtils.toJson(emptyIfNull(snapshot.getDownstreamRefs())));
        Instant now = Instant.now();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private List<MetadataLineageRef> readLineageRefs(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.objectMapper().readValue(json, LINEAGE_REFS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("元数据血缘引用反序列化失败", ex);
        }
    }

    private List<MetadataLineageRef> emptyIfNull(List<MetadataLineageRef> refs) {
        return refs == null ? Collections.<MetadataLineageRef>emptyList() : refs;
    }
}
