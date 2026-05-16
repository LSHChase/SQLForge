package com.company.governance.application.service;

import com.company.governance.application.controller.vo.BusinessLogicalViewVO;
import com.company.governance.application.controller.vo.LogicalObjectMappingVO;
import com.company.governance.domain.logicalview.entity.BusinessLogicalView;
import com.company.governance.domain.logicalview.entity.LogicalObjectMapping;
import com.company.governance.domain.logicalview.repository.BusinessLogicalViewRepository;
import com.company.governance.domain.logicalview.repository.LogicalObjectMappingRepository;
import com.company.governance.domain.metadata.entity.MetadataSnapshot;
import com.company.governance.domain.metadata.repository.MetadataSnapshotRepository;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LogicalViewCatalogApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogicalViewCatalogApplicationService.class);
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final BusinessLogicalViewRepository businessLogicalViewRepository;
    private final LogicalObjectMappingRepository logicalObjectMappingRepository;
    private final MetadataSnapshotRepository metadataSnapshotRepository;
    private final TenantAccessLogic tenantAccessLogic;

    public LogicalViewCatalogApplicationService(BusinessLogicalViewRepository businessLogicalViewRepository,
                                                LogicalObjectMappingRepository logicalObjectMappingRepository,
                                                MetadataSnapshotRepository metadataSnapshotRepository,
                                                TenantAccessLogic tenantAccessLogic) {
        this.businessLogicalViewRepository = businessLogicalViewRepository;
        this.logicalObjectMappingRepository = logicalObjectMappingRepository;
        this.metadataSnapshotRepository = metadataSnapshotRepository;
        this.tenantAccessLogic = tenantAccessLogic;
    }

    public List<BusinessLogicalViewVO> listLogicalViews(String tenantId, String datasourceCode) {
        String effectiveTenantId = resolveAndAuthorizeTenant(tenantId, datasourceCode);
        List<BusinessLogicalView> records = businessLogicalViewRepository.findByTenantAndDatasource(
            effectiveTenantId,
            trimToNull(datasourceCode)
        );
        LOGGER.info("正在列出业务逻辑视图，tenantId={}, datasourceCode={}, count={}, traceId={}",
            effectiveTenantId,
            datasourceCode,
            Integer.valueOf(records.size()),
            RequestContext.getTraceId());
        List<BusinessLogicalViewVO> items = new ArrayList<BusinessLogicalViewVO>();
        for (BusinessLogicalView record : records) {
            items.add(toView(record));
        }
        return items;
    }

    public BusinessLogicalViewVO findLogicalView(String tenantId, String viewCode) {
        String effectiveTenantId = resolveAndAuthorizeTenant(tenantId, null);
        if (!StringUtils.hasText(viewCode)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "视图编码不能为空"
            );
        }
        BusinessLogicalView record = businessLogicalViewRepository.findByTenantAndViewCode(effectiveTenantId, viewCode)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "业务逻辑视图不存在"
            ));
        LOGGER.info("已加载业务逻辑视图，tenantId={}, viewCode={}, traceId={}",
            effectiveTenantId,
            viewCode,
            RequestContext.getTraceId());
        return toView(record);
    }

    private String resolveAndAuthorizeTenant(String tenantId, String datasourceCode) {
        String currentTenantId = TenantContext.get();
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN);
        if (!StringUtils.hasText(currentTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "租户上下文缺失"
            );
        }
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : currentTenantId;
        if (!platformAdmin && !currentTenantId.equals(effectiveTenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
        if (!platformAdmin && StringUtils.hasText(datasourceCode)
            && !tenantAccessLogic.validateDataSourceAccess(currentTenantId, datasourceCode)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }
        return effectiveTenantId;
    }

    private BusinessLogicalViewVO toView(BusinessLogicalView record) {
        BusinessLogicalViewVO vo = new BusinessLogicalViewVO();
        vo.setViewId(record.getId());
        vo.setTenantId(record.getTenantId());
        vo.setViewCode(record.getViewCode());
        vo.setObjectKey("LOGICAL_VIEW:" + record.getViewCode());
        vo.setViewName(record.getViewName());
        vo.setDatasourceCode(record.getDatasourceCode());
        vo.setSubjectArea(record.getSubjectArea());
        vo.setOwnerUser(record.getOwnerUser());
        vo.setFreshnessStatus(record.getFreshnessStatus());
        vo.setSlaStatus(record.getSlaStatus());
        vo.setQueryable(record.getQueryable());
        vo.setQueryabilityStatus(record.getQueryable() == null ? null : (record.getQueryable().booleanValue() ? "QUERYABLE" : "BLOCKED"));
        vo.setLatestRefreshTime(record.getLatestRefreshTime() == null
            ? null
            : record.getLatestRefreshTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        applySnapshotSummary(vo, record.getTenantId(), vo.getObjectKey());
        vo.setDescription(record.getDescription());
        vo.setPhysicalTargets(toMappings(record.getTenantId(), record.getId()));
        return vo;
    }

    private void applySnapshotSummary(BusinessLogicalViewVO vo, String tenantId, String objectKey) {
        MetadataSnapshot snapshot = metadataSnapshotRepository.findByTenantAndObjectKey(tenantId, objectKey).orElse(null);
        if (snapshot == null) {
            return;
        }
        vo.setFreshnessStatus(firstNonBlank(snapshot.getFreshnessStatus(), vo.getFreshnessStatus(), "UNKNOWN"));
        vo.setSlaStatus(firstNonBlank(snapshot.getSlaStatus(), vo.getSlaStatus(), "UNKNOWN"));
        vo.setQueryabilityStatus(firstNonBlank(snapshot.getQueryabilityStatus(), vo.getQueryabilityStatus(), "UNKNOWN"));
        vo.setEvidenceStatus(firstNonBlank(snapshot.getEvidenceStatus(), "UNCOLLECTED"));
        vo.setQueryable(Boolean.valueOf("QUERYABLE".equalsIgnoreCase(vo.getQueryabilityStatus())));
        if (snapshot.getLatestRefreshTime() != null) {
            vo.setLatestRefreshTime(snapshot.getLatestRefreshTime().toString());
        }
        if (snapshot.getSnapshotTime() != null) {
            vo.setSnapshotTime(snapshot.getSnapshotTime().toString());
        }
        vo.setUpstreamCount(Integer.valueOf(snapshot.getUpstreamRefs() == null ? 0 : snapshot.getUpstreamRefs().size()));
        vo.setDownstreamCount(Integer.valueOf(snapshot.getDownstreamRefs() == null ? 0 : snapshot.getDownstreamRefs().size()));
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

    private List<LogicalObjectMappingVO> toMappings(String tenantId, String logicalViewId) {
        if (!StringUtils.hasText(logicalViewId)) {
            return Collections.emptyList();
        }
        List<LogicalObjectMapping> mappings = logicalObjectMappingRepository.findByTenantAndLogicalViewId(tenantId, logicalViewId);
        List<LogicalObjectMappingVO> items = new ArrayList<LogicalObjectMappingVO>();
        for (LogicalObjectMapping mapping : mappings) {
            LogicalObjectMappingVO item = new LogicalObjectMappingVO();
            item.setTargetObjectType(mapping.getTargetObjectType());
            item.setTargetObjectKey(mapping.getTargetObjectKey());
            item.setTargetObjectName(mapping.getTargetObjectName());
            item.setMappingRole(mapping.getMappingRole());
            items.add(item);
        }
        return items;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
