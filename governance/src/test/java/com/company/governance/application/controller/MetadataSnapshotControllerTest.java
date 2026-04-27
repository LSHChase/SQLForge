package com.company.governance.application.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.MetadataLineageVO;
import com.company.governance.application.controller.vo.MetadataSnapshotVO;
import com.company.governance.application.service.MetadataSnapshotApplicationService;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MetadataSnapshotControllerTest {

    @Test
    void shouldExposeMetadataSnapshotListContract() throws Exception {
        MetadataSnapshotApplicationService service = mock(MetadataSnapshotApplicationService.class);
        when(service.list("tenant-a", "hetu_main", "LOGICAL_VIEW", null, "FRESH", null, "QUERYABLE", null))
            .thenReturn(Collections.singletonList(buildSnapshot()));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MetadataSnapshotController(service)).build();

        mockMvc.perform(get("/api/governance/metadata/snapshots")
                .param("tenantId", "tenant-a")
                .param("datasourceCode", "hetu_main")
                .param("objectType", "LOGICAL_VIEW")
                .param("freshnessStatus", "FRESH")
                .param("queryabilityStatus", "QUERYABLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].snapshotId").value("snapshot-001"))
            .andExpect(jsonPath("$[0].objectKey").value("LOGICAL_VIEW:RPT_SALES_DAILY"))
            .andExpect(jsonPath("$[0].upstreamCount").value(1))
            .andExpect(jsonPath("$[0].upstreamRefs[0].relationshipType").value("SOURCE"))
            .andExpect(jsonPath("$[0].contractStage").value("LONG_TERM_BASELINE"));

        verify(service).list("tenant-a", "hetu_main", "LOGICAL_VIEW", null, "FRESH", null, "QUERYABLE", null);
    }

    private MetadataSnapshotVO buildSnapshot() {
        MetadataLineageVO upstream = new MetadataLineageVO();
        upstream.setObjectType("DB_VIEW");
        upstream.setObjectKey("DB_VIEW:analytics.vw_sales_daily");
        upstream.setObjectName("analytics.vw_sales_daily");
        upstream.setRelationshipType("SOURCE");

        MetadataSnapshotVO snapshot = new MetadataSnapshotVO();
        snapshot.setSnapshotId("snapshot-001");
        snapshot.setTenantId("tenant-a");
        snapshot.setDatasourceCode("hetu_main");
        snapshot.setObjectType("LOGICAL_VIEW");
        snapshot.setObjectKey("LOGICAL_VIEW:RPT_SALES_DAILY");
        snapshot.setObjectName("Sales Daily");
        snapshot.setFreshnessStatus("FRESH");
        snapshot.setSlaStatus("ON_TRACK");
        snapshot.setQueryabilityStatus("QUERYABLE");
        snapshot.setEvidenceStatus("CAPTURED");
        snapshot.setLatestRefreshTime(Instant.parse("2026-04-27T03:00:00Z"));
        snapshot.setSnapshotTime(Instant.parse("2026-04-27T03:05:00Z"));
        snapshot.setUpstreamCount(Integer.valueOf(1));
        snapshot.setDownstreamCount(Integer.valueOf(0));
        snapshot.setUpstreamRefs(Collections.singletonList(upstream));
        snapshot.setDownstreamRefs(Collections.<MetadataLineageVO>emptyList());
        snapshot.setContractStage("LONG_TERM_BASELINE");
        snapshot.setImplementationStage("METADATA_SNAPSHOT_BASELINE");
        return snapshot;
    }
}
