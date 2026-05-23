package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.MetadataSnapshotVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.infrastructure.repository.InMemoryMetadataSnapshotRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MetadataSnapshotApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldListSnapshotsAndNormalizeUnknownEvidenceState() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "hetu_main", "USE")).thenReturn(true);
        MetadataSnapshotApplicationService service = new MetadataSnapshotApplicationService(
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );
        RequestContext.set("tenant-a", "user-002", "request-020", "trace-020", "header", 1L, 2L);

        List<MetadataSnapshotVO> snapshots = service.list("tenant-a", "hetu_main", null, null, null, null, null, null);

        assertEquals(3, snapshots.size());
        MetadataSnapshotVO tableSnapshot = snapshots.get(2);
        assertEquals("TABLE:sales.orders", tableSnapshot.getObjectKey());
        assertEquals("UNKNOWN", tableSnapshot.getFreshnessStatus());
        assertEquals("UNKNOWN", tableSnapshot.getSlaStatus());
        assertEquals("UNKNOWN", tableSnapshot.getQueryabilityStatus());
        assertEquals("UNCOLLECTED", tableSnapshot.getEvidenceStatus());
        assertEquals("UNCOLLECTED", tableSnapshot.getEvidenceSource());
        assertEquals(Integer.valueOf(0), tableSnapshot.getUpstreamCount());
        assertEquals(Integer.valueOf(1), tableSnapshot.getDownstreamCount());
    }

    @Test
    void shouldSupportSnapshotFilters() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "hetu_main", "USE")).thenReturn(true);
        MetadataSnapshotApplicationService service = new MetadataSnapshotApplicationService(
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );
        RequestContext.set("tenant-a", "user-002", "request-021", "trace-021", "header", 1L, 2L);

        List<MetadataSnapshotVO> snapshots = service.list(
            "tenant-a",
            "hetu_main",
            "DB_VIEW",
            null,
            "STALE",
            "AT_RISK",
            "QUERYABLE",
            "CAPTURED"
        );

        assertEquals(1, snapshots.size());
        assertEquals("DB_VIEW:analytics.vw_sales_daily", snapshots.get(0).getObjectKey());
    }

    @Test
    void shouldRejectUnauthorizedCrossTenantLookup() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MetadataSnapshotApplicationService service = new MetadataSnapshotApplicationService(
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );
        RequestContext.set("tenant-a", "user-002", "request-022", "trace-022", "header", 1L, 2L);

        assertThrows(BizException.class, () -> service.list("tenant-b", null, null, null, null, null, null, null));
    }
}
