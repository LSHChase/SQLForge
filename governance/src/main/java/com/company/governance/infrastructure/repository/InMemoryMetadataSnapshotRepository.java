package com.company.governance.infrastructure.repository;

import com.company.governance.domain.metadata.entity.MetadataLineageRef;
import com.company.governance.domain.metadata.entity.MetadataSnapshot;
import com.company.governance.domain.metadata.repository.MetadataSnapshotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class InMemoryMetadataSnapshotRepository implements MetadataSnapshotRepository {

    private final Map<String, MetadataSnapshot> snapshots = new LinkedHashMap<String, MetadataSnapshot>();

    public InMemoryMetadataSnapshotRepository() {
        MetadataSnapshot logicalViewSnapshot = new MetadataSnapshot(
            "snapshot-001",
            "tenant-a",
            "hetu_main",
            "LOGICAL_VIEW",
            "LOGICAL_VIEW:RPT_SALES_DAILY",
            "Sales Daily",
            "lakehouse",
            "analytics",
            "FRESH",
            "ON_TRACK",
            "QUERYABLE",
            "CAPTURED",
            Instant.parse("2026-04-27T03:00:00Z"),
            Instant.parse("2026-04-27T04:00:00Z"),
            Instant.parse("2026-04-27T03:05:00Z"),
            "GOVERNANCE_CATALOG",
            Integer.valueOf(24),
            Integer.valueOf(1),
            Long.valueOf(120000L),
            Long.valueOf(67108864L),
            "request-snapshot-001",
            "trace-snapshot-001",
            null,
            "history-001",
            null,
            "RPT_SALES_DAILY",
            "fp-sales-daily",
            Arrays.asList(new MetadataLineageRef("DB_VIEW", "DB_VIEW:analytics.vw_sales_daily", "analytics.vw_sales_daily", "SOURCE")),
            Arrays.asList(new MetadataLineageRef("TABLE", "TABLE:report.sales_daily_export", "report.sales_daily_export", "CONSUMED_BY"))
        );
        MetadataSnapshot dbViewSnapshot = new MetadataSnapshot(
            "snapshot-002",
            "tenant-a",
            "hetu_main",
            "DB_VIEW",
            "DB_VIEW:analytics.vw_sales_daily",
            "analytics.vw_sales_daily",
            "lakehouse",
            "analytics",
            "STALE",
            "AT_RISK",
            "QUERYABLE",
            "CAPTURED",
            Instant.parse("2026-04-26T02:00:00Z"),
            Instant.parse("2026-04-27T01:00:00Z"),
            Instant.parse("2026-04-27T03:05:00Z"),
            "QUERY_HISTORY",
            Integer.valueOf(18),
            Integer.valueOf(2),
            Long.valueOf(980000L),
            Long.valueOf(268435456L),
            "request-snapshot-002",
            "trace-snapshot-002",
            "execution-002",
            "history-002",
            "parse-002",
            "RPT_SALES_DAILY",
            "fp-sales-daily",
            Arrays.asList(
                new MetadataLineageRef("TABLE", "TABLE:sales.orders", "sales.orders", "SOURCE"),
                new MetadataLineageRef("TABLE", "TABLE:sales.order_items", "sales.order_items", "SOURCE")
            ),
            Arrays.asList(new MetadataLineageRef("LOGICAL_VIEW", "LOGICAL_VIEW:RPT_SALES_DAILY", "Sales Daily", "EXPOSED_AS"))
        );
        MetadataSnapshot physicalTableSnapshot = new MetadataSnapshot(
            "snapshot-003",
            "tenant-a",
            "hetu_main",
            "TABLE",
            "TABLE:sales.orders",
            "sales.orders",
            "lakehouse",
            "sales",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Integer.valueOf(48),
            Integer.valueOf(12),
            Long.valueOf(1280000L),
            Long.valueOf(536870912L),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new ArrayList<MetadataLineageRef>(),
            Arrays.asList(new MetadataLineageRef("DB_VIEW", "DB_VIEW:analytics.vw_sales_daily", "analytics.vw_sales_daily", "CONSUMED_BY"))
        );
        snapshots.put(key(logicalViewSnapshot.getTenantId(), logicalViewSnapshot.getObjectKey()), logicalViewSnapshot);
        snapshots.put(key(dbViewSnapshot.getTenantId(), dbViewSnapshot.getObjectKey()), dbViewSnapshot);
        snapshots.put(key(physicalTableSnapshot.getTenantId(), physicalTableSnapshot.getObjectKey()), physicalTableSnapshot);
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
        List<MetadataSnapshot> results = new ArrayList<MetadataSnapshot>();
        for (MetadataSnapshot snapshot : snapshots.values()) {
            if (!matches(snapshot.getTenantId(), tenantId)) {
                continue;
            }
            if (!matches(snapshot.getDatasourceCode(), datasourceCode)) {
                continue;
            }
            if (!matches(snapshot.getObjectType(), objectType)) {
                continue;
            }
            if (!matches(snapshot.getObjectKey(), objectKey)) {
                continue;
            }
            if (!matches(snapshot.getFreshnessStatus(), freshnessStatus)) {
                continue;
            }
            if (!matches(snapshot.getSlaStatus(), slaStatus)) {
                continue;
            }
            if (!matches(snapshot.getQueryabilityStatus(), queryabilityStatus)) {
                continue;
            }
            if (!matches(snapshot.getEvidenceStatus(), evidenceStatus)) {
                continue;
            }
            results.add(snapshot);
        }
        return results;
    }

    @Override
    public Optional<MetadataSnapshot> findByTenantAndObjectKey(String tenantId, String objectKey) {
        return Optional.ofNullable(snapshots.get(key(tenantId, objectKey)));
    }

    private boolean matches(String actual, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        if (!StringUtils.hasText(actual)) {
            return false;
        }
        return actual.trim().toUpperCase(Locale.ROOT).equals(expected.trim().toUpperCase(Locale.ROOT));
    }

    private String key(String tenantId, String objectKey) {
        return String.valueOf(tenantId) + "::" + String.valueOf(objectKey);
    }
}
