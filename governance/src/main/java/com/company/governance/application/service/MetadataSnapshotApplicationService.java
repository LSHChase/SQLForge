package com.company.governance.application.service;

import com.company.governance.application.controller.vo.MetadataLineageVO;
import com.company.governance.application.controller.vo.MetadataSnapshotVO;
import com.company.governance.domain.metadata.entity.MetadataLineageRef;
import com.company.governance.domain.metadata.entity.MetadataSnapshot;
import com.company.governance.domain.metadata.repository.MetadataSnapshotRepository;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetadataSnapshotApplicationService {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "METADATA_SNAPSHOT_BASELINE";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String EVIDENCE_UNCOLLECTED = "UNCOLLECTED";

    private final MetadataSnapshotRepository metadataSnapshotRepository;
    private final TenantAccessLogic tenantAccessLogic;

    public MetadataSnapshotApplicationService(MetadataSnapshotRepository metadataSnapshotRepository,
                                              TenantAccessLogic tenantAccessLogic) {
        this.metadataSnapshotRepository = metadataSnapshotRepository;
        this.tenantAccessLogic = tenantAccessLogic;
    }

    public List<MetadataSnapshotVO> list(String tenantId,
                                         String datasourceCode,
                                         String objectType,
                                         String objectKey,
                                         String freshnessStatus,
                                         String slaStatus,
                                         String queryabilityStatus,
                                         String evidenceStatus) {
        String effectiveTenantId = requireTenant(tenantId);
        authorizeDatasource(effectiveTenantId, datasourceCode);
        List<MetadataSnapshot> snapshots = metadataSnapshotRepository.findSnapshots(
            effectiveTenantId,
            trimToNull(datasourceCode),
            trimToNull(objectType),
            trimToNull(objectKey),
            trimToNull(freshnessStatus),
            trimToNull(slaStatus),
            trimToNull(queryabilityStatus),
            trimToNull(evidenceStatus)
        );
        List<MetadataSnapshotVO> responses = new ArrayList<MetadataSnapshotVO>(snapshots.size());
        for (MetadataSnapshot snapshot : snapshots) {
            responses.add(toVo(snapshot));
        }
        return responses;
    }

    private MetadataSnapshotVO toVo(MetadataSnapshot snapshot) {
        MetadataSnapshotVO response = new MetadataSnapshotVO();
        response.setSnapshotId(snapshot.getSnapshotId());
        response.setTenantId(snapshot.getTenantId());
        response.setDatasourceCode(snapshot.getDatasourceCode());
        response.setObjectType(snapshot.getObjectType());
        response.setObjectKey(snapshot.getObjectKey());
        response.setObjectName(snapshot.getObjectName());
        response.setCatalogName(snapshot.getCatalogName());
        response.setSchemaName(snapshot.getSchemaName());
        response.setFreshnessStatus(firstNonBlank(snapshot.getFreshnessStatus(), STATUS_UNKNOWN));
        response.setSlaStatus(firstNonBlank(snapshot.getSlaStatus(), STATUS_UNKNOWN));
        response.setQueryabilityStatus(firstNonBlank(snapshot.getQueryabilityStatus(), STATUS_UNKNOWN));
        response.setEvidenceStatus(firstNonBlank(snapshot.getEvidenceStatus(), EVIDENCE_UNCOLLECTED));
        response.setLatestRefreshTime(snapshot.getLatestRefreshTime());
        response.setExpectedSlaTime(snapshot.getExpectedSlaTime());
        response.setSnapshotTime(snapshot.getSnapshotTime());
        response.setEvidenceSource(firstNonBlank(snapshot.getEvidenceSource(), EVIDENCE_UNCOLLECTED));
        response.setColumnCount(snapshot.getColumnCount());
        response.setPartitionCount(snapshot.getPartitionCount());
        response.setRowCount(snapshot.getRowCount());
        response.setStorageBytes(snapshot.getStorageBytes());
        response.setRequestId(snapshot.getRequestId());
        response.setTraceId(snapshot.getTraceId());
        response.setExecutionId(snapshot.getExecutionId());
        response.setHistoryId(snapshot.getHistoryId());
        response.setParseTaskId(snapshot.getParseTaskId());
        response.setReportCode(snapshot.getReportCode());
        response.setSqlFingerprint(snapshot.getSqlFingerprint());
        response.setUpstreamRefs(toLineage(snapshot.getUpstreamRefs()));
        response.setDownstreamRefs(toLineage(snapshot.getDownstreamRefs()));
        response.setUpstreamCount(Integer.valueOf(response.getUpstreamRefs().size()));
        response.setDownstreamCount(Integer.valueOf(response.getDownstreamRefs().size()));
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private List<MetadataLineageVO> toLineage(List<MetadataLineageRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        List<MetadataLineageVO> items = new ArrayList<MetadataLineageVO>(refs.size());
        for (MetadataLineageRef ref : refs) {
            MetadataLineageVO item = new MetadataLineageVO();
            item.setObjectType(ref.getObjectType());
            item.setObjectKey(ref.getObjectKey());
            item.setObjectName(ref.getObjectName());
            item.setRelationshipType(ref.getRelationshipType());
            items.add(item);
        }
        return items;
    }

    private String requireTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context");
        }
        String normalized = trimToNull(requestTenantId);
        if (StringUtils.hasText(normalized) && !contextTenantId.equals(normalized)
            && !RequestContext.hasRole(PLATFORM_ADMIN)) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return StringUtils.hasText(normalized) ? normalized : contextTenantId;
    }

    private void authorizeDatasource(String tenantId, String datasourceCode) {
        String normalizedDatasourceCode = trimToNull(datasourceCode);
        if (!StringUtils.hasText(normalizedDatasourceCode) || RequestContext.hasRole(PLATFORM_ADMIN)) {
            return;
        }
        if (!tenantAccessLogic.validateDataSourceAccess(tenantId, normalizedDatasourceCode)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(trimToNull(value))) {
                return trimToNull(value);
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
