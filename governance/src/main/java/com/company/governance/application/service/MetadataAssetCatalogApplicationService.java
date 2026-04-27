package com.company.governance.application.service;

import com.company.governance.application.controller.vo.MetadataLineageVO;
import com.company.governance.application.controller.vo.MetadataSchemaAssetVO;
import com.company.governance.application.controller.vo.MetadataTableAssetVO;
import com.company.governance.domain.metadata.entity.MetadataLineageRef;
import com.company.governance.domain.metadata.entity.MetadataSnapshot;
import com.company.governance.domain.metadata.repository.MetadataSnapshotRepository;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetadataAssetCatalogApplicationService {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "DATA_ASSET_CATALOG_BASELINE";

    private final MetadataSnapshotRepository metadataSnapshotRepository;
    private final TenantAccessLogic tenantAccessLogic;

    public MetadataAssetCatalogApplicationService(MetadataSnapshotRepository metadataSnapshotRepository,
                                                  TenantAccessLogic tenantAccessLogic) {
        this.metadataSnapshotRepository = metadataSnapshotRepository;
        this.tenantAccessLogic = tenantAccessLogic;
    }

    public List<MetadataSchemaAssetVO> listSchemas(String tenantId, String datasourceCode) {
        String effectiveTenantId = requireTenant(tenantId);
        authorizeDatasource(effectiveTenantId, datasourceCode);
        List<MetadataSnapshot> snapshots = metadataSnapshotRepository.findSnapshots(
            effectiveTenantId,
            trimToNull(datasourceCode),
            null,
            null,
            null,
            null,
            null,
            null
        );
        Map<String, SchemaAccumulator> grouped = new LinkedHashMap<String, SchemaAccumulator>();
        for (MetadataSnapshot snapshot : snapshots) {
            if (!StringUtils.hasText(snapshot.getSchemaName())) {
                continue;
            }
            String key = snapshot.getCatalogName() + "::" + snapshot.getSchemaName();
            SchemaAccumulator accumulator = grouped.get(key);
            if (accumulator == null) {
                accumulator = new SchemaAccumulator(snapshot.getTenantId(), snapshot.getDatasourceCode(),
                    snapshot.getCatalogName(), snapshot.getSchemaName());
                grouped.put(key, accumulator);
            }
            accumulator.accept(snapshot);
        }
        List<MetadataSchemaAssetVO> responses = new ArrayList<MetadataSchemaAssetVO>(grouped.size());
        for (SchemaAccumulator accumulator : grouped.values()) {
            responses.add(accumulator.toVo());
        }
        return responses;
    }

    public MetadataSchemaAssetVO findSchema(String tenantId, String datasourceCode, String schemaName) {
        String normalizedSchemaName = requireText(schemaName, "schemaName");
        List<MetadataSchemaAssetVO> schemas = listSchemas(tenantId, datasourceCode);
        for (MetadataSchemaAssetVO schema : schemas) {
            if (normalizedSchemaName.equalsIgnoreCase(schema.getSchemaName())) {
                return schema;
            }
        }
        throw new BizException(ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Schema asset not found");
    }

    public List<MetadataTableAssetVO> listTables(String tenantId, String datasourceCode, String schemaName) {
        String effectiveTenantId = requireTenant(tenantId);
        authorizeDatasource(effectiveTenantId, datasourceCode);
        List<MetadataSnapshot> snapshots = metadataSnapshotRepository.findSnapshots(
            effectiveTenantId,
            trimToNull(datasourceCode),
            "TABLE",
            null,
            null,
            null,
            null,
            null
        );
        List<MetadataTableAssetVO> responses = new ArrayList<MetadataTableAssetVO>();
        for (MetadataSnapshot snapshot : snapshots) {
            if (StringUtils.hasText(schemaName) && !schemaName.equalsIgnoreCase(snapshot.getSchemaName())) {
                continue;
            }
            responses.add(toTableVo(snapshot));
        }
        return responses;
    }

    public MetadataTableAssetVO findTable(String tenantId, String datasourceCode, String schemaName, String tableName) {
        String normalizedTableName = requireText(tableName, "tableName");
        List<MetadataTableAssetVO> tables = listTables(tenantId, datasourceCode, schemaName);
        for (MetadataTableAssetVO table : tables) {
            if (normalizedTableName.equalsIgnoreCase(table.getTableName())) {
                return table;
            }
        }
        throw new BizException(ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Table asset not found");
    }

    private MetadataTableAssetVO toTableVo(MetadataSnapshot snapshot) {
        MetadataTableAssetVO response = new MetadataTableAssetVO();
        response.setTenantId(snapshot.getTenantId());
        response.setDatasourceCode(snapshot.getDatasourceCode());
        response.setCatalogName(snapshot.getCatalogName());
        response.setSchemaName(snapshot.getSchemaName());
        response.setTableName(resolveTableName(snapshot.getObjectName(), snapshot.getObjectKey()));
        response.setObjectKey(snapshot.getObjectKey());
        response.setColumnCount(snapshot.getColumnCount());
        response.setPartitionCount(snapshot.getPartitionCount());
        response.setRowCount(snapshot.getRowCount());
        response.setStorageBytes(snapshot.getStorageBytes());
        response.setFreshnessStatus(firstNonBlank(snapshot.getFreshnessStatus(), "UNKNOWN"));
        response.setSlaStatus(firstNonBlank(snapshot.getSlaStatus(), "UNKNOWN"));
        response.setQueryabilityStatus(firstNonBlank(snapshot.getQueryabilityStatus(), "UNKNOWN"));
        response.setEvidenceStatus(firstNonBlank(snapshot.getEvidenceStatus(), "UNCOLLECTED"));
        response.setUpstreamRefs(toLineage(snapshot.getUpstreamRefs()));
        response.setDownstreamRefs(toLineage(snapshot.getDownstreamRefs()));
        response.setUpstreamCount(Integer.valueOf(response.getUpstreamRefs().size()));
        response.setDownstreamCount(Integer.valueOf(response.getDownstreamRefs().size()));
        response.setLatestRefreshTime(snapshot.getLatestRefreshTime());
        response.setLatestSnapshotTime(snapshot.getSnapshotTime());
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

    private String resolveTableName(String objectName, String objectKey) {
        String source = StringUtils.hasText(objectName) ? objectName : objectKey;
        if (!StringUtils.hasText(source)) {
            return null;
        }
        int index = source.lastIndexOf('.');
        return index >= 0 ? source.substring(index + 1) : source;
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

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST,
                fieldName + " must not be empty");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static final class SchemaAccumulator {

        private final MetadataSchemaAssetVO response = new MetadataSchemaAssetVO();
        private final List<String> freshnessStatuses = new ArrayList<String>();
        private final List<String> slaStatuses = new ArrayList<String>();
        private final List<String> queryabilityStatuses = new ArrayList<String>();
        private final List<String> evidenceStatuses = new ArrayList<String>();

        private SchemaAccumulator(String tenantId, String datasourceCode, String catalogName, String schemaName) {
            response.setTenantId(tenantId);
            response.setDatasourceCode(datasourceCode);
            response.setCatalogName(catalogName);
            response.setSchemaName(schemaName);
            response.setSnapshotCount(Integer.valueOf(0));
            response.setTableCount(Integer.valueOf(0));
            response.setDbViewCount(Integer.valueOf(0));
            response.setLogicalViewCount(Integer.valueOf(0));
            response.setRowCount(Long.valueOf(0L));
            response.setStorageBytes(Long.valueOf(0L));
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
        }

        private void accept(MetadataSnapshot snapshot) {
            response.setSnapshotCount(Integer.valueOf(response.getSnapshotCount().intValue() + 1));
            if ("TABLE".equals(snapshot.getObjectType())) {
                response.setTableCount(Integer.valueOf(response.getTableCount().intValue() + 1));
                response.setRowCount(Long.valueOf(response.getRowCount().longValue()
                    + (snapshot.getRowCount() == null ? 0L : snapshot.getRowCount().longValue())));
                response.setStorageBytes(Long.valueOf(response.getStorageBytes().longValue()
                    + (snapshot.getStorageBytes() == null ? 0L : snapshot.getStorageBytes().longValue())));
            } else if ("DB_VIEW".equals(snapshot.getObjectType())) {
                response.setDbViewCount(Integer.valueOf(response.getDbViewCount().intValue() + 1));
            } else if ("LOGICAL_VIEW".equals(snapshot.getObjectType())) {
                response.setLogicalViewCount(Integer.valueOf(response.getLogicalViewCount().intValue() + 1));
            }
            addStatus(freshnessStatuses, snapshot.getFreshnessStatus());
            addStatus(slaStatuses, snapshot.getSlaStatus());
            addStatus(queryabilityStatuses, snapshot.getQueryabilityStatus());
            addStatus(evidenceStatuses, snapshot.getEvidenceStatus());
            if (snapshot.getLatestRefreshTime() != null
                && (response.getLatestRefreshTime() == null || snapshot.getLatestRefreshTime().isAfter(response.getLatestRefreshTime()))) {
                response.setLatestRefreshTime(snapshot.getLatestRefreshTime());
            }
            if (snapshot.getSnapshotTime() != null
                && (response.getLatestSnapshotTime() == null || snapshot.getSnapshotTime().isAfter(response.getLatestSnapshotTime()))) {
                response.setLatestSnapshotTime(snapshot.getSnapshotTime());
            }
        }

        private MetadataSchemaAssetVO toVo() {
            response.setFreshnessStatus(resolveFreshnessStatus(freshnessStatuses));
            response.setSlaStatus(resolveSlaStatus(slaStatuses));
            response.setQueryabilityStatus(resolveQueryabilityStatus(queryabilityStatuses));
            response.setEvidenceStatus(resolveEvidenceStatus(evidenceStatuses));
            return response;
        }

        private void addStatus(List<String> statuses, String status) {
            if (StringUtils.hasText(status)) {
                statuses.add(status.trim().toUpperCase(Locale.ROOT));
            }
        }

        private String resolveFreshnessStatus(List<String> statuses) {
            return statuses.contains("STALE") ? "STALE" : (statuses.contains("FRESH") ? "FRESH" : "UNKNOWN");
        }

        private String resolveSlaStatus(List<String> statuses) {
            return statuses.contains("AT_RISK") || statuses.contains("BREACHED")
                ? "AT_RISK"
                : (statuses.contains("ON_TRACK") || statuses.contains("ON_TIME") ? "ON_TRACK" : "UNKNOWN");
        }

        private String resolveQueryabilityStatus(List<String> statuses) {
            if (statuses.contains("BLOCKED")) {
                return "BLOCKED";
            }
            if (statuses.contains("LIMITED")) {
                return "LIMITED";
            }
            return statuses.contains("QUERYABLE") ? "QUERYABLE" : "UNKNOWN";
        }

        private String resolveEvidenceStatus(List<String> statuses) {
            return statuses.contains("CAPTURED") ? "CAPTURED" : "UNCOLLECTED";
        }
    }
}
