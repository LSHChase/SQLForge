package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.MetadataSnapshotVO;
import com.company.governance.application.service.MetadataSnapshotApplicationService;
import com.company.sqlforge.common.context.RequestContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/metadata/snapshots")
public class MetadataSnapshotController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSnapshotController.class);

    private final MetadataSnapshotApplicationService metadataSnapshotApplicationService;

    public MetadataSnapshotController(MetadataSnapshotApplicationService metadataSnapshotApplicationService) {
        this.metadataSnapshotApplicationService = metadataSnapshotApplicationService;
    }

    @GetMapping
    public List<MetadataSnapshotVO> list(@RequestParam(value = "tenantId", required = false) String tenantId,
                                         @RequestParam(value = "datasourceCode", required = false) String datasourceCode,
                                         @RequestParam(value = "objectType", required = false) String objectType,
                                         @RequestParam(value = "objectKey", required = false) String objectKey,
                                         @RequestParam(value = "freshnessStatus", required = false) String freshnessStatus,
                                         @RequestParam(value = "slaStatus", required = false) String slaStatus,
                                         @RequestParam(value = "queryabilityStatus", required = false) String queryabilityStatus,
                                         @RequestParam(value = "evidenceStatus", required = false) String evidenceStatus) {
        LOGGER.info("Handling metadata snapshot list, tenantId={}, datasourceCode={}, objectType={}, objectKey={}, traceId={}",
            tenantId,
            datasourceCode,
            objectType,
            objectKey,
            RequestContext.getTraceId());
        return metadataSnapshotApplicationService.list(
            tenantId,
            datasourceCode,
            objectType,
            objectKey,
            freshnessStatus,
            slaStatus,
            queryabilityStatus,
            evidenceStatus
        );
    }
}
